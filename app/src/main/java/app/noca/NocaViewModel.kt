package app.noca

import android.app.Application
import android.net.Uri
import android.nfc.Tag
import android.nfc.TagLostException
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NocaViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = CardRepository(app)
    private val artwork = ArtworkStore(app)
    private val preferences = app.getSharedPreferences("preferences", 0)
    private val databaseLock = Mutex()
    private val reading = AtomicBoolean(false)
    private var lastId = ""
    private var lastReadAt = 0L
    private val mutable =
        MutableStateFlow(
            UiState(
                preferences =
                    Preferences(
                        runCatching { ThemeColor.valueOf(preferences.getString("color", "LIME")!!) }
                            .getOrDefault(ThemeColor.LIME),
                        runCatching { ThemeMode.valueOf(preferences.getString("mode", "SYSTEM")!!) }
                            .getOrDefault(ThemeMode.SYSTEM),
                    )
            )
        )
    val state = mutable.asStateFlow()

    init {
        viewModelScope.coroutineContext[Job]?.invokeOnCompletion { repository.close() }
        changeCards { artwork.retain(repository.cards().map { it.art }.toSet()) }
    }

    fun nfcStatus(available: Boolean, enabled: Boolean) {
        mutable.update { it.copy(nfcAvailable = available, nfcEnabled = enabled) }
    }

    fun read(tag: Tag) {
        if (!reading.compareAndSet(false, true)) return
        mutable.update { it.copy(reading = true, error = null, syncNotice = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = CardReader().read(tag)
                val now = SystemClock.elapsedRealtime()
                databaseLock.withLock {
                    if (snapshot.id != lastId || now - lastReadAt > 1500) {
                        val saved = repository.sync(snapshot)
                        mutable.update {
                            it.copy(
                                current = snapshot,
                                cards = repository.cards(),
                                syncNotice = saved,
                            )
                        }
                        lastId = snapshot.id
                        lastReadAt = now
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val key =
                    when (failure) {
                        is ReadFailure -> failure.key
                        is TagLostException -> "card_moved"
                        is IOException -> "read_failed"
                        is android.database.SQLException -> "storage_error"
                        else -> "invalid_card_data"
                    }
                mutable.update { it.copy(error = key) }
            } finally {
                mutable.update { it.copy(reading = false) }
                reading.set(false)
            }
        }
    }

    fun dismissError() {
        mutable.update { it.copy(error = null) }
    }

    fun remember(snapshot: CardSnapshot, name: String, art: String) = changeCards {
        val latest =
            mutable.value.current?.takeIf { it.id == snapshot.id && it.readAt > snapshot.readAt }
                ?: snapshot
        repository.remember(latest, name, art)
    }

    suspend fun savedSnapshot(card: SavedCard): CardSnapshot =
        withContext(Dispatchers.IO) {
            databaseLock.withLock {
                CardSnapshot(
                    card.id,
                    card.balance,
                    card.readAt,
                    repository.history(card.id),
                    card.partial,
                )
            }
        }

    fun edit(id: String, name: String, art: String) = changeCards {
        val previous = repository.cards().find { it.id == id }?.art
        repository.edit(id, name, art)
        if (previous != null) artwork.discard(previous, repository.cards().map { it.art }.toSet())
    }

    fun forget(id: String) = changeCards {
        val previous = repository.cards().find { it.id == id }?.art
        repository.forget(id)
        if (previous != null) artwork.discard(previous, repository.cards().map { it.art }.toSet())
        mutable.update {
            if (it.current?.id == id) it.copy(current = null, syncNotice = false) else it
        }
        if (lastId == id) {
            lastId = ""
            lastReadAt = 0L
        }
    }

    suspend fun importArtwork(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                artwork.import(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(error = "image_error") }
                null
            }
        }

    fun discardArtwork(id: String) = changeCards {
        artwork.discard(id, repository.cards().map { it.art }.toSet())
    }

    fun color(value: ThemeColor) {
        preferences.edit().putString("color", value.name).apply()
        mutable.update { it.copy(preferences = it.preferences.copy(color = value)) }
    }

    fun mode(value: ThemeMode) {
        preferences.edit().putString("mode", value.name).apply()
        mutable.update { it.copy(preferences = it.preferences.copy(mode = value)) }
    }

    private fun changeCards(action: () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseLock.withLock {
                    action()
                    mutable.update { it.copy(cards = repository.cards()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(error = "storage_error") }
            }
        }
}
