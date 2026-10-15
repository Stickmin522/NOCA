package app.noca

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class NocaColors(val accent:Color,val ink:Color,val background:Color,val surface:Color,val raised:Color,val shadow:Color,val highlight:Color)
val LocalNocaColors=staticCompositionLocalOf{NocaColors(Color(0xffd2ed7a),Color(0xff242b20),Color(0xfff4f6ed),Color(0xffeef1e6),Color(0xfff8faf3),Color(0xffcad0bc),Color.White)}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun NocaTheme(p:Preferences,content:@Composable()->Unit){
    val dark=when(p.mode){ThemeMode.SYSTEM->isSystemInDarkTheme();ThemeMode.DARK->true;ThemeMode.LIGHT->false}
    val accent=when(p.color){ThemeColor.LIME->Color(0xffd2ed7a);ThemeColor.MIST->Color(0xffbfd3fa);ThemeColor.CORAL->Color(0xffffb09b)}
    val ink=when(p.color){ThemeColor.LIME->Color(0xff263719);ThemeColor.MIST->Color(0xff203454);ThemeColor.CORAL->Color(0xff522e23)}
    val bg=if(dark)when(p.color){ThemeColor.LIME->Color(0xff191d17);ThemeColor.MIST->Color(0xff171d27);ThemeColor.CORAL->Color(0xff241a18)}else when(p.color){ThemeColor.LIME->Color(0xfff4f6ed);ThemeColor.MIST->Color(0xfff0f3f8);ThemeColor.CORAL->Color(0xfffaf1eb)}
    val surface=if(dark)when(p.color){ThemeColor.LIME->Color(0xff252b21);ThemeColor.MIST->Color(0xff252e3c);ThemeColor.CORAL->Color(0xff352723)}else when(p.color){ThemeColor.LIME->Color(0xffeef1e6);ThemeColor.MIST->Color(0xffe8edf5);ThemeColor.CORAL->Color(0xfff2e6de)}
    val raised=if(dark)Color(0xff30372d)else when(p.color){ThemeColor.LIME->Color(0xfff9fbf3);ThemeColor.MIST->Color(0xfff7faff);ThemeColor.CORAL->Color(0xfffff7f1)}
    val onBg=if(dark)Color(0xffeeeae4)else ink
    val scheme=if(dark)darkColorScheme(primary=accent,onPrimary=ink,primaryContainer=accent,onPrimaryContainer=ink,background=bg,onBackground=onBg,surface=surface,onSurface=onBg,surfaceContainer=surface,surfaceContainerLow=bg,surfaceContainerHigh=raised,outline=onBg.copy(alpha=.28f),outlineVariant=onBg.copy(alpha=.12f))
        else lightColorScheme(primary=ink,onPrimary=accent,primaryContainer=accent,onPrimaryContainer=ink,background=bg,onBackground=onBg,surface=surface,onSurface=onBg,surfaceContainer=surface,surfaceContainerLow=bg,surfaceContainerHigh=raised,outline=ink.copy(alpha=.28f),outlineVariant=ink.copy(alpha=.12f))
    val base=Typography()
    val type=Typography(displayLarge=base.displayLarge.copy(fontFamily=FontFamily.Default),displayMedium=base.displayMedium.copy(fontFamily=FontFamily.Default),headlineLarge=base.headlineLarge.copy(fontFamily=FontFamily.Default),headlineMedium=base.headlineMedium.copy(fontFamily=FontFamily.Default))
    CompositionLocalProvider(LocalNocaColors provides NocaColors(accent,ink,bg,surface,raised,if(dark)Color.Black else ink.copy(alpha=.14f),if(dark)onBg.copy(alpha=.04f)else Color.White)){
        MaterialExpressiveTheme(colorScheme=scheme,typography=type,content=content)
    }
}
