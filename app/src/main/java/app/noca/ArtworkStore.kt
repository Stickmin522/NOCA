package app.noca

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID

internal fun artworkFileName(id: String): String? =
    id.removePrefix("custom:").takeIf {
        id.startsWith("custom:") &&
            it.matches(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.png"))
    }

class ArtworkStore(private val context: Context) {
    private val directory
        get() = File(context.filesDir, "artwork").also { it.mkdirs() }

    fun import(uri: Uri): String {
        val data =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > 20 * 1024 * 1024) throw IOException("image_too_large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: throw IOException("image_unavailable")
        val source = ImageDecoder.createSource(ByteBuffer.wrap(data))
        val bitmap =
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = minOf(1.0, 1600.0 / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    maxOf(1, (info.size.width * scale).toInt()),
                    maxOf(1, (info.size.height * scale).toInt()),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        val file = File(directory, "${UUID.randomUUID()}.png")
        try {
            // Re-encoding preserves the image without location or camera metadata.
            file.outputStream().use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    throw IOException("image_invalid")
            }
        } catch (failure: Exception) {
            file.delete()
            throw failure
        } finally {
            bitmap.recycle()
        }
        return "custom:${file.name}"
    }

    fun discard(id: String, used: Set<String>) {
        if (id !in used) artworkFileName(id)?.let { File(directory, it).delete() }
    }

    fun retain(used: Set<String>) {
        val names = used.mapNotNull(::artworkFileName).toSet()
        directory.listFiles()?.forEach { if (it.name !in names) it.delete() }
    }
}
