@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package app.noca

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.os.LocaleListCompat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Page {
    SCAN,
    WALLET,
    SETTINGS,
}

private fun yen(amount: Int): String =
    NumberFormat.getCurrencyInstance()
        .apply {
            currency = java.util.Currency.getInstance("JPY")
            maximumFractionDigits = 0
        }
        .format(amount)

private fun timestamp(time: Long): String =
    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
        .format(Date(time))

private fun transactionDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrDefault(value)

private class TicketShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { 28.dp.toPx() }
        val notch = with(density) { 11.dp.toPx() }
        val base =
            Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius))) }
        val holes =
            Path().apply {
                addOval(Rect(-notch, size.height * .5f - notch, notch, size.height * .5f + notch))
                addOval(
                    Rect(
                        size.width - notch,
                        size.height * .5f - notch,
                        size.width + notch,
                        size.height * .5f + notch,
                    )
                )
            }
        return Outline.Generic(Path.combine(PathOperation.Difference, base, holes))
    }
}

@Composable
private fun SoftPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalNocaColors.current
    Surface(
        modifier.shadow(5.dp, shape, ambientColor = c.shadow, spotColor = c.shadow),
        shape = shape,
        color = color ?: c.raised,
        contentColor = if (color != null) c.ink else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, c.highlight.copy(alpha = c.highlight.alpha * .55f)),
    ) {
        Column(content = content)
    }
}

