package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.AiChatMode
import com.example.ai.AiChatUiState
import com.example.ai.AiImageGeneratorService
import com.example.ai.AiParserService
import com.example.ai.AiProvider
import com.example.ai.SvgCodeToImageUtil
import com.example.data.ActivityLog
import com.example.data.Character
import com.example.data.ChatMessage
import com.example.data.DiceRoll
import com.example.data.EquipmentSlotItem
import com.example.data.GridCell
import com.example.data.JsonCodecs
import com.example.data.MapPaintOutcome
import com.example.data.MapToken
import com.example.data.PreferencesRepository
import com.example.data.QuickMapAsset
import com.example.data.QuickMapBlock
import com.example.data.Story
import com.example.data.StoryRepository
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.firebase.unwrapFirestoreErrorMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val summary: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

sealed interface AiMapUiState {
    data object Idle : AiMapUiState
    data object Loading : AiMapUiState
    data class Error(val message: String) : AiMapUiState
}

class StoryViewModel(
    private val repository: StoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val aiParser: AiParserService,
    private val storyId: String,
    val currentUserId: String,
) : ViewModel() {

    val storyState: StateFlow<UiState<Story?>> = repository.observeStory(storyId, currentUserId)
        .map<Story?, UiState<Story?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing story", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load campaign.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val charactersState: StateFlow<UiState<List<Character>>> = repository.observeCharacters(storyId, currentUserId)
        .map<List<Character>, UiState<List<Character>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing characters", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load characters.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val worldItemsState: StateFlow<UiState<List<WorldItem>>> = repository.observeWorldItems(storyId, currentUserId)
        .map<List<WorldItem>, UiState<List<WorldItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing world items", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load world items.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val rollsState: StateFlow<UiState<List<DiceRoll>>> = repository.observeRolls(storyId, currentUserId)
        .map<List<DiceRoll>, UiState<List<DiceRoll>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing rolls", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load dice rolls.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = repository.observeMessages(storyId, currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing chat messages", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load chat.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val logsState: StateFlow<UiState<List<ActivityLog>>> = repository.observeLogs(storyId, currentUserId)
        .map<List<ActivityLog>, UiState<List<ActivityLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing logs", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load logs.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val quickMapAssets: StateFlow<List<QuickMapAsset>> = preferencesRepository.quickMapAssetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quickMapBlocks: StateFlow<List<QuickMapBlock>> = preferencesRepository.quickMapBlocksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userSettings: StateFlow<UserSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettings())

    private val _viewAsPlayer = MutableStateFlow(false)
    val viewAsPlayer: StateFlow<Boolean> = _viewAsPlayer.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _aiMapState = MutableStateFlow<AiMapUiState>(AiMapUiState.Idle)
    val aiMapState: StateFlow<AiMapUiState> = _aiMapState.asStateFlow()

    private val _aiChatState = MutableStateFlow(AiChatUiState())
    val aiChatState: StateFlow<AiChatUiState> = _aiChatState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var mapSyncJob: Job? = null
    private var lastSyncedMemberSignature: String = ""

    fun toggleViewAsPlayer() {
        _viewAsPlayer.value = !_viewAsPlayer.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun resetAiState() {
        _aiState.value = AiUiState.Idle
    }

    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch { preferencesRepository.setAiProvider(provider) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { preferencesRepository.setAiModel(model) }
    }

    fun setAiEndpointUrl(url: String) {
        viewModelScope.launch { preferencesRepository.setAiEndpointUrl(url) }
    }

    fun setAiApiKey(apiKey: String) {
        viewModelScope.launch { preferencesRepository.setAiApiKey(apiKey) }
    }

    fun setAiImageEndpointUrl(url: String) {
        viewModelScope.launch { preferencesRepository.setAiImageEndpointUrl(url) }
    }

    fun setAiImageModel(model: String) {
        viewModelScope.launch { preferencesRepository.setAiImageModel(model) }
    }

    fun setAiImageApiKey(apiKey: String) {
        viewModelScope.launch { preferencesRepository.setAiImageApiKey(apiKey) }
    }

    fun generateEntityImage(
        prompt: String,
        maxDimension: Int = 320,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val settings = userSettings.value
        viewModelScope.launch {
            AiImageGeneratorService.generateImageDataUrl(
                prompt = prompt,
                model = settings.aiImageModel,
                endpointUrl = settings.aiImageEndpointUrl,
                imageApiKeyOverride = settings.aiImageApiKey,
                chatApiKeyOverride = settings.aiApiKey,
                maxDimension = maxDimension,
                quality = if (maxDimension <= 200) 72 else 78,
            ).onSuccess { dataUrl ->
                onSuccess(dataUrl)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    fun clearAiChat() {
        _aiChatState.value = AiChatUiState()
    }

    fun clearAiChatError() {
        _aiChatState.value = _aiChatState.value.copy(errorMessage = null)
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun sendAiChatMessage(
        story: Story?,
        prompt: String,
        contextHint: String? = null,
        mode: AiChatMode = AiChatMode.TEXT,
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _aiChatState.value.isLoading) return
        val settings = userSettings.value
        val activeModelLabel = if (mode == AiChatMode.IMAGE && settings.aiProvider == AiProvider.GEMINI) {
            settings.aiImageModel
        } else {
            settings.aiModel
        }
        val userMsg = AiChatMessage(
            role = "user",
            content = if (mode == AiChatMode.IMAGE) "🎨 [Image] $clean" else clean,
        )
        val updatedHistory = _aiChatState.value.messages + userMsg
        _aiChatState.value = _aiChatState.value.copy(
            messages = updatedHistory,
            isLoading = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            if (mode == AiChatMode.IMAGE) {
                val targetEndpoint = if (settings.aiProvider == AiProvider.GEMINI) {
                    settings.aiImageEndpointUrl
                } else {
                    settings.aiEndpointUrl
                }
                val targetModel = if (settings.aiProvider == AiProvider.GEMINI) {
                    settings.aiImageModel
                } else {
                    settings.aiModel
                }
                AiImageGeneratorService.generateImageDataUrl(
                    prompt = clean,
                    model = targetModel,
                    endpointUrl = targetEndpoint,
                    imageApiKeyOverride = settings.aiImageApiKey,
                    chatApiKeyOverride = settings.aiApiKey,
                    maxDimension = 1024,
                    quality = 90,
                ).onSuccess { dataUrl ->
                    val assistantMsg = AiChatMessage(
                        role = "assistant",
                        content = "Generated image for \"$clean\".",
                        imageDataUrl = dataUrl,
                        modelUsed = targetModel,
                    )
                    _aiChatState.value = _aiChatState.value.copy(
                        messages = _aiChatState.value.messages + assistantMsg,
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onFailure { err ->
                    _aiChatState.value = _aiChatState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage
                            ?: "Model \"$activeModelLabel\" cannot generate images.",
                    )
                }
            } else {
                aiParser.chatWithAi(
                    history = updatedHistory,
                    story = story,
                    provider = settings.aiProvider,
                    modelName = settings.aiModel,
                    endpointUrl = settings.aiEndpointUrl,
                    apiKeyOverride = settings.aiApiKey,
                    contextHint = contextHint,
                ).onSuccess { reply ->
                    val renderedCodeImg = SvgCodeToImageUtil.extractAndRenderCodeImage(reply)
                    val assistantMsg = AiChatMessage(
                        role = "assistant",
                        content = reply,
                        imageDataUrl = renderedCodeImg,
                        modelUsed = settings.aiModel,
                    )
                    _aiChatState.value = _aiChatState.value.copy(
                        messages = _aiChatState.value.messages + assistantMsg,
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onFailure { err ->
                    _aiChatState.value = _aiChatState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "AI request failed.",
                    )
                }
            }
        }
    }

    fun onStoryObserved(story: Story) {
        val signature = story.memberUids.sorted().joinToString(",")
        if (story.gmUserId == currentUserId && signature != lastSyncedMemberSignature) {
            lastSyncedMemberSignature = signature
            viewModelScope.launch {
                repository.syncMembersToSubcollections(story)
            }
        }
    }

    fun toggleGuideStep(story: Story, stepId: String) {
        val current = JsonCodecs.decodeStringSet(story.guideProgressJson).toMutableSet()
        if (stepId in current) current.remove(stepId) else current.add(stepId)
        viewModelScope.launch {
            repository.updateGuideProgress(story.id, JsonCodecs.encodeStringSet(current))
        }
    }

    fun resetGuideProgress(story: Story) {
        viewModelScope.launch {
            repository.updateGuideProgress(story.id, "[]")
        }
    }

    fun updateSessionSchedule(
        story: Story,
        sessionAtMillis: Long,
        note: String,
        onSaved: (Long, String) -> Unit = { _, _ -> },
    ) {
        viewModelScope.launch {
            val formatted = com.example.notifications.SessionNotificationScheduler.formatSessionDateTime(sessionAtMillis)
            repository.updateSessionSchedule(
                story = story,
                sessionAtMillis = sessionAtMillis,
                note = note,
                formattedSummary = formatted,
            ).onSuccess {
                _toastMessage.value = if (sessionAtMillis > 0L) {
                    "Session scheduled: $formatted"
                } else {
                    "Session schedule cleared"
                }
                onSaved(sessionAtMillis, note)
            }.onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to update session schedule.")
            }
        }
    }

    fun saveCharacter(story: Story, character: Character, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveCharacter(story, character)
            result.onSuccess {
                _toastMessage.value = "Saved ${character.name}"
                onSaved()
            }.onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to save character.")
            }
        }
    }

    fun deleteCharacters(story: Story, ids: List<String>) {
        viewModelScope.launch {
            repository.deleteCharacters(story, ids).onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to delete characters.")
            }
        }
    }

    fun saveWorldItem(story: Story, item: WorldItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveWorldItem(story, item).onSuccess {
                onSaved()
            }.onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to save entry.")
            }
        }
    }

    fun reorderWorldItems(story: Story, reordered: List<WorldItem>) {
        viewModelScope.launch {
            repository.reorderWorldItems(story, reordered).onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to reorder items.")
            }
        }
    }

    fun deleteWorldItems(story: Story, ids: List<String>) {
        viewModelScope.launch {
            repository.deleteWorldItems(story, ids).onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to delete items.")
            }
        }
    }

    fun syncMapDebounced(
        story: Story,
        width: Int,
        height: Int,
        grid: Map<String, GridCell>,
        tokens: List<MapToken>,
        backgroundUrl: String?,
        immediate: Boolean = false,
    ) {
        mapSyncJob?.cancel()
        mapSyncJob = viewModelScope.launch {
            if (!immediate) delay(450L)
            val gridJson = JsonCodecs.encodeGrid(grid)
            val tokensJson = JsonCodecs.encodeTokens(tokens)
            val isGm = story.gmUserId == currentUserId && !_viewAsPlayer.value
            val result = if (isGm) {
                repository.updateStoryMap(story.id, width, height, gridJson, tokensJson, backgroundUrl)
            } else {
                repository.updateStoryTokens(story.id, tokensJson)
            }
            result.onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to sync map.")
            }
        }
    }

    fun addQuickMapAsset(asset: QuickMapAsset) {
        viewModelScope.launch {
            val next = (listOf(asset) + quickMapAssets.value).distinctBy { it.id }.take(16)
            preferencesRepository.saveQuickMapAssets(next)
        }
    }

    fun removeQuickMapAsset(id: String) {
        viewModelScope.launch {
            preferencesRepository.saveQuickMapAssets(quickMapAssets.value.filterNot { it.id == id })
        }
    }

    fun addQuickMapBlock(block: QuickMapBlock) {
        viewModelScope.launch {
            val current = quickMapBlocks.value.filterNot {
                it.colorHex == block.colorHex &&
                    it.emoji == block.emoji &&
                    it.imageBase64 == block.imageBase64
            }
            val next = (listOf(block) + current).take(6)
            preferencesRepository.saveQuickMapBlocks(next)
        }
    }

    fun removeQuickMapBlock(id: String) {
        viewModelScope.launch {
            preferencesRepository.saveQuickMapBlocks(quickMapBlocks.value.filterNot { it.id == id })
        }
    }

    fun sendChatMessage(story: Story, body: String) {
        val role = if (story.gmUserId == currentUserId && !_viewAsPlayer.value) "GM" else "Player"
        viewModelScope.launch {
            repository.sendMessage(story, role, body).onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to send message.")
            }
        }
    }

    fun rollDice(
        story: Story,
        diceTerms: List<Pair<Int, Int>>, // (count, sides)
        modifierSteps: List<Pair<Char, Int>>, // ('+' | '-' | '*' | '/', value)
        rollerName: String,
    ) {
        if (diceTerms.isEmpty()) return
        val role = if (story.gmUserId == currentUserId && !_viewAsPlayer.value) "GM" else "Player"
        val rollParts = mutableListOf<String>()
        val exprParts = mutableListOf<String>()
        var running = 0
        diceTerms.forEach { (count, sides) ->
            val rolls = List(count.coerceIn(1, 50)) { Random.nextInt(1, sides + 1) }
            running += rolls.sum()
            exprParts += "${count}d$sides"
            rollParts += "${count}d$sides[${rolls.joinToString(",")}]"
        }
        val exprBuilder = StringBuilder(exprParts.joinToString(" + "))
        val breakBuilder = StringBuilder(rollParts.joinToString(" + "))
        modifierSteps.forEach { (op, v) ->
            exprBuilder.append(" $op $v")
            breakBuilder.append(" $op $v")
            running = when (op) {
                '+' -> running + v
                '-' -> running - v
                '×', '*' -> running * v
                '÷', '/' -> if (v != 0) running / v else running
                else -> running
            }
        }
        viewModelScope.launch {
            repository.submitRoll(
                story = story,
                rollerName = rollerName,
                senderRole = role,
                expression = exprBuilder.toString(),
                total = running,
                breakdown = breakBuilder.toString(),
            ).onFailure { err ->
                _toastMessage.value = unwrapFirestoreErrorMessage(err, "Failed to save roll.")
            }
        }
    }

    fun importCharactersWithAi(
        story: Story,
        rawText: String,
        kind: String,
        useRegexFallback: Boolean,
        onComplete: () -> Unit,
    ) {
        if (rawText.isBlank()) return
        val settings = userSettings.value
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            if (useRegexFallback) {
                val items = aiParser.parseCharactersRegex(rawText, kind, story, currentUserId)
                items.forEach { repository.saveCharacter(story, it) }
                _aiState.value = AiUiState.Success("Imported ${items.size} $kind sheet(s) via quick parser.")
                onComplete()
            } else {
                val res = aiParser.parseCharactersWithAi(
                    rawText = rawText,
                    kind = kind,
                    story = story,
                    ownerId = currentUserId,
                    provider = settings.aiProvider,
                    modelName = settings.aiModel,
                    endpointUrl = settings.aiEndpointUrl,
                    apiKeyOverride = settings.aiApiKey,
                )
                res.onSuccess { items ->
                    items.forEach { repository.saveCharacter(story, it) }
                    _aiState.value = AiUiState.Success("AI imported ${items.size} $kind sheet(s).")
                    onComplete()
                }.onFailure { err ->
                    _aiState.value = AiUiState.Error(err.localizedMessage ?: "AI parsing failed.")
                }
            }
        }
    }

    fun importWorldItemsWithAi(
        story: Story,
        rawText: String,
        category: String,
        startOrder: Int,
        useRegexFallback: Boolean,
        onComplete: () -> Unit,
    ) {
        if (rawText.isBlank()) return
        val settings = userSettings.value
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            if (useRegexFallback) {
                val items = aiParser.parseWorldItemsRegex(rawText, category, story, currentUserId, startOrder)
                items.forEach { repository.saveWorldItem(story, it) }
                _aiState.value = AiUiState.Success("Imported ${items.size} $category card(s) via quick parser.")
                onComplete()
            } else {
                val res = aiParser.parseWorldItemsWithAi(
                    rawText = rawText,
                    category = category,
                    story = story,
                    ownerId = currentUserId,
                    startOrder = startOrder,
                    provider = settings.aiProvider,
                    modelName = settings.aiModel,
                    endpointUrl = settings.aiEndpointUrl,
                    apiKeyOverride = settings.aiApiKey,
                )
                res.onSuccess { items ->
                    items.forEach { repository.saveWorldItem(story, it) }
                    _aiState.value = AiUiState.Success("AI imported ${items.size} $category card(s).")
                    onComplete()
                }.onFailure { err ->
                    _aiState.value = AiUiState.Error(err.localizedMessage ?: "AI parsing failed.")
                }
            }
        }
    }

    fun parseEquipmentWithAi(
        rawText: String,
        onParsed: (Map<String, EquipmentSlotItem>) -> Unit,
    ) {
        if (rawText.isBlank()) return
        val settings = userSettings.value
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            aiParser.parseEquipmentWithAi(
                rawText = rawText,
                provider = settings.aiProvider,
                modelName = settings.aiModel,
                endpointUrl = settings.aiEndpointUrl,
                apiKeyOverride = settings.aiApiKey,
            )
                .onSuccess { slots ->
                    _aiState.value = AiUiState.Success("Mapped ${slots.size} equipment slot(s).")
                    onParsed(slots)
                }
                .onFailure { err ->
                    _aiState.value = AiUiState.Error(err.localizedMessage ?: "AI equipment parse failed.")
                }
        }
    }

    fun clearAiMapError() {
        if (_aiMapState.value is AiMapUiState.Error) {
            _aiMapState.value = AiMapUiState.Idle
        }
    }

    fun generateMapWithAi(
        story: Story,
        prompt: String,
        currentWidth: Int,
        currentHeight: Int,
        currentGrid: Map<String, GridCell>,
        currentTokens: List<MapToken>,
        replaceEntireMap: Boolean,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        onApplied: (MapPaintOutcome) -> Unit,
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _aiMapState.value is AiMapUiState.Loading) return
        val settings = userSettings.value
        _aiMapState.value = AiMapUiState.Loading
        viewModelScope.launch {
            aiParser.generateMapWithAi(
                userPrompt = clean,
                story = story,
                ownerId = currentUserId,
                currentWidth = currentWidth,
                currentHeight = currentHeight,
                currentGrid = currentGrid,
                currentTokens = currentTokens,
                replaceEntireMap = replaceEntireMap,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                provider = settings.aiProvider,
                modelName = settings.aiModel,
                endpointUrl = settings.aiEndpointUrl,
                apiKeyOverride = settings.aiApiKey,
            ).onSuccess { outcome ->
                _aiMapState.value = AiMapUiState.Idle
                onApplied(outcome)
            }.onFailure { err ->
                _aiMapState.value = AiMapUiState.Error(
                    err.localizedMessage ?: "Failed to generate map with AI."
                )
            }
        }
    }

    private companion object {
        const val TAG = "StoryVM"
    }
}
