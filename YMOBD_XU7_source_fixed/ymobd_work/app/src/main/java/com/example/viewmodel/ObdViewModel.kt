package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiDtcService
import com.example.data.models.DtcCode
import com.example.data.models.EcuConfigItem
import com.example.data.models.VehicleData
import com.example.data.obd.ConnectionState
import com.example.data.obd.EcuConfigService
import com.example.data.obd.Elm327Manager
import com.example.data.obd.IranKhodroDtcDatabase
import com.example.data.obd.TerminalLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppNavTab {
    DASHBOARD,
    DIAGNOSTICS,
    ECU_CONFIG,
    TERMINAL,
    BLUETOOTH
}

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val elmManager = Elm327Manager(application.applicationContext)

    val vehicleData: StateFlow<VehicleData> = elmManager.vehicleData
    val connectionState: StateFlow<ConnectionState> = elmManager.connectionState
    val storedDtcs: StateFlow<List<DtcCode>> = elmManager.storedDtcs
    val terminalLogs: StateFlow<List<TerminalLog>> = elmManager.terminalLogs
    val isSimulatedMode: StateFlow<Boolean> = elmManager.isSimulatedMode

    private val _currentTab = MutableStateFlow(AppNavTab.DASHBOARD)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _ecuConfigs = MutableStateFlow<List<EcuConfigItem>>(EcuConfigService.defaultConfigs)
    val ecuConfigs: StateFlow<List<EcuConfigItem>> = _ecuConfigs.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    private val _selectedDtcForAi = MutableStateFlow<DtcCode?>(null)
    val selectedDtcForAi: StateFlow<DtcCode?> = _selectedDtcForAi.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<String?>(null)
    val aiAnalysisResult: StateFlow<String?> = _aiAnalysisResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Pre-select first DTC for quick AI analysis demonstration
        viewModelScope.launch {
            val first = storedDtcs.value.firstOrNull()
            if (first != null) {
                _selectedDtcForAi.value = first
                analyzeDtc(first.code)
            }
        }
    }

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun setSimulationMode(enabled: Boolean) {
        elmManager.setSimulatedMode(enabled)
        _statusMessage.value = if (enabled) "حالت شبیه‌ساز (دمو زنده) فعال شد" else "حالت اتصال واقعی انتخاب شد"
    }

    fun connectRealDevice(device: BluetoothDevice) {
        elmManager.connectRealDevice(device)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return elmManager.getPairedDevices()
    }

    fun disconnect() {
        elmManager.disconnect()
    }

    fun scanTroubleCodes() {
        viewModelScope.launch {
            _statusMessage.value = "در حال اسکن کدهای خطای موتور و شبکه..."
            val codes = elmManager.scanTroubleCodes()
            _statusMessage.value = if (codes.isEmpty()) "هیچ کد خطایی ثبت نشده است (سیستم سالم)" else "${codes.size} کد خطا در ایسیو زیمنس یافت شد"
            if (codes.isNotEmpty()) {
                selectDtcForAi(codes.first())
            }
        }
    }

    fun clearTroubleCodes() {
        viewModelScope.launch {
            _statusMessage.value = "در حال ارسال دستور ریست خطاهای ایسیو..."
            val cleared = elmManager.clearTroubleCodes()
            if (cleared) {
                _aiAnalysisResult.value = null
                _selectedDtcForAi.value = null
                _statusMessage.value = if (isSimulatedMode.value)
                    "حالت شبیه‌ساز: فهرست نمونه پاک شد؛ خودروی واقعی تغییری نکرد"
                else "ECU پاسخ مثبت به فرمان پاک‌کردن خطا داد"
            } else {
                _statusMessage.value = "پاک‌کردن خطا تأیید نشد؛ اتصال و پاسخ واقعی ECU را بررسی کن"
            }
        }
    }

    fun selectDtcForAi(dtc: DtcCode) {
        _selectedDtcForAi.value = dtc
        analyzeDtc(dtc.code)
    }

    fun analyzeCustomDtc(code: String) {
        val found = IranKhodroDtcDatabase.findCode(code) ?: DtcCode(
            code = code.uppercase(),
            titleFa = "کد خطای $code",
            titleEn = "Diagnostic Trouble Code $code",
            descriptionFa = "کد ارسالی جهت تحلیل توسط هوش مصنوعی",
            xu7SpecificCause = "بررسی پارامترهای سیستم انژکتور زیمنس و XU7",
            severity = com.example.data.models.DtcSeverity.MEDIUM,
            symptoms = listOf("روشن شدن چراغ چک"),
            repairSteps = listOf("بررسی با دستگاه دیاگ و تست سنسورها")
        )
        _selectedDtcForAi.value = found
        analyzeDtc(code)
    }

    private fun analyzeDtc(code: String) {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            _statusMessage.value = "در حال تحلیل هوشمند کد خطای $code با Gemini AI..."
            val result = GeminiDtcService.diagnoseDtc(code)
            _aiAnalysisResult.value = result
            _isAiAnalyzing.value = false
            _statusMessage.value = "تحلیل هوش مصنوعی برای $code آماده شد"
        }
    }

    fun toggleEcuConfig(configId: String) {
        viewModelScope.launch {
            val currentList = _ecuConfigs.value
            val target = currentList.find { it.id == configId } ?: return@launch
            val newEnabled = !target.isEnabled

            if (isSimulatedMode.value) {
                _statusMessage.value = "حالت شبیه‌ساز فعال است؛ هیچ تغییری روی خودرو اعمال نشد"
                return@launch
            }
            if (!target.supportedOnElm || target.commandRoutine.isBlank()) {
                _statusMessage.value = "این قابلیت تا شناسایی دقیق ECU و تأیید مستند فرمان غیرفعال است: ${target.titleFa}"
                return@launch
            }
            if (connectionState.value !is ConnectionState.Connected) {
                _statusMessage.value = "ابتدا باید ارتباط واقعی با ECU برقرار شود"
                return@launch
            }

            _statusMessage.value = "در حال ارسال فرمان به ECU: ${target.titleFa}..."
            // Enter a standard KWP diagnostic session first. If this ECU does not
            // support the session request, the ECU response is shown instead of
            // pretending the configuration was written.
            val sessionResponse = elmManager.sendRawCommand("10 81")
            val response = if (sessionResponse.uppercase().replace(" ", "").contains("50 81".replace(" ", ""))) {
                elmManager.sendRawCommand(target.commandRoutine)
            } else {
                sessionResponse
            }
            val upper = response.uppercase()
            val failed = response.isBlank() || listOf(
                "ERROR", "NO DATA", "UNABLE TO CONNECT", "BUS INIT", "CAN ERROR", "STOPPED"
            ).any { upper.contains(it) } || upper.replace(" ", "").contains("7F")

            if (failed) {
                _statusMessage.value = "ECU فرمان را نپذیرفت؛ پاسخ: ${response.ifBlank { "بدون پاسخ" }}"
                return@launch
            }

            _ecuConfigs.value = currentList.map { item ->
                if (item.id == configId) {
                    item.copy(
                        isEnabled = newEnabled,
                        currentValueDisplay = if (newEnabled) "فعال (تأیید شده توسط ECU)" else "غیرفعال (تأیید شده توسط ECU)"
                    )
                } else item
            }
            _statusMessage.value = "ECU پاسخ مثبت داد؛ تغییر اعمال شد"
        }
    }

    fun sendTerminalCommand(command: String) {
        viewModelScope.launch {
            elmManager.sendRawCommand(command)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
