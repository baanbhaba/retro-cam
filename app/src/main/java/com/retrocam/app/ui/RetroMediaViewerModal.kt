package com.retrocam.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun RetroMediaViewerModal(
    latestUri: Uri?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .safeDrawingPadding()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121218))
                .border(2.dp, Color(0xFF2C2C38), RoundedCornerShape(16.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* stop backdrop dismiss */ }
        ) {
            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C24))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "●",
                        color = Color(0xFF00FF66),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "VCR PLAYBACK DECK",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // MEDIA PREVIEW CONTAINER
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (latestUri != null) {
                    // Display captured photo or video thumbnail
                    AsyncImage(
                        model = latestUri,
                        contentDescription = "Recorded media preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Retro VCR On-Screen Display Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PLAY ▶",
                                color = Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "SP 1.33x",
                                color = Color(0xFFFFCC00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AUTO TRACKING",
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "MINI-DV",
                                color = Color(0xFF99BBFF),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    // Empty tape state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "[ NO CASSETTE INSERTED ]",
                            color = Color(0xFFFFB326),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Snap a photo or record a video with RetroCam to view your playback here.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // FOOTER ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181820))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (latestUri != null) {
                    // Share Button
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (latestUri.toString().contains("mp4")) "video/mp4" else "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, latestUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share RetroCam Media"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3C)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFFFB326), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    // Open in Device Gallery
                    Button(
                        onClick = {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(latestUri, if (latestUri.toString().contains("mp4")) "video/*" else "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(viewIntent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3C)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GALLERY", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Return to Camera Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("EJECT / CAMERA", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
