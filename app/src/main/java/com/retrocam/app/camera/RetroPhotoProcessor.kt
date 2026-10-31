package com.retrocam.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object RetroPhotoProcessor {

    private const val TAG = "RetroPhotoProcessor"

    data class EffectConfig(
        val squishFactor: Float = 1.35f,
        val bloomEnabled: Boolean = true,
        val chromaticAberration: Boolean = true,
        val scanlinesEnabled: Boolean = true,
        val grainIntensity: Float = 0.12f, // Enhanced gritty camcorder noise
        val chromaNoiseEnabled: Boolean = true,
        val colorGrade: Int = 0, // 0: 90s Warm, 1: MiniDV
        val customDateText: String? = null
    )

    suspend fun processAndSavePhoto(
        context: Context,
        originalBitmap: Bitmap,
        config: EffectConfig
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = originalBitmap.width
            val height = originalBitmap.height

            // 1. Apply Anamorphic Squeeze / Ratio
            // Squeeze vertically by compressing the height into a letterbox frame or squishing the content
            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)
            canvas.drawColor(Color.rgb(10, 10, 10)) // Vintage black background

            val srcRect = Rect(0, 0, width, height)
            val squishedHeight = (height / config.squishFactor).toInt()
            val topOffset = (height - squishedHeight) / 2
            val dstRect = Rect(0, topOffset, width, topOffset + squishedHeight)

            // Draw squeezed bitmap
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            
            // Apply CCD Color Grading Matrix
            val colorMatrix = ColorMatrix()
            when (config.colorGrade) {
                0 -> {
                    // 90s CCD Warmth (Sony Handycam)
                    colorMatrix.set(floatArrayOf(
                        1.18f, 0.0f,  0.0f,  0.0f, 15f,
                        0.0f,  1.05f, 0.0f,  0.0f, 8f,
                        0.0f,  0.0f,  0.90f, 0.0f, 5f,
                        0.0f,  0.0f,  0.0f,  1.0f, 0f
                    ))
                }
                1 -> {
                    // Early 2000s MiniDV (Panasonic)
                    colorMatrix.set(floatArrayOf(
                        0.97f, 0.0f,  0.0f,  0.0f, 5f,
                        0.0f,  1.02f, 0.0f,  0.0f, 5f,
                        0.0f,  0.0f,  1.10f, 0.0f, 10f,
                        0.0f,  0.0f,  0.0f,  1.0f, 0f
                    ))
                }
                2 -> {
                    // JVC Hi8 (Saturated analogue tape)
                    colorMatrix.set(floatArrayOf(
                        1.22f, 0.0f,  0.0f,  0.0f, 20f,
                        0.0f,  1.08f, 0.0f,  0.0f, 10f,
                        0.0f,  0.0f,  0.84f, 0.0f, 0f,
                        0.0f,  0.0f,  0.0f,  1.0f, 0f
                    ))
                }
                3 -> {
                    // B&W Surveillance CCTV
                    colorMatrix.setSaturation(0.0f)
                }
                4 -> {
                    // NightShot 0-Lux Phosphor Green
                    colorMatrix.set(floatArrayOf(
                        0.15f, 0.0f,  0.0f,  0.0f, 0f,
                        0.0f,  1.25f, 0.0f,  0.0f, 30f,
                        0.0f,  0.0f,  0.30f, 0.0f, 0f,
                        0.0f,  0.0f,  0.0f,  1.0f, 0f
                    ))
                }
                else -> {
                    colorMatrix.set(floatArrayOf(
                        1.15f, 0.0f,  0.0f,  0.0f, 15f,
                        0.0f,  1.05f, 0.0f,  0.0f, 8f,
                        0.0f,  0.0f,  0.92f, 0.0f, 5f,
                        0.0f,  0.0f,  0.0f,  1.0f, 0f
                    ))
                }
            }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(originalBitmap, srcRect, dstRect, paint)

            // 2. Chromatic Aberration & Highlight Halation
            if (config.chromaticAberration) {
                val chromaPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                chromaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                val offset = (width * 0.003f).toInt()
                
                // Red channel shift
                val redFilter = ColorMatrix(floatArrayOf(
                    0.25f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0.5f, 0f
                ))
                chromaPaint.colorFilter = ColorMatrixColorFilter(redFilter)
                canvas.drawBitmap(originalBitmap, srcRect, Rect(offset, topOffset, width + offset, topOffset + squishedHeight), chromaPaint)
            }

            // 3. Scanlines overlay (only in active picture region)
            if (config.scanlinesEnabled) {
                val scanlinePaint = Paint().apply {
                    color = Color.argb(22, 0, 0, 0)
                    strokeWidth = 2f
                }
                var y = topOffset.toFloat()
                while (y < topOffset + squishedHeight) {
                    canvas.drawLine(0f, y, width.toFloat(), y, scanlinePaint)
                    y += 4f
                }
            }

            // 4. Crunchy CCD Analog Grain & Chroma Noise
            if (config.grainIntensity > 0.01f) {
                val random = Random(System.currentTimeMillis())
                val noisePaint = Paint().apply {
                    style = Paint.Style.FILL
                    strokeWidth = max(1f, width * 0.0012f)
                }

                // A. Multi-Color CCD Chroma Noise Specks (Red, Blue, Green, Cyan noise in dark/mid areas)
                val chromaColors = intArrayOf(
                    Color.argb(70, 255, 60, 60),   // Red chroma noise
                    Color.argb(70, 60, 180, 255),  // Cyan chroma noise
                    Color.argb(60, 60, 255, 120),  // Green chroma noise
                    Color.argb(70, 255, 230, 80),  // Yellow chroma noise
                    Color.argb(80, 240, 240, 255), // Salt noise
                    Color.argb(80, 20, 20, 25)     // Pepper noise
                )

                val noiseCount = (width * squishedHeight * 0.0035f * (config.grainIntensity / 0.08f)).toInt()
                for (i in 0 until noiseCount) {
                    val gx = random.nextFloat() * width
                    val gy = topOffset + random.nextFloat() * squishedHeight
                    noisePaint.color = chromaColors[random.nextInt(chromaColors.size)]
                    canvas.drawPoint(gx, gy, noisePaint)
                }

                // B. Subtle Camcorder Horizontal Banding Jitter
                val bandPaint = Paint().apply {
                    color = Color.argb(18, 255, 255, 255)
                    strokeWidth = 2f
                }
                for (j in 0 until 12) {
                    val bandY = topOffset + random.nextFloat() * squishedHeight
                    bandPaint.alpha = random.nextInt(8, 25)
                    canvas.drawLine(0f, bandY, width.toFloat(), bandY, bandPaint)
                }
            }

            // 5. Crisp Camcorder OSD Overlay
            val dateText = config.customDateText ?: getRetroDateString()
            drawCamcorderOsd(canvas, width, height, topOffset, squishedHeight, dateText)

            // 6. Save to Gallery
            saveBitmapToGallery(context, outputBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process and save photo", e)
            null
        }
    }

    private fun drawCamcorderOsd(
        canvas: Canvas,
        width: Int,
        height: Int,
        topOffset: Int,
        activeHeight: Int,
        dateText: String
    ) {
        val fontSize = width * 0.038f

        // Vintage Monospace font
        val osdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 175, 45) // Vintage Camcorder Amber / Orange
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            setShadowLayer(fontSize * 0.15f, 2f, 2f, Color.argb(200, 0, 0, 0))
        }

        val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 220, 220)
            textSize = fontSize * 0.85f
            typeface = Typeface.MONOSPACE
            setShadowLayer(fontSize * 0.15f, 2f, 2f, Color.argb(200, 0, 0, 0))
        }

        val recPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 40, 40)
            textSize = fontSize * 0.95f
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            setShadowLayer(fontSize * 0.15f, 2f, 2f, Color.argb(200, 0, 0, 0))
        }

        val padding = width * 0.05f

        // Top-Left: REC / SP
        canvas.drawText("● REC", padding, topOffset + padding * 1.5f, recPaint)
        canvas.drawText("SP", padding + fontSize * 4.5f, topOffset + padding * 1.5f, hudPaint)

        // Top-Right: Battery gauge
        val battText = "[||||]"
        val battWidth = hudPaint.measureText(battText)
        canvas.drawText(battText, width - padding - battWidth, topOffset + padding * 1.5f, hudPaint)

        // Bottom-Left: Vintage Date & Timestamp
        val bottomY = topOffset + activeHeight - padding * 1.2f
        canvas.drawText(dateText, padding, bottomY, osdPaint)

        // Bottom-Right: Camcorder model aesthetic stamp
        val modelText = "CCD 640x"
        val modelWidth = hudPaint.measureText(modelText)
        canvas.drawText(modelText, width - padding - modelWidth, bottomY, hudPaint)
    }

    fun getRetroDateString(year: Int = 1999): String {
        val now = Calendar.getInstance()
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val month = monthNames[now.get(Calendar.MONTH)]
        val day = String.format("%02d", now.get(Calendar.DAY_OF_MONTH))
        val hour = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = String.format("%02d", now.get(Calendar.MINUTE))
        val amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

        return "$month. $day $year  $hour:$minute $amPm"
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "RETRO_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RetroCam")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Log.e(TAG, "Error saving bitmap to gallery", e)
            return null
        }
    }
}
