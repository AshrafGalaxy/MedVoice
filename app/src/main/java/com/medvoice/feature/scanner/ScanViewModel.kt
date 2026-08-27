package com.medvoice.feature.scanner

import android.app.Application
import android.util.Log
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
    private val ttsManager = VernacularTtsManager(application)

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _selectedLocale = MutableStateFlow("mr-IN") // Default: Marathi
    val selectedLocale: StateFlow<String> = _selectedLocale.asStateFlow()

    private val _medicationLogs = MutableStateFlow<List<MedicationLogEntity>>(emptyList())
    val medicationLogs: StateFlow<List<MedicationLogEntity>> = _medicationLogs.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var isProcessingEvaluation = false

    init {
        refreshLogs()
    }

    fun setLocale(locale: String) {
        _selectedLocale.value = locale
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
                    saltId = result.medicine.primary_salt_id,
                    timingRuleCode = result.medicine.rule_code
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
                    recipientPhone = "+919876543210",
                    patientName = "Aaji",
                    scannedDrug = result.medicine.brand_name,
                    conflictDetails = "DUPLICATE DOSE: Already took ${result.recentLog.scannedBrandName} (${result.medicine.salt_name})"
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
                    recipientPhone = "+919876543210",
                    patientName = "Aaji",
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

            val successMsg = if (_selectedLocale.value == "mr-IN") "नोंद झाली आहे." else "दवा दर्ज कर ली गई है।"
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
