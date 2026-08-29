# 🛡️ MedVoice — On-Device Medication Safety & Vernacular Ambient Assistant

> **100% Offline, Privacy-First Edge AI System for Senior Medication Safety and Blister Pack Verification**

[![GitHub Release](https://img.shields.io/badge/Release-v1.0.0-00875A?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AshrafGalaxy/MedVoice/releases/tag/v1.0.0)
[![Judge Demo Pack](https://img.shields.io/badge/🧪_Judge_Demo-Test_Pack-0052CC?style=for-the-badge)](DEMO_TEST_PACK.md)
[![Build Status](https://img.shields.io/badge/Unit_Tests-37%2F37_Passed_(100%25)-brightgreen?style=for-the-badge)](app/src/test/java/com/medvoice/)

### 📲 [⬇️ Direct Download APK (v1.0.0)](https://github.com/AshrafGalaxy/MedVoice/releases/download/v1.0.0/MedVoice-v1.0.0.apk) | [🧪 2-Minute Judge Evaluation Guide](DEMO_TEST_PACK.md)

---

## 🌟 Overview

**MedVoice** is an on-device Android application designed to protect elderly patients from accidental medication errors, duplicate dosage toxicity, and dangerous drug-to-drug interactions. Operating entirely on the physical device with **Zero-Cloud Dependency**, MedVoice combines real-time CameraX OCR, an on-device SQLite FTS5 catalog (~30,000 Indian pharmaceutical brands), and the **MedGemma Medical AI Reasoning Engine** to deliver instant spoken guidance in vernacular Indian languages (Hindi, Marathi, English).

```
   [ Point Camera at Blister Pack / Bottle / Drops / Syrup ]
                               │
                               ▼
        [ On-Device ML Kit OCR Frame Analyzer @ 8 FPS ]
                               │
                               ▼
        [ SQLite FTS5 Fast Match (<5ms) / Zero-Shot Fallback ]
                               │
                               ▼
          [ MedGemma On-Device Clinical Reasoning Engine ]
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

- **⚡ 100% Edge Execution (Zero-Cloud Law):** Complete OCR, database queries, clinical reasoning, and speech synthesis run offline on the device processor/NPU. Operates seamlessly in Airplane Mode.
- **🧠 MedGemma AI-First Architecture:** Eliminates rigid rule tables. MedGemma zero-shot analyzes chemical formulations, duplicate salts, and food rules directly from raw packaging text.
- **📦 ~30,000 Indian Medicines Catalog:** Pre-indexed SQLite database with FTS5 virtual table covering commercial brands, salts, and manufacturers across India.
- **👁️ Multi-Form Packaging Support:** Universally recognizes Blister Packs, Ophthalmic Eye Drops, Cough Syrups/Tonics, Topical Gels/Ointments, Inhalers, and Capsules.
- **🎙️ Vernacular Speech & Hands-Free Confirmation:** Natural voice announcements in Hindi (`hi-IN`), Marathi (`mr-IN`), and English (`en-IN`) with on-device speech recognition for hands-free confirmation (*"हाँ ले ली"* / *"Yes taken"*).
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
│   │   │   │   └── MedGemmaOrchestrator.kt     # MedGemma clinical reasoning engine
│   │   │   ├── audio/
│   │   │   │   ├── VernacularTtsManager.kt     # Multi-engine TTS (Device / Sarvam / ElevenLabs)
│   │   │   │   └── VoiceConfirmationListener.kt# Offline hands-free speech listener
│   │   │   ├── data/local/
│   │   │   │   ├── AppDatabase.kt              # Room database master configuration
│   │   │   │   ├── dao/MedicineDao.kt          # FTS5 & log query interfaces
│   │   │   │   └── entity/Entities.kt          # MedicineEntity & MedicationLogEntity
│   │   │   ├── domain/engine/
│   │   │   │   └── SafetyEvaluationEngine.kt   # Two-tier lookup & MedGemma safety pipeline
│   │   │   ├── scheduler/                      # Exact AlarmManager background scheduler
│   │   │   └── vision/
│   │   │       └── TextAnalyzer.kt             # Throttled ML Kit CameraX frame analyzer
│   │   ├── feature/
│   │   │   ├── scanner/                        # Real-time CameraX OCR scanner screen & HUD
│   │   │   ├── home/                           # Patient dashboard & schedule
│   │   │   ├── cabinet/                        # Searchable medicine catalogue & voice readout
│   │   │   ├── history/                        # Caregiver audit log screen
│   │   │   ├── settings/                       # Voice Studio & MedGemma testing sandbox
│   │   │   └── onboarding/                     # 3-step accessible senior setup wizard
│   │   └── ui/                                 # Jetpack Compose high-contrast theme
├── docs/                                       # Comprehensive project documentation
│   ├── INDEX.md                                # Documentation master index
│   ├── PRD.md                                  # Product requirements document
│   ├── ARCHITECTURE.md                         # System architecture & edge pipeline
│   ├── CODE_SPEC.md                            # Detailed Kotlin engineering specification
│   ├── DATABASE.md                             # SQLite FTS5 database documentation
│   ├── DATASET_GUIDE.md                        # Indian medicine dataset guide
│   ├── GETTING_STARTED_MANUAL.md               # Quick start developer guide
│   ├── MANUAL_TESTING_GUIDE.md                 # Clinical & device testing manual
│   ├── TESTING_AND_DEPLOYMENT.md               # Automated testing & CI/CD checklist
│   ├── STITCH_UI_PROMPTS.md                    # Google Stitch UI generation prompt guide
│   ├── PITCH.md                                # Hackathon pitch deck script
│   └── SUBMISSION.md                           # Final submission executive summary
├── scripts/
│   └── compile_catalog_db.py                   # Automated CSV downloader & SQLite FTS5 compiler
└── AGENTS.md                                   # Autonomous agent system rules & directives
```

---

## 📖 Documentation Index

For detailed technical guides, please refer to the [`docs/`](file:///docs/) directory:

- 📋 [**Documentation Master Index**](file:///docs/INDEX.md)
- 📐 [**Product Requirements Document (PRD)**](file:///docs/PRD.md)
- 🏗️ [**System Architecture & Edge Pipeline**](file:///docs/ARCHITECTURE.md)
- 💻 [**Engineering Code Specification**](file:///docs/CODE_SPEC.md)
- 🗄️ [**SQLite FTS5 Database Manual**](file:///docs/DATABASE.md)
- 📊 [**Dataset Guide (~30,000 Indian Medicines)**](file:///docs/DATASET_GUIDE.md)
- 🚀 [**Getting Started & Developer Setup**](file:///docs/GETTING_STARTED_MANUAL.md)
- 🧪 [**Manual Testing & Device Verification Guide**](file:///docs/MANUAL_TESTING_GUIDE.md)
- 🚢 [**Testing, Lint & Deployment Guide**](file:///docs/TESTING_AND_DEPLOYMENT.md)
- 🎨 [**Google Stitch UI Prompts & Design Tokens**](file:///docs/STITCH_UI_PROMPTS.md)
- 🎤 [**Pitch Deck & Value Proposition**](file:///docs/PITCH.md)
- 🏆 [**Final Hackathon Submission Summary**](file:///docs/SUBMISSION.md)

---

## 🛠️ Tech Stack

```
Language             : Kotlin 2.0+ (Coroutines & StateFlow)
UI Toolkit           : Jetpack Compose + Material 3 (WCAG AAA High Contrast)
Target Android SDK   : Min SDK: 28 (Android 9.0) | Target SDK: 34 (Android 14)
Local Storage        : Android Jetpack Room 2.6+ with Native SQLite FTS5
Vision Engine        : Google ML Kit On-Device Text Recognition v2
Edge AI Runtime      : MedGemma Medical SLM (LiteRT INT4 / Qualcomm QNN Runtime)
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
