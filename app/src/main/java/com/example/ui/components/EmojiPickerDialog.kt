package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class EmojiEntry(
    val symbol: String,
    val keywords: String,
)

data class EmojiCategory(
    val id: String,
    val label: String,
    val entries: List<EmojiEntry>,
)

object EmojiCatalog {
    private fun parsePairs(raw: String, categoryKeywords: String): List<EmojiEntry> =
        raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct().map { sym ->
            EmojiEntry(symbol = sym, keywords = "$categoryKeywords $sym")
        }

    private fun tagged(vararg pairs: Pair<String, String>): List<EmojiEntry> =
        pairs.map { (sym, kw) -> EmojiEntry(sym, kw) }.distinctBy { it.symbol }

    val categories: List<EmojiCategory> = listOf(
        EmojiCategory(
            id = "rpg",
            label = "RPG & Gear",
            entries = tagged(
                "⚔️" to "sword weapon combat attack blade",
                "🗡️" to "dagger blade rogue assassin knife",
                "🛡️" to "shield armor defense guard",
                "🏹" to "bow arrow ranger archer",
                "🪓" to "axe barbarian battleaxe",
                "🔨" to "hammer warhammer blacksmith",
                "⛏️" to "pickaxe mine dwarf",
                "⚒️" to "forge smith craft",
                "🛠️" to "tools repair craft",
                "🔪" to "knife dagger",
                "💣" to "bomb explosive",
                "🧨" to "dynamite firecracker explosive",
                "🪃" to "boomerang ranged",
                "🔮" to "crystal ball magic arcane orb",
                "🪄" to "wand spell magic wizard",
                "📿" to "amulet necklace beads prayer",
                "💎" to "gem diamond treasure crystal",
                "👑" to "crown king queen royal",
                "💍" to "ring jewelry magic",
                "🏆" to "trophy prize victory",
                "📜" to "scroll lore parchment spell",
                "📖" to "book tome grimoire spellbook",
                "📕" to "red book tome",
                "📗" to "green book tome",
                "📘" to "blue book tome",
                "📙" to "orange book tome",
                "📚" to "books library",
                "🗝️" to "old key skeleton key dungeon",
                "🔑" to "key lock treasure",
                "🔒" to "lock chest",
                "🔓" to "unlock",
                "🚪" to "door dungeon entrance",
                "🪙" to "coin gold money",
                "💰" to "gold bag loot treasure money",
                "🎲" to "dice roll tabletop rpg",
                "♟️" to "chess strategy board",
                "🎯" to "target bullseye aim",
                "🪖" to "helmet head armor",
                "🎭" to "mask theater eyes disguise",
                "🦺" to "chest armor vest",
                "🧥" to "cloak cape back coat",
                "⌚" to "wrist bracer watch",
                "🧤" to "gloves gauntlets hands",
                "🥋" to "belt sash waist",
                "👖" to "legs greaves pants",
                "🥾" to "boots feet shoes",
                "🎒" to "backpack inventory bag",
                "🧪" to "potion alchemy poison vial",
                "⚗️" to "alembic alchemy potion lab",
                "🕯️" to "candle light dark",
                "🏮" to "lantern lamp",
                "🔦" to "torch flashlight",
                "🧭" to "compass explore map",
                "🗺️" to "map world travel",
                "⚖️" to "scales justice balance",
                "⛓️" to "chains prison trap",
                "🩸" to "blood wound hp",
                "☠️" to "skull poison death undead",
                "💀" to "skull skeleton undead",
                "👻" to "ghost spirit haunt",
                "👹" to "oni monster ogre",
                "👺" to "goblin monster mask",
                "🐉" to "dragon wyrm boss",
                "🐲" to "dragon head beast",
                "🧙" to "wizard mage npc",
                "🧝" to "elf ranger",
                "🧛" to "vampire undead",
                "🧟" to "zombie undead",
                "🧚" to "fairy fey magic",
                "🧞" to "genie djinn magic",
                "🧜" to "merfolk sea",
                "🦸" to "hero champion",
                "🦹" to "villain dark lord",
                "🔥" to "fire flame burn",
                "❄️" to "ice frost cold snow",
                "⚡" to "lightning thunder storm shock",
                "✨" to "magic sparkle heal radiant",
                "💥" to "explosion impact blast",
                "🌪️" to "wind tornado storm",
                "🌊" to "water wave ocean",
                "🕳️" to "pit hole trap cave",
            )
        ),
        EmojiCategory(
            id = "smileys",
            label = "Smileys & People",
            entries = parsePairs(
                "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩 👻 💀 ☠️ 👽 👾 🤖 🎃 😺 😸 😹 😻 😼 😽 🙀 😿 😾 " +
                    "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 🖕 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✍️ 💪 🦾 🦵 🦶 👂 👃 🧠 🫀 🫁 🦷 🦴 👀 👁️ 👅 👄 💋 " +
                    "👶 🧒 👦 👧 🧑 👨 👩 🧔 🧓 👴 👵 👲 👳 🧕 👮 👷 💂 🕵️ 👩‍⚕️ 👨‍⚕️ 👩‍🌾 👨‍🌾 👩‍🍳 👨‍🍳 👩‍🎓 👨‍🎓 👩‍🎤 👨‍🎤 👩‍🏫 👨‍🏫 👩‍🏭 👨‍🏭 👩‍💻 👨‍💻 👩‍💼 👨‍💼 👩‍🔧 👨‍🔧 👩‍🔬 👨‍🔬 👩‍🎨 👨‍🎨 👩‍🚒 👨‍🚒 👩‍✈️ 👨‍✈️ 👩‍🚀 👨‍🚀 👩‍⚖️ 👨‍⚖️ 👰 🤵 👸 🤴 🦸 🦹 🤶 🎅 🧙 🧝 🧛 🧟 🧞 🧜 🧚 👼 💃 🕺 👯 🚶 🏃 🧎 🧍",
                "face smiley person people hand expression character"
            )
        ),
        EmojiCategory(
            id = "animals",
            label = "Animals & Nature",
            entries = parsePairs(
                "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐻‍❄️ 🐨 🐯 🦁 🐮 🐷 🐽 🐸 🐵 🙈 🙉 🙊 🐒 🐔 🐧 🐦 🐤 🐣 🐥 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🪱 🐛 🦋 🐌 🐞 🐜 🪰 🪲 🪳 🦟 🦗 🕷️ 🕸️ 🦂 🐢 🐍 🦎 🦖 🦕 🐙 🦑 🦐 🦞 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🦭 🐊 🐅 🐆 🦓 🦍 🦧 🐘 🦛 🦏 🐪 🐫 🦒 🦘 🦬 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🦙 🐐 🦌 🐕 🐩 🐈 🐈‍⬛ 🪶 🐓 🦃 🦤 🦚 🦜 🦢 🦩 🕊️ 🐇 🦝 🦨 🦡 🦫 🦦 🦥 🐁 🐀 🐿️ 🦔 🐾 🐉 🐲 " +
                    "🌵 🎄 🌲 🌳 🌴 🪵 🌱 🌿 ☘️ 🍀 🎍 🪴 🎋 🍃 🍂 🍁 🍄 🐚 🪨 🌾 💐 🌷 🌹 🥀 🌺 🌸 🌼 🌻 🌞 🌝 🌛 🌜 🌚 🌕 🌖 🌗 🌘 🌑 🌒 🌓 🌔 🌙 🌎 🌍 🌏 🪐 💫 ⭐ 🌟 ✨ ⚡ ☄️ 💥 🔥 🌪️ 🌈 ☀️ 🌤️ ⛅ 🌥️ ☁️ 🌦️ 🌧️ ⛈️ 🌩️ 🌨️ ❄️ ☃️ ⛄ 🌬️ 💨 💧 💦 🫧 ☔ ☂️ 🌊 🌫️",
                "animal beast nature plant weather moon sun fire water"
            )
        ),
        EmojiCategory(
            id = "food",
            label = "Food & Drink",
            entries = parsePairs(
                "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🫑 🌽 🥕 🫒 🧄 🧅 🥔 🍠 🥐 🥯 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🦴 🌭 🍔 🍟 🍕 🫓 🥪 🥙 🧆 🌮 🌯 🫔 🥗 🥘 🫕 🥫 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🦪 🍤 🍙 🍚 🍘 🍥 🥠 🥮 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🌰 🥜 🫘 🍯 🥛 🍼 🫖 ☕ 🍵 🧃 🥤 🧋 🍶 🍺 🍻 🥂 🍷 🥃 🍸 🍹 🧉 🍾 🧊 🥄 🍴 🍽️ 🥣 🥡 🥢 🧂",
                "food drink potion tavern ale meal fruit"
            )
        ),
        EmojiCategory(
            id = "places",
            label = "Places & Travel",
            entries = parsePairs(
                "🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🚲 🛵 🏍️ 🚨 🚔 🚍 🚘 🚖 🚡 🚠 🚟 🚃 🚋 🚞 🚝 🚄 🚅 🚈 🚂 🚆 🚇 🚊 🚉 ✈️ 🛫 🛬 🛩️ 💺 🛰️ 🚀 🛸 🚁 🛶 ⛵ 🚤 🛥️ 🛳️ ⛴️ 🚢 ⚓ 🪝 ⛽ 🚧 🚦 🚥 🗺️ 🗿 🗽 🗼 🏰 🏯 🏟️ 🎡 🎢 🎠 ⛲ ⛱️ 🏖️ 🏝️ 🏜️ 🌋 ⛰️ 🏔️ 🗻 🏕️ ⛺ 🏠 🏡 🏘️ 🏚️ 🏗️ 🏭 🏢 🏬 🏣 🏤 🏥 🏦 🏨 🏪 🏫 🏩 💒 🏛️ ⛪ 🕌 🕍 🛕 🕋 ⛩️ 🛤️ 🛣️ 🗾 🎑 🏞️ 🌅 🌄 🌠 🎇 🎆 🌇 🌆 🏙️ 🌃 🌌 🌉 🌁",
                "place location building castle tavern mountain travel ship"
            )
        ),
        EmojiCategory(
            id = "objects",
            label = "Objects & Arts",
            entries = parsePairs(
                "👓 🕶️ 🥽 🥼 🦺 👔 👕 👖 🧣 🧤 🧥 🧦 👗 👘 🥻 🩱 🩲 🩳 👙 👚 👛 👜 👝 🎒 🧳 👞 👟 🥾 🥿 👠 👡 🩰 👢 👑 👒 🎩 🎓 🧢 🪖 ⛑️ 📿 💄 💍 💎 🔕 🔔 📢 📣 📯 🔦 🏮 🪔 🕯️ 💡 🔋 🔌 💻 🖥️ 🖨️ ⌨️ 🖱️ 📱 ☎️ 📞 📟 📠 📺 📻 🧭 ⏱️ ⏲️ ⏰ 🕰️ ⌛ ⏳ 📡 🔬 🔭 💉 💊 🩹 🩺 🩻 🪜 🧱 🪣 🪝 🪢 🪡 🧵 🧶 🧲 🧰 🪤 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🪕 🎻 🎮 🎰 🧩 🃏 🀄 🎴 🪙 💵 💴 💶 💷 💸 💳 🧾 ✉️ 📧 📨 📩 📤 📥 📦 📫 📪 📬 📭 📮 🗳️ ✏️ ✒️ 🖋️ 🖊️ 🖌️ 🖍️ 📝 💼 📁 📂 🗂️ 📅 📆 🗒️ 🗓️ 📇 📈 📉 📊 📋 📌 📍 📎 🖇️ 📏 📐 ✂️ 🗃️ 🗄️ 🗑️ 🔒 🔓 🔏 🔐 🔑 🗝️ 🔨 🪓 ⛏️ ⚒️ 🛠️ 🗡️ ⚔️ 🔫 🪃 🏹 🛡️ 🪚 🔧 🪛 🔩 ⚙️ 🗜️ ⚖️ 🦯 🔗 ⛓️",
                "object gear clothing instrument tool weapon scroll bag"
            )
        ),
        EmojiCategory(
            id = "symbols",
            label = "Symbols & Marks",
            entries = parsePairs(
                "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ 🕉️ ☸️ ✡️ 🔯 🕎 ☯️ ☦️ 🛐 ⛎ ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ 🆔 ⚛️ ☢️ ☣️ ✴️ 🆚 💮 🉐 ㊙️ ⭕ 🛑 ⛔ 📛 🚫 💯 💢 ♨️ ❗ ❕ ❓ ❔ ‼️ ⁉️ 🔅 🔆 ⚠️ 🚸 🔱 ⚜️ 🔰 ♻️ ✅ ❇️ ✳️ ❎ 🌐 💠 🌀 💤 🏧 🚾 ♿ 🅿️ 📶 🎦 🔣 ℹ️ 🔤 🔡 🔠 🆗 🆙 🆒 🆕 🆓 0️⃣ 1️⃣ 2️⃣ 3️⃣ 4️⃣ 5️⃣ 6️⃣ 7️⃣ 8️⃣ 9️⃣ 🔟 ➕ ➖ ➗ ✖️ ♾️ 💲 💱 ™️ ©️ ®️ 〰️ ➰ ➿ ✔️ ☑️ 🔘 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🟤 🔺 🔻 🔸 🔹 🔶 🔷 🔳 🔲 ▪️ ▫️ ◾ ◽ ◼️ ◻️ 🟥 🟧 🟨 🟩 🟦 🟪 ⬛ ⬜ 🟫 🔈 🔉 🔊 🔇 🔔 🔕 📣 📢 💬 💭 🗯️ ♠️ ♣️ ♥️ ♦️ 🃏 🎴 🀄 🕐 🕑 🕒 🕓 🕔 🕕 🕖 🕗 🕘 🕙 🕚 🕛 🏁 🚩 🏴 🏳️ 🏳️‍🌈 🏴‍☠️",
                "symbol heart star flag mark dice card magic rune"
            )
        ),
    )

