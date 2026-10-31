package com.retrocam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrocam.app.settings.*

@Composable
fun RetroTapeStatusModal(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141418))
                .border(2.dp, Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* stop backdrop dismiss */ }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MINI-DV TAPE STATUS",
                    color = Color(0xFFFFB326),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TapeMetricRow("TAPE FORMAT", "DVM60 (Standard Play)")
            TapeMetricRow("ESTIMATED CAPACITY", "58 MIN 32 SEC")
            TapeMetricRow("VIDEO CODEC", "H.264 / 24 FPS HARDWARE")
            TapeMetricRow("AUDIO PROFILE", "16-BIT 48kHz LOW-PASS")
            TapeMetricRow("BATTERY LEVEL", "84% [||||]")
            TapeMetricRow("DEVICE SENSOR", "MOTO G86 CCD PIPELINE")

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("EJECT / CLOSE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun RetroDateCustomizerModal(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141418))
                .border(2.dp, Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* stop backdrop dismiss */ }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VCR DATE & HUD COLOR SETUP",
                    color = Color(0xFFFFB326),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("SELECT VINTAGE YEAR:", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1994, 1998, 1999, 2002, null).forEach { yr ->
                    val selected = settings.osdCustomYear == yr
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Color(0xFFFFB326) else Color(0xFF22222A))
                            .clickable { settingsManager.update { it.copy(osdCustomYear = yr) } }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = yr?.toString() ?: "NOW",
                            color = if (selected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("HUD PHOSPHOR COLOR:", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OsdColor.values().forEach { col ->
                    val selected = settings.osdColor == col
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { settingsManager.update { it.copy(osdColor = col) } }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(col.colorHex))
                                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun RetroAspectQuickModal(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141418))
                .border(2.dp, Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* stop backdrop dismiss */ }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASPECT RATIO & SQUISH TUNER",
                    color = Color(0xFFFFB326),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vertical Squeeze:", color = Color.White, fontSize = 12.sp)
                Text(String.format("%.2fx", settings.squishFactor), color = Color(0xFFFFB326), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            Slider(
                value = settings.squishFactor,
                onValueChange = { settingsManager.update { s -> s.copy(squishFactor = it) } },
                valueRange = 1.0f..1.65f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFB326),
                    activeTrackColor = Color(0xFFFFB326),
                    inactiveTrackColor = Color(0xFF33333D)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Letterbox Matte Bars:", color = Color.White, fontSize = 12.sp)
                Switch(
                    checked = settings.letterboxMatte,
                    onCheckedChange = { settingsManager.update { s -> s.copy(letterboxMatte = it) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB326),
                        checkedTrackColor = Color(0xFF4A3E20)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("SAVE RATIO", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun TapeMetricRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
