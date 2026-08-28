# PRD.md — Product Requirement Document (DOC-01)

## Project: MedVoice (Edge Medication Safety & Ambient Voice Assistant)

---

## 1. Document Control & Executive Summary

| Attribute | Specification |
| :--- | :--- |
| **Product Name** | **MedVoice** |
| **Target Platforms** | Android (Min SDK 28 / Target SDK 34) |
| **Primary Architecture** | 100% On-Device Edge Compute (Zero-Cloud Core) |
| **Target Users** | Elderly individuals, low-literacy patients, chronic illness patients, remote caregivers |
| **Supported Locales** | Hindi (`hi-IN`), Marathi (`mr-IN`), Indian English (`en-IN`) |
| **Hardware Target** | Standard Android (ARM64-v8a) + Snapdragon NPU Acceleration (Qualcomm QNN / LiteRT) |

### Executive Summary
MedVoice is an offline, camera-and-voice-first medication safety assistant for Android. It enables elderly and low-literacy users to point their smartphone camera at any medicine blister pack, bottle, or strip to instantly hear clear, vernacular audio instructions (Hindi/Marathi) regarding dosage, active composition, food timing rules, and potential drug interactions. 

By combining real-time on-device OCR (Google ML Kit), a pre-compiled offline Indian Pharmacopeia database (~30,000+ entries via SQLite FTS5), an on-device medical language model (MedGemma / Gemma-2-2B INT4), and native speech synthesis, MedVoice detects lethal brand duplications and adverse drug interactions in under 1 second without internet access.

---

## 2. Problem Statement & Root Cause Analysis

* **Microscopic & Reflective Print:** Medicine blister packs use tiny, reflective silver/colored foil typography (4–6 pt) that is physically illegible for elderly patients with presbyopia, cataracts, or motor tremors.
* **The Vernacular Medical Gap:** While over 80% of patients in Maharashtra and northern India communicate in Marathi or Hindi, over 90% of pharmaceutical packaging is printed exclusively in English clinical terms.
* **Brand Duplication (Accidental Overdoses):** Pharmacies frequently substitute doctor-prescribed brands with equivalents (e.g., *Glycomet-SR* for *Gluconorm*). Unable to identify that both share the active molecule *Metformin*, patients take both concurrently, causing acute hypoglycemia or hypotension.
* **Temporal & Food Contradictions:** Strict clinical constraints (e.g., *Thyronorm* 30 minutes before tea; *Iron* and *Calcium* taken separately) are often forgotten after rushed clinic visits.
* **Caregiver Blindspot:** Family members working away from home have no real-time way to verify if daily doses were taken correctly without disruptive, repetitive phone calls.

---

## 3. User Personas & Core User Journeys

### Persona Profiles

#### Persona 1: Aaji / Dadi (The Primary Consumer)
* **Age:** 70 years old (Pune, Maharashtra).
* **Profile:** Diagnosed with Type-2 Diabetes and Hypertension. Speaks Marathi; struggles to read English blister packs.
* **Pain Point:** Has 4 different strips on her nightstand; terrified of mixing up her morning blood pressure pill with her evening sugar pill.
* **User Goal:** Point the phone at a strip, hear what it is in Marathi, and verbally confirm she took it.

#### Persona 2: The Working Caregiver
* **Age:** 34 years old (IT Professional, Hinjewadi).
* **Profile:** Works 9-hour shifts on a laptop away from his elderly parents.
* **Pain Point:** Constant worry that his mother missed her heart medication or took a duplicate pill.
* **User Goal:** Receive instant emergency SMS/dashboard notifications only when an anomaly, duplicate dose, or missed critical medicine occurs.

