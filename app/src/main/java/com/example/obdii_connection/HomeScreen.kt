package com.example.obdii_connection
import android.R
import android.content.Context
import androidx.compose.foundation.background
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(context: Context, viewModel: HomeScreenViewModel) {

    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val rpms by viewModel.rpms.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val coolant by viewModel.coolantTemp.collectAsState()
    val load by viewModel.engineLoad.collectAsState()
    val map by viewModel.map.collectAsState()
    val throttle by viewModel.throttle.collectAsState()
    val voltage by viewModel.voltage.collectAsState()
    val timing by viewModel.timing.collectAsState()
    val maf by viewModel.maf.collectAsState()
    val afr by viewModel.afr.collectAsState()
    val fuelStatus by viewModel.fuelStatus.collectAsState()
    val o2 by viewModel.o2.collectAsState()
    val isSocketConnected by viewModel.isSocketConnected.collectAsState()
    val checkEngine by viewModel.checkEngine.collectAsState()
    val devices by viewModel.availableDevices.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row {
            Text(text = "Bluetooth: ", color = Color.White)
            Text(text = "disconnected", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            shape = RoundedCornerShape(10.dp),
            onClick = {
                viewModel.discoverDevices(context)
                showDeviceDialog = true
            }
        ) {
            Text(text = "Connect To ELM327")
        }
        Button(
            shape = RoundedCornerShape(10.dp),
            onClick = {
                viewModel.checkEngine()
            }
        ) {
            Text("Check Engine")
        }
        Button(
            shape = RoundedCornerShape(10.dp),
            onClick = {
                //viewModel.closeSocket()
                viewModel.toggleConnection()
            }
        ) {
            Text(text = "Toggle")

        }

        Spacer(modifier = Modifier.height(50.dp))
        Text("Bluetooth is ${if (isBluetoothConnected) "connected" else "disconnected" } ", color = Color.White)
        Text("Stock is ${if (isSocketConnected) "connected" else "disconnected" } ", color = Color.White)
        Text("RPM: $rpms", color = Color.White)
        Text("Speed: $speed", color = Color.White)
        Text("Coolant Temp: $coolant", color = Color.White)
        Text("Engine Load: $load", color = Color.White)
        Text("MAP: $map", color = Color.White)
        Text("Throttle: $throttle", color = Color.White)
        Text("Voltage: $voltage", color = Color.White)
        Text("Ignition Timing: $timing", color = Color.White)
        Text("MAF: $maf", color = Color.White)
        Text("AFR: $afr", color = Color.White)
        Text("Fuel Status: $fuelStatus", color = Color.White)
        Text("O2 Sensor: $o2", color = Color.White)
        Text("Check Engine: $checkEngine", color = Color.White)
    }

    if (showDeviceDialog) {
        DeviceSelectionDialog(
            devices = devices,
            onDeviceSelected = {
                viewModel.connectToDevice(context, it)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false }
        )
    }

    if (isConnecting) {
        ConnectingDialog()
    }
}

@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Device") },
        text = {
            if (devices.isEmpty()) {
                Text("No devices found")
            } else {
                LazyColumn {
                    items(devices) { device ->
                        Text(
                            text = device.name ?: device.address,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ConnectingDialog() {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Connecting...")
            }
        },
        confirmButton = {}
    )
}
