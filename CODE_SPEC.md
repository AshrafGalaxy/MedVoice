# CODE_SPEC.md — Core Implementation Code Specification (DOC-05)

## Project: MedVoice (Edge Medication Safety & Ambient Voice Assistant)

---

## 1. Project Dependencies & Build Configuration

### 1.1 `gradle/libs.versions.toml`
```toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.3"
activityCompose = "1.9.0"
composeBom = "2024.06.00"
cameraX = "1.3.4"
mlkitText = "19.0.0"
room = "2.6.1"
coroutines = "1.8.1"
ksp = "2.0.0-1.0.21"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }

# CameraX & ML Kit
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "cameraX" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraX" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraX" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraX" }
google-mlkit-text = { group = "com.google.android.gms", name = "play-services-mlkit-text-recognition", version.ref = "mlkitText" }

# Room SQLite
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 1.2 `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.medvoice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.medvoice"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.mlkit.text)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
}
```

---

## 2. Android Manifest & Permissions

### `app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)">

    <!-- Camera Hardware & Flash -->
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
    <uses-feature android:name="android.hardware.camera.flash" android:required="false" />

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:name=".MedVoiceApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## 3. Vision & Frame Analysis Pipeline

### 3.1 `app/src/main/java/com/medvoice/core/vision/TextAnalyzer.kt`
```kotlin
package com.medvoice.core.vision

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextAnalyzer(
    private val onTextDetected: (List<String>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        
        // Rate-limit frame analysis to ~8 FPS (every 120ms) to conserve battery/prevent thermal throttling
        if (currentTimestamp - lastAnalyzedTimestamp < 120L) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text.trim() }
                    }.filter { it.isNotBlank() && it.length >= 3 }

                    if (lines.isNotEmpty()) {
                        onTextDetected(lines)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("TextAnalyzer", "OCR extraction failed", error)
                }
                .addOnCompleteListener {
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

## 4. Local Database & Repository Layer

### 4.1 `app/src/main/java/com/medvoice/core/data/local/AppDatabase.kt`
```kotlin
package com.medvoice.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.ActiveSaltEntity
import com.medvoice.core.data.local.entity.FoodRuleEntity
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import com.medvoice.core.data.local.entity.SaltContraindicationEntity

