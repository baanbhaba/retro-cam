package com.retrocam.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.retrocam.app.camera.RetroCamManager
import com.retrocam.app.settings.SettingsManager
import com.retrocam.app.ui.RetroCamScreen

class MainActivity : ComponentActivity() {

    private lateinit var camManager: RetroCamManager
    private lateinit var settingsManager: SettingsManager

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make app edge-to-edge with transparent system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        camManager = RetroCamManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)

        setContent {
            MaterialTheme {
                val permissions = remember {
                    val list = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    list
                }

                val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

                if (permissionsState.allPermissionsGranted) {
                    RetroCamScreen(
                        camManager = camManager,
                        settingsManager = settingsManager
                    )
                } else {
                    PermissionRequestScreen {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camManager.release()
    }
}

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101014)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "RETROCAM",
                color = Color(0xFFFFB326),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CCD VINTAGE RECORDER",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Camera and microphone permissions are needed to capture vintage video and photos.",
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "GRANT PERMISSION",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
