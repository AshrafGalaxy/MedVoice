# ARCHITECTURE.md — System Architecture & Technical Specification (DOC-02)

## Project: MedVoice (Edge Medication Safety & Ambient Voice Assistant)

---

## 1. High-Level Architecture Overview

MedVoice operates as a **100% local, phone-autonomous edge system**. The architecture is structured into five decoupled layers: Ingestion & Vision, Deterministic Resolution, Neural Edge Reasoning (NPU), Ambient Audio/Voice I/O, and Emergency Dispatch.

```
+---------------------------------------------------------------------------------------------------+
|                                      ANDROID DEVICE BOUNDARY                                      |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [ LAYER 1: VISION & FRAME INGESTION ]                                                            |
|  CameraX (60 FPS Stream) ──► Analyzer Throttler (8-10 FPS) ──► ML Kit On-Device OCR v2            |
|                                                                      │ (Extracted Text Tokens)    |
|                                                                      ▼                            |
|  [ LAYER 2: DETERMINISTIC RESOLUTION & NORMALIZATION ]                                             |
|  OCR Text Stream ──► Normalizer Regex ──► SQLite FTS5 Pharmacopeia (30k+ Indian Medicines)        |
|                                                  │                                                |
|                                ┌─────────────────┴──────────────────┐                             |
|                                │ Match Found?                       │                             |
|                                ▼                                    ▼                             |
|                            [ YES ]                               [ NO ]                           |
|                                │                                    │ (Unlisted Drug String)      |
|                                │                                    ▼                             |
|                                │                  [ LAYER 3: NEURAL EDGE REASONING ]              |
|                                │                  Qualcomm QNN / LiteRT Runtime (NPU Engine)      |
|                                │                  MedGemma-2B / Clinical-Gemma INT4               |
|                                │                  (Zero-Shot Chemical & Food Rule Extraction)     |
|                                │                                    │                             |
|                                └─────────────────┬──────────────────┘                             |
|                                                  ▼                                                |
|  [ LAYER 4: CLINICAL SAFETY & CONFLICT MATRIX ]                                                   |
|  Active Medication History (SQLite Room) ◄──► Cross-Interaction & Duplicate Salt Rule Engine      |
|                                                  │                                                |
|                        ┌─────────────────────────┴─────────────────────────┐                      |
|                        ▼ (Safe / Dosage Guidance)                          ▼ (Conflict / Danger)  |
|  [ LAYER 5: AMBIENT AUDIO & UI ]                         [ EMERGENCY DISPATCH ]                   |
|  • Android Native TTS (Hindi/Marathi)                    • 🚨 High-Contrast Red Warning Screen    |
|  • Vosk Offline STT (Voice Confirmation)                 • 🔊 High-Pitch Spoken Alert             |
|  • Jetpack Compose WCAG AAA Screen                       • 📲 Direct Cellular SMS (SmsManager)    |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Concurrency, Threading & Memory Architecture

```
                                  THREAD CONCURRENCY MODEL
                                  
 [ Camera Hardware Thread ]
             │ (YUV_420_888 Frames)
             ▼
 [ CameraX Analysis Executor (SingleThreadExecutor) ]
             │  • Dropping stale frames if OCR is busy
             │  • Memory Buffer Pooling (prevents GC churn)
             ▼
 [ ML Kit Vision Thread (Dispatchers.Default) ]
             │ (Extracted Tokens: List<String>)
             ▼
 [ Safety & DB Pipeline (Dispatchers.IO) ]
      ├── SQLite FTS5 Full-Text Match
      ├── Conflict & Temporal Matrix Check
      └── Room DB History Update
             │
             ├──────────────────────────────────────────────┐
             ▼ (NPU Offload)                                ▼ (UI / Audio Dispatch)
 [ Qualcomm QNN Runtime Thread ]               [ Android Main Thread (Dispatchers.Main) ]
      └── MedGemma INT4 Inference Execution          ├── Jetpack Compose State Mutex
                                                     ├── TextToSpeech Audio Playback
                                                     └── Vosk Offline STT Listener
