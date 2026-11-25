package com.example.data

data class GmGuideStep(
    val id: String,
    val tier: String, // "beginner" | "advanced" | "master"
    val title: String,
    val summary: String,
    val tip: String,
)

data class IdeaGuideContent(
    val key: String,
    val title: String,
    val subtitle: String,
    val prompts: List<String>,
    val checklist: List<String>,
)

object GuidesContent {
    val gmSteps = listOf(
        // Beginner
        GmGuideStep("b1", "beginner", "Define Core Premise", "Write a 2-sentence hook explaining why the party meets and what threatens the realm.", "Keep stakes personal in Session 1."),
        GmGuideStep("b2", "beginner", "Share Campaign Join Code", "Copy the 6-character Join Code from the campaign header and send it to your players.", "Players can join immediately from their Dashboard."),
        GmGuideStep("b3", "beginner", "Create Starting Location", "Add one safe hub (tavern, guildhall, outpost) and one immediate danger site in Locations.", "Include 2 sensory details and 1 NPC hook."),
        GmGuideStep("b4", "beginner", "Prepare 3 Key NPCs", "Create one ally, one quest-giver, and one suspicious rival in the NPCs tab.", "Give each NPC a distinct want and flaw."),
        GmGuideStep("b5", "beginner", "Sketch a First Map", "Paint a 12x12 tactical grid in Map with walls, terrain tiles, and player tokens.", "Use Flood Fill for quick room floors."),
        GmGuideStep("b6", "beginner", "Run a Warm-Up Roll", "Ask every player to introduce their character and roll a skill check using Dice Roller.", "Rolls automatically broadcast to Party Chat."),
        // Advanced
        GmGuideStep("a1", "advanced", "Build a Faction Clock", "Track 2 rival factions in Notes with 4-step escalation clocks.", "Advance clocks whenever players ignore a threat."),
        GmGuideStep("a2", "advanced", "Stock Custom Monster Tactics", "Add boss monsters in Monsters with custom HP, AC, and signature powers.", "Use AI Import to convert stat blocks rapidly."),
        GmGuideStep("a3", "advanced", "Layer Dynamic Map Hazards", "Place fire, trap, or elevation emojis and custom tile assets on battle maps.", "Switch brush size to 2x2 or 3x3 for wide corridors."),
        GmGuideStep("a4", "advanced", "Seed Meaningful Loot", "Assign themed gear to bosses and parse rewards into player Equipment silhouettes.", "Include one consumable and one lore clue."),
        GmGuideStep("a5", "advanced", "Track Party Missions", "Update character Missions after every session milestone so goals stay clear.", "Reward partial progress, not only final victories."),
        GmGuideStep("a6", "advanced", "Maintain Session Logs", "Review Logs before each game for a fast 60-second recap.", "Pin cliffhangers in Notes right after play."),
        // Master
        GmGuideStep("m1", "master", "Weave Backstory Callbacks", "Tie at least one player secret from Character Notes into the main villain's plan.", "Reveal clues in threes across separate scenes."),
        GmGuideStep("m2", "master", "Design Multi-Phase Encounters", "Pair environmental terrain changes with monster reinforcements at half HP.", "Use Rect Select + Fill to reshape rooms mid-fight."),
        GmGuideStep("m3", "master", "Custom Homebrew Powers", "Craft signature relics and spells in the Powers tab with clear resource costs.", "Balance high-impact powers with narrative drawbacks."),
        GmGuideStep("m4", "master", "Cross-Campaign World Echoes", "Let resolved quests visibly transform recurring Locations and NPC attitudes.", "Archive old rumors and promote new legends."),
        GmGuideStep("m5", "master", "Audio Atmosphere & Pacing", "Coordinate live voice check-ins via the Chat Voice Room while keeping turns brisk.", "Spotlight quiet players every 10 minutes."),
        GmGuideStep("m6", "master", "Run Session Zero Check-Ins", "Revisit tone, safety boundaries, and campaign pacing after major arcs.", "Celebrate favorite moments together!"),
    )

