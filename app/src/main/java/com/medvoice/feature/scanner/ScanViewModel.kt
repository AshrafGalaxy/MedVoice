package com.medvoice.feature.scanner

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medvoice.core.ai.AiEngineTier
import com.medvoice.core.ai.MedGemmaOrchestrator
import com.medvoice.core.audio.VernacularTtsManager
import com.medvoice.core.audio.VoiceConfirmationListener
import com.medvoice.core.audio.VoiceGender
import com.medvoice.core.data.local.AppDatabase
import com.medvoice.core.data.local.entity.CabinetPrescriptionEntity
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

    data class AnalyzingSnap(
        val stageMessage: String = "Capturing high-resolution photo...",
        val sideIndex: Int = 1
    ) : ScanUiState()

    data class SafeDetected(
        val brandName: String,
        val saltName: String,
        val instructionText: String,
        val medicineId: Long = 0L,
        val saltId: Long = 0L,
        val timingRuleCode: String = "AFTER_FOOD",
        val dosageForm: String = "TABLET",
        val rawComposition: String = "",
        val sourceTier: com.medvoice.core.ai.AiEngineTier = com.medvoice.core.ai.AiEngineTier.CLOUD_MEDGEMMA_HOSTED
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

    data class ExpiredAlert(
        val brandName: String,
        val expiryDate: String,
        val alertMessage: String
    ) : ScanUiState()

    data class UnidentifiedAlert(
        val alertMessage: String,
        val clinicalReason: String
    ) : ScanUiState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val dualSideOcrManager = com.medvoice.core.vision.DualSideOcrManager()
    val aiEngine = com.medvoice.core.ai.AiPharmacologyEngine(application)
    val medGemmaOrchestrator = MedGemmaOrchestrator(application, aiEngine)
    private val safetyEngine = SafetyEvaluationEngine(db.medicineDao(), medGemmaOrchestrator)
    val ttsManager = VernacularTtsManager(application)
    val alarmScheduler = com.medvoice.core.scheduler.MedicationAlarmScheduler(application)
    private val prefs = application.getSharedPreferences("medvoice_prefs", android.content.Context.MODE_PRIVATE)

    private val _liveOcrSnippet = MutableStateFlow("")
    val liveOcrSnippet: StateFlow<String> = _liveOcrSnippet.asStateFlow()

    private val _isDailyRemindersEnabled = MutableStateFlow(prefs.getBoolean("daily_reminders_enabled", true))
    val isDailyRemindersEnabled: StateFlow<Boolean> = _isDailyRemindersEnabled.asStateFlow()

    private val _cabinetMedicines = MutableStateFlow<List<MedicineEntity>>(emptyList())
    val cabinetMedicines: StateFlow<List<MedicineEntity>> = _cabinetMedicines.asStateFlow()

    private val _cabinetPrescriptions = MutableStateFlow<List<CabinetPrescriptionEntity>>(emptyList())
    val cabinetPrescriptions: StateFlow<List<CabinetPrescriptionEntity>> = _cabinetPrescriptions.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _currentTab = MutableStateFlow(MedVoiceTab.HOME)
    val currentTab: StateFlow<MedVoiceTab> = _currentTab.asStateFlow()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _isCloudApiKeyConfigured = MutableStateFlow(aiEngine.cloudMedGemmaApiKey.isNotBlank())
    val isCloudApiKeyConfigured: StateFlow<Boolean> = _isCloudApiKeyConfigured.asStateFlow()

    private val _activeAiTier = MutableStateFlow(aiEngine.activeTier)
    val activeAiTier: StateFlow<com.medvoice.core.ai.AiEngineTier> = _activeAiTier.asStateFlow()

    private val _hardwareReport = MutableStateFlow(com.medvoice.core.ai.DeviceHardwareDetector.evaluateHardware(application))
    val hardwareReport: StateFlow<com.medvoice.core.ai.HardwareEligibilityReport> = _hardwareReport.asStateFlow()

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

    private val _caregiverPhone = MutableStateFlow(prefs.getString("caregiver_phone", "") ?: "")
    val caregiverPhone: StateFlow<String> = _caregiverPhone.asStateFlow()

    private val _patientName = MutableStateFlow(prefs.getString("patient_name", "") ?: "")
    val patientName: StateFlow<String> = _patientName.asStateFlow()

    private val _medicationLogs = MutableStateFlow<List<MedicationLogEntity>>(emptyList())
    val medicationLogs: StateFlow<List<MedicationLogEntity>> = _medicationLogs.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _prescriptionTimeSlots = MutableStateFlow(alarmScheduler.getSlotConfigs())
    val prescriptionTimeSlots: StateFlow<List<com.medvoice.core.scheduler.PrescriptionSlotConfig>> = _prescriptionTimeSlots.asStateFlow()

    fun updateSlotTime(slotId: String, hour: Int, minute: Int) {
        prefs.edit {
            putInt("alarm_slot_${slotId}_hour", hour)
            putInt("alarm_slot_${slotId}_minute", minute)
        }
        _prescriptionTimeSlots.value = alarmScheduler.getSlotConfigs()
        if (_isDailyRemindersEnabled.value) {
            alarmScheduler.scheduleRemindersForMedicines(_cabinetMedicines.value)
        }
    }

    fun toggleSlotEnabled(slotId: String, enabled: Boolean) {
        prefs.edit {
            putBoolean("alarm_slot_${slotId}_enabled", enabled)
        }
        _prescriptionTimeSlots.value = alarmScheduler.getSlotConfigs()
        if (_isDailyRemindersEnabled.value) {
            alarmScheduler.scheduleRemindersForMedicines(_cabinetMedicines.value)
        }
    }

    private var isProcessingEvaluation = false

    // Hands-Free Speech Recognizer
    private val voiceConfirmationListener = VoiceConfirmationListener(application) {
        val currentState = _uiState.value
        if (currentState is ScanUiState.SafeDetected) {
            confirmDoseTaken(
                medicineId = currentState.medicineId,
                saltId = currentState.saltId,
                brandName = currentState.brandName,
                rawComposition = currentState.rawComposition
            )
        }
    }

    init {
        // Hydrate audio & MedGemma persistent configuration
        aiEngine.cloudMedGemmaApiKey = prefs.getString("cloud_medgemma_api_key", aiEngine.cloudMedGemmaApiKey) ?: aiEngine.cloudMedGemmaApiKey
        aiEngine.cloudModelName = prefs.getString("cloud_model_name", "qwen/qwen3.8-27b") ?: "qwen/qwen3.8-27b"
        aiEngine.allowCloudPrivacyEgress = prefs.getBoolean("cloud_privacy_egress", true)
        
        val initialTier = if (aiEngine.cloudMedGemmaApiKey.isNotBlank() && aiEngine.allowCloudPrivacyEgress) {
            com.medvoice.core.ai.AiEngineTier.CLOUD_MEDGEMMA_HOSTED
        } else {
            try {
                com.medvoice.core.ai.AiEngineTier.valueOf(prefs.getString("ai_tier", "CLOUD_MEDGEMMA_HOSTED") ?: "CLOUD_MEDGEMMA_HOSTED")
            } catch (_: Exception) {
                com.medvoice.core.ai.AiEngineTier.CLOUD_MEDGEMMA_HOSTED
            }
        }
        _activeAiTier.value = initialTier
        medGemmaOrchestrator.activeTier = initialTier
        aiEngine.activeTier = initialTier

        refreshLogs()
        loadCabinetPrescriptions()
        refreshHardwareAudit()
    }

    fun loadCabinetPrescriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prescriptions = db.medicineDao().getAllCabinetPrescriptions()
                _cabinetPrescriptions.value = prescriptions

                // Also map to MedicineEntity for legacy alarm schedulers
                val mapped = prescriptions.map {
                    MedicineEntity(
                        id = it.id,
                        brandName = it.brandName,
                        rawComposition = it.rawComposition,
                        manufacturer = it.foodTimingRule,
                        dosageForm = it.dosageForm
                    )
                }
                val legacy = db.medicineDao().getCabinetMedicines()
                _cabinetMedicines.value = (mapped + legacy).distinctBy { it.brandName.lowercase(Locale.ROOT) }
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error loading cabinet prescriptions", e)
            }
        }
    }

    fun addScannedMedicineToCabinet(
        brandName: String,
        rawComposition: String,
        dosageForm: String,
        foodTimingRule: String = "AFTER_FOOD"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = CabinetPrescriptionEntity(
                    brandName = brandName.trim(),
                    rawComposition = rawComposition.trim().ifBlank { "Active Formulation" },
                    dosageForm = dosageForm,
                    foodTimingRule = foodTimingRule
                )
                db.medicineDao().insertCabinetPrescription(item)
                loadCabinetPrescriptions()
                val successMsg = if (_selectedLocale.value == "hi") "$brandName दवा पेटी में जोड़ दी गई है।" else "$brandName added to your active prescription cabinet."
                ttsManager.speak(successMsg, _selectedLocale.value)
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error adding scanned medicine to cabinet", e)
            }
        }
    }

    fun addManualMedicineToCabinet(
        brandName: String,
        rawComposition: String,
        dosageForm: String,
        foodTimingRule: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = CabinetPrescriptionEntity(
                    brandName = brandName.trim(),
                    rawComposition = rawComposition.trim().ifBlank { "Active Formulation" },
                    dosageForm = dosageForm,
                    foodTimingRule = foodTimingRule
                )
                db.medicineDao().insertCabinetPrescription(item)
                loadCabinetPrescriptions()
                val successMsg = if (_selectedLocale.value == "hi") "$brandName दवा पेटी में जोड़ दी गई है।" else "$brandName added to your active prescription cabinet."
                ttsManager.speak(successMsg, _selectedLocale.value)
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error adding manual medicine to cabinet", e)
            }
        }
    }

    fun removeCabinetPrescription(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.medicineDao().deleteCabinetPrescription(id)
                loadCabinetPrescriptions()
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error deleting cabinet prescription", e)
            }
        }
    }

    fun speakCabinetInstruction(item: CabinetPrescriptionEntity) {
        val timingText = when (item.foodTimingRule) {
            "BEFORE_FOOD" -> if (_selectedLocale.value == "hi") "भोजन से पहले लें" else "take before food"
            "EMPTY_STOMACH" -> if (_selectedLocale.value == "hi") "सुबह खाली पेट लें" else "take on empty stomach in the morning"
            "BEDTIME" -> if (_selectedLocale.value == "hi") "रात को सोने से पहले लें" else "take before bedtime"
            else -> if (_selectedLocale.value == "hi") "भोजन के बाद पानी के साथ लें" else "take after food with water"
        }
        val msg = if (_selectedLocale.value == "hi") {
            "${item.brandName}। घटक: ${item.rawComposition}। कृपया इसे $timingText।"
        } else {
            "${item.brandName}. Active salts: ${item.rawComposition}. Please $timingText."
        }
        ttsManager.speak(msg, _selectedLocale.value)
    }

    fun loadCabinetMedicines() {
        loadCabinetPrescriptions()
    }

    fun deleteMedicineFromCabinet(medicineId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.medicineDao().deleteCabinetPrescription(medicineId)
                db.medicineDao().deleteLogsForMedicine(medicineId)
                refreshLogs()
                loadCabinetPrescriptions()
            } catch (e: Exception) {
                Log.e("MedVoice_ScanVM", "Error deleting medicine logs", e)
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

    fun setCloudMedGemmaApiKey(key: String) {
        val clean = key.trim()
        aiEngine.cloudMedGemmaApiKey = clean
        prefs.edit { putString("cloud_medgemma_api_key", clean) }
        _isCloudApiKeyConfigured.value = clean.isNotBlank()
        if (clean.isNotBlank()) {
            setAiTier(com.medvoice.core.ai.AiEngineTier.CLOUD_MEDGEMMA_HOSTED)
        }
    }

    fun clearCloudMedGemmaApiKey() {
        aiEngine.cloudMedGemmaApiKey = ""
        prefs.edit { putString("cloud_medgemma_api_key", "") }
        _isCloudApiKeyConfigured.value = false
        setAiTier(com.medvoice.core.ai.AiEngineTier.ON_DEVICE_MEDGEMMA_INT4)
    }

    fun setCloudPrivacyEgress(allow: Boolean) {
        aiEngine.allowCloudPrivacyEgress = allow
        prefs.edit { putBoolean("cloud_privacy_egress", allow) }
    }

    fun setAiTier(tier: com.medvoice.core.ai.AiEngineTier) {
        medGemmaOrchestrator.activeTier = tier
        aiEngine.activeTier = tier
        _activeAiTier.value = tier
        prefs.edit { putString("ai_tier", tier.name) }
        android.util.Log.d("ScanViewModel", "AI Engine Tier set to: $tier")
    }

    fun refreshHardwareAudit() {
        _hardwareReport.value = com.medvoice.core.ai.DeviceHardwareDetector.evaluateHardware(getApplication())
    }

    fun updateCaregiverInfo(name: String, phone: String) {
        val sanitizedName = name.trim().ifBlank { "Senior Patient" }
        _patientName.value = sanitizedName
        _caregiverPhone.value = phone
        prefs.edit {
            putString("patient_name", sanitizedName)
            putString("caregiver_phone", phone)
        }
        val confirmation = if (_selectedLocale.value == "hi") "केयरगिवर और रोगी का विवरण सहेज लिया गया है।" else "Patient and caregiver details saved successfully."
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
        val currentName = _patientName.value.trim().takeIf {
            it.isNotBlank() && it != "Senior Patient"
        }
        val sampleText = if (_selectedLocale.value == "hi") {
            if (currentName != null) {
                "नमस्ते $currentName जी! मेडवॉयस आपकी सभी दवाएं समय पर और सुरक्षित रूप से लेने में मदद करेगा।"
            } else {
                "नमस्ते! मेडवॉयस आपकी सभी दवाएं समय पर और सुरक्षित रूप से लेने में मदद करेगा।"
            }
        } else {
            if (currentName != null) {
                "Hello $currentName! MedVoice will ensure all your medications are taken safely on schedule."
            } else {
                "Hello! MedVoice will ensure all your medications are taken safely on schedule."
            }
        }
        ttsManager.speak(sampleText, _selectedLocale.value)
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun snapPhoto(
        imageCapture: androidx.camera.core.ImageCapture,
        context: android.content.Context,
        sideIndex: Int = 1
    ) {
        if (_uiState.value is ScanUiState.AnalyzingSnap) return

        val stageText = if (_selectedLocale.value == "hi") {
            if (sideIndex == 1) "दवा की फोटो ले रहे हैं..." else "दूसरी तरफ की फोटो ले रहे हैं..."
        } else {
            if (sideIndex == 1) "Capturing medication photo..." else "Capturing back side photo..."
        }
        _uiState.value = ScanUiState.AnalyzingSnap(stageText, sideIndex)

        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        imageCapture.takePicture(
            executor,
            object : androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val bitmap = imageProxy.toBitmap()
                    imageProxy.close()

                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            _uiState.value = ScanUiState.AnalyzingSnap(
                                if (_selectedLocale.value == "hi") "लिखावट और घटक पढ़ रहे हैं..." else "Reading packaging and chemical salts...",
                                sideIndex
                            )

                            val capture = dualSideOcrManager.processHighResBitmap(bitmap, rotationDegrees, sideIndex)
                            val synthesizedTokens = dualSideOcrManager.getSynthesizedTokens()

                            _uiState.value = ScanUiState.AnalyzingSnap(
                                if (_selectedLocale.value == "hi") "सुरक्षा नियमों की जांच कर रहे हैं..." else "Evaluating clinical safety matrix...",
                                sideIndex
                            )

                            val result = safetyEngine.evaluateCandidateTokens(
                                tokens = synthesizedTokens,
                                locale = _selectedLocale.value,
                                isExplicitSnap = true
                            )
                            handleEvaluationResult(result)
                        } catch (e: Exception) {
                            Log.e("MedVoice_ScanVM", "Error in snapPhoto pipeline", e)
                            _uiState.value = ScanUiState.UnidentifiedAlert(
                                alertMessage = if (_selectedLocale.value == "hi") "पहचान में त्रुटि हुई। कृपया दोबारा प्रयास करें।" else "Error analyzing photo. Please try snapping again.",
                                clinicalReason = e.message ?: "Unknown processing error"
                            )
                        }
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Log.e("MedVoice_ScanVM", "ImageCapture failed", exception)
                    _uiState.value = ScanUiState.UnidentifiedAlert(
                        alertMessage = if (_selectedLocale.value == "hi") "कैमरा फोटो नहीं ले सका। कृपया दोबारा प्रयास करें।" else "Camera could not take photo. Please try again.",
                        clinicalReason = exception.message ?: "Camera capture exception"
                    )
                }
            }
        )
    }

    fun resetScanState() {
        dualSideOcrManager.clear()
        voiceConfirmationListener.stopListening()
        _isVoiceListening.value = false
        _uiState.value = ScanUiState.Scanning
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
                val result = safetyEngine.evaluateCandidateTokens(tokens, _selectedLocale.value)
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
                _uiState.value = ScanUiState.SafeDetected(
                    brandName = result.brandName,
                    saltName = result.saltName,
                    instructionText = result.instructionText,
                    medicineId = result.matchedMedicine?.id ?: 0L,
                    saltId = result.matchedMedicine?.id ?: 0L,
                    timingRuleCode = result.safetyResult.foodTimingRule.name,
                    dosageForm = result.dosageForm,
                    rawComposition = result.matchedMedicine?.rawComposition ?: result.saltName,
                    sourceTier = result.sourceTier
                )

                // Speak vernacular dosage instruction aloud, then start hands-free voice listener
                ttsManager.speak(result.instructionText, _selectedLocale.value) {
                    voiceConfirmationListener.startListening(_selectedLocale.value) { isListening ->
                        _isVoiceListening.value = isListening
                    }
                }
            }

            is SafetyEvaluationResult.DuplicateDoseBlocked -> {
                _uiState.value = ScanUiState.DuplicateAlert(
                    brandName = result.brandName,
                    saltName = result.saltName,
                    alertMessage = result.alertMessage,
                    previousBrand = result.brandName
                )

                ttsManager.speak(result.alertMessage, _selectedLocale.value)

                // Log blocked duplicate attempt
                db.medicineDao().logIntake(
                    MedicationLogEntity(
                        medicineId = result.matchedMedicine?.id ?: 0L,
                        scannedText = result.brandName,
                        parsedSalts = result.saltName,
                        status = "BLOCKED_DUPLICATE",
                        voiceConfirmed = false,
                        sosSmsDispatched = true
                    )
                )
                refreshLogs()

                // Dispatch Offline SOS SMS to Caregiver
                if (result.safetyResult.isEmergencyAlert) {
                    SmsDispatcher.sendEmergencyAlert(
                        context = getApplication(),
                        recipientPhone = _caregiverPhone.value,
                        patientName = _patientName.value,
                        scannedDrug = result.brandName,
                        conflictDetails = "DUPLICATE DOSE: ${result.clinicalReason}"
                    )
                }
            }

            is SafetyEvaluationResult.CriticalInteractionBlocked -> {
                _uiState.value = ScanUiState.ConflictAlert(
                    brandName = result.brandName,
                    conflictRisk = result.conflictRisk,
                    alertMessage = result.alertMessage,
                    severityLevel = "CRITICAL"
                )

                ttsManager.speak(result.alertMessage, _selectedLocale.value)

                // Log conflict attempt
                db.medicineDao().logIntake(
                    MedicationLogEntity(
                        medicineId = result.matchedMedicine?.id ?: 0L,
                        scannedText = result.brandName,
                        parsedSalts = result.saltName,
                        status = "BLOCKED_INTERACTION",
                        voiceConfirmed = false,
                        sosSmsDispatched = true
                    )
                )
                refreshLogs()

                // Dispatch Offline SOS SMS to Caregiver
                if (result.safetyResult.isEmergencyAlert) {
                    SmsDispatcher.sendEmergencyAlert(
                        context = getApplication(),
                        recipientPhone = _caregiverPhone.value,
                        patientName = _patientName.value,
                        scannedDrug = result.brandName,
                        conflictDetails = "CRITICAL DRUG CONFLICT: ${result.conflictRisk}"
                    )
                }
            }

            is SafetyEvaluationResult.ExpiredMedicineBlocked -> {
                _uiState.value = ScanUiState.ExpiredAlert(
                    brandName = result.brandName,
                    expiryDate = result.expiryDateString ?: "Expired",
                    alertMessage = result.alertMessage
                )

                ttsManager.speak(result.alertMessage, _selectedLocale.value)

                // Log expired drug attempt
                db.medicineDao().logIntake(
                    MedicationLogEntity(
                        medicineId = result.matchedMedicine?.id ?: 0L,
                        scannedText = result.brandName,
                        parsedSalts = result.expiryDateString ?: "EXPIRED",
                        status = "BLOCKED_EXPIRED",
                        voiceConfirmed = false,
                        sosSmsDispatched = true
                    )
                )
                refreshLogs()

                if (result.safetyResult.isEmergencyAlert) {
                    SmsDispatcher.sendEmergencyAlert(
                        context = getApplication(),
                        recipientPhone = _caregiverPhone.value,
                        patientName = _patientName.value,
                        scannedDrug = result.brandName,
                        conflictDetails = "EXPIRED DRUG DETECTED (EXP: ${result.expiryDateString}): Dose was automatically blocked."
                    )
                }
            }

            is SafetyEvaluationResult.UnidentifiedMedicineBlocked -> {
                _uiState.value = ScanUiState.UnidentifiedAlert(
                    alertMessage = result.alertMessage,
                    clinicalReason = result.clinicalReason
                )

                ttsManager.speak(result.alertMessage, _selectedLocale.value)
            }

            is SafetyEvaluationResult.NoMatchFound -> {
                // Continue scanning seamlessly without interrupting user
            }
        }
    }

    fun confirmDoseTaken(
        medicineId: Long,
        saltId: Long,
        brandName: String,
        rawComposition: String = ""
    ) {
        voiceConfirmationListener.stopListening()
        _isVoiceListening.value = false

        viewModelScope.launch(Dispatchers.IO) {
            // Auto-persist unlisted medicine into SQLite Room DB if not present
            val existing = db.medicineDao().findMedicineByFts(brandName)
            if (existing == null && _uiState.value is ScanUiState.SafeDetected) {
                val state = _uiState.value as ScanUiState.SafeDetected
                try {
                    val medEntity = MedicineEntity(
                        brandName = brandName,
                        rawComposition = if (rawComposition.isNotBlank()) rawComposition else state.saltName,
                        manufacturer = "Verified Pharmaceutical",
                        dosageForm = state.dosageForm
                    )
                    db.medicineDao().insertMedicine(medEntity)
                    loadCabinetMedicines()
                    Log.d("MedVoice_ScanVM", "Auto-persisted discovered medicine to Room DB: $brandName")
                } catch (e: Exception) {
                    Log.e("MedVoice_ScanVM", "Could not auto-persist discovered medicine", e)
                }
            }

            db.medicineDao().logIntake(
                MedicationLogEntity(
                    medicineId = medicineId,
                    scannedText = brandName,
                    parsedSalts = rawComposition.ifBlank { brandName },
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
            alarmScheduler.scheduleRemindersForMedicines(_cabinetMedicines.value)
        } else {
            listOf(101, 102, 103, 104, 201, 202, 203, 204).forEach { alarmScheduler.cancelReminder(it) }
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
