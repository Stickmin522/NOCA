package app.noca

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CardArtwork(
    val id: String,
    val name: String,
    val file: String?,
    val source: String?,
    val color: String? = null,
    val crop: List<Int>? = null,
    val notched: Boolean = false,
) {
    companion object {
        fun fromJson(j: JSONObject) =
            CardArtwork(
                j.getString("id"),
                j.getString("name"),
                nullable(j, "file"),
                nullable(j, "source"),
                nullable(j, "color"),
                j.optJSONArray("crop")?.let { a -> (0 until a.length()).map { a.getInt(it) } },
                j.optBoolean("notched", false),
            )

        private fun nullable(j: JSONObject, key: String) =
            if (j.isNull(key)) null else j.optString(key).takeIf { it.isNotBlank() }
    }
}

private const val CARD_ASPECT_RATIO = 85.60f / 53.98f

private class TransitCardShape(private val notched: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val radius = h * (3.18f / 53.98f)
        val card =
            Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius)))
            }
        if (!notched) return Outline.Generic(card)
        // The shallow orientation cut follows the lower right edge of the physical card.
        val cut =
            Path().apply {
                moveTo(w + 1f, h * .68f)
                lineTo(w, h * .68f)
                cubicTo(w, h * .715f, w * .986f, h * .72f, w * .986f, h * .745f)
                cubicTo(w * .986f, h * .80f, w, h * .90f, w, h * .94f)
                lineTo(w + 1f, h * .94f)
                close()
            }
        return Outline.Generic(Path.combine(PathOperation.Difference, card, cut))
    }
}

fun artworks(context: Context): List<CardArtwork> {
    val a =
        JSONArray(context.assets.open("cards/catalog.json").bufferedReader().use { it.readText() })
    return (0 until a.length()).map { CardArtwork.fromJson(a.getJSONObject(it)) }
}

@Composable
fun CardArt(id: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val art = remember(context, id) { artworks(context).firstOrNull { it.id == id } }
    var targetWidth by remember { mutableIntStateOf(0) }
    val shape = remember(art?.notched) { TransitCardShape(art?.notched == true) }
    val display =
        modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .onSizeChanged { targetWidth = it.width }
            .shadow(
                6.dp,
                shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = .10f),
                spotColor = Color.Black.copy(alpha = .16f),
            )
            .clip(shape)
    val bitmap by
        produceState<ImageBitmap?>(null, context, id, targetWidth) {
            value = null
            if (targetWidth == 0) return@produceState
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        decodeArtwork(context, id, art, targetWidth)
                    }
                        .getOrNull()
                }
        }
    val visibleBitmap = bitmap
    if (visibleBitmap != null)
        Image(
            visibleBitmap,
            contentDescription = art?.name,
            modifier = display.background(Color.White),
            contentScale = if (art != null) ContentScale.FillBounds else ContentScale.Crop,
        )
    else
        Box(
            display.background(
                art?.color?.let { Color(it.toColorInt()) } ?: LocalNocaColors.current.accent
            ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = LocalNocaColors.current.ink,
                )
                Text(
                    art?.name ?: "NOCA",
                    color = LocalNocaColors.current.ink,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
}

private fun decodeArtwork(
    context: Context,
    id: String,
    art: CardArtwork?,
    targetWidth: Int,
): ImageBitmap? {
    val custom = artworkFileName(id)
    fun stream() =
        if (custom != null) File(context.filesDir, "artwork/$custom").inputStream()
        else art?.file?.let { context.assets.open("cards/$it") }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val header = stream() ?: return null
    header.use { BitmapFactory.decodeStream(it, null, bounds) }
    val width = art?.crop?.get(2) ?: bounds.outWidth
    if (width <= 0) return null
    var sample = 1
    while (width / (sample * 2) >= targetWidth) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val image = stream()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val cropped =
        art?.crop?.let { crop ->
            val x = (crop[0] / sample).coerceIn(0, image.width - 1)
            val y = (crop[1] / sample).coerceIn(0, image.height - 1)
            android.graphics.Bitmap.createBitmap(
                image,
                x,
                y,
                (crop[2] / sample).coerceIn(1, image.width - x),
                (crop[3] / sample).coerceIn(1, image.height - y),
            )
        } ?: image
    return cropped.asImageBitmap()
}
