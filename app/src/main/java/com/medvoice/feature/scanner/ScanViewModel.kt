package com.medvoice.feature.scanner

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medvoice.core.audio.VernacularTtsManager
import com.medvoice.core.audio.VoiceEngineMode
import com.medvoice.core.audio.VoiceGender
import com.medvoice.core.data.local.AppDatabase
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import com.medvoice.feature.navigation.MedVoiceTab
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
        val saltId: Long,
        val timingRuleCode: String
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
    private val safetyEngine = SafetyEvaluationEngine(db.medicineDao())
    val ttsManager = VernacularTtsManager(application)

    private val _currentTab = MutableStateFlow(MedVoiceTab.HOME)
    val currentTab: StateFlow<MedVoiceTab> = _currentTab.asStateFlow()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _selectedLocale = MutableStateFlow("en") // "en" or "hi"
    val selectedLocale: StateFlow<String> = _selectedLocale.asStateFlow()

    private val _selectedGender = MutableStateFlow(VoiceGender.FEMALE)
    val selectedGender: StateFlow<VoiceGender> = _selectedGender.asStateFlow()

    private val _speechRate = MutableStateFlow(0.88f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _engineMode = MutableStateFlow(VoiceEngineMode.OFFLINE_DEVICE)
    val engineMode: StateFlow<VoiceEngineMode> = _engineMode.asStateFlow()

    private val _caregiverPhone = MutableStateFlow("+919876543210")
    val caregiverPhone: StateFlow<String> = _caregiverPhone.asStateFlow()

    private val _patientName = MutableStateFlow("Dadi (आजी)")
    val patientName: StateFlow<String> = _patientName.asStateFlow()

    private val _medicationLogs = MutableStateFlow<List<MedicationLogEntity>>(emptyList())
    val medicationLogs: StateFlow<List<MedicationLogEntity>> = _medicationLogs.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private var isProcessingEvaluation = false

    init {
        refreshLogs()
    }

    fun navigateToTab(tab: MedVoiceTab) {
        _currentTab.value = tab
    }

    fun setLocale(locale: String) {
        _selectedLocale.value = locale
    }

    fun setVoiceGender(gender: VoiceGender) {
        _selectedGender.value = gender
        ttsManager.selectedGender = gender
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        ttsManager.speechRate = rate
    }

    fun setEngineMode(mode: VoiceEngineMode, sarvamKey: String = "") {
        _engineMode.value = mode
        ttsManager.engineMode = mode
        ttsManager.sarvamApiKey = sarvamKey
    }

    fun updateCaregiverInfo(name: String, phone: String) {
        _patientName.value = name
        _caregiverPhone.value = phone
        val confirmation = if (_selectedLocale.value == "hi") "केयरगिवर का विवरण सहेज लिया गया है।" else "Caregiver details saved successfully."
        ttsManager.speak(confirmation, _selectedLocale.value)
    }

    fun testEmergencySms() {
        SmsDispatcher.sendEmergencyAlert(
            context = getApplication(),
            recipientPhone = _caregiverPhone.value,
            patientName = _patientName.value,
            scannedDrug = "TEST_ALARM",
            conflictDetails = "This is a MedVoice test emergency SOS notification."
        )
        val alert = if (_selectedLocale.value == "hi") "परीक्षण आपातकालीन एसएमएस भेज दिया गया है।" else "Test emergency SOS SMS dispatched."
        ttsManager.speak(alert, _selectedLocale.value)
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun testVoicePreview() {
        val sampleText = if (_selectedLocale.value == "hi") {
            "नमस्ते! यह मेडवॉयस की भारतीय आवाज का नमूना है।"
        } else {
            "Hello! This is a preview of the MedVoice Indian voice assistant."
        }
        ttsManager.speak(sampleText, _selectedLocale.value)
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun processOcrTokens(tokens: List<String>) {
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

    fun simulateScan(brandName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = safetyEngine.evaluateCandidateTokens(listOf(brandName))
            handleEvaluationResult(result)
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
                    timingRuleCode = result.medicine.rule_code
                )

                ttsManager.speak(instruction, _selectedLocale.value)
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

    fun resetScanner() {
        ttsManager.stop()
        _uiState.value = ScanUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
