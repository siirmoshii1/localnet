package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Utility to convert CameraX ImageProxy (YUV_420_888) to compressed JPEG byte array
 * and Bitmap for local display or network transmission.
 */
object YuvToJpegConverter {

    fun imageProxyToJpeg(image: ImageProxy, quality: Int = 50, rotationDegrees: Int = 0): ByteArray? {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped in NV21
        yBuffer.get(nv21, 0, ySize)
        val pixelStride = image.planes[1].pixelStride
        val rowStride = image.planes[1].rowStride

        // If rowStride == width and pixelStride == 2, direct copy
        var pos = ySize
        val width = image.width
        val height = image.height

        val vPlane = image.planes[2]
        val uPlane = image.planes[1]
        val vBuf = vPlane.buffer
        val uBuf = uPlane.buffer

        // Standard NV21 packing from YUV_420_888
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                if (pos < nv21.size - 1) {
                    nv21[pos++] = vBuf.get(vIndex)
                    nv21[pos++] = uBuf.get(uIndex)
                }
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
        val jpegBytes = out.toByteArray()

        if (rotationDegrees == 0) {
            return jpegBytes
        }

        // Apply rotation if needed
        return try {
            val originalBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
            )
            val rotOut = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, rotOut)
            rotOut.toByteArray()
        } catch (e: Exception) {
            jpegBytes
        }
    }
}
