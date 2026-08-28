package com.medvoice.feature.scanner

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medvoice.core.ai.AiEngineTier
import com.medvoice.core.audio.VernacularTtsManager
import com.medvoice.core.audio.VoiceConfirmationListener
import com.medvoice.core.audio.VoiceEngineMode
import com.medvoice.core.audio.VoiceGender
import com.medvoice.core.data.local.AppDatabase
import com.medvoice.core.data.local.entity.ActiveSaltEntity
import com.medvoice.core.data.local.entity.FoodRuleEntity
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import com.medvoice.feature.navigation.MedVoiceTab
import com.medvoice.ui.util.SmsDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class ScanUiState {
    data object Scanning : ScanUiState()

    data class SafeDetected(
        val brandName: String,
        val saltName: String,
        val instructionText: String,
        val medicineId: Long,
        val saltId: Long,
        val timingRuleCode: String,
        val dosageForm: String = "TABLET"
    ) : ScanUiState()

    data class DuplicateAlert(
        val brandName: String,
        val saltName: String,
        val alertMessage: String,
        val previousBrand: String
    ) : ScanUiState()

    data class ConflictAlert(
        val brandName: String,
        val conflictRisk: String,
        val alertMessage: String,
        val severityLevel: String
    ) : ScanUiState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val aiEngine = com.medvoice.core.ai.AiPharmacologyEngine(application)
    private val safetyEngine = SafetyEvaluationEngine(db.medicineDao(), aiEngine)
    val ttsManager = VernacularTtsManager(application)
    val alarmScheduler = com.medvoice.core.scheduler.MedicationAlarmScheduler(application)
    private val prefs = application.getSharedPreferences("medvoice_prefs", android.content.Context.MODE_PRIVATE)

    private val _liveOcrSnippet = MutableStateFlow("")
    val liveOcrSnippet: StateFlow<String> = _liveOcrSnippet.asStateFlow()

    private val _isDailyRemindersEnabled = MutableStateFlow(prefs.getBoolean("daily_reminders_enabled", true))
    val isDailyRemindersEnabled: StateFlow<Boolean> = _isDailyRemindersEnabled.asStateFlow()

    private val _allMedicines = MutableStateFlow<List<com.medvoice.core.data.local.dao.MedicineQueryResult>>(emptyList())
    val allMedicines: StateFlow<List<com.medvoice.core.data.local.dao.MedicineQueryResult>> = _allMedicines.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", true))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _currentTab = MutableStateFlow(MedVoiceTab.HOME)
    val currentTab: StateFlow<MedVoiceTab> = _currentTab.asStateFlow()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _selectedLocale = MutableStateFlow(prefs.getString("selected_locale", "en") ?: "en")
    val selectedLocale: StateFlow<String> = _selectedLocale.asStateFlow()

    private val _selectedGender = MutableStateFlow(
        try {
            VoiceGender.valueOf(prefs.getString("voice_gender", "FEMALE") ?: "FEMALE")
        } catch (_: Exception) {
            VoiceGender.FEMALE
        }
    )
    val selectedGender: StateFlow<VoiceGender> = _selectedGender.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 0.88f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _engineMode = MutableStateFlow(
        try {
            VoiceEngineMode.valueOf(prefs.getString("engine_mode", "OFFLINE_DEVICE") ?: "OFFLINE_DEVICE")
        } catch (_: Exception) {
            VoiceEngineMode.OFFLINE_DEVICE
        }
    )
    val engineMode: StateFlow<VoiceEngineMode> = _engineMode.asStateFlow()

    private val _caregiverPhone = MutableStateFlow(prefs.getString("caregiver_phone", "+919876543210") ?: "+919876543210")
    val caregiverPhone: StateFlow<String> = _caregiverPhone.asStateFlow()

    private val _patientName = MutableStateFlow(prefs.getString("patient_name", "Dadi (आजी)") ?: "Dadi (आजी)")
    val patientName: StateFlow<String> = _patientName.asStateFlow()

    private val _medicationLogs = MutableStateFlow<List<MedicationLogEntity>>(emptyList())
    val medicationLogs: StateFlow<List<MedicationLogEntity>> = _medicationLogs.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private var isProcessingEvaluation = false

    // Hands-Free Speech Recognizer
    private val voiceConfirmationListener = VoiceConfirmationListener(application) {
        val currentState = _uiState.value
        if (currentState is ScanUiState.SafeDetected) {
            confirmDoseTaken(
                medicineId = currentState.medicineId,
                saltId = currentState.saltId,
                brandName = currentState.brandName
            )
        }
    }

    init {
        // Hydrate audio & AI manager persistent states
        ttsManager.selectedGender = _selectedGender.value
        ttsManager.speechRate = _speechRate.value
        ttsManager.engineMode = _engineMode.value
        ttsManager.sarvamApiKey = prefs.getString("sarvam_api_key", "") ?: ""
        ttsManager.elevenLabsApiKey = prefs.getString("elevenlabs_api_key", "") ?: ""
        aiEngine.cloudMedGemmaApiKey = prefs.getString("cloud_medgemma_api_key", "") ?: ""
        aiEngine.allowCloudPrivacyEgress = prefs.getBoolean("cloud_privacy_egress", false)
        try {
            aiEngine.activeTier = AiEngineTier.valueOf(prefs.getString("ai_tier", "ON_DEVICE_MEDGEMMA_INT4") ?: "ON_DEVICE_MEDGEMMA_INT4")
        } catch (_: Exception) {
            aiEngine.activeTier = AiEngineTier.ON_DEVICE_MEDGEMMA_INT4
        }

        refreshLogs()
        loadAllMedicines()
    }

    fun loadAllMedicines() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _allMedicines.value = db.medicineDao().getAllMedicines()
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error loading master medicines", e)
            }
        }
    }

    fun completeOnboarding() {
        prefs.edit { putBoolean("onboarding_done", true) }
        _isOnboardingCompleted.value = true
    }

    fun restartOnboarding() {
        prefs.edit { putBoolean("onboarding_done", false) }
        _isOnboardingCompleted.value = false
    }

    fun navigateToTab(tab: MedVoiceTab) {
        _currentTab.value = tab
    }

    fun setLocale(locale: String) {
        _selectedLocale.value = locale
        prefs.edit { putString("selected_locale", locale) }
    }

    fun setVoiceGender(gender: VoiceGender) {
        _selectedGender.value = gender
        ttsManager.selectedGender = gender
        prefs.edit { putString("voice_gender", gender.name) }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        ttsManager.speechRate = rate
        prefs.edit { putFloat("speech_rate", rate) }
    }

    fun setEngineMode(mode: VoiceEngineMode, sarvamKey: String = "") {
        _engineMode.value = mode
        ttsManager.engineMode = mode
        prefs.edit { putString("engine_mode", mode.name) }
        if (sarvamKey.isNotBlank()) {
            setSarvamApiKey(sarvamKey)
        }
    }

    fun setSarvamApiKey(key: String) {
        ttsManager.sarvamApiKey = key
        prefs.edit { putString("sarvam_api_key", key) }
    }

    fun setElevenLabsApiKey(key: String) {
        ttsManager.elevenLabsApiKey = key
        prefs.edit { putString("elevenlabs_api_key", key) }
    }

    fun setCloudMedGemmaApiKey(key: String) {
        aiEngine.cloudMedGemmaApiKey = key
        prefs.edit { putString("cloud_medgemma_api_key", key) }
    }

    fun setCloudPrivacyEgress(allow: Boolean) {
        aiEngine.allowCloudPrivacyEgress = allow
        prefs.edit { putBoolean("cloud_privacy_egress", allow) }
    }

    fun setAiTier(tier: AiEngineTier) {
        aiEngine.activeTier = tier
        prefs.edit { putString("ai_tier", tier.name) }
    }

    fun updateCaregiverInfo(name: String, phone: String) {
        _patientName.value = name
        _caregiverPhone.value = phone
        prefs.edit {
            putString("patient_name", name)
            putString("caregiver_phone", phone)
        }
        val confirmation = if (_selectedLocale.value == "hi") "केयरगिवर का विवरण सहेज लिया गया है।" else "Caregiver details saved successfully."
        ttsManager.speak(confirmation, _selectedLocale.value)
    }

    fun testEmergencySms(): Boolean {
        val success = SmsDispatcher.sendEmergencyAlert(
            context = getApplication(),
            recipientPhone = _caregiverPhone.value,
            patientName = _patientName.value,
            scannedDrug = "TEST_ALARM",
            conflictDetails = "This is a MedVoice test emergency SOS notification."
        )
        val alert = if (success) {
            if (_selectedLocale.value == "hi") "परीक्षण आपातकालीन एसएमएस भेज दिया गया है।" else "Test emergency SOS SMS dispatched."
        } else {
            if (_selectedLocale.value == "hi") "एसएमएस अनुमति आवश्यक है।" else "SMS permission required to send alert."
        }
        ttsManager.speak(alert, _selectedLocale.value)
        return success
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun testVoicePreview() {
        val sampleText = if (_selectedLocale.value == "hi") {
            "नमस्ते दादीजी! मेडवॉयस आपकी सभी दवाएं समय पर और सुरक्षित रूप से लेने में मदद करेगा।"
        } else {
            "Hello Dadi! MedVoice will ensure all your medications are taken safely on schedule."
        }
        ttsManager.speak(sampleText, _selectedLocale.value)
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun processOcrTokens(tokens: List<String>) {
        if (tokens.isNotEmpty()) {
            val preview = tokens.take(2).joinToString(" • ")
            if (preview.length > 50) {
                _liveOcrSnippet.value = preview.take(47) + "…"
            } else {
                _liveOcrSnippet.value = preview
            }
        }
        if (isProcessingEvaluation) return
        if (_uiState.value !is ScanUiState.Scanning) return

        viewModelScope.launch(Dispatchers.IO) {
            isProcessingEvaluation = true
            try {
                val result = safetyEngine.evaluateCandidateTokens(tokens)
                handleEvaluationResult(result)
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error evaluating tokens", e)
            } finally {
                isProcessingEvaluation = false
            }
        }
    }

    private suspend fun handleEvaluationResult(result: SafetyEvaluationResult) {
        when (result) {
            is SafetyEvaluationResult.SafeToTake -> {
                val instruction = when (_selectedLocale.value) {
                    "hi" -> result.vernacularInstructionHi
                    "mr" -> result.vernacularInstructionMr
                    else -> result.vernacularInstructionEn
                }

                _uiState.value = ScanUiState.SafeDetected(
                    brandName = result.medicine.brand_name,
                    saltName = result.medicine.salt_name,
                    instructionText = instruction,
                    medicineId = result.medicine.id,
                    saltId = result.medicine.primary_salt_id,
                    timingRuleCode = result.medicine.rule_code,
                    dosageForm = result.medicine.dosage_form
                )

                // Speak vernacular dosage instruction aloud, then start hands-free voice listener
                ttsManager.speak(instruction, _selectedLocale.value) {
                    voiceConfirmationListener.startListening(_selectedLocale.value) { isListening ->
                        _isVoiceListening.value = isListening
                    }
                }
            }

            is SafetyEvaluationResult.DuplicateDoseBlocked -> {
                val alert = when (_selectedLocale.value) {
                    "hi" -> result.spokenAlertHi
                    "mr" -> result.spokenAlertMr
                    else -> result.spokenAlertEn
                }

                _uiState.value = ScanUiState.DuplicateAlert(
                    brandName = result.medicine.brand_name,
                    saltName = result.medicine.salt_name,
                    alertMessage = alert,
                    previousBrand = result.recentLog.scannedBrandName
                )

                ttsManager.speak(alert, _selectedLocale.value)

                // Log blocked duplicate attempt
                db.medicineDao().logIntake(
                    MedicationLogEntity(
                        medicineId = result.medicine.id,
                        scannedBrandName = result.medicine.brand_name,
                        resolvedSaltId = result.medicine.primary_salt_id,
                        status = "BLOCKED_DUPLICATE",
                        voiceConfirmed = false,
                        sosSmsDispatched = true
                    )
                )
                refreshLogs()

                // Dispatch Offline SOS SMS to Caregiver
                SmsDispatcher.sendEmergencyAlert(
                    context = getApplication(),
                    recipientPhone = _caregiverPhone.value,
                    patientName = _patientName.value,
                    scannedDrug = result.medicine.brand_name,
                    conflictDetails = "DUPLICATE DOSE: Already took ${result.recentLog.scannedBrandName} (${result.medicine.salt_name})"
                )
            }

            is SafetyEvaluationResult.CriticalInteractionBlocked -> {
                val alert = when (_selectedLocale.value) {
                    "hi" -> result.conflict.spoken_warning_hi
                    "mr" -> result.conflict.spoken_warning_mr
                    else -> result.conflict.spoken_warning_en
                }

                _uiState.value = ScanUiState.ConflictAlert(
                    brandName = result.medicine.brand_name,
                    conflictRisk = result.conflict.clinical_risk_mechanism,
                    alertMessage = alert,
                    severityLevel = result.conflict.severity_level
                )

                ttsManager.speak(alert, _selectedLocale.value)

                // Log conflict attempt
                db.medicineDao().logIntake(
                    MedicationLogEntity(
                        medicineId = result.medicine.id,
                        scannedBrandName = result.medicine.brand_name,
                        resolvedSaltId = result.medicine.primary_salt_id,
                        status = "CONFLICT_WARNED",
                        voiceConfirmed = false,
                        sosSmsDispatched = true
                    )
                )
                refreshLogs()

                // Dispatch Offline SOS SMS to Caregiver
                SmsDispatcher.sendEmergencyAlert(
                    context = getApplication(),
                    recipientPhone = _caregiverPhone.value,
                    patientName = _patientName.value,
                    scannedDrug = result.medicine.brand_name,
                    conflictDetails = "CRITICAL DRUG CONFLICT: ${result.conflict.clinical_risk_mechanism}"
                )
            }

            is SafetyEvaluationResult.NoMatchFound -> {
                // Continue scanning seamlessly without interrupting user
            }
        }
    }

    fun confirmDoseTaken(medicineId: Long, saltId: Long, brandName: String) {
        voiceConfirmationListener.stopListening()
        _isVoiceListening.value = false

        viewModelScope.launch(Dispatchers.IO) {
            // Auto-persist unlisted medicine into SQLite Room DB if not present
            val existing = db.medicineDao().findMedicineByPrefix(brandName)
            if (existing == null && _uiState.value is ScanUiState.SafeDetected) {
                val state = _uiState.value as ScanUiState.SafeDetected
                try {
                    val saltEntity = ActiveSaltEntity(
                        id = saltId,
                        saltName = state.saltName,
                        therapeuticClass = "Prescription Medication",
                        maxDailyDoseMg = 2000.0,
                        halfLifeHours = 6.0,
                        activeWindowHours = 8.0,
                        vernacularSaltDescEn = state.saltName,
                        vernacularSaltDescHi = state.saltName,
                        vernacularSaltDescMr = state.saltName
                    )
                    db.medicineDao().insertSalt(saltEntity)

                    val ruleEntity = FoodRuleEntity(
                        id = if (state.timingRuleCode == "EMPTY_STOMACH") 1 else 2,
                        ruleCode = state.timingRuleCode,
                        foodRelation = "WITH_OR_AFTER_FOOD",
                        leadTimeMinutes = 0,
                        dietaryRestriction = null,
                        vernacularInstructionEn = state.instructionText,
                        vernacularInstructionHi = state.instructionText,
                        vernacularInstructionMr = state.instructionText
                    )
                    db.medicineDao().insertTimingRule(ruleEntity)

                    val medEntity = MedicineEntity(
                        id = medicineId,
                        brandName = brandName,
                        manufacturer = "Verified Pharmaceutical",
                        dosageForm = state.dosageForm,
                        strengthMg = 500.0,
                        primarySaltId = saltId,
                        secondarySaltId = null,
                        timingRuleId = ruleEntity.id,
                        isHighRisk = false,
                        vernacularUsageEn = state.instructionText,
                        vernacularUsageHi = state.instructionText,
                        vernacularUsageMr = state.instructionText
                    )
                    db.medicineDao().insertMedicine(medEntity)
                    loadAllMedicines()
                    Log.d("MedVoice_ScanVM", "Auto-persisted discovered medicine to Room DB: $brandName")
                } catch (e: Exception) {
                    Log.e("MedVoice_ScanVM", "Could not auto-persist discovered medicine", e)
                }
            }

            db.medicineDao().logIntake(
                MedicationLogEntity(
                    medicineId = medicineId,
                    scannedBrandName = brandName,
                    resolvedSaltId = saltId,
                    status = "TAKEN",
                    voiceConfirmed = true
                )
            )
            refreshLogs()

            val successMsg = if (_selectedLocale.value == "hi") "दवा सफलतापूर्वक दर्ज कर ली गई है।" else "Medication logged successfully."
            ttsManager.speak(successMsg, _selectedLocale.value)
            resetScanner()
        }
    }

    fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _medicationLogs.value = db.medicineDao().getAllLogs()
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.medicineDao().clearAllLogs()
            refreshLogs()
        }
    }

    fun triggerTestAlarm() {
        alarmScheduler.triggerInstantTestAlarm()
    }

    fun toggleDailyReminders(enabled: Boolean) {
        prefs.edit { putBoolean("daily_reminders_enabled", enabled) }
        _isDailyRemindersEnabled.value = enabled
        if (enabled) {
            alarmScheduler.scheduleAllReminders()
        } else {
            listOf(101, 102, 103, 104).forEach { alarmScheduler.cancelReminder(it) }
        }
    }

    fun resetScanner() {
        voiceConfirmationListener.stopListening()
        _isVoiceListening.value = false
        ttsManager.stop()
        _uiState.value = ScanUiState.Scanning
    }

    override fun onCleared() {
        voiceConfirmationListener.stopListening()
        ttsManager.shutdown()
    }
}