### Core Scan Flow
1. Patient holds medicine strip up to the camera view.
2. The real-time camera analyzer detects text tokens and queries the local SQLite FTS5 database in <10ms.
3. If an exact brand is found, active salts and safety constraints are retrieved immediately.
4. If an unlisted/rare brand is detected, the on-device Medical SLM (MedGemma 2B) parses the active chemical formulation line zero-shot.
5. The conflict engine evaluates active medication logs for duplicate molecules or food/timing contraindications.
6. The app speaks the dosage instructions in Marathi/Hindi via native offline TTS and prompts for spoken voice confirmation.
7. If a duplicate molecule or dangerous interaction is identified, a high-contrast visual alert triggers, accompanied by an urgent spoken warning and an automated SOS SMS to the caregiver.

---

## 4. Functional Requirements Matrix

| Module ID | Feature Name | Priority | Technical Requirement | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **FR-01** | **Live Macro-Scan OCR** | P0 (Must Have) | Real-time camera feed using CameraX + Google ML Kit Text Recognition v2. | Scans printed brand names and active salt formulas from blister packs in under 100ms with zero tap-to-focus requirement. |
| **FR-02** | **Pharmacopeia FTS5 Resolver** | P0 (Must Have) | Bundled SQLite database (30,000+ Indian drugs) with FTS5 and Levenshtein distance matching. | Resolves brand names (e.g., *Telma-H*, *Pan-D*) to generic salts with fuzzy tolerance (typos/poor lighting) in <10ms. |
| **FR-03** | **Duplicate Salt Detection** | P0 (Must Have) | Cross-reference active medication log with current scanned molecule. | Instantly triggers visual/audio alarms if the active salt matches a medicine scanned within its therapeutic active window. |
| **FR-04** | **Temporal & Food Conflict Engine** | P0 (Must Have) | Deterministic safety rule matrix. | Flags critical interactions (e.g., Calcium + Iron, NSAIDs on empty stomach, Thyroxine with dairy/caffeine). |
| **FR-05** | **Vernacular Speech Interface** | P0 (Must Have) | Android Native `TextToSpeech` (`hi-IN`, `mr-IN`) + Vosk-Android offline STT. | Speaks dosage rules in natural Hindi/Marathi; captures voice confirmation (*"Haan, le li"*, *"Ghetli"*) with 100% offline accuracy. |
| **FR-06** | **Zero-Shot Medical SLM Fallback** | P1 (Core NPU) | Quantized MedGemma 4B / Gemma-2-2B (INT4) running on Qualcomm NPU via LiteRT/QNN. | Parses unlisted or imported medicines directly from the raw composition text string zero-shot. |
| **FR-07** | **Emergency Caregiver Alert** | P1 (Should Have)| Android native `SmsManager` + Local Network WebSockets. | Dispatches automated SMS to caregiver contact if a critical contraindication, expired drug, or double dose is scanned. |
| **FR-08** | **Physical Expiry Date Parser** | P2 (Nice to Have)| Custom Regex engine targeting `EXP`, `EXPIRY`, `B.No.`, `MM/YY` print formats. | Warns the user verbally if the medicine is past its expiration date. |

---

## 5. UI/UX & Frontend Design System

### 5.1 Design Philosophy: "Zero-Cognitive-Load" & WCAG AAA Compliance
Because the primary user has declining eyesight, motor tremors, and limited smartphone familiarity, the UI avoids nested menus, small touch targets, and low-contrast palettes.

### 5.2 Color Palette (Clinical & High-Contrast)
* **Primary / Safety Green:** `#00875A` (Contrast Ratio: 7.8:1 against white)
* **Alert / Danger Red:** `#DE350B` (Contrast Ratio: 8.2:1 against white)
* **Warning Amber:** `#FF8B00` (Contrast Ratio: 5.1:1 against black)
* **Background Charcoal:** `#121212` (OLED-friendly dark mode for power saving)
* **Surface Card Dark:** `#1E1E1E` (Elevation 2dp)
* **High-Contrast Text:** `#FFFFFF` (Pure white, 21:1 contrast on dark surface)
* **Muted Subtext:** `#E0E0E0` (Large format, accessible)

### 5.3 Typography Specifications (Optimized for Vernacular Scripts)