```

### 2.1 Thread Concurrency Rules
1. **Zero Camera Blocking:** CameraX analysis executes on a dedicated background `SingleThreadExecutor`. The `analyze(ImageProxy)` callback must release the image proxy immediately (`imageProxy.close()`) to avoid stalling the camera pipeline.
2. **Deterministic Queries on `Dispatchers.IO`:** All SQLite FTS5 lookups, history inserts, and regex normalizations run strictly off the main thread.
3. **NPU Isolation:** The quantized SLM (MedGemma / Gemma-2-2B) runs on its own isolated background worker thread managed by the LiteRT/QNN runtime, communicating back via Kotlin `StateFlow`.
4. **UI Updates via Unidirectional Data Flow (UDF):** The UI layer subscribes to an immutable `MedicationScanState` exposed by the `ScanViewModel`.

### 2.2 Memory Budget Breakdown (<450 MB Total Footprint)

| Subsystem | Max Allocated RAM | Lifecycle & Eviction Policy |
| :--- | :--- | :--- |
| **CameraX Frame Buffer** | 35 MB | 2 rotating buffers (YUV format), recycled continuously |
| **ML Kit Vision Models** | 40 MB | Kept in heap; initialized at app cold-start |
| **SQLite FTS5 DB Cache** | 25 MB | In-memory page cache capped at 2,000 pages (PRAGMA cache_size = -25000) |
| **MedGemma 2B INT4 Model** | 280 MB | Mapped directly via `mmap` to NPU/DRAM memory space |
| **Compose UI & Audio Heap**| 50 MB | Standard Android JVM heap allocations |
| **Total Peak Budget** | **430 MB** | **Safe headroom under standard 4GB/6GB RAM devices** |

---

## 3. CameraX & ML Kit Vision Processing Pipeline

```
+------------------------------------------------------------------------------------+
|                         FRAME ANALYSIS & STABILIZATION FLOW                        |
+------------------------------------------------------------------------------------+
|                                                                                    |
|   ImageProxy (1280x720) ──► Luminance Check ──► Below 30 Lux? ──► Auto-Flash ON    |
|                                  │                                                 |
|                                  ▼                                                 |
|                     InputImage (Rotation 90°/270°)                                 |
|                                  │                                                 |
|                                  ▼                                                 |
|             ML Kit TextRecognition.getClient().process()                          |
|                                  │                                                 |
|                                  ▼                                                 |
|                      Raw Text Blocks Extracted                                     |
|                                  │                                                 |
|                                  ▼                                                 |
|         Temporal Multi-Frame Stabilization (Sliding Window: 3 Frames)              |
|        [Frame N-2] ∩ [Frame N-1] ∩ [Frame N] ──► Confirmed Text Candidates         |
|                                  │                                                 |
|                                  ▼                                                 |
|                      Pass to Resolver Pipeline                                     |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

### 3.1 Frame Throttling & Glare Mitigation
* **Adaptive Rate Limiting:** Camera hardware runs at 60 FPS for smooth preview, but `ImageAnalysis` analyzer drops frames to throttle ML Kit processing to **8 FPS (125ms per analysis cycle)**. This prevents CPU core thermal throttling and battery drain.
* **Temporal Confidence Accumulator:** High-contrast foil printing can cause character misreads. A candidate string is only dispatched to the safety engine if it appears with $>80\%$ string similarity across **3 consecutive analysis cycles**.

---

## 4. Master Resolution & Safety Engine Specification

```
                                RESOLUTION PIPELINE FLOW
                                
                             [ Raw OCR Text Candidates ]
                                          │
                                          ▼
                [ Step 1: Pre-Processing & Noise Filtering ]
                • Strip special chars: [^a-zA-Z0-9\.\-\s]
                • Convert to uppercase
                • Extract Dosage Tokens: Regex `\b\d+(\.\d+)?\s*(MG|MCG|GM|ML|IU)\b`
                • Extract Expiry Tokens: Regex `\b(EXP|EXPIRY|VAL)\.?\s*[:\-\/]?\s*(\d{2}[\/\-\.]\d{2,4})\b`
                                          │
                                          ▼
                [ Step 2: SQLite FTS5 Full-Text Match ]
                • Query: `SELECT * FROM medicines_fts WHERE brand_name MATCH 'QUERY*'`
                                          │
                        ┌─────────────────┴─────────────────┐
                        ▼                                   ▼
                  [ Record Found ]                   [ Record Missing ]
                        │                                   │
                        │                                   ▼
                        │                  [ Step 3: Zero-Shot SLM Parser ]
                        │                  • Feed raw composition text to MedGemma
                        │                  • Output JSON: { salt, dosage, class, rule }
                        │                                   │
                        └─────────────────┬─────────────────┘
                                          │
                                          ▼
                [ Step 4: Clinical Contraindication Matrix Evaluation ]
                • Duplicate Salt Check: `active_salt IN (SELECT active_salt FROM daily_log WHERE is_active=1)`
                • Food/Temporal Check: Check `timing_rule` against current system time (Morning/Night)
                • Severe Drug-Drug Interaction: Check `conflicting_salt_ids` against active patient profile
```

