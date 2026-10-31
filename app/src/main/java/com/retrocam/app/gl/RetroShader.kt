package com.retrocam.app.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RetroShader {

    companion object {
        private const val TAG = "RetroShader"

        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTextureCoord;

            void main() {
                gl_Position = aPosition;
                vTextureCoord = (uTexMatrix * aTextureCoord).xy;
            }
        """

        const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            varying vec2 vTextureCoord;
            uniform samplerExternalOES uSampler;
            uniform float uTime;
            uniform float uSquishFactor;
            uniform int uAspectMode;
            uniform float uBloomIntensity;
            uniform float uChromaAberration;
            uniform float uGrainIntensity;
            uniform float uScanlineIntensity;
            uniform int uColorGrade;

            float hash(vec2 p) {
                vec3 p3 = fract(vec3(p.xyx) * 0.1031);
                p3 += dot(p3, p3.yzx + 33.33);
                return fract((p3.x + p3.y) * p3.z);
            }

            void main() {
                vec2 uv = vTextureCoord;
                
                // Anamorphic vertical squeeze
                if (uAspectMode == 0) {
                    uv.y = (uv.y - 0.5) * uSquishFactor + 0.5;
                    if (uv.y < 0.0 || uv.y > 1.0) {
                        gl_FragColor = vec4(0.03, 0.03, 0.03, 1.0);
                        return;
                    }
                }

                // Chromatic Aberration
                vec2 center = vec2(0.5, 0.5);
                vec2 dir = uv - center;
                float dist = length(dir);
                vec2 shift = dir * (uChromaAberration * (1.0 + dist * 1.5));

                float r = texture2D(uSampler, uv + shift).r;
                float g = texture2D(uSampler, uv).g;
                float b = texture2D(uSampler, uv - shift).b;
                vec3 col = vec3(r, g, b);

                // Highlight Bloom / Halation
                if (uBloomIntensity > 0.01) {
                    float luma = dot(col, vec3(0.299, 0.587, 0.114));
                    if (luma > 0.60) {
                        float blurStep = 0.0035;
                        float bSample = 0.0;
                        bSample += texture2D(uSampler, uv + vec2(-blurStep, -blurStep)).r;
                        bSample += texture2D(uSampler, uv + vec2( blurStep, -blurStep)).r;
                        bSample += texture2D(uSampler, uv + vec2(-blurStep,  blurStep)).r;
                        bSample += texture2D(uSampler, uv + vec2( blurStep,  blurStep)).r;
                        bSample *= 0.25;

                        vec3 halation = vec3(1.0, 0.82, 0.60) * (bSample * uBloomIntensity);
                        col += halation;
                    }
                }

                // CCD Color Science
                if (uColorGrade == 0) {
                    // 90s CCD Warmth (Sony Handycam)
                    col = pow(col, vec3(0.92, 0.95, 1.02));
                    col.r *= 1.08;
                    col.g *= 1.02;
                    col.b *= 0.92;
                    col += vec3(0.035, 0.03, 0.025);
                } else if (uColorGrade == 1) {
                    // MiniDV Clean Y2K (Panasonic)
                    col.r *= 0.97;
                    col.b *= 1.06;
                    col += vec3(0.02, 0.025, 0.035);
                } else if (uColorGrade == 2) {
                    // JVC Hi8 1994 (Saturated warm tape)
                    col = pow(col, vec3(0.88, 0.92, 1.05));
                    col.r *= 1.14;
                    col.g *= 1.04;
                    col.b *= 0.86;
                } else if (uColorGrade == 3) {
                    // B&W Surveillance / CCTV
                    float gray = dot(col, vec3(0.299, 0.587, 0.114));
                    col = vec3(pow(gray, 1.15));
                } else if (uColorGrade == 4) {
                    // NightShot 0-Lux Infrared Phosphor Green
                    float lum = dot(col, vec3(0.299, 0.587, 0.114));
                    col = vec3(lum * 0.18, lum * 1.18, lum * 0.35);
                }

                // Scanlines
                if (uScanlineIntensity > 0.01) {
                    float scanline = sin(uv.y * 640.0) * 0.5 + 0.5;
                    col -= scanline * (uScanlineIntensity * 0.12);
                }

                // Micro Grain
                if (uGrainIntensity > 0.01) {
                    float noise = hash(uv * 480.0 + fract(uTime * 19.17));
                    col += (noise - 0.5) * uGrainIntensity;
                }

                gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
            }
        """

        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }

        fun createProgram(): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            if (vertexShader == 0 || fragmentShader == 0) return 0

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Program link failed: " + GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                return 0
            }
            return program
        }
    }
}
