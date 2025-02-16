package io.shaderfoundry.lib.program

import android.opengl.GLES31
import android.util.Log
import io.shaderfoundry.lib.util.GLUtils

/**
 * Vertex shader program.
 * Implemented to have eager initialization because it is an internal API which is guaranteed to
 * be called on a thread that has EGL context.
 */
internal class VertexShader: ShaderProgram {
    val shaderProgramId: VertexShaderProgramId
    private val texMatrixLocation: Int
    private val transMatrixLoc: Int
    private val positionLoc: Int
    private val texCoordLoc: Int

    init {
        shaderProgramId = VertexShaderProgramId(GLES31.glCreateShaderProgramv(GLES31.GL_VERTEX_SHADER, arrayOf(VERTEX_SHADER)))
        GLUtils.checkGlErrorOrThrow("vertexShadeProgramId $shaderProgramId")

        val linkStatus = IntArray(1)
        GLES31.glGetProgramiv(shaderProgramId.handle, GLES31.GL_LINK_STATUS, linkStatus,  /*offset=*/0)
        if (linkStatus[0] != GLES31.GL_TRUE) {
            Log.e(GLUtils.TAG, GLES31.glGetProgramInfoLog(shaderProgramId.handle))
        }

        texMatrixLocation = GLES31.glGetUniformLocation(shaderProgramId.handle, "uTexMatrix")
        transMatrixLoc = GLES31.glGetUniformLocation(shaderProgramId.handle, "uTransMatrix")
        positionLoc = GLES31.glGetAttribLocation(shaderProgramId.handle, "aPosition")
        texCoordLoc = GLES31.glGetAttribLocation(shaderProgramId.handle, "aTextureCoord")
    }

    internal fun use() {
        // Enable the "aPosition" vertex attribute.
        GLES31.glEnableVertexAttribArray(positionLoc)
        GLUtils.checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        val coordsPerVertex = 2
        val vertexStride = 0
        GLES31.glVertexAttribPointer(
            positionLoc, coordsPerVertex, GLES31.GL_FLOAT,  /*normalized=*/
            false, vertexStride, GLUtils.VERTEX_BUF
        )
        GLUtils.checkGlErrorOrThrow("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES31.glEnableVertexAttribArray(texCoordLoc)
        GLUtils.checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        val coordsPerTex = 2
        val texStride = 0
        GLES31.glVertexAttribPointer(
            texCoordLoc, coordsPerTex, GLES31.GL_FLOAT,  /*normalized=*/
            false, texStride, GLUtils.TEX_BUF
        )
        GLUtils.checkGlErrorOrThrow("glVertexAttribPointer")

        updateTransformMatrix(GLUtils.create4x4IdentityMatrix())
        updateTextureMatrix(GLUtils.create4x4IdentityMatrix())

        GLUtils.checkGlErrorOrThrow("update values")
    }

    internal fun updateTextureMatrix(textureTransform: FloatArray) {
        GLES31.glProgramUniformMatrix4fv(
            shaderProgramId.handle,
            texMatrixLocation,
            /*count=*/1,
            /*transpose=*/false,
            textureTransform,
            /*offset=*/0
        )
        GLUtils.checkGlErrorOrThrow("glUniformMatrix4fv")
    }

    // TODO: Figure out why it is not updated now?
    internal fun updateTransformMatrix(transformMat: FloatArray) {
        GLES31.glProgramUniformMatrix4fv(
            shaderProgramId.handle,
            transMatrixLoc,
            /*count=*/ 1,
            /*transpose=*/ false,
            transformMat,
            /*offset=*/ 0
        )

        GLUtils.checkGlErrorOrThrow("glUniformMatrix4fv")
    }

    internal fun dispose() {
        GLES31.glDeleteProgram(shaderProgramId.handle)
    }
}

private val VERTEX_SHADER = """
    #version 310 es
    
    precision mediump float;

    in vec4 aPosition;
    in vec4 aTextureCoord;
    uniform mat4 uTexMatrix;
    uniform mat4 uTransMatrix;
    
    out vec2 vTextureCoord;
    void main() {
        gl_Position = uTransMatrix * aPosition;
        vTextureCoord = (uTexMatrix * aTextureCoord).xy;   
    }
""".trimIndent().trim()