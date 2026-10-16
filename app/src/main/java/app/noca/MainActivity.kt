package app.noca

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: NocaViewModel by viewModels()
    private val adapter by lazy { NfcAdapter.getDefaultAdapter(this) }
    private val scanSound = ScanSoundPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state
                    .map { it.preferences.soundEnabled }
                    .distinctUntilChanged()
                    .collectLatest { enabled ->
                        if (enabled) viewModel.feedback.collectLatest { scanSound.play(it) }
                        else scanSound.stop()
                    }
            }
        }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark =
                when (state.preferences.mode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            NocaTheme(state.preferences) {
                NocaUi(state, viewModel) {
                    runCatching { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val nfc = adapter
        viewModel.nfcStatus(nfc != null, nfc?.isEnabled == true)
        if (nfc?.isEnabled == true) {
            runCatching {
                nfc.enableReaderMode(
                    this,
                    { viewModel.read(it) },
                    NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) },
                )
            }
                .onFailure { viewModel.nfcStatus(true, false) }
        }
    }

    override fun onPause() {
        runCatching { adapter?.disableReaderMode(this) }
        scanSound.stop()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanSound.close()
    }
}
