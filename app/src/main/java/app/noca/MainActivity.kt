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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {
    private val viewModel: NocaViewModel by viewModels()
    private val adapter by lazy { NfcAdapter.getDefaultAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) },
                )
            }
                .onFailure { viewModel.nfcStatus(true, false) }
        }
    }

    override fun onPause() {
        runCatching { adapter?.disableReaderMode(this) }
        super.onPause()
    }
}
