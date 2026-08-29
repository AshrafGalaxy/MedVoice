# 🛡️ MedVoice — Medication Safety & Vernacular Ambient Voice Assistant

> **Edge Medication Safety & Ambient Voice Assistant (Hybrid Edge-Cloud AI Architecture)**

[![GitHub Release](https://img.shields.io/badge/Release-v1.0.0-00875A?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AshrafGalaxy/MedVoice/releases/tag/v1.0.0)
[![Judge Demo Pack](https://img.shields.io/badge/🧪_Judge_Demo-Test_Pack-0052CC?style=for-the-badge)](DEMO_TEST_PACK.md)
[![Build Status](https://img.shields.io/badge/Unit_Tests-37%2F37_Passed_(100%25)-brightgreen?style=for-the-badge)](app/src/test/java/com/medvoice/)

### 📲 [⬇️ Direct Download APK (v1.0.0)](https://github.com/AshrafGalaxy/MedVoice/releases/download/v1.0.0/MedVoice-v1.0.0.apk) | [🧪 2-Minute Judge Evaluation Guide](DEMO_TEST_PACK.md)

---

## 🌟 Overview

**MedVoice** is a medication safety and ambient voice assistant application designed to protect elderly patients from accidental medication errors, duplicate dosage toxicity, and dangerous drug-to-drug interactions. 

MedVoice operates with a **Hybrid Edge-Cloud Architecture**:
- **On-Device Clinical Safety Matrix**: All drug contraindications, salt matches, duplicate dose warnings, and the ~30,000 Indian pharmaceutical brand SQLite FTS5 database execute deterministically on the physical device.
- **Multimodal Visual AI & OCR**: Combines on-device Google ML Kit Vision with cloud-hosted visual language models (Groq `llama-3.2-11b-vision-preview` & MedGemma Qwen 27B) to extract high-accuracy pharmaceutical brand names and active compositions from complex blister packs.
- **Vernacular Audio & Hands-Free Interaction**: Delivers instant spoken guidance in vernacular Indian languages (Hindi, Marathi, English) with voice confirmation.

```
   [ Point Camera at Blister Pack / Bottle / Drops / Syrup ]
                               │
                               ▼
        [ On-Device ML Kit OCR / Groq Multimodal Visual AI ]
                               │
                               ▼
        [ SQLite FTS5 Fast Match (<5ms) / Fuzzy Salt Matcher ]
                               │
                               ▼
          [ Clinical Safety & Pharmacology Engine ]
            ├── Evaluates Active 24h Medication History
            ├── Traps Duplicate Active Chemical Molecules
            ├── Checks Severe Drug-to-Drug Contraindications
            └── Formulates Vernacular Food/Temporal Rules
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
     [ Safe to Take ]   [ Duplicate Trap ]  [ Drug Conflict ]
            │                  │                  │
    [ Vernacular TTS ]   [ Spoken Alert ]   [ Spoken Alert ]
    [ Voice Confirm  ]   [ Caregiver SOS ]  [ Caregiver SOS ]
```

---

## ✨ Key Features

- **🛡️ Deterministic Clinical Safety Matrix:** All duplicate dose warnings, contraindications, and active salt evaluations run deterministically on the local SQLite FTS5 database before any action is confirmed.
- **👁️ Multimodal Visual AI & OCR:** Intelligently recognizes Blister Packs, Ophthalmic Eye Drops, Cough Syrups/Tonics, Topical Gels/Ointments, Inhalers, and Capsules using a hybrid edge-cloud vision pipeline.
- **📦 ~30,000 Indian Medicines Catalog:** Pre-indexed SQLite database with FTS5 virtual table covering commercial brands, salts, and manufacturers across India.
- **🎙️ Vernacular Speech & Hands-Free Confirmation:** Natural voice announcements in Hindi (`hi-IN`), Marathi (`mr-IN`), and English (`en-IN`) with speech recognition for hands-free confirmation (*"हाँ ले ली"* / *"Yes taken"*).
- **⏰ Daily Spoken Voice Alarms:** `AlarmManager` wakeup alarms that announce prescription times aloud even when the phone is locked.
- **🚨 Direct Cellular SOS Dispatcher:** Offline GSM cellular SMS automatically sent to caregivers if a critical drug interaction or duplicate dose is attempted.
- **♿ Senior Accessibility (WCAG AAA):** High-contrast palette (`#00875A` Safe, `#DE350B` Alert), 48dp+ accessible touch targets, and zero layout cutoffs.

---

## 📂 Project Structure & Architecture

```
MedVoice/
├── app/
│   ├── src/main/assets/databases/
│   │   └── medvoice_master.db          # ~30k Indian medicines SQLite FTS5 catalog
│   ├── src/main/java/com/medvoice/
│   │   ├── core/
│   │   │   ├── ai/
│   │   │   │   ├── AiPharmacologyEngine.kt     # Multimodal Vision & MedGemma engine
│   │   │   │   └── FuzzySaltMatcher.kt         # On-device fuzzy chemical matcher
│   │   │   ├── audio/
│   │   │   │   ├── VernacularTtsManager.kt     # Multi-engine TTS (Device / Sarvam / ElevenLabs)
│   │   │   │   └── VoiceConfirmationListener.kt# Hands-free speech listener
│   │   │   ├── data/local/
│   │   │   │   ├── AppDatabase.kt              # Room database master configuration
│   │   │   │   ├── dao/MedicineDao.kt          # FTS5 & log query interfaces
│   │   │   │   └── entity/Entities.kt          # MedicineEntity & MedicationLogEntity
│   │   │   ├── domain/engine/
│   │   │   │   └── SafetyEvaluationEngine.kt   # Two-tier lookup & safety pipeline
│   │   │   ├── scheduler/                      # Exact AlarmManager background scheduler
│   │   │   └── vision/
│   │   │       └── TextAnalyzer.kt             # Throttled ML Kit CameraX frame analyzer
│   │   ├── feature/
│   │   │   ├── scanner/                        # Real-time CameraX OCR scanner screen & HUD
│   │   │   ├── home/                           # Patient dashboard & schedule
│   │   │   ├── cabinet/                        # Searchable medicine catalogue & voice readout
│   │   │   ├── history/                        # Caregiver audit log screen
│   │   │   ├── settings/                       # Voice Studio & Diagnostics sandbox
│   │   │   └── onboarding/                     # 3-step accessible senior setup wizard
│   │   └── ui/                                 # Jetpack Compose high-contrast theme
│   └── test/                                   # 37 comprehensive unit tests (100% pass)
├── scripts/
│   └── compile_catalog_db.py                   # Automated CSV downloader & SQLite FTS5 compiler
├── DEMO_TEST_PACK.md                           # 2-Minute Judge Evaluation Cards
└── AGENTS.md                                   # Autonomous agent system rules & directives
```

---

## 🛠️ Tech Stack

```
Language             : Kotlin 2.0+ (Coroutines & StateFlow)
UI Toolkit           : Jetpack Compose + Material 3 (WCAG AAA High Contrast)
Target Android SDK   : Min SDK: 28 (Android 9.0) | Target SDK: 34 (Android 14)
Local Storage        : Android Jetpack Room 2.6+ with Native SQLite FTS5 (~30k medicines)
Vision Engine        : Google ML Kit On-Device Text Recognition v2 + Groq Multimodal Vision
AI Inference         : Groq llama-3.2-11b-vision-preview / MedGemma Qwen 27B + LiteRT INT4
Speech Engine        : Native Android TextToSpeech (`hi-IN`, `mr-IN`, `en-IN`) + SpeechRecognizer
SMS Safety Gateway   : Android Telephony SmsManager (Offline Direct Cellular)
```

---

## 🚀 Quick Start & Build Instructions

### Prerequisites
- Android Studio Ladybug / Koala or newer
- Android SDK 34 & JDK 17+ (e.g., Android Studio JBR)
- Python 3.10+ (for dataset compilation script)

### 1. Compile the Catalog Database
```bash
python scripts/compile_catalog_db.py
```

### 2. Run Automated Unit Tests
```bash
.\gradlew testDebugUnitTest
```

### 3. Run Android Lint Checks
```bash
.\gradlew lintDebug
```

### 4. Build & Install to Device
```bash
.\gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Open-Source Directives

MedVoice is developed under the rules outlined in [`AGENTS.md`](file:///AGENTS.md) following Conventional Commits 1.0.0 and zero-cloud edge execution standards.
