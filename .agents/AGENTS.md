# AGENTS.md — Autonomous Coding Agent Rules & System Directive (DOC-04)

## Project: MedVoice (Edge Medication Safety & Ambient Voice Assistant)
## Target Environments: Autonomous Coding Agents (Antigravity, Cursor, Windsurf, Claude Dev)

---

## 1. Primary Directives & Zero-Tolerance Constraints

You are the lead systems and Android engineer building **MedVoice**, a 100% offline, privacy-first, on-device medication safety system for Android. Every code snippet, architecture decision, and dependency you introduce MUST adhere to the following non-negotiable rules:

### 1.1 The Hybrid Edge-Cloud Architecture
* **Dynamic Execution:** MedVoice uses a hybrid model. When capable NPU hardware or sufficient RAM (>6GB) is present, all OCR, SLM inference, and matching MUST execute strictly on the physical device. 
* **Cloud Fallback Permission:** When running on memory-constrained devices (e.g., older MediaTek or Snapdragon chips with <6GB RAM), the app MAY securely route OCR text parsing to Cloud-hosted AI models (e.g., Gemini Generative AI SDK) to ensure robust performance.
* **Core Safety is Offline:** Irrespective of how the text is parsed, the clinical safety matrix (contraindications, duplicate warnings) MUST always be executed deterministically on-device using the local SQLite FTS5 database.

### 1.2 Deterministic Clinical Priority Law
* **Never Hallucinate Medical Facts:** The language model (MedGemma/SLM) is strictly a fallback parser and conversational scaffolding tool.
* **Database Precedence:** All drug contraindications, salt matches, and duplicate warnings MUST be evaluated against the deterministic local SQLite FTS5 database before invoking any neural network.
* **Explicit Refusal:** If a scanned blister pack cannot be parsed with $>80\%$ confidence by either OCR/FTS5 or the SLM, the code MUST return a safe clarification prompt (e.g., *"कृपया पट्टी पुन्हा स्कॅन करा"* / *"Please scan again"*) rather than guessing.

---

## 2. Tech Stack & Platform Constraints

```
Language             : Kotlin 2.0+ (Strict Type Safety, Coroutines & Flow)
Target Android SDK   : Minimum SDK: 28 (Android 9.0) | Target SDK: 34 (Android 14)
UI Toolkit           : Jetpack Compose + Material 3 (No XML Views except CameraX PreviewView)
Architecture Pattern : Unidirectional Data Flow (UDF) / MVVM + Clean Architecture
Asynchronous Model   : Kotlin Coroutines + StateFlow / SharedFlow (Zero RxJava)
Local Storage        : Android Jetpack Room 2.6+ with Native SQLite FTS5 Extensions
Vision Engine        : Google ML Kit On-Device Text Recognition v2 (Bundled Play Services / Unbundled)
Edge AI Runtime      : Qualcomm QNN Runtime / LiteRT / ONNX Runtime Mobile v1.18+
Speech Engine        : Native Android TextToSpeech (`hi-IN`, `mr-IN`) + Vosk-Android (Offline STT)
Dependency Injection : Manual DI / Kotlin Singletons (Keep prototype simple; avoid complex Dagger/Hilt boilerplate in 24h sprint)
```

---

## 3. Project Directory & Package Structure

When generating or modifying files, strictly maintain this module structure:

