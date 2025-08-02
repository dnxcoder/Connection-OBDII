package com.example.obdii_connection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HomeScreenViewModel {

    private val _isBluetoothConnected = MutableStateFlow(false)
    private val _rpms = MutableStateFlow("N/A")
    private val _speed = MutableStateFlow("N/A")
    private val _coolantTemp = MutableStateFlow("N/A")
    private val _engineLoad = MutableStateFlow("N/A")
    private val _map = MutableStateFlow("N/A")
    private val _throttle = MutableStateFlow("N/A")
    private val _voltage = MutableStateFlow("N/A")
    private val _timing = MutableStateFlow("N/A")
    private val _maf = MutableStateFlow("N/A")
    private val _afr = MutableStateFlow("N/A")
    private val _fuelStatus = MutableStateFlow("N/A")
    private val _o2 = MutableStateFlow("N/A")
    private val _isSocketConnected = MutableStateFlow(false)
    private val _checkEngine = MutableStateFlow("")
    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _isConnecting = MutableStateFlow(false)

    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected
    val rpms: StateFlow<String> = _rpms
    val speed: StateFlow<String> = _speed
    val coolantTemp: StateFlow<String> = _coolantTemp
    val engineLoad: StateFlow<String> = _engineLoad
    val map: StateFlow<String> = _map
    val throttle: StateFlow<String> = _throttle
    val voltage: StateFlow<String> = _voltage
    val timing: StateFlow<String> = _timing
    val maf: StateFlow<String> = _maf
    val afr: StateFlow<String> = _afr
    val fuelStatus: StateFlow<String> = _fuelStatus
    val o2: StateFlow<String> = _o2
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected
    val checkEngine: StateFlow<String> = _checkEngine
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices
    val isConnecting: StateFlow<Boolean> = _isConnecting

    lateinit var socket: BluetoothSocket
    private var dataJob: Job? = null
    val macAddress: String = "81:23:45:67:89:BA" // Replace with your ELM327 MAC address
    val macAdressOBDII: String = "01:23:45:67:89:BA"

    fun discoverDevices(context: Context) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return

        if (!bluetoothAdapter.isEnabled) {
            Log.e("devBluetooth", "Bluetooth is turned off")
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("devBluetooth", "Missing BLUETOOTH_CONNECT permission")
            return
        }

        _availableDevices.value = bluetoothAdapter.bondedDevices.toList()
    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        _isConnecting.value = true
        CoroutineScope(Dispatchers.IO).launch {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


            if (bluetoothAdapter == null) {
                Log.e("devBluetooth", "Bluetooth is not supported on this device")
                _isConnecting.value = false
                return@launch
            }

            if (!bluetoothAdapter.isEnabled) {
                Log.e("devBluetooth", "Bluetooth is turned off")
                _isConnecting.value = false
                return@launch
            }

            if(bluetoothAdapter.isEnabled){
                Log.d("devBluetooth", "Bluetooth is enabled")
                _isBluetoothConnected.value = true
            }

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("devBluetooth", "Missing BLUETOOTH_CONNECT permission")
                _isConnecting.value = false
                return@launch
            }else {
                Log.d("devBluetooth", "Bluetooth permission is granted")
            }

            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                socket = try {
                    Log.d("devBluetooth", "Socket granted")
                    device.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    Log.e("devBluetooth", "Failed to create socket: ${e.message}")
                    _isConnecting.value = false
                    return@launch
                }

                socket.connect()
                _isSocketConnected.value = true
                startRealTimeData()
            } catch (e: Exception) {
                Log.e("devBluetooth", "Error: ${e.message}")
            } finally {
                _isConnecting.value = false
            }
        }
    }

    private fun sendCommand(cmd: String): String {
        socket.outputStream.write("$cmd\r".toByteArray())
        socket.outputStream.flush()

        val buffer = ByteArray(1024)
        val bytes = socket.inputStream.read(buffer)
        return String(buffer, 0, bytes)
    }

    private fun extractBytes(response: String, expectedPid: String, byteCount: Int): List<Int>? {
        val clean = response.replace("\r", "").trim()
        val parts = clean.split(" ").filter { it.isNotBlank() }
        val index = parts.indexOfFirst { it.equals("41", true) }
        if (index == -1 || parts.size < index + 2 + byteCount) return null
        if (!parts[index + 1].equals(expectedPid, true)) return null
        return (0 until byteCount).map { parts[index + 2 + it].toInt(16) }
    }

    private fun parseRPM(resp: String): String {
        val bytes = extractBytes(resp, "0C", 2) ?: return "N/A"
        val rpm = (bytes[0] * 256 + bytes[1]) / 4
        return rpm.toString()
    }

    private fun parseSpeed(resp: String): String {
        val bytes = extractBytes(resp, "0D", 1) ?: return "N/A"
        return bytes[0].toString()
    }

    private fun parseCoolant(resp: String): String {
        val bytes = extractBytes(resp, "05", 1) ?: return "N/A"
        return (bytes[0] - 40).toString()
    }

    private fun parseEngineLoad(resp: String): String {
        val bytes = extractBytes(resp, "04", 1) ?: return "N/A"
        val value = bytes[0] * 100 / 255.0
        return String.format("%.1f", value)
    }

    private fun parseMap(resp: String): String {
        val bytes = extractBytes(resp, "0B", 1) ?: return "N/A"
        return bytes[0].toString()
    }

    private fun parseThrottle(resp: String): String {
        val bytes = extractBytes(resp, "11", 1) ?: return "N/A"
        val value = bytes[0] * 100 / 255.0
        return String.format("%.1f", value)
    }

    private fun parseVoltage(resp: String): String {
        val bytes = extractBytes(resp, "42", 2) ?: return "N/A"
        val value = (bytes[0] * 256 + bytes[1]) / 1000.0
        return String.format("%.2f", value)
    }

    private fun parseTiming(resp: String): String {
        val bytes = extractBytes(resp, "0E", 1) ?: return "N/A"
        val value = bytes[0] / 2.0 - 64
        return String.format("%.1f", value)
    }

    private fun parseMaf(resp: String): String {
        val bytes = extractBytes(resp, "10", 2) ?: return "N/A"
        val value = (bytes[0] * 256 + bytes[1]) / 100.0
        return String.format("%.2f", value)
    }

    private fun parseAfr(resp: String): String {
        val bytes = extractBytes(resp, "44", 2) ?: return "N/A"
        val lambda = (bytes[0] * 256 + bytes[1]) / 32768.0
        val afr = lambda * 14.7
        return String.format("%.2f", afr)
    }

    private fun parseFuelStatus(resp: String): String {
        val bytes = extractBytes(resp, "03", 2) ?: return "N/A"
        return "${bytes[0]} ${bytes[1]}"
    }

    private fun parseO2(resp: String): String {
        val bytes = extractBytes(resp, "14", 2) ?: return "N/A"
        val voltage = bytes[0] / 200.0
        val trim = bytes[1] * 100 / 255.0
        return String.format("%.2fV %.1f%%", voltage, trim)
    }

    private fun startRealTimeData() {
        if (dataJob?.isActive == true) return
        dataJob = CoroutineScope(Dispatchers.IO).launch {
            while (_isSocketConnected.value) {
                try {
                    _rpms.value = parseRPM(sendCommand("010C"))
                    _speed.value = parseSpeed(sendCommand("010D"))
                    _coolantTemp.value = parseCoolant(sendCommand("0105"))
                    _engineLoad.value = parseEngineLoad(sendCommand("0104"))
                    _map.value = parseMap(sendCommand("010B"))
                    _throttle.value = parseThrottle(sendCommand("0111"))
                    _voltage.value = parseVoltage(sendCommand("0142"))
                    _timing.value = parseTiming(sendCommand("010E"))
                    _maf.value = parseMaf(sendCommand("0110"))
                    _afr.value = parseAfr(sendCommand("0144"))
                    _fuelStatus.value = parseFuelStatus(sendCommand("0103"))
                    _o2.value = parseO2(sendCommand("0114"))
                } catch (e: Exception) {
                    Log.e("devBluetooth", "Error reading data: ${e.message}")
                    _isSocketConnected.value = false
                }
                delay(1000)
            }
        }
    }

    fun checkEngine(): Unit{
        socket.outputStream.write("0101\r".toByteArray())
        socket.outputStream.flush()

        val buffer = ByteArray(1024)
        val bytes = socket.inputStream.read(buffer)
        val response = String(buffer, 0, bytes)

        val clean = response.replace(" ", "").uppercase()
        if (!clean.startsWith("4101") || clean.length < 10) {
            Log.e("devBluetooth", "Invalid OBD-II response")
            _checkEngine.value = "Invalid OBD-II response"
        }

        val byte1 = clean.substring(4, 6).toInt(16)
        val byte2 = clean.substring(6, 8).toInt(16)
        val byte3 = clean.substring(8, 10).toInt(16)
        val byte4 = if (clean.length >= 12) clean.substring(10, 12).toInt(16) else 0

        val milOn = (byte1 and 0b10000000) != 0
        val dtcCount = byte1 and 0b01111111

        val tests = mutableListOf<String>()

        fun testStatus(byte: Int, bit: Int, name: String) {
            if ((byte shr bit) and 1 == 1) {
                tests.add("$name test NOT completed")
            } else {
                tests.add("$name test completed")
            }
        }

        testStatus(byte2, 0, "Misfire")
        testStatus(byte2, 1, "Fuel system")
        testStatus(byte2, 2, "Components")
        testStatus(byte2, 4, "Catalyst")
        testStatus(byte2, 5, "Heated Catalyst")
        testStatus(byte2, 6, "Evaporative system")
        testStatus(byte2, 7, "Secondary Air System")

        testStatus(byte3, 0, "O2 Sensor")
        testStatus(byte3, 1, "O2 Sensor Heater")
        testStatus(byte3, 2, "EGR System")

         val result = buildString {
            appendLine("MIL (Check Engine Light): ${if (milOn) "ON" else "OFF"}")
            appendLine("Number of stored DTCs: $dtcCount")
            appendLine("Emission System Test Statuses:")
            tests.forEach { appendLine(" - $it") }
        }

        Log.d("devBluetooth", result)
        _checkEngine.value = result
    }
    fun closeSocket(){
        Log.d("devBluetooth", "Closing Socket")
        socket.close()
    }

    fun toggleConnection(){
        _isSocketConnected.value = !_isSocketConnected.value
    }
}


