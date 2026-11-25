# TTRPG Toolkit — Complete Android Kotlin Specification Extraction & In-App Export

This plan outlines the extraction of the entire TTRPG Toolkit application—every screen, tab, sub-tab, interactive tool, AI parser, guide, translation, theme token, and Firestore schema—into a self-contained Markdown specification tailored for rebuilding the app natively in Android Kotlin (Jetpack Compose, Material 3, and Firebase), including repository creation details, automatic APK builds immediately after pushing to GitHub, and an in-app download action so you can easily export and paste it into a new chat.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All core extraction, target-platform, and CI/CD decisions have been incorporated from your answers and follow-up request. Please review the updated plan before we generate the complete specification and wire up the in-app download controls.

- **Confirmed Decision 1 — Delivery Method**: Both a saved Markdown specification file in the workspace and an in-app download button inside the application.
- **Confirmed Decision 2 — Target Android Stack**: Native Android in Kotlin using **Jetpack Compose**, **Material 3**, and **Firebase** (Authentication + Cloud Firestore).
- **Confirmed Decision 3 — Depth of Detail**: Complete, lossless documentation including Firestore schemas, security rules, AI parser prompt templates and regex logic, all 10 Idea Guides, the 3-tier GM Step Guide, emoji helper mappings, all 12-language translations, theming tokens, and exact tab/tool behaviors.
- **Confirmed Decision 4 — GitHub Repo Info & Immediate APK Build on Push**: The extracted specification will include a dedicated **GitHub Repository & CI/CD Setup** section providing the recommended repository name, description, Android App Name, Application ID (`com.ttrpg.storyweaver`), Firebase config/secrets setup, and a ready-to-use GitHub Actions workflow that automatically compiles and uploads a Debug APK immediately on every push to GitHub.

---

## 1. Overview & Core Concept

- **What It Does**: Extracts the full functional, visual, behavioral, data, and CI/CD build specification of the **TTRPG Toolkit** into a single structured blueprint designed so a Kotlin Android engineering session can rebuild the entire native app—and configure its GitHub repository for automatic APK builds on push—without missing any tab, edge case, or content asset.
- **Target Audience / Persona**: An Android developer or AI coding session starting fresh in a new chat to build the native Kotlin version of TTRPG Toolkit for Game Masters (GMs) and Players.
- **Key Value**: Eliminates guesswork and feature loss across role-based views (GM vs. Player), complex interactive tools (the Grid Map editor, Equipment Silhouette, Tap-based Dice Roller), rich embedded content (Guides, AI Parsers, and 12-language localization), and GitHub repo/APK automation.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **In-App Export Flow**: From the Dashboard, you open **Settings** and find a dedicated **Export Android Kotlin Spec** card.
  2. **Download or Copy**: Tapping **Download Specification (.md)** triggers a clean browser file download of the complete Markdown document; a companion **Copy to Clipboard** button provides instant one-tap copying with visual confirmation.
  3. **Workspace Access**: Simultaneously, the exact same Markdown specification file is stored in the project workspace for direct inspection or download.
  4. **GitHub Repo & APK Flow**: When pasting the spec into a new chat, the AI session immediately gets the exact repository name, metadata, and GitHub Actions workflow needed so every push to `main` triggers an immediate Android Debug APK build and uploads the downloadable `.apk` artifact.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Matches the existing dark-first, translucent card-based TTRPG Toolkit interface using CSS theme variables (`--bg-main`, `--bg-secondary`, `--accent-color`, `--text-main`, `--text-secondary`) so the new export card feels completely native to the Settings screen.
  - *Component Styling & Layout*: Placed cleanly at the bottom of the Settings screen as a rounded card matching the existing Colors, Text Size, and Language sections, using a primary-tinted action button and subtle secondary copy button.
- **Interactive Feedback & Motion**: Uses the existing Framer Motion entrance/exit transitions, hover scaling, and a temporary "Copied!" checkmark state when copying.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Target Stack Blueprinting (Jetpack Compose + Material 3 + Firebase)**
  - *Chosen Approach*: Map every web UI pattern to idiomatic Android Kotlin equivalents (e.g., `ModalNavigationDrawer` / `NavigationBar` / `ScrollableTabRow` for role tabs, `Canvas` + `pointerInput` (`detectTransformGestures` / `detectDragGestures`) for the interactive Grid Map, `LazyVerticalGrid` / `LazyColumn` with drag-to-reorder for `ListView`, `BoxWithConstraints` for the 12-slot Equipment Silhouette, `WebView` or Jitsi Android SDK for voice chat, and `kotlinx.serialization` / `Gson` for JSON-serialized character/story fields).
  - *Why*: Ensures the new chat receives concrete, ready-to-implement Kotlin & Jetpack Compose architectural guidance alongside the exact behavioral rules.
  - *Alternatives Considered*: Keeping the spec purely abstract—rejected because concrete Compose & Firebase Android mappings prevent mismatched UI paradigms (such as desktop hover tooltips vs. Android long-press/tap sheets).

