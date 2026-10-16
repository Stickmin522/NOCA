package app.noca

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
            )

        private fun nullable(j: JSONObject, key: String) =
            if (j.isNull(key)) null else j.optString(key).takeIf { it.isNotBlank() }
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
    val bitmap by
        produceState<ImageBitmap?>(null, context, id) {
            value = null
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val custom = artworkFileName(id)
                        if (custom != null)
                            File(context.filesDir, "artwork/$custom").inputStream().use {
                                BitmapFactory.decodeStream(it)?.asImageBitmap()
                            }
                        else
                            art?.file?.let { file ->
                                context.assets.open("cards/$file").use { stream ->
                                    BitmapFactory.decodeStream(stream)
                                        ?.let { image ->
                                            art.crop?.let { crop ->
                                                android.graphics.Bitmap.createBitmap(
                                                    image,
                                                    crop[0],
                                                    crop[1],
                                                    crop[2],
                                                    crop[3],
                                                )
                                            } ?: image
                                        }
                                        ?.asImageBitmap()
                                }
                            }
                    }
                        .getOrNull()
                }
        }
    val visibleBitmap = bitmap
    if (visibleBitmap != null)
        Image(
            visibleBitmap,
            contentDescription = art?.name,
            modifier = modifier.aspectRatio(1.586f).clip(RoundedCornerShape(percent = 6)),
            contentScale = ContentScale.Crop,
        )
    else
        Box(
            modifier
                .aspectRatio(1.586f)
                .clip(RoundedCornerShape(percent = 6))
                .background(
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
