package com.example.obdii_connection
import android.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue

@Composable
fun HomeScreen(context: Context, viewModel: HomeScreenViewModel) {

    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val rpms by viewModel.rpms.collectAsState()
    val isSocketConnected by viewModel.isSocketConnected.collectAsState()
    val checkEngine by viewModel.checkEngine.collectAsState()



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
                viewModel.connectToELM327(context)
            }
        ) {
            Text(text = "Connect To ELM327")
        }
        Button(
            shape = RoundedCornerShape(10.dp),
            onClick = {
                viewModel.getRPMs()
            }
        ) {
            Text(text = "get RPMs")
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
        Text("RPMS: $rpms ", color = Color.White)
        Text("Check Engine: $checkEngine", color = Color.White)
    }
}
