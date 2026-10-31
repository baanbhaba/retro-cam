package com.retrocam.app.gl

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RetroGlRenderer {

    companion object {
        private const val TAG = "RetroGlRenderer"

        // Fullscreen quad
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )

        private val TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
    }

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null

    private var program: Int = 0
    private var aPositionLoc: Int = -1
    private var aTexCoordLoc: Int = -1
    private var uTexMatrixLoc: Int = -1
    private var uSamplerLoc: Int = -1
    private var uTimeLoc: Int = -1
    private var uSquishFactorLoc: Int = -1
    private var uAspectModeLoc: Int = -1
    private var uBloomIntensityLoc: Int = -1
    private var uChromaAberrationLoc: Int = -1
    private var uGrainIntensityLoc: Int = -1
    private var uScanlineIntensityLoc: Int = -1
    private var uColorGradeLoc: Int = -1

    var textureId: Int = -1
        private set

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(VERTEX_COORDS)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(TEX_COORDS)
            position(0)
        }

    private val startTime = System.currentTimeMillis()

    // Configurable effect parameters
    var squishFactor: Float = 1.30f
    var aspectMode: Int = 0 // 0: Squish, 1: 4:3 Box, 2: Full
    var bloomIntensity: Float = 0.40f
    var chromaAberration: Float = 0.005f
    var grainIntensity: Float = 0.06f
    var scanlineIntensity: Float = 0.09f
    var colorGrade: Int = 0 // 0: 90s CCD Warm, 1: MiniDV

    fun initEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
            return // Already initialized
        }
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("eglInitialize failed")
        }

        val EGL_RECORDABLE_ANDROID = 0x3142
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0) || numConfigs[0] == 0) {
            throw RuntimeException("eglChooseConfig failed")
        }
        eglConfig = configs[0]

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext failed")
        }

        // Create an offscreen pbuffer surface to make context current during shader compilation
        val pbufferAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        val pbufferSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, pbufferSurface, pbufferSurface, eglContext)

        // Initialize GL program and external texture
        program = RetroShader.createProgram()
        if (program == 0) {
            throw RuntimeException("Failed to create RetroShader program")
        }

        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uSamplerLoc = GLES20.glGetUniformLocation(program, "uSampler")
        uTimeLoc = GLES20.glGetUniformLocation(program, "uTime")
        uSquishFactorLoc = GLES20.glGetUniformLocation(program, "uSquishFactor")
        uAspectModeLoc = GLES20.glGetUniformLocation(program, "uAspectMode")
        uBloomIntensityLoc = GLES20.glGetUniformLocation(program, "uBloomIntensity")
        uChromaAberrationLoc = GLES20.glGetUniformLocation(program, "uChromaAberration")
        uGrainIntensityLoc = GLES20.glGetUniformLocation(program, "uGrainIntensity")
        uScanlineIntensityLoc = GLES20.glGetUniformLocation(program, "uScanlineIntensity")
        uColorGradeLoc = GLES20.glGetUniformLocation(program, "uColorGrade")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        EGL14.eglDestroySurface(eglDisplay, pbufferSurface)
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    }

    fun createWindowSurface(surface: Surface): EGLSurface {
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            val error = EGL14.eglGetError()
            throw RuntimeException("eglCreateWindowSurface failed: $error")
        }
        return eglSurface
    }

    fun releaseSurface(eglSurface: EGLSurface) {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }
    }

    fun draw(eglSurface: EGLSurface, width: Int, height: Int, texMatrix: FloatArray, timestampNs: Long = 0) {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "eglMakeCurrent failed: " + EGL14.eglGetError())
            return
        }

        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0.04f, 0.04f, 0.04f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        // Pass attributes
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        // Set uniforms
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uSamplerLoc, 0)

        val timeSec = (System.currentTimeMillis() - startTime) / 1000.0f
        GLES20.glUniform1f(uTimeLoc, timeSec)
        GLES20.glUniform1f(uSquishFactorLoc, squishFactor)
        GLES20.glUniform1i(uAspectModeLoc, aspectMode)
        GLES20.glUniform1f(uBloomIntensityLoc, bloomIntensity)
        GLES20.glUniform1f(uChromaAberrationLoc, chromaAberration)
        GLES20.glUniform1f(uGrainIntensityLoc, grainIntensity)
        GLES20.glUniform1f(uScanlineIntensityLoc, scanlineIntensity)
        GLES20.glUniform1i(uColorGradeLoc, colorGrade)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)

        if (timestampNs > 0) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timestampNs)
        }

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (textureId != -1) {
                val textures = intArrayOf(textureId)
                GLES20.glDeleteTextures(1, textures, 0)
                textureId = -1
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }
}