- **Decision 2 — Complete Lossless Content Inclusion**
  - *Chosen Approach*: Read every single source module in full—including all 10 Idea Guides, the 3-tier GM Step Guide, both AI Parser templates and regex rules, `emojiHelper` keyword mappings, all 12 language translation tables, Firestore blueprint + security rules, and every Grid Map tool behavior—and embed them systematically into the specification document.
  - *Why*: Prevents losing localized strings, custom prompt formats, or domain guides when starting fresh in a new chat.
  - *Alternatives Considered*: Summarizing guides or truncating translation tables—rejected per your request for complete details.

- **Decision 3 — Bundled In-App Download Mechanism**
  - *Chosen Approach*: Bundle the generated Markdown specification directly as a static text asset with the app so the in-app **Download Spec (.md)** button works instantaneously offline and online without requiring a backend route.
  - *Why*: Zero network latency, reliable download via `Blob` + `<a download>`, and guaranteed 1:1 parity between the workspace file and the downloaded file.
  - *Alternatives Considered*: Dynamically generating the spec at runtime or adding an Express endpoint—unnecessary overhead for static documentation extraction.

- **Decision 4 — Automatic GitHub Actions Debug APK on Push & Repo Metadata**
  - *Chosen Approach*: Specify a GitHub Actions workflow triggered immediately on `push` to `main` (and `master`, plus manual `workflow_dispatch`) that sets up JDK 17, caches Gradle, decodes the Firebase Android config from a GitHub Secret if not committed, runs `./gradlew assembleDebug`, and uploads the resulting APK artifact. Pair this with explicit repository creation metadata (suggested repo name, description, App Name, and Application ID matching the existing Android configuration).
  - *Why*: Lets you create the GitHub repo quickly with all required identifiers and receive a ready-to-install APK automatically on every push without manual Android Studio builds.
  - *Alternatives Considered*: Requiring signed Release APKs on every push—deferred because a Debug APK requires zero keystore management to start testing immediately on push.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Extraction Flow Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Existing App Source Inspection                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Auth & Nav   │  │ GM & Player  │  │ Map, Dice,   │  │ Guides, AI  │  │
│  │ (Dashboard,  │  │ Tabs & Sheet │  │ Chat, Logs,  │  │ Parsers, CI │  │
│  │  StoryView)  │  │ (Equipment,  │  │ Settings &   │  │ & 12-Lang   │  │
│  │              │  │  ListView)   │  │ Themes       │  │ i18n + DB   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘  │
└─────────┼─────────────────┼─────────────────┼─────────────────┼─────────┘
          └─────────────────┴────────┬────────┴─────────────────┘
                                     ▼
          ┌─────────────────────────────────────────────────────┐
          │   Comprehensive Markdown Specification Document     │
          │  • App Overview & Kotlin/Compose/Firebase Stack     │
          │  • GitHub Repo Name, Info & Push-to-Build APK CI/CD │
          │  • Data Models, Firestore Paths & Security Rules    │
          │  • Theme System (5 Presets + Custom Color Tokens)   │
          │  • Complete Screen & Tab-by-Tab Functional Specs    │
          │  • Interactive GridMap Engine & Asset Persistence   │
          │  • AI Parsers (Prompts + Regex) & Emoji Helper      │
          │  • Full GM Step Guide & 10 Contextual Idea Guides   │
          │  • Full 12-Language Localization Dictionary         │
          └──────────────┬───────────────────────┬──────────────┘
                         │                       │
                         ▼                       ▼
          ┌──────────────────────────┐  ┌──────────────────────────────┐
          │ Workspace Markdown File  │  │ Settings Screen Export Card  │
          │ (Direct download/copy)   │  │ • Download .md file button   │
          │                          │  │ • Copy to Clipboard button   │
          └──────────────────────────┘  └──────────────────────────────┘