    val allUniqueEntries: List<EmojiEntry> by lazy {
        val seen = LinkedHashMap<String, EmojiEntry>()
        categories.forEach { cat ->
            cat.entries.forEach { entry ->
                val existing = seen[entry.symbol]
                if (existing == null) {
                    seen[entry.symbol] = entry
                } else {
                    seen[entry.symbol] = existing.copy(keywords = "${existing.keywords} ${entry.keywords}")
                }
            }
        }
        seen.values.toList()
    }

    fun filterEntries(categoryIndex: Int, query: String): List<EmojiEntry> {
        val base = if (categoryIndex <= 0) allUniqueEntries else categories.getOrNull(categoryIndex - 1)?.entries ?: allUniqueEntries
        val clean = query.trim().lowercase()
        val result = if (clean.isEmpty()) {
            base
        } else {
            allUniqueEntries.filter {
                it.symbol.contains(clean) || it.keywords.lowercase().contains(clean)
            }
        }
        return result.distinctBy { it.symbol }
    }
}

@Composable
fun AllEmojiPickerDialog(
    currentEmoji: String,
    title: String = "Select Emoji",
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var customEmojiInput by remember { mutableStateOf(currentEmoji) }

    val tabLabels = remember { listOf("All") + EmojiCatalog.categories.map { it.label } }
    val filteredEntries = remember(selectedTabIndex, searchQuery) {
        EmojiCatalog.filterEntries(selectedTabIndex, searchQuery)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("all_emoji_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_emoji_picker_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Direct OS keyboard emoji input so literally every system Unicode emoji can be picked
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customEmojiInput,
                        onValueChange = { customEmojiInput = it.take(16) },
                        label = { Text("Any Keyboard Emoji") },
                        placeholder = { Text("Tap 😊 on keyboard") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("custom_emoji_keyboard_input")
                    )
                    Button(
                        onClick = {
                            val clean = customEmojiInput.trim()
                            if (clean.isNotEmpty()) {
                                onSelect(clean)
                                onDismiss()
                            }
                        },
                        enabled = customEmojiInput.isNotBlank(),
                        modifier = Modifier.testTag("apply_custom_emoji_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Use emoji")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search emojis (e.g. sword, dragon, fire, heart)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("search_emoji_input")
                )

                if (searchQuery.isBlank()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp
                    ) {
                        tabLabels.forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTabIndex == idx,
                                onClick = { selectedTabIndex = idx },
                                text = { Text(label) },
                                modifier = Modifier.testTag("emoji_category_tab_$idx")
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 50.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .testTag("emoji_picker_grid")
                ) {
                    items(filteredEntries, key = { it.symbol }) { entry ->
                        val isSelected = entry.symbol == currentEmoji
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onSelect(entry.symbol)
                                    onDismiss()
                                }
                                .testTag("emoji_cell_${entry.symbol}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = entry.symbol,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}
