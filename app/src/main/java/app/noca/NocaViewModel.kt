package app.noca

import android.app.Application
import android.nfc.Tag
import android.nfc.TagLostException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class NocaViewModel(app:Application):AndroidViewModel(app) {
    private val repo=CardRepository(app)
    private val prefs=app.getSharedPreferences("preferences",0)
    private val busy=AtomicBoolean(false)
    private var lastId="";private var lastAt=0L
    private val mutable=MutableStateFlow(UiState(preferences=Preferences(
        runCatching{ThemeColor.valueOf(prefs.getString("color","LIME")!!)}.getOrDefault(ThemeColor.LIME),
        runCatching{ThemeMode.valueOf(prefs.getString("mode","SYSTEM")!!)}.getOrDefault(ThemeMode.SYSTEM))))
    val state=mutable.asStateFlow()
    init{refresh()}
    private fun refresh(){viewModelScope.launch(Dispatchers.IO){val cards=repo.cards();mutable.update{it.copy(cards=cards)}}}
    fun nfcStatus(available:Boolean,enabled:Boolean){mutable.update{it.copy(nfcAvailable=available,nfcEnabled=enabled)}}
    fun read(tag:Tag){
        if(!busy.compareAndSet(false,true))return
        mutable.update{it.copy(reading=true,error=null,syncNotice=false)}
        viewModelScope.launch(Dispatchers.IO){
            try{
                val snapshot=CardReader().read(tag)
                if(snapshot.id!=lastId || snapshot.readAt-lastAt>1500){
                    val saved=repo.sync(snapshot);val cards=repo.cards()
                    mutable.update{it.copy(current=snapshot,cards=cards,syncNotice=saved)}
                    lastId=snapshot.id;lastAt=snapshot.readAt
                }
            }catch(e:Exception){val message=when(e){is ReadFailure->e.key;is TagLostException->"card_moved";is IOException->"read_failed";else->"invalid_card_data"};mutable.update{it.copy(error=message)}}
            finally{mutable.update{it.copy(reading=false)};busy.set(false)}
        }
    }
    fun dismissError(){mutable.update{it.copy(error=null)}}
    fun remember(s:CardSnapshot,name:String,art:String){viewModelScope.launch(Dispatchers.IO){repo.remember(s,name.trim(),art);mutable.update{it.copy(cards=repo.cards())}}}
    suspend fun savedSnapshot(card:SavedCard):CardSnapshot=withContext(Dispatchers.IO){CardSnapshot(card.id,card.balance,card.readAt,repo.history(card.id))}
    fun edit(id:String,name:String,art:String){viewModelScope.launch(Dispatchers.IO){repo.edit(id,name.trim(),art);mutable.update{it.copy(cards=repo.cards())}}}
    fun color(value:ThemeColor){prefs.edit().putString("color",value.name).apply();mutable.update{it.copy(preferences=it.preferences.copy(color=value))}}
    fun mode(value:ThemeMode){prefs.edit().putString("mode",value.name).apply();mutable.update{it.copy(preferences=it.preferences.copy(mode=value))}}
    override fun onCleared(){repo.close()}
}
