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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray

data class CardArtwork(val id:String,val name:String,val file:String?,val source:String?)
fun artworks(context:Context):List<CardArtwork>{val a=JSONArray(context.assets.open("cards/catalog.json").bufferedReader().use{it.readText()});return (0 until a.length()).map{val j=a.getJSONObject(it);CardArtwork(j.getString("id"),j.getString("name"),j.optString("file").takeIf{f->f.isNotBlank()},j.optString("source").takeIf{f->f.isNotBlank()})}}
@Composable fun CardArt(id:String,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val art=remember(id){artworks(context).firstOrNull{it.id==id}}
    val bitmap=remember(art?.file){art?.file?.let{runCatching{context.assets.open("cards/$it").use{stream->BitmapFactory.decodeStream(stream)?.asImageBitmap()}}.getOrNull()}}
    if(bitmap!=null)Image(bitmap,contentDescription=art?.name,modifier=modifier.aspectRatio(1.586f).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Fit)
    else Box(modifier.aspectRatio(1.586f).clip(RoundedCornerShape(9.dp)).background(LocalNocaColors.current.accent),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.CreditCard,contentDescription=null,tint=LocalNocaColors.current.ink);Text(art?.name?:"NOCA",color=LocalNocaColors.current.ink,style=MaterialTheme.typography.labelSmall)}}
}
