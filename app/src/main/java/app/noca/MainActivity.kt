package app.noca

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity:AppCompatActivity() {
    private val vm:NocaViewModel by viewModels()
    private val adapter by lazy{NfcAdapter.getDefaultAdapter(this)}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        setContent{val state by vm.state.collectAsStateWithLifecycle();NocaTheme(state.preferences){NocaUi(state,vm){startActivity(Intent(Settings.ACTION_NFC_SETTINGS))}}}
    }
    override fun onResume(){super.onResume();vm.nfcStatus(adapter!=null,adapter?.isEnabled==true)
        if(adapter?.isEnabled==true) adapter?.enableReaderMode(this,{vm.read(it)},NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,Bundle().apply{putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,250)})
    }
    override fun onPause(){adapter?.disableReaderMode(this);super.onPause()}
}
