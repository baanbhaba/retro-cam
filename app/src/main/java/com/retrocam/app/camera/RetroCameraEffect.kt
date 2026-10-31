package com.retrocam.app.camera

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.retrocam.app.gl.RetroGlRenderer

class RetroCameraEffect(
    val glRenderer: RetroGlRenderer,
    private val glThread: HandlerThread = HandlerThread("RetroGlThread").apply { start() }
) : CameraEffect(
    VIDEO_CAPTURE,
    { command -> Handler(glThread.looper).post(command) },
    object : SurfaceProcessor {
        private val TAG = "RetroSurfaceProcessor"

        private var inputSurfaceTexture: SurfaceTexture? = null
        private var inputSurface: Surface? = null
        private var outputEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var outputSize: Size? = null
        private val texMatrix = FloatArray(16)
        private val glHandler = Handler(glThread.looper)

        override fun onInputSurface(request: SurfaceRequest) {
            glHandler.post {
                try {
                    glRenderer.initEgl()

                    inputSurfaceTexture?.release()
                    inputSurface?.release()

                    val surfaceTexture = SurfaceTexture(glRenderer.textureId).apply {
                        setDefaultBufferSize(request.resolution.width, request.resolution.height)
                        setOnFrameAvailableListener({
                            try {
                                updateTexImage()
                                getTransformMatrix(texMatrix)

                                if (outputEglSurface != EGL14.EGL_NO_SURFACE && outputSize != null) {
                                    glRenderer.draw(
                                        outputEglSurface,
                                        outputSize!!.width,
                                        outputSize!!.height,
                                        texMatrix,
                                        timestamp
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Frame render error", e)
                            }
                        }, glHandler)
                    }
                    inputSurfaceTexture = surfaceTexture
                    val surface = Surface(surfaceTexture)
                    inputSurface = surface

                    request.provideSurface(surface, { cmd -> glHandler.post(cmd) }) {
                        glHandler.post {
                            try {
                                surfaceTexture.release()
                                surface.release()
                                if (inputSurface === surface) inputSurface = null
                                if (inputSurfaceTexture === surfaceTexture) inputSurfaceTexture = null
                            } catch (e: Exception) {
                                Log.e(TAG, "Error releasing input surface", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to setup input surface", e)
                }
            }
        }

        override fun onOutputSurface(output: SurfaceOutput) {
            glHandler.post {
                outputSize = output.size
                try {
                    glRenderer.initEgl()
                    output.getSurface({ cmd -> glHandler.post(cmd) }) { _ ->
                        glHandler.post {
                            if (outputEglSurface != EGL14.EGL_NO_SURFACE) {
                                glRenderer.releaseSurface(outputEglSurface)
                                outputEglSurface = EGL14.EGL_NO_SURFACE
                            }
                        }
                    }
                    val surface = output.getSurface({ cmd -> glHandler.post(cmd) }) { /* close listener */ }
                    if (outputEglSurface != EGL14.EGL_NO_SURFACE) {
                        glRenderer.releaseSurface(outputEglSurface)
                    }
                    outputEglSurface = glRenderer.createWindowSurface(surface)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create window surface for video encoder", e)
                }
            }
        }
    },
    { error ->
        Log.e("RetroCameraEffect", "CameraEffect error", error)
    }
)
