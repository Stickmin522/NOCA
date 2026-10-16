package app.noca

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object StationCache {
    @Volatile private var directory: StationDirectory? = null

    @Synchronized
    fun load(context: android.content.Context): StationDirectory {
        return directory
            ?: context.assets
                .open("stations/stations.tsv")
                .reader(Charsets.UTF_8)
                .let(StationDirectory::read)
                .also { directory = it }
    }
}

@Composable
fun stationNames(): StationDirectory {
    val context = LocalContext.current.applicationContext
    val directory by
        produceState(StationDirectory.EMPTY, context) {
            value = withContext(Dispatchers.IO) { StationCache.load(context) }
        }
    return directory
}