```
app/src/main/java/com/medvoice/
├── MedVoiceApp.kt                      // Application subclass (Warm-up routines)
├── core/
│   ├── ai/
│   │   ├── MedGemmaRunner.kt           // LiteRT / QNN runtime wrapper
│   │   └── PromptTemplates.kt          // Zero-shot system prompts & schemas
│   ├── audio/
│   │   ├── VernacularTtsManager.kt     // Android TTS engine (Marathi/Hindi)
│   │   └── VoskSpeechRecognizer.kt     // Offline voice confirmation listener
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt          // Room master DB configuration
│   │   │   ├── dao/
│   │   │   │   └── MedicineDao.kt      // FTS5 & Conflict query interfaces
│   │   │   └── entity/
│   │   │       ├── MedicineEntity.kt
│   │   │       ├── ActiveSaltEntity.kt
│   │   │       └── MedicationLogEntity.kt
│   │   └── repository/
│   │       └── MedicineRepository.kt   // Data mediation & search coordination
│   ├── domain/
│   │   ├── model/
│   │   │   ├── MedicineResult.kt       // Domain UI models
│   │   │   └── SafetyWarning.kt
│   │   └── engine/
│   │       ├── ConflictEngine.kt       // Deterministic clinical safety matrix
│   │       └── ExpiryParser.kt         // Regex date extraction
│   └── vision/
│       ├── CameraXManager.kt           // Lifecycle-bound CameraX controller
│       └── TextAnalyzer.kt             // ML Kit frame analyzer & throttler
├── feature/
│   ├── scanner/
│   │   ├── ScanViewModel.kt            // StateFlow UDF orchestrator
│   │   ├── ScanScreen.kt               // High-contrast Compose scanner view
│   │   └── components/
│   │       ├── CameraPreview.kt        // AndroidView PreviewView wrapper
│   │       ├── HighContrastOverlay.kt  // Dynamic bounding boxes & banners
│   │       └── LargeActionButton.kt    // Accessible 80dp touch buttons
│   └── history/
│       └── CaregiverAuditScreen.kt     // Daily medication log view
└── ui/
    ├── theme/
    │   ├── Color.kt                    // High-contrast clinical palette
    │   ├── Type.kt                     // Noto Sans Devanagari typography
    │   └── Theme.kt
    └── util/
        └── SmsDispatcher.kt            // Direct cellular SOS SMS trigger
```

---

## 4. Coding Standards & Concurrency Conventions

### 4.1 Coroutines & Thread Allocation
* **`Dispatchers.Main`:** Exclusively for Compose state emission and triggering TTS audio. Never run database queries, regex loops, or OCR parsing on this thread.
* **`Dispatchers.IO`:** All Room database reads/writes, SQLite FTS5 queries, asset copying, and file I/O.
* **`Dispatchers.Default`:** ML Kit token normalization, regex string matching, string distance algorithms (Levenshtein), and data formatting.
* **Dedicated Single-Thread Executors:** CameraX analysis frames MUST run on a dedicated `Executors.newSingleThreadExecutor()`.

```kotlin
// CORRECT: Safe frame analysis and proxy lifecycle
class TextAnalyzer(
    private val onTextExtracted: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        // Throttle to 8 FPS (every 125ms) to preserve CPU/battery
        if (currentTimestamp - lastAnalyzedTimestamp < 125L) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotBlank()) {
                        onTextExtracted(visionText.text)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("TextAnalyzer", "OCR Processing failed", error)
                }
                .addOnCompleteListener {
                    // CRITICAL: Always close proxy to prevent camera pipeline deadlocks
                    imageProxy.close()
                    lastAnalyzedTimestamp = currentTimestamp
                }
        } else {
            imageProxy.close()
        }
    }
}
```

---

## 5. UI/UX & Accessibility Implementation Rules

### 5.1 WCAG AAA & Senior Accessibility Laws
* **Minimum Touch Dimensions:** Every clickable Compose component must have `Modifier.sizeIn(minWidth = 80.dp, minHeight = 80.dp)`.
* **Zero Low-Contrast Colors:** Never use light grays on white or dark grays on black. Always use `#FFFFFF` on `#121212` or `#1E1E1E`.
* **Dynamic Feedback Colors:**
  * Safe / Success: `#00875A`
  * Critical Alert / Blocked Dose: `#DE350B`
  * Warning / Check Food: `#FF8B00`
* **Haptic Feedback:** Every state transition (Safe Scanned, Duplicate Blocked, Confirmed) MUST trigger immediate hardware haptic feedback via `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`.

```kotlin
// CORRECT: Accessible Large Action Button Component
@Composable
fun AccessibleActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}
```

---

## 6. Error Handling, Edge Cases & Fallback Protocols

```
+---------------------------------------------------------------------------------------------------+
|                                      GRACEFUL FALLBACK PIPELINE                                   |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  Scanned OCR String ──► SQLite FTS5 Match Found?                                                  |
|                                │                                                                  |
|               ┌────────────────┴────────────────┐                                                 |
|               ▼ (Yes: <5ms)                     ▼ (No: Unlisted Brand)                            |
|     [ Direct Local DB Rule ]         [ Execute MedGemma INT4 via QNN ]                            |
|                                                 │                                                 |
|                                ┌────────────────┴────────────────┐                                |
|                                ▼ (Success: Valid JSON)           ▼ (Timeout >350ms / Crash)       |
|                      [ Extract Active Salt ]         [ Fallback: Regex Chemical Tokenizer ]       |
|                                │                                 │                                |
|                                └────────────────┬────────────────┘                                |
|                                                 ▼                                                 |
|                                     [ Evaluate Conflict Engine ]                                  |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

### 6.1 Strict Fallback Rules
1. **NPU/SLM Failure Safety Net:** If the quantized model fails to initialize, encounters an out-of-memory error, or takes $>350\text{ ms}$, abort the neural inference immediately and fall back to the deterministic `Regex Chemical Tokenizer`.
2. **Database Read Guard:** Wrap all database calls with coroutine `runCatching {}` blocks. Never let an uncaught SQL syntax or FTS token error crash the UI.
3. **Camera Permission Denial:** If camera permission is denied, do not show a blank screen. Display a full-screen accessible banner in Marathi and Hindi with a single 80dp button that opens the system app settings.

---

## 7. Direct Cellular SMS Dispatch (Offline Guardrail)

When dispatching emergency SMS for critical drug interactions or duplicate doses:

```kotlin
package com.medvoice.ui.util

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

