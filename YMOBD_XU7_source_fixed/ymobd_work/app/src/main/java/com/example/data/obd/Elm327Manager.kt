package com.example.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.models.DtcCode
import com.example.data.models.VehicleData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String, val address: String, val isSimulated: Boolean) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class TerminalLog(
    val timestamp: String,
    val command: String,
    val response: String,
    val isSuccess: Boolean = true
)

class Elm327Manager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandMutex = Mutex()
    private var pollingJob: Job? = null
    private var simulationJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()

    private val _storedDtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val storedDtcs: StateFlow<List<DtcCode>> = _storedDtcs.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _isSimulatedMode = MutableStateFlow(false)
    val isSimulatedMode: StateFlow<Boolean> = _isSimulatedMode.asStateFlow()

    init {
        // Real mode is the default. Simulation must be explicitly enabled by the user.
    }

    fun setSimulatedMode(enabled: Boolean) {
        _isSimulatedMode.value = enabled
        if (enabled) {
            disconnect()
            connectSimulated()
        } else {
            disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun connectSimulated() {
        disconnect()
        _connectionState.value = ConnectionState.Connecting

        scope.launch {
            delay(800)
            _connectionState.value = ConnectionState.Connected(
                deviceName = "OBDII (ELM327 Bluetooth Mini)",
                address = "DD:7D:F2:6E:E6:0C",
                isSimulated = true
            )
            addTerminalLog("ATZ", "ELM327 v1.5 (OBD-II Interface)")
            addTerminalLog("ATE0", "OK")
            addTerminalLog("ATSP5", "OK (ISO 14230-4 KWP Fast Init)")
            addTerminalLog("0100", "41 00 BE 3F B8 11")

            // Add sample common DTCs for XU7
            _storedDtcs.value = listOf(
                IranKhodroDtcDatabase.findCode("P0115")!!,
                IranKhodroDtcDatabase.findCode("P0505")!!
            )

            startSimulationLoop()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectRealDevice(device: BluetoothDevice) {
        disconnect()
        _isSimulatedMode.value = false
        _connectionState.value = ConnectionState.Connecting

        scope.launch {
            try {
                bluetoothAdapter?.cancelDiscovery()
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket = newSocket
                newSocket.connect()
                inputStream = newSocket.inputStream
                outputStream = newSocket.outputStream

                // Initialization runs while Connecting; it must use the real socket,
                // never the simulator as a fallback.
                val reset = transactReal("ATZ", 5000)
                if (reset.isBlank() || reset.contains("ERROR", true) ||
                    reset.contains("UNABLE TO CONNECT", true)) {
                    throw java.io.IOException("ELM327 reset failed: ${reset.ifBlank { "no response" }}")
                }
                val echo = transactReal("ATE0", 2500)
                if (echo.isBlank() || echo.contains("ERROR", true)) {
                    throw java.io.IOException("ELM327 did not acknowledge ATE0: ${echo.ifBlank { "no response" }}")
                }

                for (cmd in listOf("ATL0", "ATS0", "ATH1", "ATAT1", "ATSP0")) {
                    val response = transactReal(cmd, 2500)
                    if (response.isBlank() || response.contains("ERROR", true)) {
                        throw java.io.IOException("$cmd failed: ${response.ifBlank { "no response" }}")
                    }
                }
                val protocol = transactReal("ATDP", 3000)
                addTerminalLog("ECU protocol", protocol.ifBlank { "<NO RESPONSE>" }, protocol.isNotBlank())
                val support = transactReal("0100", 6000)
                if (!hasPositivePid00(support)) {
                    throw java.io.IOException("Adapter responded, but ECU communication was not confirmed by 0100: ${support.ifBlank { "no response" }}")
                }

                _connectionState.value = ConnectionState.Connected(
                    deviceName = device.name ?: "ELM327 Bluetooth",
                    address = device.address,
                    isSimulated = false
                )
                addTerminalLog("CONNECT", "ELM327 initialized; ECU replied to PID 0100. Protocol: ${protocol.ifBlank { "unknown" }}", true)
                startRealPollingLoop()
            } catch (e: Exception) {
                closeTransport()
                _connectionState.value = ConnectionState.Error(
                    "اتصال/مقداردهی اولیه ناموفق: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
                addTerminalLog("CONNECT", "ERROR: ${e.localizedMessage ?: e.javaClass.simpleName}", false)
            }
        }
    }

    private fun hasPositivePid00(response: String): Boolean {
        val compact = response.uppercase().replace(Regex("[^0-9A-F]"), "")
        return compact.contains("4100")
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var simSpeed = 86
            var simRpm = 2850
            var simTemp = 88
            var tempDirection = 1
            var instantLiters = 7.4f

            while (isActive) {
                delay(350)
                // Smooth organic fluctuation mimicking real driving
                val speedDelta = Random.nextInt(-2, 3)
                simSpeed = (simSpeed + speedDelta).coerceIn(40, 120)

                val rpmDelta = speedDelta * 40 + Random.nextInt(-30, 30)
                simRpm = (simRpm + rpmDelta).coerceIn(1800, 4200)

                if (Random.nextInt(0, 10) == 0) {
                    simTemp += tempDirection
                    if (simTemp >= 96) tempDirection = -1
                    if (simTemp <= 87) tempDirection = 1
                }

                instantLiters = ((simRpm / 400.0f) + (simSpeed / 25.0f) + Random.nextFloat() * 0.4f).coerceIn(4.5f, 13.5f)
                val simVoltage = 14.1f + (Random.nextInt(-1, 2) * 0.1f)
                val simMap = 32 + (simRpm / 150)

                _vehicleData.value = _vehicleData.value.copy(
                    speed = simSpeed,
                    rpm = simRpm,
                    coolantTemp = simTemp,
                    instantFuelRate = String.format("%.1f", instantLiters).toFloat(),
                    avgFuelRate = 8.2f,
                    batteryVoltage = String.format("%.1f", simVoltage).toFloat(),
                    mapPressureKpa = simMap,
                    throttlePositionPercent = (12f + (simSpeed * 0.25f)).coerceIn(10f, 65f),
                    intakeAirTemp = 31,
                    milActive = _storedDtcs.value.isNotEmpty(),
                    dtcCount = _storedDtcs.value.size
                )
            }
        }
    }

    private fun startRealPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    // Poll Speed (PID 010D)
                    val speedResp = sendRawCommand("010D")
                    val parsedSpeed = parseSpeed(speedResp)

                    // Poll RPM (PID 010C)
                    val rpmResp = sendRawCommand("010C")
                    val parsedRpm = parseRpm(rpmResp)

                    // Poll Coolant Temp (PID 0105)
                    val tempResp = sendRawCommand("0105")
                    val parsedTemp = parseCoolant(tempResp)

                    // Poll Battery Voltage (ATRV)
                    val voltResp = sendRawCommand("ATRV")
                    val parsedVolt = parseVoltage(voltResp)

                    _vehicleData.value = _vehicleData.value.copy(
                        speed = parsedSpeed ?: _vehicleData.value.speed,
                        rpm = parsedRpm ?: _vehicleData.value.rpm,
                        coolantTemp = parsedTemp ?: _vehicleData.value.coolantTemp,
                        batteryVoltage = parsedVolt ?: _vehicleData.value.batteryVoltage
                    )

                    delay(200)
                } catch (e: Exception) {
                    delay(1000)
                }
            }
        }
    }

    suspend fun sendRawCommand(command: String): String = withContext(Dispatchers.IO) {
        val commands = command.replace("\r", "\n").split("\n")
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (commands.isEmpty()) return@withContext ""
        val responses = mutableListOf<String>()
        for (cmd in commands) {
            val response = if (_isSimulatedMode.value) {
                delay(80)
                getSimulatedResponse(cmd).also {
                    addTerminalLog(cmd, it, !it.contains("ERROR", true))
                }
            } else {
                if (_connectionState.value !is ConnectionState.Connected &&
                    _connectionState.value !is ConnectionState.Connecting) {
                    addTerminalLog(cmd, "ERROR: adapter is disconnected", false)
                    return@withContext "ERROR: DISCONNECTED"
                }
                transactReal(cmd, 3000)
            }
            responses += response
            val upper = response.uppercase()
            if (upper.contains("ERROR") || upper.contains("UNABLE TO CONNECT") ||
                upper.contains("BUS INIT") || upper.contains("CAN ERROR") ||
                upper.contains("STOPPED") || upper.contains("DISCONNECTED")) break
        }
        responses.joinToString("\n")
    }

    private suspend fun transactReal(command: String, timeoutMs: Long): String =
        commandMutex.withLock {
            val activeSocket = socket
            val input = inputStream
            val output = outputStream
            if (activeSocket == null || !activeSocket.isConnected || input == null || output == null) {
                addTerminalLog(command, "ERROR: socket not connected", false)
                throw java.io.IOException("Bluetooth socket is not connected")
            }
            try {
                output.write((command.trim() + "\r").toByteArray(Charsets.US_ASCII))
                output.flush()
                val buffer = ByteArray(1024)
                val result = StringBuilder()
                val started = System.currentTimeMillis()
                var gotPrompt = false
                while (System.currentTimeMillis() - started < timeoutMs) {
                    if (!activeSocket.isConnected) throw java.io.IOException("Bluetooth socket disconnected")
                    val available = input.available()
                    if (available > 0) {
                        val count = input.read(buffer, 0, minOf(buffer.size, available))
                        if (count < 0) throw java.io.IOException("Bluetooth input stream reached EOF")
                        result.append(String(buffer, 0, count, Charsets.US_ASCII))
                        if (result.contains(">")) {
                            gotPrompt = true
                            break
                        }
                    } else {
                        delay(20)
                    }
                }
                val response = result.toString().replace(">", "").replace("\r", "\n")
                    .lines().map { it.trim() }.filter { it.isNotEmpty() && !it.equals(command, true) }
                    .joinToString(" ").trim()
                if (!gotPrompt && response.isBlank()) {
                    throw java.net.SocketTimeoutException("Timeout waiting for ELM327 prompt after $command")
                }
                addTerminalLog(command, response.ifBlank { "<NO RESPONSE>" }, gotPrompt && isEcuResponseSuccessful(response))
                if (!activeSocket.isConnected) throw java.io.IOException("Bluetooth socket disconnected after response")
                response
            } catch (e: Exception) {
                addTerminalLog(command, "ERROR: ${e.localizedMessage ?: e.javaClass.simpleName}", false)
                if (e is java.io.IOException || e is java.net.SocketException) {
                    closeTransport()
                    _connectionState.value = ConnectionState.Error(
                        "ارتباط بلوتوث قطع شد: ${e.localizedMessage ?: "خطای سوکت"}"
                    )
                }
                "ERROR: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

    private fun isEcuResponseSuccessful(response: String): Boolean {
        if (response.isBlank()) return false
        val upper = response.uppercase()
        if (listOf("ERROR", "NO DATA", "UNABLE TO CONNECT", "BUS INIT", "CAN ERROR", "STOPPED").any { upper.contains(it) }) return false
        // KWP/OBD negative response: 7F <service> <negative-response-code>
        val hex = upper.replace(" ", "").replace("\n", "")
        if (hex.contains("7F")) {
            val i = hex.indexOf("7F")
            if (i >= 0 && i + 6 <= hex.length) return false
        }
        return upper.contains("OK") || upper.any { it in 'A'..'F' || it in '0'..'9' }
    }

    private fun getSimulatedResponse(cmd: String): String {
        return when (cmd.uppercase()) {
            "ATZ" -> "ELM327 v1.5"
            "ATE0", "ATL0", "ATS0", "ATH0", "ATH1", "ATAL", "ATAT1" -> "OK"
            "ATRV" -> "14.2V"
            "ATSP0", "ATSP5" -> "OK"
            "ATDP" -> "ISO 14230-4 KWP (fast init)"
            "0100" -> "48 6B 10 41 00 BE 3F B8 11"
            "10 81" -> "50 81"
            "010C" -> "41 0C 1A F8" // ~1726 RPM
            "010D" -> "41 0D 56"    // 86 km/h
            "0105" -> "41 05 80"    // 88 °C
            "010B" -> "41 0B 23"    // 35 kPa
            "0111" -> "41 11 28"    // 15.6%
            "03" -> "43 01 15 05 05 00" // P0115, P0505
            "04" -> "44" // Cleared
            "07" -> "47 00"
            else -> if (cmd.startsWith("3B") || cmd.startsWith("31") || cmd.startsWith("14")) "OK (Routine Executed)" else "OK"
        }
    }

    suspend fun clearTroubleCodes(): Boolean = withContext(Dispatchers.IO) {
        if (_isSimulatedMode.value) {
            _storedDtcs.value = emptyList()
            _vehicleData.value = _vehicleData.value.copy(milActive = false, dtcCount = 0)
            return@withContext true
        }
        if (_connectionState.value !is ConnectionState.Connected) return@withContext false
        val response = sendRawCommand("04")
        val positive = response.replace(" ", "").contains("44", ignoreCase = true) &&
            !response.contains("ERROR", true) && !response.contains("NO DATA", true)
        if (positive) {
            _storedDtcs.value = emptyList()
            _vehicleData.value = _vehicleData.value.copy(milActive = false, dtcCount = 0)
        }
        positive
    }

    suspend fun scanTroubleCodes(): List<DtcCode> = withContext(Dispatchers.IO) {
        val resp = sendRawCommand("03")
        delay(600)
        if (_isSimulatedMode.value) {
            val list = listOf(
                IranKhodroDtcDatabase.findCode("P0115")!!,
                IranKhodroDtcDatabase.findCode("P0505")!!
            )
            _storedDtcs.value = list
            _vehicleData.value = _vehicleData.value.copy(milActive = true, dtcCount = list.size)
            return@withContext list
        }
        // In real mode, parse standard OBD response 43 XX XX
        if (resp.contains("ERROR", true) || resp.contains("NO DATA", true) ||
            resp.contains("UNABLE TO CONNECT", true) || resp.isBlank()) {
            return@withContext emptyList()
        }
        val parsedCodes = parseDtcResponse(resp)
        val result = parsedCodes.map { code ->
            IranKhodroDtcDatabase.findCode(code) ?: DtcCode(
                code = code,
                titleFa = "کد خطای $code",
                titleEn = "Diagnostic Trouble Code $code",
                descriptionFa = "شرح اختصاصی این کد در پایگاه داده برنامه موجود نیست.",
                xu7SpecificCause = "برای تعیین علت، داده‌های ECU و مستندات سازنده بررسی شود.",
                severity = com.example.data.models.DtcSeverity.MEDIUM,
                symptoms = emptyList(),
                repairSteps = listOf("کد را پیش از تعویض قطعه با اطلاعات فنی خودرو بررسی کنید.")
            )
        }
        _storedDtcs.value = result
        _vehicleData.value = _vehicleData.value.copy(milActive = result.isNotEmpty(), dtcCount = result.size)
        return@withContext result
    }

    private fun parseDtcResponse(resp: String): List<String> {
        val hex = resp.uppercase().replace(Regex("[^0-9A-F]"), "")
        val serviceIndex = hex.indexOf("43")
        if (serviceIndex < 0 || serviceIndex + 6 > hex.length) return emptyList()
        val payload = hex.substring(serviceIndex + 2)
        val result = mutableListOf<String>()
        var i = 0
        while (i + 4 <= payload.length) {
            val a = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val b = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            if (a == 0 && b == 0) break
            val family = when ((a shr 6) and 0x03) {
                0 -> 'P'; 1 -> 'C'; 2 -> 'B'; else -> 'U'
            }
            val digit = (a shr 4) and 0x03
            result += "$family$digit${a and 0x0F}${"%02X".format(b)}"
            i += 4
        }
        return result
    }

    private fun parseSpeed(resp: String): Int? {
        val clean = resp.replace(" ", "").trim()
        val idx = clean.indexOf("410D")
        if (idx != -1 && clean.length >= idx + 6) {
            val hex = clean.substring(idx + 4, idx + 6)
            return hex.toIntOrNull(16)
        }
        return null
    }

    private fun parseRpm(resp: String): Int? {
        val clean = resp.replace(" ", "").trim()
        val idx = clean.indexOf("410C")
        if (idx != -1 && clean.length >= idx + 8) {
            val hexA = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val hexB = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((hexA * 256) + hexB) / 4
        }
        return null
    }

    private fun parseCoolant(resp: String): Int? {
        val clean = resp.replace(" ", "").trim()
        val idx = clean.indexOf("4105")
        if (idx != -1 && clean.length >= idx + 6) {
            val hex = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return hex - 40
        }
        return null
    }

    private fun parseVoltage(resp: String): Float? {
        val clean = resp.replace("V", "").trim()
        return clean.toFloatOrNull()
    }

    private fun addTerminalLog(cmd: String, resp: String, success: Boolean = true) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val log = TerminalLog(time, cmd, resp, success)
        _terminalLogs.value = (listOf(log) + _terminalLogs.value).take(100)
    }

    private fun closeTransport() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    fun disconnect() {
        pollingJob?.cancel()
        simulationJob?.cancel()
        closeTransport()
        _connectionState.value = ConnectionState.Disconnected
    }
}