    val ideaGuides: Map<String, IdeaGuideContent> = listOf(
        IdeaGuideContent(
            key = "players",
            title = "Player Character Ideas",
            subtitle = "Memorable heroes with hooks",
            prompts = listOf(
                "A disgraced cartographer seeking a vanished island",
                "An oath-bound clockwork medic running out of spare parts",
                "A street poet whose ballads accidentally summon spirits",
            ),
            checklist = listOf("Clear motivation", "One trusted ally", "One secret flaw"),
        ),
        IdeaGuideContent(
            key = "npcs",
            title = "NPC Generator Guide",
            subtitle = "Instant personality & motives",
            prompts = listOf(
                "Archivist who trades memories instead of coin",
                "Retired siege-captain running a floating tea barge",
                "Smuggler with a strict code against harming beasts",
            ),
            checklist = listOf("Voice/mannerism", "What they want now", "Leverage over the party"),
        ),
        IdeaGuideContent(
            key = "monsters",
            title = "Monster & Encounter Guide",
            subtitle = "Tactical threats beyond bags of HP",
            prompts = listOf(
                "Crystal Stalker that refracts spell damage onto nearby allies",
                "Belfry Gargoyle that grapples heroes toward cliff edges",
                "Bog Alchemist lobbing fog flasks to break line of sight",
            ),
            checklist = listOf("Signature attack", "Retreat/morale condition", "Terrain synergy"),
        ),
        IdeaGuideContent(
            key = "map",
            title = "Tactical Map Design",
            subtitle = "Dynamic battlegrounds",
            prompts = listOf(
                "Collapsed bridge with 2 chokepoints and rushing river tiles",
                "Circular arcane library with cover pillars and teleport runes",
                "Two-level tavern with balcony sniping lanes",
            ),
            checklist = listOf("3+ cover obstacles", "1 environmental hazard", "Alternate route"),
        ),
        IdeaGuideContent(
            key = "locations",
            title = "Location Worldbuilding",
            subtitle = "Places that feel alive",
            prompts = listOf(
                "The Lantern Market — open only during eclipses",
                "Ironroot Citadel — fortress grown inside a petrified titan",
                "Saltglass Cove — harbor where sunken bells ring at low tide",
            ),
            checklist = listOf("Distinct sensory detail", "Local tension/rumor", "Notable landmark"),
        ),
        IdeaGuideContent(
            key = "powers",
            title = "Powers & Relics Guide",
            subtitle = "Flavorful abilities with trade-offs",
            prompts = listOf(
                "Echo Step: Teleport 3 tiles, leaving an explosive shadow decoy",
                "Aegis Choir: Grant nearby allies +2 AC while singing",
                "Ember Pact: Add 1d6 fire damage at the cost of 2 HP",
            ),
            checklist = listOf("Clear trigger/cost", "Tactical niche", "Narrative flavor"),
        ),
        IdeaGuideContent(
            key = "notes",
            title = "Campaign Notes & Clues",
            subtitle = "Secrets, clocks, and lore",
            prompts = listOf(
                "Three-clue mystery structure for the missing magistrate",
                "4-tick countdown clock: Cult opens the Obsidian Gate",
                "Rumors table: 2 true leads and 1 tempting red herring",
            ),
            checklist = listOf("Next session hook", "Unresolved threads", "NPC debts & favors"),
        ),
        IdeaGuideContent(
            key = "chat",
            title = "Table Etiquette & Voice",
            subtitle = "Smooth table communication",
            prompts = listOf(
                "Post scene descriptions in chat so players can re-read clues",
                "Use Voice Room for live roleplay and combat callouts",
                "Broadcast skill checks directly from the Dice Roller",
            ),
            checklist = listOf("Clear turn cues", "Recap pinned", "Respect spotlight"),
        ),
        IdeaGuideContent(
            key = "equipment",
            title = "12-Slot Loadout Guide",
            subtitle = "Meaningful gear across every slot",
            prompts = listOf(
                "Head: Brass Spyglass Visor (+2 Perception)",
                "Cloak: Mantle of Mist (Advantage on Stealth in dim light)",
                "Main Hand: Moonsteel Rapier (+1 Attack, glows near undead)",
            ),
            checklist = listOf("Weapon synergy", "Utility trinket", "Signature visual piece"),
        ),
        IdeaGuideContent(
            key = "dice",
            title = "Dice Builder Tips",
            subtitle = "Fast combos & modifiers",
            prompts = listOf(
                "Tap d20 once + add modifier for quick attack or skill checks",
                "Tap 2d20 for Advantage/Disadvantage comparison",
                "Chain + / - / × / ÷ steps for damage & critical calculations",
            ),
            checklist = listOf("Check modifier sign", "Verify crit rules", "Review chat broadcast"),
        ),
    ).associateBy { it.key }
}