object SmsDispatcher {
    fun sendEmergencyAlert(
        context: Context,
        recipientPhone: String,
        patientName: String,
        scannedDrug: String,
        conflictDetails: String
    ): Boolean {
        return try {
            if (recipientPhone.isBlank()) return false
            val smsManager = context.getSystemService(SmsManager::class.java)
            val message = "🚨 [MEDVOICE ALERT] $patientName scanned '$scannedDrug'. " +
                    "WARNING: $conflictDetails. Dose was automatically blocked."
            
            smsManager.sendTextMessage(recipientPhone, null, message, null, null)
            Log.d("SmsDispatcher", "Emergency alert SMS successfully dispatched to $recipientPhone")
            true
        } catch (e: Exception) {
            Log.e("SmsDispatcher", "Failed to dispatch emergency SMS", e)
            false
        }
    }
}
```

---

## 8. Agent Code Generation Protocol

When generating code for any MedVoice component:
1. **Write complete, self-contained files:** Never use placeholder comments like `// TODO: Implement later` or `// ... rest of code`. Provide production-ready, compilable Kotlin code.
2. **Include all required imports explicitly:** Never use wildcard imports (e.g., `import androidx.compose.material3.*`).
3. **Verify Jetpack Compose state safety:** Always use `remember` or derive state via `collectAsStateWithLifecycle()` to prevent recomposition loops.
4. **Enforce clean logging:** Use Android `Log.d("MedVoice_<Module>", ...)` tags for edge tracing.

---

## 9. Git Commit & Repository Maintenance Protocols (DOC-08)

All autonomous coding agents and maintainers MUST strictly adhere to standard professional open-source Git practices:

### 9.1 Conventional Commits Specification
All commit messages must follow the standard **Conventional Commits 1.0.0** format:
```
<type>(<scope>): <short imperative description in present tense>

[optional body providing technical context and rationale]

[optional footer(s) referencing issue IDs or breaking changes]
```

#### Allowed Types:
* **`feat`**: A new user-facing or platform capability (e.g., `feat(vision): integrate throttled ML Kit text analyzer`)
* **`fix`**: A bug fix or clinical edge case mitigation (e.g., `fix(engine): correct active window duplicate threshold calculation`)
* **`docs`**: Documentation updates (e.g., `docs: update ARCHITECTURE.md with SQLite FTS5 PRAGMAs`)
* **`build`**: Build configuration, Gradle changes, or dependency version updates (e.g., `build(deps): bump Room runtime to 2.6.1`)
* **`test`**: Adding or refactoring unit and instrumentation tests (e.g., `test(engine): add contraindication matrix unit test suite`)
* **`refactor`**: Code refactoring with zero behavioral changes (e.g., `refactor(audio): decouple TTS utterance callbacks`)
* **`perf`**: Performance optimizations (e.g., `perf(database): enable SQLite memory mapped I/O PRAGMA`)
* **`chore`**: Maintenance tasks, gitignore updates, or repository scaffolding.

### 9.2 Strict Atomic Commits Rule
* **Zero Mega-Commits:** Never batch unrelated features, bug fixes, database changes, and UI updates into a single monolithic commit.
* **Separation of Concerns:** Each logical module (Database, CameraX, UI, Audio, Domain Engine, Tests, Docs) must be staged and committed independently with a dedicated, descriptive message.

### 9.3 Repository Cleanliness & Zero-Leak Law
* **Strict `.gitignore` Enforcement:** Never commit local machine properties (`local.properties`), IDE caches (`.idea/`, `.vscode/`), Gradle caches (`.gradle/`, `build/`), compiled native binaries, or Android signing keys/keystores (`*.jks`, `*.keystore`).