```

- **Document Organization (Sections Inside the Extracted Spec)**:
  1. **System Prompt / Rebuild Context**: Clear instructions for the new Kotlin chat, target Android SDK (`minSdk 26`, `targetSdk 35`, `compileSdk 35`), Jetpack Compose + Material 3, Firebase Auth (Google Credential Manager) + Cloud Firestore + Kotlin Coroutines/Flow.
  2. **GitHub Repository Setup & Immediate APK Build CI/CD**:
     - *Repository Creation Info*: Recommended repo name (`ttrpg-story-weaver-android` or `ttrpg-toolkit-android`), concise repo description, default branch (`main`), Android App Name (`TTRPG Story Weaver`), Application ID (`com.ttrpg.storyweaver`), required Android permissions (`INTERNET`, `CAMERA`, `RECORD_AUDIO`), and instructions for registering the Android app in Firebase (`google-services.json` + SHA-1 fingerprint) and optionally storing it as a base64 GitHub Secret (`GOOGLE_SERVICES_JSON`) for CI builds.
     - *Immediate APK Workflow*: Complete GitHub Actions YAML specification triggered immediately on every `push` to `main`/`master` (and `workflow_dispatch`) that provisions JDK 17 + Android SDK, restores Gradle cache, injects `google-services.json` from secret if present, runs `./gradlew assembleDebug`, and publishes `app-debug.apk` as a workflow artifact.
  3. **Global State, Theme & Localization**: All 5 themes (`dark`, `light`, `midnight`, `forest`, `sunset`), 5 custom color overrides, 4 text-size scales, and the complete 12-language translation dictionary (`en`, `es`, `fr`, `de`, `it`, `pt`, `ja`, `ko`, `zh`, `ru`, `ar`, `hi`).
  4. **Firebase Schema & Security Rules**: Complete `User`, `Story`, `Character`, `Roll`, `Message`, and `Log` models, collection/subcollection paths, Firestore size constraints (e.g., 500KB `mapData`, 1MB `avatar`), and exact security validation rules.
  5. **Screen & Navigation Flow**: Auth screen -> Dashboard (GM Stories + Player Characters + Create/Join Story Modals + Settings) -> StoryView (Collapsible Header, Role/View-As-Player Toggle, Floating DicePanel).
  6. **GM View (All 10 Tabs)**:
     - *Guide*: 3-tier (`beginner`, `advanced`, `master`) interactive checklist with percentage progress & reset, persisted in `guideProgress`.
     - *Players / NPCs / Monsters*: Searchable sidebar lists, multi-select batch delete, quick create, AI Text Parser import, and full `CharacterSheet` editor.
     - *Map (`GridMap`)*: Pan/Zoom vs. Edit mode, brush sizes (1x1, 2x2, 3x3), Brush, Eraser, Flood Fill, Rectangular Select + Fill, drag-to-paint/erase, color-only blocks, Emoji Picker, Custom Image Asset uploader (resized to 128px max, persisted in local storage Quick Bar), Map Background Image (`object-contain`, top-left aligned, non-cropping), grid resizing (`WxH` with out-of-bounds cleanup), Clear Grid, Player/NPC/Monster token placement & drag-to-move with debounced Firestore sync.
     - *Locations / Powers / Notes*: `ListView` cards with drag-and-drop reordering, multi-select move up/down & delete, search, IconPicker (Lucide icons, emojis, custom URL/image upload), AI Text Parser, and contextual `IdeaGuide`.
     - *Chat*: Real-time party chat (`stories/{storyId}/messages`), offline detection banner, and embedded Jitsi Meet audio-only voice call (`aistudio_rpg_{storyId}`).
     - *Logs*: Real-time chronological activity feed (`logs` collection filtered by `storyId`).
  7. **Player View (All 4 Tabs)**: *Players* (Party roster + editable own CharacterSheet + read-only view rules), *Map* (view mode + move own character token), *Chat*, and *Logs*.
  8. **Character Sheet (All 6 Sub-Tabs)**: *Info* (Avatar picker + editable name + ID), *Notes*, *Inventory*, *Missions*, *Powers*, *Equipment & Stats* (12-slot interactive body silhouette + Equipment AI Parser + freeform Stats editor + 5-second debounced auto-save).
  9. **Floating Dice Roller (`DicePanel`)**: Tap-based builder (`count`, `d4/d6/d8/d10/d12/d20/d100`, chained `+/-/×/÷` modifiers), rolls history synced to `stories/{storyId}/rolls` and auto-posted to party chat.
  10. **AI Parsers & Utilities**: Exact prompt templates, regex parsing rules, `emojiHelper` auto-assignment rules, and all 10 Idea Guides + GM Step Guide text.
- **Interactive Component & State Mapping**:
  - In `SettingsView`, an **Export Specification** section triggers a client-side `Blob` download (`TTRPG_Toolkit_Android_Kotlin_Spec.md`) and uses `navigator.clipboard.writeText` for one-click copy, reusing existing state feedback patterns.