| Text Style | Font Family | Size (sp) | Weight | Line Height | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Display Header** | *Noto Sans Devanagari* (Marathi/Hindi) / *Outfit* (English) | **32 sp** | Bold (700) | 40 sp | Critical alert messages, Salt Names |
| **Dosage Instruction** | *Noto Sans Devanagari* | **24 sp** | SemiBold (600) | 32 sp | "Take after food" actionable guidance |
| **Button Text** | *Noto Sans Devanagari* | **20 sp** | Bold (700) | 26 sp | Primary voice/tap action triggers |
| **Secondary Metadata**| *Roboto Flex* | **16 sp** | Medium (500) | 22 sp | Expiry dates, batch numbers |

### 5.4 Iconography & Visual Feedback
* **Icon Library:** **Lucide-Android** (clean, uniform 2.5dp stroke width) + **Google Material Symbols Rounded**.
* **Key Icons:**
  * `Scan` (Viewfinder overlay)
  * `Volume2` (Replay vernacular audio)
  * `CheckCircle2` (Confirm dose taken)
  * `AlertTriangle` (Drug conflict alert)
  * `Pill` (Active medication badge)
* **Touch Targets:** All clickable interactive buttons maintain a minimum bounding box of **80dp × 80dp** to accommodate motor tremors and inaccurate taps.

### 5.5 Frontend UI Framework & Third-Party Dependencies

```toml
[libraries]
# Jetpack Compose UI Suite
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version = "2024.06.00" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# Camera & Vision
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version = "1.3.4" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version = "1.3.4" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version = "1.3.4" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version = "1.3.4" }
google-mlkit-text-recognition = { group = "com.google.android.gms", name = "play-services-mlkit-text-recognition", version = "19.0.0" }

# Micro-Animations & Tactile Feedback
lottie-compose = { group = "com.airbnb.android", name = "lottie-compose", version = "6.4.1" }
lucide-icons = { group = "com.composables", name = "core", version = "1.2.0" }

# On-Device Runtimes
onnxruntime-mobile = { group = "com.microsoft.onnxruntime", name = "onnxruntime-android", version = "1.18.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version = "2.6.1" }
room-compiler = { group = "androidx.room", name = "room-compiler", version = "2.6.1" }
room-ktx = { group = "androidx.room", name = "room-ktx", version = "2.6.1" }
```

---

## 6. System Architecture & Technical Specifications

### 6.1 Performance Budgets & Latency SLAs

| Metric | Target Threshold | Maximum Acceptable | Fallback Mechanism |
| :--- | :--- | :--- | :--- |
| **OCR Scan-to-Detection** | **<80 ms** | 150 ms | Drop frame resolution to 720p |
| **FTS5 DB Query Latency** | **<5 ms** | 20 ms | Indexed in-memory SQLite table |
| **SLM Inference (First Token)**| **<120 ms** | 350 ms | Pre-compiled template string |
| **Audio Playback Latency** | **<100 ms** | 200 ms | Pre-warmed `TextToSpeech` engine |
| **APK Binary Footprint** | **<45 MB** (excl. SLM) | 60 MB | Bundled SQLite DB compressed via GZIP |
| **RAM Utilization** | **<450 MB** | 800 MB | Auto-release CameraX frame buffers |

---

## 7. Edge Cases, Failure Modes & Safety Guardrails

| Edge Case / Failure Condition | System Safety Guardrail & Mitigation Action |
| :--- | :--- |
| **Severe Foil Glare / Motion Blur** | Continuous frame confidence scoring. Requires 3 matching consecutive OCR frames before triggering audio. |
| **Partial / Torn Blister Pack** | If active salt is missing from partial brand text, app prompts: *"कृपया पट्टी थोडी पुढे करा"* (Move strip closer). |
| **Zero Internet / Airplane Mode** | 100% of OCR, FTS5 DB, Safety Rules, and TTS run in RAM; zero network connection check is ever performed. |
| **Ambiguous Brand Match (<80% match)** | App refuses to guess; falls back to raw composition reading to eliminate clinical hallucination risk. |
| **Emergency Battery / Low Light Mode** | Automatically turns on the rear camera flash (torch) when average pixel luminance falls below 30 Lux. |