package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun requireDisplayName(): String =
        auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Adventurer"

    suspend fun upsertCurrentUserProfile(): Result<Unit> {
        var path = "users"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in.")
            val uid = user.uid
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val name = (user.displayName?.takeIf { it.isNotBlank() } ?: "Adventurer").take(80)
            val photoUrl = user.photoUrl?.toString()?.take(1000)

            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (photoUrl != null) updates["photoUrl"] = photoUrl
                docRef.update(updates).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", name)
                    if (photoUrl != null) put("photoUrl", photoUrl)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeStories(userId: String): Flow<List<Story>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Story::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStory(storyId: String, userId: String): Flow<Story?> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "stories/$storyId"
        emitAll(
            db.collection("stories").document(storyId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        Story::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createStory(
        title: String,
        description: String,
        systemName: String,
    ): Result<String> {
        var path = "stories"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Story title cannot be empty." }
            val cleanDesc = description.trim().take(1000)
            val cleanSystem = systemName.trim().ifBlank { "D&D 5e" }.take(60)
            val storyId = "st_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val joinCode = generateJoinCode()
            path = "stories/$storyId"

            val storyPayload = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "systemName" to cleanSystem,
                "joinCode" to joinCode,
                "gmUserId" to uid,
                "memberUids" to listOf(uid),
                "mapWidth" to 12L,
                "mapHeight" to 12L,
                "mapGridJson" to "{}",
                "mapTokensJson" to "[]",
                "guideProgressJson" to "[]",
                "nextSessionAtMillis" to 0L,
                "nextSessionNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val invitePayload = mapOf(
                "storyId" to storyId,
                "title" to cleanTitle,
                "systemName" to cleanSystem,
                "gmUserId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val batch = db.batch()
            batch.set(db.collection("stories").document(storyId), storyPayload)
            batch.set(db.collection("story_invites").document(joinCode), invitePayload)
            batch.commit().await()

            appendLogInternal(
                storyId = storyId,
                gmUserId = uid,
                memberUids = listOf(uid),
                actionType = "story",
                summary = "Created campaign \"$cleanTitle\" ($cleanSystem)",
            )
            Result.success(storyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinStoryByCode(rawCode: String): Result<String> {
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        var path = "story_invites/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..20) { "Join code must be 4-20 alphanumeric characters." }
            val inviteSnap = db.collection("story_invites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("No campaign found for join code \"$code\"."))
            }
            val storyId = inviteSnap.getString("storyId")
                ?: return Result.failure(IllegalStateException("Invalid invite record."))
            val gmUserId = inviteSnap.getString("gmUserId") ?: ""
            path = "stories/$storyId"

            val storyRef = db.collection("stories").document(storyId)
            val updatedMembers = db.runTransaction { tx ->
                val snap = tx.get(storyRef)
                val currentMembers = (snap.get("memberUids") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                if (uid in currentMembers) {
                    currentMembers
                } else {
                    val next = (currentMembers + uid).distinct()
                    tx.update(
                        storyRef,
                        mapOf(
                            "memberUids" to next,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    next
                }
            }.await()

            appendLogInternal(
                storyId = storyId,
                gmUserId = gmUserId,
                memberUids = updatedMembers,
                actionType = "party",
                summary = "${requireDisplayName()} joined the campaign",
            )
            Result.success(storyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(story: Story): Result<Unit> {
        val path = "stories/${story.id}"
        return try {
            val uid = requireUserId()
            check(uid == story.gmUserId) { "Only the Game Master can delete this campaign." }
            val batch = db.batch()
            batch.delete(db.collection("stories").document(story.id))
            if (story.joinCode.isNotBlank()) {
                batch.delete(db.collection("story_invites").document(story.joinCode))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncMembersToSubcollections(story: Story) {
        val uid = auth.currentUser?.uid ?: return
        if (uid != story.gmUserId || story.id.isBlank()) return
        val expectedMembers = story.memberUids
        val subcollections = listOf("characters", "world_items", "rolls", "messages", "logs")
        try {
            for (sub in subcollections) {
                val docs = db.collection("stories").document(story.id)
                    .collection(sub)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                val batch = db.batch()
                var count = 0
                for (doc in docs.documents) {
                    val current = (doc.get("memberUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    if (current.toSet() != expectedMembers.toSet()) {
                        if (sub == "characters" || sub == "world_items") {
                            batch.update(
                                doc.reference,
                                mapOf(
                                    "memberUids" to expectedMembers,
                                    "updatedAt" to FieldValue.serverTimestamp(),
                                )
                            )
                        } else {
                            batch.update(doc.reference, mapOf("memberUids" to expectedMembers))
                        }
                        count++
                    }
                }
                if (count > 0) batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "stories/${story.id}")
        }
    }

    suspend fun updateStoryMap(
        storyId: String,
        width: Int,
        height: Int,
        gridJson: String,
        tokensJson: String,
        backgroundUrl: String?,
    ): Result<Unit> {
        val path = "stories/$storyId"
        return try {
            requireUserId()
            val w = width.coerceIn(4, 30).toLong()
            val h = height.coerceIn(4, 30).toLong()
            val updates = mutableMapOf<String, Any?>(
                "mapWidth" to w,
                "mapHeight" to h,
                "mapGridJson" to gridJson.take(450_000),
                "mapTokensJson" to tokensJson.take(100_000),
                "mapBackgroundUrl" to backgroundUrl?.take(400_000),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (backgroundUrl == null) {
                updates["mapBackgroundUrl"] = null
            }
            db.collection("stories").document(storyId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStoryTokens(storyId: String, tokensJson: String): Result<Unit> {
        val path = "stories/$storyId"
        return try {
            requireUserId()
            db.collection("stories").document(storyId).update(
                mapOf(
                    "mapTokensJson" to tokensJson.take(100_000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGuideProgress(storyId: String, progressJson: String): Result<Unit> {
        val path = "stories/$storyId"
        return try {
            requireUserId()
            db.collection("stories").document(storyId).update(
                mapOf(
                    "guideProgressJson" to progressJson.take(50_000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSessionSchedule(
        story: Story,
        sessionAtMillis: Long,
        note: String,
        formattedSummary: String,
    ): Result<Unit> {
        val path = "stories/${story.id}"
        return try {
            val uid = requireUserId()
            check(uid == story.gmUserId) { "Only the Game Master can schedule sessions." }
            val cleanNote = note.trim().take(120)
            db.collection("stories").document(story.id).update(
                mapOf(
                    "nextSessionAtMillis" to sessionAtMillis.coerceAtLeast(0L),
                    "nextSessionNote" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val summaryText = if (sessionAtMillis > 0L) {
                if (cleanNote.isNotBlank()) "Scheduled next session: $formattedSummary ($cleanNote)"
                else "Scheduled next session: $formattedSummary"
            } else {
                "Cleared scheduled game session"
            }

            appendLogInternal(
                storyId = story.id,
                gmUserId = story.gmUserId,
                memberUids = story.memberUids,
                actionType = "schedule",
                summary = summaryText,
            )

            if (sessionAtMillis > 0L) {
                val chatBody = if (cleanNote.isNotBlank()) {
                    "📅 Next session scheduled for $formattedSummary — $cleanNote"
                } else {
                    "📅 Next session scheduled for $formattedSummary"
                }
                sendMessage(story, "GM", chatBody, messageType = "system")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeCharacters(storyId: String, userId: String): Flow<List<Character>> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories/$storyId/characters"
        emitAll(
            db.collection("stories").document(storyId).collection("characters")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Character::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCharacter(story: Story, character: Character): Result<String> {
        val isNew = character.id.isBlank()
        val charId = character.id.ifBlank {
            "ch_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val path = "stories/${story.id}/characters/$charId"
        return try {
            val uid = requireUserId()
            val cleanName = character.name.trim().ifBlank { "Unnamed Hero" }.take(100)
            val docRef = db.collection("stories").document(story.id)
                .collection("characters").document(charId)

            if (isNew) {
                val payload = buildMap<String, Any> {
                    put("storyId", story.id)
                    put("gmUserId", story.gmUserId)
                    put("ownerId", uid)
                    put("memberUids", story.memberUids.ifEmpty { listOf(uid) })
                    put("kind", character.kind)
                    put("name", cleanName)
                    put("avatarEmoji", character.avatarEmoji.take(16))
                    character.avatarUrl?.take(250_000)?.let { put("avatarUrl", it) }
                    put("roleTitle", character.roleTitle.take(100))
                    put("locationName", character.locationName.trim().take(120))
                    put("level", character.level.coerceIn(0L, 999L))
                    put("hpCurrent", character.hpCurrent.coerceIn(-999L, 99_999L))
                    put("hpMax", character.hpMax.coerceIn(0L, 99_999L))
                    put("ac", character.ac.coerceIn(0L, 999L))
                    put("notes", character.notes.take(20_000))
                    put("inventoryJson", character.inventoryJson.take(50_000))
                    put("missionsJson", character.missionsJson.take(50_000))
                    put("powersJson", character.powersJson.take(50_000))
                    put("equipmentJson", character.equipmentJson.take(250_000))
                    put("statsJson", character.statsJson.take(50_000))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
                appendLogInternal(
                    storyId = story.id,
                    gmUserId = story.gmUserId,
                    memberUids = story.memberUids,
                    actionType = "character",
                    summary = "Created ${character.kind}: $cleanName",
                )
            } else {
                val updates = mutableMapOf<String, Any?>(
                    "kind" to character.kind,
                    "name" to cleanName,
                    "avatarEmoji" to character.avatarEmoji.take(16),
                    "avatarUrl" to character.avatarUrl?.take(250_000),
                    "roleTitle" to character.roleTitle.take(100),
                    "locationName" to character.locationName.trim().take(120),
                    "level" to character.level.coerceIn(0L, 999L),
                    "hpCurrent" to character.hpCurrent.coerceIn(-999L, 99_999L),
                    "hpMax" to character.hpMax.coerceIn(0L, 99_999L),
                    "ac" to character.ac.coerceIn(0L, 999L),
                    "notes" to character.notes.take(20_000),
                    "inventoryJson" to character.inventoryJson.take(50_000),
                    "missionsJson" to character.missionsJson.take(50_000),
                    "powersJson" to character.powersJson.take(50_000),
                    "equipmentJson" to character.equipmentJson.take(250_000),
                    "statsJson" to character.statsJson.take(50_000),
                    "memberUids" to story.memberUids.ifEmpty { character.memberUids },
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (character.avatarUrl == null) {
                    updates["avatarUrl"] = null
                }
                docRef.update(updates).await()
            }
            Result.success(charId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacters(story: Story, ids: List<String>): Result<Unit> {
        val path = "stories/${story.id}/characters"
        return try {
            requireUserId()
            val cleanIds = ids.filter { it.isNotBlank() }.distinct()
            if (cleanIds.isEmpty()) return Result.success(Unit)
            cleanIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(
                        db.collection("stories").document(story.id)
                            .collection("characters").document(id)
                    )
                }
                batch.commit().await()
            }
            appendLogInternal(
                storyId = story.id,
                gmUserId = story.gmUserId,
                memberUids = story.memberUids,
                actionType = "character",
                summary = "Removed ${cleanIds.size} character sheet(s)",
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeWorldItems(storyId: String, userId: String): Flow<List<WorldItem>> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories/$storyId/world_items"
        emitAll(
            db.collection("stories").document(storyId).collection("world_items")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorldItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<WorldItem> { it.sortOrder }.thenBy { it.title.lowercase() })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveWorldItem(story: Story, item: WorldItem): Result<String> {
        val isNew = item.id.isBlank()
        val itemId = item.id.ifBlank {
            "wi_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val path = "stories/${story.id}/world_items/$itemId"
        return try {
            val uid = requireUserId()
            val cleanTitle = item.title.trim().ifBlank { "Untitled Entry" }.take(120)
            val docRef = db.collection("stories").document(story.id)
                .collection("world_items").document(itemId)

            if (isNew) {
                val payload = buildMap<String, Any> {
                    put("storyId", story.id)
                    put("gmUserId", story.gmUserId)
                    put("ownerId", uid)
                    put("memberUids", story.memberUids.ifEmpty { listOf(uid) })
                    put("category", item.category)
                    put("title", cleanTitle)
                    put("subtitle", item.subtitle.take(120))
                    put("description", item.description.take(10_000))
                    put("iconName", item.iconName.take(60))
                    put("iconEmoji", item.iconEmoji.take(16))
                    item.imageUrl?.take(250_000)?.let { put("imageUrl", it) }
                    put("sortOrder", item.sortOrder.coerceIn(0L, 100_000L))
                    put("tagsJson", item.tagsJson.take(5000))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
                appendLogInternal(
                    storyId = story.id,
                    gmUserId = story.gmUserId,
                    memberUids = story.memberUids,
                    actionType = "world",
                    summary = "Added ${item.category}: $cleanTitle",
                )
            } else {
                val updates = mutableMapOf<String, Any?>(
                    "category" to item.category,
                    "title" to cleanTitle,
                    "subtitle" to item.subtitle.take(120),
                    "description" to item.description.take(10_000),
                    "iconName" to item.iconName.take(60),
                    "iconEmoji" to item.iconEmoji.take(16),
                    "imageUrl" to item.imageUrl?.take(250_000),
                    "sortOrder" to item.sortOrder.coerceIn(0L, 100_000L),
                    "tagsJson" to item.tagsJson.take(5000),
                    "memberUids" to story.memberUids.ifEmpty { item.memberUids },
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updates).await()
            }
            Result.success(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reorderWorldItems(story: Story, items: List<WorldItem>): Result<Unit> {
        val path = "stories/${story.id}/world_items"
        return try {
            requireUserId()
            val batch = db.batch()
            items.forEachIndexed { index, item ->
                if (item.id.isNotBlank()) {
                    val ref = db.collection("stories").document(story.id)
                        .collection("world_items").document(item.id)
                    batch.update(
                        ref,
                        mapOf(
                            "sortOrder" to index.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorldItems(story: Story, ids: List<String>): Result<Unit> {
        val path = "stories/${story.id}/world_items"
        return try {
            requireUserId()
            val cleanIds = ids.filter { it.isNotBlank() }.distinct()
            if (cleanIds.isEmpty()) return Result.success(Unit)
            cleanIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(
                        db.collection("stories").document(story.id)
                            .collection("world_items").document(id)
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeRolls(storyId: String, userId: String): Flow<List<DiceRoll>> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories/$storyId/rolls"
        emitAll(
            db.collection("stories").document(storyId).collection("rolls")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DiceRoll::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }.take(40)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitRoll(
        story: Story,
        rollerName: String,
        senderRole: String,
        expression: String,
        total: Int,
        breakdown: String,
    ): Result<Unit> {
        val rollId = "rl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgId = "mg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "stories/${story.id}/rolls/$rollId"
        return try {
            val uid = requireUserId()
            val members = story.memberUids.ifEmpty { listOf(uid) }
            val cleanName = rollerName.ifBlank { requireDisplayName() }.take(80)
            val rollPayload = mapOf(
                "storyId" to story.id,
                "gmUserId" to story.gmUserId,
                "memberUids" to members,
                "userId" to uid,
                "rollerName" to cleanName,
                "expression" to expression.take(200),
                "total" to total.toLong(),
                "breakdown" to breakdown.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
            )
            val msgPayload = mapOf(
                "storyId" to story.id,
                "gmUserId" to story.gmUserId,
                "memberUids" to members,
                "senderId" to uid,
                "senderName" to cleanName,
                "senderRole" to senderRole.take(30),
                "body" to "🎲 Rolled $expression = $total ($breakdown)",
                "messageType" to "roll",
                "createdAt" to FieldValue.serverTimestamp(),
            )
            val batch = db.batch()
            batch.set(
                db.collection("stories").document(story.id).collection("rolls").document(rollId),
                rollPayload
            )
            batch.set(
                db.collection("stories").document(story.id).collection("messages").document(msgId),
                msgPayload
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(storyId: String, userId: String): Flow<List<ChatMessage>> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories/$storyId/messages"
        emitAll(
            db.collection("stories").document(storyId).collection("messages")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        story: Story,
        senderRole: String,
        body: String,
        messageType: String = "chat",
        senderNameOverride: String? = null,
        replyToMessageId: String = "",
        replyToSenderName: String = "",
        replyToSnippet: String = "",
        entityRef: ChatEntityRef? = null,
        pinned: Boolean = false,
        targetUserId: String = "",
        targetCharacterId: String = "",
        targetCharacterName: String = "",
    ): Result<String> {
        val msgId = "mg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "stories/${story.id}/messages/$msgId"
        return try {
            val uid = requireUserId()
            val fallbackBody = when {
                body.isNotBlank() -> body
                pinned && targetCharacterName.isNotBlank() -> "📌 Pinned character: $targetCharacterName"
                entityRef != null -> "${entityRef.emoji.ifBlank { "📚" }} ${entityRef.title}"
                else -> ""
            }
            val cleanBody = fallbackBody.trim().take(2000)
            require(cleanBody.isNotEmpty()) { "Message cannot be empty." }
            val resolvedSender = senderNameOverride?.trim()?.takeIf { it.isNotBlank() } ?: requireDisplayName()
            val payload = buildMap<String, Any> {
                put("storyId", story.id)
                put("gmUserId", story.gmUserId)
                put("memberUids", story.memberUids.ifEmpty { listOf(uid) })
                put("senderId", uid)
                put("senderName", resolvedSender.take(80))
                put("senderRole", senderRole.take(30))
                put("body", cleanBody)
                put("messageType", messageType)
                if (replyToMessageId.isNotBlank()) put("replyToMessageId", replyToMessageId.take(128))
                if (replyToSenderName.isNotBlank()) put("replyToSenderName", replyToSenderName.take(80))
                if (replyToSnippet.isNotBlank()) put("replyToSnippet", replyToSnippet.take(240))
                if (entityRef != null) {
                    put("refType", entityRef.refType.take(24))
                    if (entityRef.refId.isNotBlank()) put("refId", entityRef.refId.take(128))
                    put("refTitle", entityRef.title.take(140))
                    if (entityRef.subtitle.isNotBlank()) put("refSubtitle", entityRef.subtitle.take(160))
                    if (entityRef.emoji.isNotBlank()) put("refEmoji", entityRef.emoji.take(16))
                    if (entityRef.description.isNotBlank()) put("refDescription", entityRef.description.take(2000))
                    if (entityRef.tags.isNotEmpty()) {
                        put("refTagsJson", JsonCodecs.encodeStringList(entityRef.tags).take(1000))
                    }
                }
                if (pinned) {
                    put("pinned", true)
                    put("pinnedBy", uid)
                }
                if (targetUserId.isNotBlank()) put("targetUserId", targetUserId.take(128))
                if (targetCharacterId.isNotBlank()) put("targetCharacterId", targetCharacterId.take(128))
                if (targetCharacterName.isNotBlank()) put("targetCharacterName", targetCharacterName.take(100))
                put("createdAt", FieldValue.serverTimestamp())
            }
            db.collection("stories").document(story.id)
                .collection("messages").document(msgId)
                .set(payload).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePinMessage(
        story: Story,
        message: ChatMessage,
        pinned: Boolean,
        targetUserId: String = message.targetUserId,
        targetCharacterId: String = message.targetCharacterId,
        targetCharacterName: String = message.targetCharacterName,
    ): Result<Unit> {
        val path = "stories/${story.id}/messages/${message.id}"
        return try {
            val uid = requireUserId()
            db.collection("stories").document(story.id)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "pinned" to pinned,
                        "pinnedBy" to if (pinned) uid else "",
                        "targetUserId" to targetUserId.take(128),
                        "targetCharacterId" to targetCharacterId.take(128),
                        "targetCharacterName" to targetCharacterName.take(100),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageReaction(
        story: Story,
        message: ChatMessage,
        emoji: String,
    ): Result<Unit> {
        val cleanEmoji = emoji.trim().take(16)
        if (cleanEmoji.isBlank()) return Result.success(Unit)
        val path = "stories/${story.id}/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val current = JsonCodecs.decodeReactions(message.reactionsJson).toMutableMap()
            val existingUsers = current[cleanEmoji].orEmpty().toMutableList()
            if (uid in existingUsers) {
                existingUsers.removeAll { it == uid }
            } else {
                existingUsers.add(uid)
            }
            if (existingUsers.isEmpty()) {
                current.remove(cleanEmoji)
            } else {
                current[cleanEmoji] = existingUsers
            }
            val nextJson = JsonCodecs.encodeReactions(current).take(20_000)
            db.collection("stories").document(story.id)
                .collection("messages").document(message.id)
                .update("reactionsJson", nextJson)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(story: Story, messageId: String): Result<Unit> {
        val path = "stories/${story.id}/messages/$messageId"
        return try {
            requireUserId()
            db.collection("stories").document(story.id)
                .collection("messages").document(messageId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessages(story: Story, messageIds: List<String>): Result<Unit> {
        val path = "stories/${story.id}/messages"
        return try {
            requireUserId()
            val cleanIds = messageIds.filter { it.isNotBlank() }.distinct()
            if (cleanIds.isEmpty()) return Result.success(Unit)
            cleanIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(
                        db.collection("stories").document(story.id)
                            .collection("messages").document(id)
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeLogs(storyId: String, userId: String): Flow<List<ActivityLog>> = flow {
        if (storyId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "stories/$storyId/logs"
        emitAll(
            db.collection("stories").document(storyId).collection("logs")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun appendLog(
        story: Story,
        actionType: String,
        summary: String,
    ): Result<Unit> {
        return try {
            appendLogInternal(
                storyId = story.id,
                gmUserId = story.gmUserId,
                memberUids = story.memberUids,
                actionType = actionType,
                summary = summary,
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "stories/${story.id}/logs")
            Result.failure(e)
        }
    }

    private suspend fun appendLogInternal(
        storyId: String,
        gmUserId: String,
        memberUids: List<String>,
        actionType: String,
        summary: String,
    ) {
        val uid = requireUserId()
        val logId = "lg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val payload = mapOf(
            "storyId" to storyId,
            "gmUserId" to gmUserId.ifBlank { uid },
            "memberUids" to memberUids.ifEmpty { listOf(uid) },
            "actorId" to uid,
            "actorName" to requireDisplayName().take(80),
            "actionType" to actionType.take(40),
            "summary" to summary.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        db.collection("stories").document(storyId)
            .collection("logs").document(logId)
            .set(payload).await()
    }

    private fun generateJoinCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) {
                append(alphabet.random())
            }
        }
    }
}