@Composable
fun NocaUi(state: UiState, vm: NocaViewModel, openNfc: () -> Unit) {
    val t = texts()
    var page by rememberSaveable { mutableStateOf(Page.SCAN) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var savedSnapshot by remember { mutableStateOf<CardSnapshot?>(null) }
    var historyError by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<CardSnapshot?>(null) }
    var editDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    val selectedCard = state.cards.firstOrNull { it.id == selected }
    val currentSaved = state.cards.firstOrNull { it.id == state.current?.id }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(selected, state.cards, state.current?.readAt) {
        savedSnapshot = null
        historyError = false
        try {
            savedSnapshot = selectedCard?.let { vm.savedSnapshot(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            historyError = true
        }
    }
    LaunchedEffect(state.current?.readAt) {
        if (state.current != null) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    BackHandler(selected != null || page != Page.SCAN) {
        if (selected != null) selected = null else page = Page.SCAN
    }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snack.showSnackbar(t[it])
            vm.dismissError()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Row(
                Modifier.statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected != null)
                    IconButton(onClick = { selected = null }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, t["back"])
                    }
                else
                    Icon(
                        Icons.Outlined.Nfc,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp),
                    )
                Text(
                    "NOCA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                FilledTonalIconButton(
                    onClick = {
                        if (selectedCard != null) editDialog = true
                        else {
                            selected = null
                            page = Page.SETTINGS
                        }
                    }
                ) {
                    Icon(
                        if (selectedCard != null) Icons.Outlined.Edit else Icons.Outlined.Palette,
                        if (selectedCard != null) t["edit"] else t["settings"],
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(12.dp, 4.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(6.dp)) {
                        val nav =
                            listOf(
                                Triple(Page.SCAN, "scan", Icons.Outlined.Nfc),
                                Triple(Page.WALLET, "wallet", Icons.Outlined.Wallet),
                                Triple(Page.SETTINGS, "settings", Icons.Outlined.Settings),
                            )
                        nav.forEach { (target, key, icon) ->
                            val active = page == target
                            Surface(
                                shape = RoundedCornerShape(25.dp),
                                color =
                                    if (active) LocalNocaColors.current.accent
                                    else Color.Transparent,
                                modifier =
                                    Modifier.weight(1f).clickable {
                                        selected = null
                                        page = target
                                    },
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        icon,
                                        null,
                                        tint =
                                            if (active) LocalNocaColors.current.ink
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(23.dp),
                                    )
                                    Text(
                                        t[key],
                                        style = MaterialTheme.typography.labelMedium,
                                        color =
                                            if (active) LocalNocaColors.current.ink
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (selectedCard != null) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        selectedCard.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CardArt(
                        selectedCard.art,
                        Modifier.width(200.dp).align(Alignment.CenterHorizontally),
                    )
                    BalanceTicket(
                        selectedCard.balance,
                        t["last_read"] + " " + timestamp(selectedCard.readAt),
                        false,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                selected = null
                                page = Page.SCAN
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Nfc, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(t["rescan"])
                        }
                        FilledTonalButton(
                            onClick = { editDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(t["edit"])
                        }
                    }
                    if (historyError)
                        Text(t["storage_error"], style = MaterialTheme.typography.bodyMedium)
                    else if (savedSnapshot == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else {
                        if (savedSnapshot?.partial == true)
                            Text(t["partial_history"], style = MaterialTheme.typography.bodySmall)
                        History(savedSnapshot?.history.orEmpty(), t["history"])
                    }
                    TextButton(
                        onClick = { deleteDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t["forget_card"])
                    }
                }
            } else
                when (page) {
                    Page.SCAN ->
                        Column(
                            Modifier.verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp)
                                .padding(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Column {
                                Text(
                                    t["title"],
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-1.3).sp,
                                )
                                Text(
                                    t["scan_hint"],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            if (!state.nfcAvailable || !state.nfcEnabled) {
                                SoftPanel {
                                    Column(
                                        Modifier.padding(22.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(Icons.Outlined.Nfc, null)
                                        Text(
                                            t[if (!state.nfcAvailable) "nfc_missing" else "nfc_off"]
                                        )
                                        if (state.nfcAvailable)
                                            Button(onClick = openNfc) { Text(t["open_nfc"]) }
                                    }
                                }
                            }
                            AnimatedContent(targetState = state.current, label = "read-result") {
                                snapshot ->
                                if (snapshot == null) {
                                    if (state.nfcAvailable && state.nfcEnabled)
                                        WaitingPanel(state.reading)
                                } else
                                    BalanceTicket(
                                        snapshot.balance,
                                        t["last_read"] + " " + timestamp(snapshot.readAt),
                                        state.reading,
                                    )
                            }
                            if (state.current != null) {
                                FilledTonalButton(
                                    onClick = {
                                        if (currentSaved == null) pendingSave = state.current
                                        else {
                                            selected = currentSaved.id
                                            page = Page.WALLET
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                    shape = RoundedCornerShape(26.dp),
                                ) {
                                    Icon(
                                        if (currentSaved == null) Icons.Outlined.BookmarkAdd
                                        else Icons.Outlined.BookmarkAdded,
                                        null,
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Text(
                                        if (currentSaved == null) t["remember"]
                                        else currentSaved.name
                                    )
                                }
                                if (state.syncNotice)
                                    Text(
                                        t["synced"],
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                if (state.current.partial)
                                    Text(
                                        t["partial_history"],
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                            }
                            History(state.current?.history.orEmpty(), t["recent"])
                        }
                    Page.WALLET ->
                        Column(
                            Modifier.verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp)
                                .padding(bottom = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Column {
                                Text(
                                    t["my_cards"],
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    t["sync_hint"],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            if (state.cards.isEmpty())
                                EmptyState(Icons.Outlined.Wallet, t["no_cards"], t["no_cards_hint"])
                            state.cards.forEach { card ->
                                SoftPanel(
                                    Modifier.fillMaxWidth().clickable { selected = card.id },
                                    shape = TicketShape(),
                                    color = LocalNocaColors.current.accent,
                                ) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        CardArt(card.art, Modifier.width(92.dp))
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                card.name,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                yen(card.balance),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                t["last_read"] + " " + timestamp(card.readAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                    LocalNocaColors.current.ink.copy(alpha = .75f),
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            null,
                                            Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    Page.SETTINGS ->
                        SettingsScreen(state.preferences, vm, { languageDialog = true })
                }
        }
    }
    pendingSave?.let { snapshot ->
        CardNameDialog(t["save_card"], "", { pendingSave = null }) { name ->
            vm.remember(snapshot, name, "generic")
            pendingSave = null
        }
    }
    if (editDialog && selectedCard != null)
        CardEditor(selectedCard, vm, { editDialog = false }) { name, art ->
            vm.edit(selectedCard.id, name, art)
            editDialog = false
        }
    if (deleteDialog && selectedCard != null)
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(t["forget_card"]) },
            text = { Text(t["forget_message"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.forget(selectedCard.id)
                        deleteDialog = false
                        selected = null
                        page = Page.WALLET
                    }
                ) {
                    Text(t["delete"])
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) { Text(t["cancel"]) }
            },
        )
    if (languageDialog) LanguageDialog { languageDialog = false }
}

@Composable
private fun WaitingPanel(reading: Boolean) {
    val t = texts()
    val c = LocalNocaColors.current
    Surface(
        shape =
            RoundedCornerShape(
                topStart = 38.dp,
                topEnd = 82.dp,
                bottomEnd = 38.dp,
                bottomStart = 38.dp,
            ),
        color = c.accent,
        modifier = Modifier.fillMaxWidth().heightIn(min = 246.dp),
    ) {
        Column(
            Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = lerp(c.accent, Color.White, .2f),
                shadowElevation = 6.dp,
            ) {
                Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Nfc, null, tint = c.ink, modifier = Modifier.size(43.dp))
                }
            }
            if (reading)
                LinearProgressIndicator(
                    Modifier.width(110.dp),
                    color = c.ink,
                    trackColor = c.ink.copy(alpha = .1f),
                )
            else Text(t["nfc_ready"], style = MaterialTheme.typography.labelMedium, color = c.ink)
            Text(
                t[if (reading) "reading" else "waiting"],
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BalanceTicket(balance: Int, caption: String, reading: Boolean) {
    val t = texts()
    val c = LocalNocaColors.current
    val shape = remember { TicketShape() }
    Surface(
        shape = shape,
        color = c.accent,
        modifier = Modifier.fillMaxWidth().heightIn(min = 222.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 26.dp, vertical = 25.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(t["balance"], style = MaterialTheme.typography.labelLarge, color = c.ink)
            BasicText(
                yen(balance),
                modifier = Modifier.fillMaxWidth(),
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = c.ink,
                    ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 56.sp),
            )
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(
                    c.ink.copy(alpha = .25f),
                    Offset.Zero,
                    Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                if (reading)
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = c.ink,
                        strokeWidth = 2.dp,
                    )
                else
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = c.ink,
                        modifier = Modifier.size(19.dp),
                    )
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null) {
    SoftPanel(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                null,
                Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun History(history: List<Transaction>, title: String) {
    val t = texts()
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (history.isEmpty()) EmptyState(Icons.Outlined.History, t["empty_history"])
        else {
            SoftPanel(Modifier.fillMaxWidth()) {
                history.forEachIndexed { index, item ->
                    val icon =
                        when (item.kind) {
                            "transit" -> Icons.Outlined.Train
                            "topup" -> Icons.Outlined.AddCircleOutline
                            "purchase" -> Icons.Outlined.ShoppingBag
                            else -> Icons.AutoMirrored.Outlined.ReceiptLong
                        }
                    val kind =
                        when (item.kind) {
                            "transit" -> "train"
                            "topup" -> "topup"
                            "purchase" -> "purchase"
                            else -> "other"
                        }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                Icon(icon, null, Modifier.size(21.dp))
                            }
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(t[kind], style = MaterialTheme.typography.bodyLarge)
                            Text(
                                transactionDate(item.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                item.delta?.let {
                                    (if (it > 0) "+" else if (it < 0) "−" else "") +
                                        yen(kotlin.math.abs(it))
                                } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                t["balance"] + " " + yen(item.balance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < history.lastIndex)
                        HorizontalDivider(
                            Modifier.padding(horizontal = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                }
            }
            Text(
                t["amount_note"],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                t["records_limit"],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsScreen(p: Preferences, vm: NocaViewModel, language: () -> Unit) {
    val t = texts()
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            t["settings"],
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(t["language_font"], style = MaterialTheme.typography.titleSmall)
        SoftPanel {
            SettingsRow(
                Icons.Outlined.Language,
                t["app_language"],
                if (tag.isBlank()) t["system"]
                else Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(tag)),
                language,
            )
            HorizontalDivider(
                Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SettingsRow(Icons.Outlined.TextFields, t["font"], t["system_font"], null)
            Text(
                t["font_hint"],
                Modifier.padding(start = 53.dp, end = 18.dp, bottom = 17.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(t["appearance"], style = MaterialTheme.typography.titleSmall)
        SoftPanel {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t["theme_mode"], style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = p.mode == mode,
                            onClick = { vm.mode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                        ) {
                            Text(
                                t[
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "system"
                                        ThemeMode.LIGHT -> "light"
                                        ThemeMode.DARK -> "dark"
                                    }],
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        Text(t["theme_color"], style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ThemeColor.entries.forEach { color ->
                val accent =
                    when (color) {
                        ThemeColor.LIME -> Color(0xffd2ed7a)
                        ThemeColor.MIST -> Color(0xffbfd3fa)
                        ThemeColor.CORAL -> Color(0xffffb09b)
                    }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border =
                        BorderStroke(
                            if (p.color == color) 2.dp else 1.dp,
                            if (p.color == color) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    modifier = Modifier.weight(1f).clickable { vm.color(color) },
                ) {
                    Column(
                        Modifier.padding(horizontal = 7.dp, vertical = 17.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = p.color == color, onClick = null)
                        Text(
                            t[
                                when (color) {
                                    ThemeColor.LIME -> "lime"
                                    ThemeColor.MIST -> "mist"
                                    ThemeColor.CORAL -> "coral"
                                }],
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                        Box(
                            Modifier.fillMaxWidth()
                                .height(44.dp)
                                .clip(TicketShape())
                                .background(accent)
                        )
                    }
                }
            }
        }
        Text(
            t["local_only"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 135.dp),
        )
        if (onClick != null)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(17.dp))
    }
}

@Composable
private fun CardNameDialog(
    title: String,
    initial: String,
    dismiss: () -> Unit,
    save: (String) -> Unit,
) {
    val t = texts()
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Icon(Icons.Outlined.BookmarkAdd, null) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { if (it.length <= 40) name = it },
                label = { Text(t["card_name"]) },
                placeholder = { Text(t["name_hint"]) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(onDone = { if (name.isNotBlank()) save(name.trim()) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { save(name.trim()) }, enabled = name.isNotBlank()) {
                Text(t["save"])
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(t["cancel"]) } },
    )
}

@Composable
private fun CardEditor(
    card: SavedCard,
    vm: NocaViewModel,
    dismiss: () -> Unit,
    save: (String, String) -> Unit,
) {
    val t = texts()
    val context = LocalContext.current
    val arts = remember { artworks(context) }
    var name by rememberSaveable(card.id) { mutableStateOf(card.name) }
    var art by rememberSaveable(card.id) { mutableStateOf(card.art) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null)
                scope.launch {
                    importing = true
                    val imported = vm.importArtwork(uri)
                    if (imported != null) {
                        if (art != card.art) vm.discardArtwork(art)
                        art = imported
                    }
                    importing = false
                }
        }
    fun cancel() {
        if (!importing) {
            if (art != card.art) vm.discardArtwork(art)
            dismiss()
        }
    }
    AlertDialog(
        onDismissRequest = { cancel() },
        title = { Text(t["edit_card"]) },
        text = {
            Column(
                Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                CardArt(art, Modifier.width(190.dp).align(Alignment.CenterHorizontally))
                OutlinedTextField(
                    name,
                    { if (it.length <= 40) name = it },
                    label = { Text(t["card_name"]) },
                    singleLine = true,
                )
                Text(t["choose_art"], style = MaterialTheme.typography.titleSmall)
                arts.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { a ->
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                border =
                                    BorderStroke(
                                        if (art == a.id) 2.dp else 1.dp,
                                        if (art == a.id) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                modifier =
                                    Modifier.weight(1f).clickable(enabled = !importing) {
                                        if (art != card.art) vm.discardArtwork(art)
                                        art = a.id
                                    },
                            ) {
                                Column(
                                    Modifier.padding(7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    CardArt(a.id, Modifier.fillMaxWidth())
                                    Text(
                                        if (a.id == "generic") t["art_generic"] else a.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t["choose_photo"])
                }
                if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(t["card_art_hint"], style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { save(name.trim(), art) },
                enabled = name.isNotBlank() && !importing,
            ) {
                Text(t["save"])
            }
        },
        dismissButton = {
            TextButton(onClick = { cancel() }, enabled = !importing) { Text(t["cancel"]) }
        },
    )
}

@Composable
private fun LanguageDialog(dismiss: () -> Unit) {
    val t = texts()
    val context = LocalContext.current
    val languages = remember { languageOptions(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(t["app_language"]) },
        text = {
            Column {
                OutlinedTextField(
                    query,
                    { query = it },
                    singleLine = true,
                    label = { Text(t["search_language"]) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { chosen = "" }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(chosen.isEmpty(), onClick = { chosen = "" })
                            Text(t["system"])
                        }
                    }
                    items(
                        languages.filter {
                            it.label.contains(query, true) ||
                                it.tag.contains(query, true) ||
                                Locale.forLanguageTag(it.tag)
                                    .getDisplayName(Locale.ENGLISH)
                                    .contains(query, true)
                        },
                        key = { it.tag },
                    ) { lang ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { chosen = lang.tag }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(chosen == lang.tag, onClick = { chosen = lang.tag })
                            Text(lang.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dismiss()
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(chosen)
                    )
                }
            ) {
                Text(t["done"])
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(t["cancel"]) } },
    )
}