### 4.1 Safety Decision Matrix (Clinical Rules)

| Condition Detected | Severity Level | System Action | Audio Output (Hindi/Marathi) | Dispatch Action |
| :--- | :--- | :--- | :--- | :--- |
| **Identical Active Salt Already Consumed** | **CRITICAL (P0)** | Screen Flashes Red (`#DE350B`); Blocks dose confirmation | *"रुकिए! यह दवा आप पहले ही ले चुके हैं। इसे दोबारा न लें।"* | Immediate SMS Alert to Caregiver |
| **Severe Drug-to-Drug Interaction** | **CRITICAL (P0)** | Screen Flashes Red; Warns against mixing | *"चेतावनी! यह दवा आपकी दूसरी दवा के साथ नहीं ली जा सकती।"* | Immediate SMS Alert to Caregiver |
| **Expired Medicine Scanned** | **HIGH (P1)** | Screen Flashes Amber (`#FF8B00`); Rejects intake | *"सावधान! इस दवा की तारीख समाप्त हो चुकी है।"* | Log in daily audit report |
| **Food Rule Violation (e.g., NSAID on empty stomach)** | **MEDIUM (P2)** | Screen Shows Amber banner; Advises food intake | *"यह दवा खाली पेट न लें। पहले कुछ खाना खाएं।"* | None (Spoken advice only) |
| **Valid & Verified Medication** | **NORMAL (P3)** | Screen Turns Safety Green (`#00875A`); Awaits Voice OK | *"यह आपकी ब्लड प्रेशर की गोली है। एक गोली पानी के साथ लें।"* | Awaits spoken confirmation |

---

## 5. On-Device NPU / SLM Inference Architecture

```
+------------------------------------------------------------------------------------+
|                         QUALCOMM QNN / LITERET RUNTIME STACK                       |
+------------------------------------------------------------------------------------+
|                                                                                    |
|   [ Kotlin Android Application Layer ]                                             |
|        │                                                                           |
|        ▼ (JNI Interface)                                                           |
|   [ LiteRT C++ Engine / ONNX Runtime Mobile v1.19+ ]                                |
|        │                                                                           |
|        ├──► [ Execution Provider: QNN HTP (Hexagon Tensor Processor / NPU) ]       |
|        │         • INT4 Quantized Weights (W4A16)                                  |
|        │         • Sub-100ms first-token latency                                   |
|        │                                                                           |
|        └──► [ Fallback Execution Provider: XNNPACK (Multi-core ARM CPU) ]          |
|                  • Activated if NPU is unavailable / low-end device                |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

### 5.1 Zero-Shot SLM Prompt Engineering Spec
When raw text cannot be resolved via SQLite FTS5, the string is sent to the local MedGemma INT4 runtime using this strict system prompt:

```text
<start_of_turn>system
You are an offline edge clinical pharmacology engine. Analyze the provided raw pharmaceutical packaging text.
Extract active chemical salts, dosage strength, therapeutic category, and critical consumption rules.
Respond strictly in valid, compact JSON matching the schema. Do not output any conversational filler.