@Database(
    entities = [
        MedicineEntity::class,
        ActiveSaltEntity::class,
        FoodRuleEntity::class,
        SaltContraindicationEntity::class,
        MedicationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medvoice_master.db"
                )
                .createFromAsset("databases/medvoice_master.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 4.2 `app/src/main/java/com/medvoice/core/data/local/entity/Entities.kt`
```kotlin
package com.medvoice.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "brand_name") val brandName: String,
    @ColumnInfo(name = "manufacturer") val manufacturer: String?,
    @ColumnInfo(name = "dosage_form") val dosageForm: String,
    @ColumnInfo(name = "strength_mg") val strengthMg: Double,
    @ColumnInfo(name = "primary_salt_id") val primarySaltId: Long,
    @ColumnInfo(name = "secondary_salt_id") val secondarySaltId: Long?,
    @ColumnInfo(name = "timing_rule_id") val timingRuleId: Long,
    @ColumnInfo(name = "is_high_risk") val isHighRisk: Boolean,
    @ColumnInfo(name = "vernacular_usage_hi") val vernacularUsageHi: String,
    @ColumnInfo(name = "vernacular_usage_mr") val vernacularUsageMr: String
)

@Entity(tableName = "active_salts")
data class ActiveSaltEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "salt_name") val saltName: String,
    @ColumnInfo(name = "therapeutic_class") val therapeuticClass: String,
    @ColumnInfo(name = "max_daily_dose_mg") val maxDailyDoseMg: Double,
    @ColumnInfo(name = "half_life_hours") val halfLifeHours: Double,
    @ColumnInfo(name = "active_window_hours") val activeWindowHours: Double,
    @ColumnInfo(name = "vernacular_salt_desc_hi") val vernacularSaltDescHi: String,
    @ColumnInfo(name = "vernacular_salt_desc_mr") val vernacularSaltDescMr: String
)

@Entity(tableName = "food_temporal_rules")
data class FoodRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_code") val ruleCode: String,
    @ColumnInfo(name = "food_relation") val foodRelation: String,
    @ColumnInfo(name = "lead_time_minutes") val leadTimeMinutes: Int,
    @ColumnInfo(name = "dietary_restriction") val dietaryRestriction: String?,
    @ColumnInfo(name = "vernacular_instruction_hi") val vernacularInstructionHi: String,
    @ColumnInfo(name = "vernacular_instruction_mr") val vernacularInstructionMr: String
)

@Entity(tableName = "salt_contraindications")
data class SaltContraindicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "salt_a_id") val saltAId: Long,
    @ColumnInfo(name = "salt_b_id") val saltBId: Long,
    @ColumnInfo(name = "severity_level") val severityLevel: String,
    @ColumnInfo(name = "clinical_risk_mechanism") val clinicalRiskMechanism: String,
    @ColumnInfo(name = "spoken_warning_hi") val spokenWarningHi: String,
    @ColumnInfo(name = "spoken_warning_mr") val spokenWarningMr: String
)

@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "medicine_id") val medicineId: Long,
    @ColumnInfo(name = "scanned_brand_name") val scannedBrandName: String,
    @ColumnInfo(name = "resolved_salt_id") val resolvedSaltId: Long,
    @ColumnInfo(name = "intake_timestamp") val intakeTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "voice_confirmed") val voiceConfirmed: Boolean = false,
    @ColumnInfo(name = "sos_sms_dispatched") val sosSmsDispatched: Boolean = false
)
```

### 4.3 `app/src/main/java/com/medvoice/core/data/local/dao/MedicineDao.kt`
```kotlin
package com.medvoice.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medvoice.core.data.local.entity.ActiveSaltEntity
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity

data class MedicineQueryResult(
    val id: Long,
    val brand_name: String,
    val dosage_form: String,
    val strength_mg: Double,
    val primary_salt_id: Long,
    val is_high_risk: Boolean,
    val vernacular_usage_hi: String,
    val vernacular_usage_mr: String,
    val salt_name: String,
    val therapeutic_class: String,
    val max_daily_dose_mg: Double,
    val active_window_hours: Double,
    val vernacular_salt_desc_hi: String,
    val vernacular_salt_desc_mr: String,
    val rule_code: String,
    val food_relation: String,
    val vernacular_instruction_hi: String,
    val vernacular_instruction_mr: String
)

data class ContraindicationResult(
    val severity_level: String,
    val clinical_risk_mechanism: String,
    val spoken_warning_hi: String,
    val spoken_warning_mr: String
)

@Dao
interface MedicineDao {

    @Query("""
        SELECT m.id, m.brand_name, m.dosage_form, m.strength_mg, m.primary_salt_id, 
               m.is_high_risk, m.vernacular_usage_hi, m.vernacular_usage_mr,
               s.salt_name, s.therapeutic_class, s.max_daily_dose_mg, s.active_window_hours,
               s.vernacular_salt_desc_hi, s.vernacular_salt_desc_mr,
               r.rule_code, r.food_relation, r.vernacular_instruction_hi, r.vernacular_instruction_mr
        FROM medicines m
        JOIN active_salts s ON m.primary_salt_id = s.id
        JOIN food_temporal_rules r ON m.timing_rule_id = r.id
        WHERE m.brand_name LIKE :query || '%'
        LIMIT 1
    """)
    suspend fun findMedicineByPrefix(query: String): MedicineQueryResult?

    @Query("""
        SELECT * FROM medication_logs
        WHERE resolved_salt_id = :saltId
        AND status = 'TAKEN'
        AND intake_timestamp >= :thresholdTime
        ORDER BY intake_timestamp DESC
        LIMIT 1
    """)
    suspend fun getRecentActiveDose(saltId: Long, thresholdTime: Long): MedicationLogEntity?

    @Query("""
        SELECT c.severity_level, c.clinical_risk_mechanism, c.spoken_warning_hi, c.spoken_warning_mr
        FROM salt_contraindications c
        JOIN medication_logs l ON (l.resolved_salt_id = c.salt_b_id OR l.resolved_salt_id = c.salt_a_id)
        WHERE (c.salt_a_id = :newSaltId OR c.salt_b_id = :newSaltId)
        AND l.status = 'TAKEN'
        AND l.intake_timestamp >= :thresholdTime
        LIMIT 1
    """)
    suspend fun checkContraindications(newSaltId: Long, thresholdTime: Long): ContraindicationResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logIntake(log: MedicationLogEntity): Long
}
```

---

## 5. Domain Safety & Resolution Engine

### 5.1 `app/src/main/java/com/medvoice/core/domain/engine/SafetyEvaluationEngine.kt`
```kotlin
package com.medvoice.core.domain.engine

import com.medvoice.core.data.local.dao.ContraindicationResult
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.dao.MedicineQueryResult
import com.medvoice.core.data.local.entity.MedicationLogEntity

sealed class SafetyEvaluationResult {
    data class SafeToTake(
        val medicine: MedicineQueryResult,
        val vernacularInstructionHi: String,
        val vernacularInstructionMr: String
    ) : SafetyEvaluationResult()

    data class DuplicateDoseBlocked(
        val medicine: MedicineQueryResult,
        val recentLog: MedicationLogEntity,
        val spokenAlertHi: String,
        val spokenAlertMr: String
    ) : SafetyEvaluationResult()

    data class CriticalInteractionBlocked(
        val medicine: MedicineQueryResult,
        val conflict: ContraindicationResult
    ) : SafetyEvaluationResult()

    data object NoMatchFound : SafetyEvaluationResult()
}

class SafetyEvaluationEngine(
    private val medicineDao: MedicineDao
) {
    suspend fun evaluateCandidateTokens(tokens: List<String>): SafetyEvaluationResult {
        // Step 1: Scan tokens against Master SQLite DB
        var matchedMedicine: MedicineQueryResult? = null
        for (token in tokens) {
            val cleanToken = token.replace(Regex("[^a-zA-Z0-9]"), "").trim()
            if (cleanToken.length >= 3) {
                matchedMedicine = medicineDao.findMedicineByPrefix(cleanToken)
                if (matchedMedicine != null) break
            }
        }

        if (matchedMedicine == null) {
            return SafetyEvaluationResult.NoMatchFound
        }

        val currentTime = System.currentTimeMillis()
        val activeWindowMillis = (matchedMedicine.active_window_hours * 3600 * 1000).toLong()
        val threshold = currentTime - activeWindowMillis

        // Step 2: Check Duplicate Active Molecule
        val recentDose = medicineDao.getRecentActiveDose(matchedMedicine.primary_salt_id, threshold)
        if (recentDose != null) {
            return SafetyEvaluationResult.DuplicateDoseBlocked(
                medicine = matchedMedicine,
                recentLog = recentDose,
                spokenAlertHi = "रुकिए! आप यह दवा पहले ही ले चुके हैं। इसे दोबारा न लें।",
                spokenAlertMr = "थांबा! तुम्ही हे औषध आधीच घेतले आहे. पुन्हा घेऊ नका."
            )
        }

        // Step 3: Check Drug-to-Drug Interaction Matrix
        val contraindication = medicineDao.checkContraindications(matchedMedicine.primary_salt_id, threshold)
        if (contraindication != null && contraindication.severity_level == "CRITICAL") {
            return SafetyEvaluationResult.CriticalInteractionBlocked(
                medicine = matchedMedicine,
                conflict = contraindication
            )
        }

        // Step 4: Validated Safe
        return SafetyEvaluationResult.SafeToTake(
            medicine = matchedMedicine,
            vernacularInstructionHi = "${matchedMedicine.vernacular_usage_hi} ${matchedMedicine.vernacular_instruction_hi}",
            vernacularInstructionMr = "${matchedMedicine.vernacular_usage_mr} ${matchedMedicine.vernacular_instruction_mr}"
        )
    }
}
```

---

## 6. Vernacular Audio Synthesis

### 6.1 `app/src/main/java/com/medvoice/core/audio/VernacularTtsManager.kt`
```kotlin
package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VernacularTtsManager(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                tts?.setSpeechRate(0.9f) // Slightly slower rate for elderly comprehension
                isInitialized = true
                onInitComplete(true)
            } else {
                Log.e("VernacularTtsManager", "TTS Initialization failed with status: $status")
                onInitComplete(false)
            }
        }
    }

    fun speak(text: String, languageCode: String = "mr-IN", onDone: () -> Unit = {}) {
        if (!isInitialized || tts == null) return

        val locale = when (languageCode) {
            "mr-IN", "mr" -> Locale("mr", "IN")
            "hi-IN", "hi" -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Hindi if Marathi voice pack is missing on device
            tts?.setLanguage(Locale("hi", "IN"))
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) {}
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MEDVOICE_UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
```

---

## 7. Presentation Layer (ViewModel & Jetpack Compose UI)

### 7.1 `app/src/main/java/com/medvoice/feature/scanner/ScanViewModel.kt`
```kotlin
package com.medvoice.feature.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medvoice.core.audio.VernacularTtsManager
import com.medvoice.core.data.local.AppDatabase
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import com.medvoice.ui.util.SmsDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    data object Scanning : ScanUiState()
    
    data class SafeDetected(
        val brandName: String,
        val saltName: String,
        val instructionText: String,
        val medicineId: Long,
        val saltId: Long
    ) : ScanUiState()

    data class DuplicateAlert(
        val brandName: String,
        val saltName: String,
        val alertMessage: String
    ) : ScanUiState()

    data class ConflictAlert(
        val brandName: String,
        val conflictRisk: String,
        val alertMessage: String
    ) : ScanUiState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val safetyEngine = SafetyEvaluationEngine(db.medicineDao())
    private val ttsManager = VernacularTtsManager(application)

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _selectedLocale = MutableStateFlow("mr-IN") // Default: Marathi
    val selectedLocale: StateFlow<String> = _selectedLocale.asStateFlow()

    private var isProcessingEvaluation = false

    fun setLocale(locale: String) {
        _selectedLocale.value = locale
    }

    fun processOcrTokens(tokens: List<String>) {
        if (isProcessingEvaluation) return
        if (_uiState.value !is ScanUiState.Scanning) return

        viewModelScope.launch(Dispatchers.IO) {
            isProcessingEvaluation = true
            val result = safetyEngine.evaluateCandidateTokens(tokens)
            
            when (result) {
                is SafetyEvaluationResult.SafeToTake -> {
                    val instruction = if (_selectedLocale.value == "mr-IN") {
                        result.vernacularInstructionMr
                    } else {
                        result.vernacularInstructionHi
                    }

                    _uiState.value = ScanUiState.SafeDetected(
                        brandName = result.medicine.brand_name,
                        saltName = result.medicine.salt_name,
                        instructionText = instruction,
                        medicineId = result.medicine.id,
                        saltId = result.medicine.primary_salt_id
                    )

                    ttsManager.speak(instruction, _selectedLocale.value)
                }

                is SafetyEvaluationResult.DuplicateDoseBlocked -> {
                    val alert = if (_selectedLocale.value == "mr-IN") {
                        result.spokenAlertMr
                    } else {
                        result.spokenAlertHi
                    }

                    _uiState.value = ScanUiState.DuplicateAlert(
                        brandName = result.medicine.brand_name,
                        saltName = result.medicine.salt_name,
                        alertMessage = alert
                    )

                    ttsManager.speak(alert, _selectedLocale.value)
                    
                    // Dispatch SOS SMS
                    SmsDispatcher.sendEmergencyAlert(
                        context = getApplication(),
                        recipientPhone = "+919876543210", // Test Caregiver Contact
                        patientName = "Aaji",
                        scannedDrug = result.medicine.brand_name,
                        conflictDetails = "DUPLICATE DOSE: Already taken in active window"
                    )
                }

                is SafetyEvaluationResult.CriticalInteractionBlocked -> {
                    val alert = if (_selectedLocale.value == "mr-IN") {
                        result.conflict.spoken_warning_mr
                    } else {
                        result.conflict.spoken_warning_hi
                    }

                    _uiState.value = ScanUiState.ConflictAlert(
                        brandName = result.medicine.brand_name,
                        conflictRisk = result.conflict.clinical_risk_mechanism,
                        alertMessage = alert
                    )

                    ttsManager.speak(alert, _selectedLocale.value)

                    // Dispatch SOS SMS
                    SmsDispatcher.sendEmergencyAlert(
                        context = getApplication(),
                        recipientPhone = "+919876543210",
                        patientName = "Aaji",
                        scannedDrug = result.medicine.brand_name,
                        conflictDetails = "CRITICAL DRUG CONFLICT: ${result.conflict.clinical_risk_mechanism}"
                    )
                }

                is SafetyEvaluationResult.NoMatchFound -> {
                    // Continue scanning seamlessly
                }
            }
            isProcessingEvaluation = false
        }
    }

    fun confirmDoseTaken(medicineId: Long, saltId: Long, brandName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.medicineDao().logIntake(
                MedicationLogEntity(
                    medicineId = medicineId,
                    scannedBrandName = brandName,
                    resolvedSaltId = saltId,
                    status = "TAKEN",
                    voiceConfirmed = true
                )
            )
            val successMsg = if (_selectedLocale.value == "mr-IN") "नोंद झाली आहे." else "दवा दर्ज कर ली गई है।"
            ttsManager.speak(successMsg, _selectedLocale.value)
            resetScanner()
        }
    }

    fun resetScanner() {
        ttsManager.stop()
        _uiState.value = ScanUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
```

### 7.2 `app/src/main/java/com/medvoice/feature/scanner/ScanScreen.kt`
```kotlin
package com.medvoice.feature.scanner

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.medvoice.core.vision.TextAnalyzer
import java.util.concurrent.Executors

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Top App Bar / Language Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MedVoice 💊",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )

                Row {
                    Button(
                        onClick = { viewModel.setLocale("mr-IN") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "mr-IN") Color(0xFF00875A) else Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("मराठी", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.setLocale("hi-IN") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "hi-IN") Color(0xFF00875A) else Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("हिंदी", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Camera Viewport with Visual Scanner Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .border(3.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, TextAnalyzer { tokens ->
                                        viewModel.processOcrTokens(tokens)
                                    })
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // High-Contrast Reticle Overlay
                Box(
                    modifier = Modifier
                        .size(280.dp, 160.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 3.dp,
                            color = when (uiState) {
                                is ScanUiState.SafeDetected -> Color(0xFF00875A)
                                is ScanUiState.DuplicateAlert, is ScanUiState.ConflictAlert -> Color(0xFFDE350B)
                                else -> Color(0xFF00B4D8)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }

            // Bottom Accessible Action Card (WCAG AAA Compliance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is ScanUiState.Scanning -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (locale == "mr-IN") "औषधाची पट्टी कॅमेऱ्यासमोर धरा..." else "दवा की पट्टी कैमरे के सामने रखें...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    is ScanUiState.SafeDetected -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF00875A), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.brandName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "घटक: ${state.saltName}",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.9f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.instructionText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.confirmDoseTaken(state.medicineId, state.saltId, state.brandName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00875A), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("घेतली / ले ली (Confirm)", color = Color(0xFF00875A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ScanUiState.DuplicateAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDE350B), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Text(
                                text = "सावधान! पुन्हा घेऊ नका",
                                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFDE350B))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("पुन्हा स्कॅन करा (Scan Next)", color = Color(0xFFDE350B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ScanUiState.ConflictAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDE350B), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Text(
                                text = "गंभीर औषध परस्परविरोध!",
                                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("समजले (Dismiss)", color = Color(0xFFDE350B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### 7.3 `app/src/main/java/com/medvoice/MainActivity.kt`
```kotlin
package com.medvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.medvoice.feature.scanner.ScanScreen
import com.medvoice.feature.scanner.ScanViewModel

class MainActivity : ComponentActivity() {

    private val scanViewModel: ScanViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        // Ready to operate
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            ScanScreen(viewModel = scanViewModel)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO
        )

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
```