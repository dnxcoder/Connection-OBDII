package com.example.obdii_connection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class HomeScreenViewModel {

    private val _isBluetoothConnected = MutableStateFlow(false)
    private val _rpms = MutableStateFlow("Nothing")
    private val _isSocketConnected = MutableStateFlow(false)
    private val _checkEngine = MutableStateFlow("")
    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _isConnecting = MutableStateFlow(false)

    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected
    val rpms: StateFlow<String> = _rpms
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected
    val checkEngine: StateFlow<String> = _checkEngine
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices
    val isConnecting: StateFlow<Boolean> = _isConnecting

    lateinit var socket: BluetoothSocket
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

            } catch (e: Exception) {
                Log.e("devBluetooth", "Error: ${e.message}")
            } finally {
                _isConnecting.value = false
            }
        }
    }

    fun getRPMs(){
        socket.outputStream.write("010C\r".toByteArray())
        socket.outputStream.flush()

        val buffer = ByteArray(1024)
        val bytes = socket.inputStream.read(buffer)
        val response = String(buffer, 0, bytes)




        Log.d("devBluetooth", "Raw Response from ELM327: $response")
        Log.d("devBluetooth", "Response from ELM327: ${extractRPMFromResponse(response)}")
        _rpms.value = "${extractRPMFromResponse(response)}"
    }

     fun getKms(){
        socket.outputStream.write("22F190\r".toByteArray())
        socket.outputStream.flush()

        val buffer = ByteArray(1024)
        val bytes = socket.inputStream.read(buffer)
        val response = String(buffer, 0, bytes)


        Log.d("devBluetooth", "Raw Response from ELM327: $response")
        Log.d("devBluetooth", "Response from ELM327: ${extractRPMFromResponse(response)}")
        //_rpms.value = "${extractRPMFromResponse(response)}"
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




    fun extractRPMFromResponse(response: String): Int {

        val clean = response.replace("010C", "").trim()
        val parts = clean.split(" ")

        return if (parts.size >= 4 && parts[0] == "41" && parts[1] == "0C") {
            val a = parts[2].toInt(16)
            val b = parts[3].toInt(16)
            ((a * 256) + b) / 4
        } else {
            -1
        }
    }

    fun closeSocket(){
        Log.d("devBluetooth", "Closing Socket")
        socket.close()
    }

    fun toggleConnection(){
        _isSocketConnected.value = !_isSocketConnected.value
    }
}