Schema:
{
  "brand_name": string,
  "active_salt": string,
  "dosage_mg": number,
  "timing_rule": "BEFORE_FOOD" | "AFTER_FOOD" | "WITH_FOOD" | "EMPTY_STOMACH",
  "vernacular_hi": string,
  "vernacular_mr": string,
  "is_high_risk": boolean
}
<end_of_turn>
<start_of_turn>user
Packaging Text: "Zita-Met 50/500 Tablets. Each film coated tablet contains: Sitagliptin Phosphate IP 50mg, Metformin Hydrochloride IP 500mg SR. Exp: 08/2027"
<end_of_turn>
<start_of_turn>model
{
  "brand_name": "Zita-Met 50/500",
  "active_salt": "Sitagliptin + Metformin Hydrochloride",
  "dosage_mg": 550,
  "timing_rule": "AFTER_FOOD",
  "vernacular_hi": "यह शुगर की दवाई है। इसे खाना खाने के बाद लें।",
  "vernacular_mr": "हे मधुमेहाचे औषध आहे. जेवणानंतर घ्या.",
  "is_high_risk": false
}
<end_of_turn>
```

---

## 6. Audio, Speech & Voice-Loop Subsystem

```
                                AMBIENT VOICE INTERACTION LOOP
                                
        [ Medicine Identification / Safety Decision Ready ]
                                 │
                                 ▼
        [ Android Native TextToSpeech.speak() ]
        • Locale: "mr-IN" (Marathi) or "hi-IN" (Hindi)
        • AudioAttributes: USAGE_ASSISTANCE_ACCESSIBILITY, CONTENT_TYPE_SPEECH
                                 │
                                 ▼
        [ Playback Finishes: UtteranceProgressListener.onDone() ]
                                 │
                                 ▼
        [ Trigger Vosk Offline Speech-to-Text Listener ]
        • AudioRecord (16kHz Mono PCM)
        • Grammar List: ["haan", "ghetli", "nahi", "repeat", "thamb", "yes", "taken", "ho"]
                                 │
                   ┌─────────────┴─────────────┐
                   ▼                           ▼
        [ Voice: "Ghetli" / "Haan" ]   [ Voice: "Nahi" / Silence (5s) ]
                   │                           │
                   ▼                           ▼
        [ Mark Dose as Completed ]     [ Re-prompt / Leave Pending ]
        • Update SQLite local log       • Schedule 15-min reminder
```

---

## 7. Emergency Alert & Dispatch Subsystem

### 7.1 Direct Cellular SMS Dispatch (Offline Guardrail)
When a `CRITICAL (P0)` contraindication or duplicate dose is flagged, MedVoice bypasses cloud APIs and dispatches a direct SMS using Android's native `SmsManager`.

```kotlin
// Direct Cellular SMS Protocol
fun dispatchEmergencyCaregiverAlert(
    context: Context,
    caregiverPhone: String,
    patientName: String,
    drugScanned: String,
    duplicateDrug: String
) {
    val message = "🚨 MEDVOICE ALERT: $patientName scanned $drugScanned, which conflicts with recently taken $duplicateDrug. Dose was blocked. Please verify."
    val smsManager = context.getSystemService(SmsManager::class.java)
    smsManager.sendTextMessage(caregiverPhone, null, message, null, null)
}
```

---

## 8. Graceful Degradation & Fault Tolerance Matrix

```
+--------------------------+------------------------------+------------------------------------------+
| Subsystem Failure        | Primary Engine               | Fallback Engine                          |
+--------------------------+------------------------------+------------------------------------------+
| NPU Acceleration Failure | Qualcomm QNN (NPU HTP)       | XNNPACK Multithreaded CPU Execution      |
+--------------------------+------------------------------+------------------------------------------+
| SLM Timeout (>400ms)     | MedGemma INT4 Reasoning      | Deterministic Regex + Template Database  |
+--------------------------+------------------------------+------------------------------------------+
| Offline STT Failure      | Vosk Speech Recognition     | Large High-Contrast On-Screen Touch Tap  |
+--------------------------+------------------------------+------------------------------------------+
| Marathi TTS Unsupported  | Native mr-IN Voice Pack      | hi-IN (Hindi) Fallback Voice Engine      |
+--------------------------+------------------------------+------------------------------------------+
| Blister Pack Glare       | 1-Frame OCR Extraction       | 3-Frame Temporal Intersection Consensus  |
+--------------------------+------------------------------+------------------------------------------+
```