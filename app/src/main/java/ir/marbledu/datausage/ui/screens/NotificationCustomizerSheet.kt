package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.LiveTick
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationLockVisibility
import ir.marbledu.datausage.data.NotificationPreset
import ir.marbledu.datausage.data.NotificationProgressMode
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NotificationTitleMode
import ir.marbledu.datausage.service.NotificationFormatter
import ir.marbledu.datausage.service.NotificationInput
import ir.marbledu.datausage.ui.AppUsageUi
import ir.marbledu.datausage.ui.DashboardUi
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.util.UsageAccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCustomizerSheet(
    settings: AppSettings,
    tick: LiveTick?,
    dashboard: DashboardUi,
    usage: AppUsageUi,
    persian: Boolean,
    onUpdate: ((NotificationConfig) -> NotificationConfig) -> Unit,
    onPreset: (NotificationPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val config = settings.notification

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MIcons.Bell, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.notif_customization),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = { onUpdate { NotificationConfig() } }) {
                    Text(if (persian) "بازگردانی پیش‌فرض" else "Reset")
                }
            }

            // پیش‌نمایش زنده (بالای شیت، همیشه دیده می‌شود)
            NotificationPreviewCard(
                settings = settings,
                tick = tick,
                dashboard = dashboard,
                usage = usage,
                persian = persian,
            )

            // بخش اسکرول‌پذیر تمام تنظیمات
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ------------------------------------------------ پیش‌تنظیم‌های آماده
                CustomizerGroup(if (persian) "چیدمان‌های آماده" else "Quick presets") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NotificationPreset.entries.toList()) { preset ->
                            FilterChip(
                                selected = config == NotificationConfig.fromPreset(preset),
                                onClick = { onPreset(preset) },
                                label = { Text(presetLabel(preset, persian)) },
                            )
                        }
                    }
                }

                // ------------------------------------------------ سبک و تیتر
                CustomizerGroup(if (persian) "سبک و تیتر اعلان" else "Style & title") {
                    Text(
                        text = if (persian) "حالت نمایش" else "Notification layout",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NotificationStyle.entries.forEach { st ->
                            FilterChip(
                                selected = config.style == st,
                                onClick = { onUpdate { it.copy(style = st) } },
                                label = { Text(styleLabel(st, persian)) },
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (persian) "عنوان اصلی اعلان" else "Primary title",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NotificationTitleMode.entries.toList()) { mode ->
                            FilterChip(
                                selected = config.titleMode == mode,
                                onClick = { onUpdate { it.copy(titleMode = mode) } },
                                label = { Text(titleModeLabel(mode, persian)) },
                            )
                        }
                    }

                    if (config.titleMode == NotificationTitleMode.CUSTOM) {
                        OutlinedTextField(
                            value = config.customTitle,
                            onValueChange = { value -> onUpdate { it.copy(customTitle = value) } },
                            label = { Text(if (persian) "الگوی دلخواه تیتر" else "Custom title template") },
                            placeholder = {
                                Text(
                                    if (persian) "{operator} • {speed} • امروز {today}"
                                    else "{operator} • {speed} • {today}",
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val tokens = listOf(
                            "{speed}" to (if (persian) "سرعت" else "Speed"),
                            "{down}" to (if (persian) "دانلود" else "Down"),
                            "{up}" to (if (persian) "آپلود" else "Up"),
                            "{operator}" to (if (persian) "اپراتور" else "Operator"),
                            "{network}" to (if (persian) "شبکه" else "Network"),
                            "{today}" to (if (persian) "امروز" else "Today"),
                            "{mobile}" to (if (persian) "موبایل" else "Mobile"),
                            "{wifi}" to (if (persian) "وای‌فای" else "Wi-Fi"),
                            "{session}" to (if (persian) "نشست" else "Session"),
                            "{cap}" to (if (persian) "مانده بسته" else "Cap left"),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tokens) { (token, desc) ->
                                AssistChip(
                                    onClick = {
                                        val cur = config.customTitle
                                        val sep = if (cur.isBlank() || cur.endsWith(" ")) "" else " • "
                                        onUpdate { it.copy(customTitle = cur + sep + token) }
                                    },
                                    label = { Text("$desc $token") },
                                )
                            }
                        }
                    }
                }

                // ------------------------------------------------ سرعت زنده
                CustomizerGroup(if (persian) "سرعت و قالب اعداد" else "Speed & formatting") {
                    NotifSwitchRow(
                        label = if (persian) "سرعت دانلود (↓)" else "Download speed (↓)",
                        checked = config.showDownloadSpeed,
                    ) { v -> onUpdate { it.copy(showDownloadSpeed = v) } }
                    NotifSwitchRow(
                        label = if (persian) "سرعت آپلود (↑)" else "Upload speed (↑)",
                        checked = config.showUploadSpeed,
                    ) { v -> onUpdate { it.copy(showUploadSpeed = v) } }
                    NotifSwitchRow(
                        label = if (persian) "سرعت مجموع (⇅)" else "Total speed (⇅)",
                        checked = config.showTotalSpeed,
                    ) { v -> onUpdate { it.copy(showTotalSpeed = v) } }
                    NotifSwitchRow(
                        label = if (persian) "واحدهای فشرده و کوتاه" else "Compact units",
                        description = if (persian) "نمایش کوتاه‌تر برای جا شدن بیشتر در نوار بالا" else "Shorter labels to fit more metrics cleanly",
                        checked = config.compactUnits,
                    ) { v -> onUpdate { it.copy(compactUnits = v) } }
                    NotifSwitchRow(
                        label = if (persian) "در زمان بی‌مصرفی «بدون ترافیک» بنویس" else "Show “Idle” when speed is near zero",
                        checked = config.hideZeroSpeed,
                    ) { v -> onUpdate { it.copy(hideZeroSpeed = v) } }
                }

                // ------------------------------------------------ شبکه و اپراتور
                CustomizerGroup(if (persian) "اپراتور و شبکه" else "Operator & network") {
                    NotifSwitchRow(
                        label = if (persian) "نام اپراتور (ایرانسل، همراه اول، شاتل و…)" else "Operator / ISP name",
                        checked = config.showOperator,
                    ) { v -> onUpdate { it.copy(showOperator = v) } }
                    NotifSwitchRow(
                        label = if (persian) "نوع اتصال (وای‌فای / داده موبایل)" else "Connection type (Wi-Fi / Mobile)",
                        checked = config.showConnectionType,
                    ) { v -> onUpdate { it.copy(showConnectionType = v) } }
                    NotifSwitchRow(
                        label = if (persian) "جزئیات فناوری و منبع (4G/5G، مودم/هات‌اسپات، VPN)" else "Network details (4G/5G, Hotspot/Router, VPN)",
                        checked = config.showNetworkTech,
                    ) { v -> onUpdate { it.copy(showNetworkTech = v) } }
                    NotifSwitchRow(
                        label = if (persian) "آدرس IP عمومی یا محلی" else "Public or local IP address",
                        checked = config.showIpAddress,
                    ) { v -> onUpdate { it.copy(showIpAddress = v) } }
                }

                // ------------------------------------------------ آمار مصرف
                CustomizerGroup(if (persian) "آمار مصرف و بسته" else "Usage & data plan") {
                    NotifSwitchRow(
                        label = if (persian) "مجموع مصرف امروز" else "Today's total usage",
                        checked = config.showTodayTotal,
                    ) { v -> onUpdate { it.copy(showTodayTotal = v) } }
                    NotifSwitchRow(
                        label = if (persian) "تفکیک موبایل و وای‌فای امروز" else "Today's mobile / Wi-Fi split",
                        checked = config.showTodayMobileWifi,
                    ) { v -> onUpdate { it.copy(showTodayMobileWifi = v) } }
                    NotifSwitchRow(
                        label = if (persian) "مصرف این نشست و ساعت شروع" else "Current session usage & start time",
                        checked = config.showSessionUsage,
                    ) { v -> onUpdate { it.copy(showSessionUsage = v) } }
                    NotifSwitchRow(
                        label = if (persian) "میانگین سرعت نشست" else "Average speed",
                        checked = config.showAverageSpeed,
                    ) { v -> onUpdate { it.copy(showAverageSpeed = v) } }
                    NotifSwitchRow(
                        label = if (persian) "اوج سرعت ثبت‌شده" else "Peak speed",
                        checked = config.showPeakSpeed,
                    ) { v -> onUpdate { it.copy(showPeakSpeed = v) } }
                    NotifSwitchRow(
                        label = if (persian) "حجم باقی‌مانده از سقف بسته" else "Remaining data cap",
                        checked = config.showDataCap,
                    ) { v -> onUpdate { it.copy(showDataCap = v) } }
                    NotifSwitchRow(
                        label = if (persian) "پرمصرف‌ترین اپ امروز" else "Top app today",
                        checked = config.showTopApp,
                    ) { v -> onUpdate { it.copy(showTopApp = v) } }
                }

                // ------------------------------------------------ نوار پیشرفت، صفحه قفل و دکمه‌ها
                CustomizerGroup(if (persian) "نوار پیشرفت، صفحه قفل و دکمه‌ها" else "Progress bar, lockscreen & actions") {
                    Text(
                        text = if (persian) "نوار پیشرفت اعلان" else "Notification progress bar",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NotificationProgressMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.progressMode == mode,
                                onClick = { onUpdate { it.copy(progressMode = mode) } },
                                label = { Text(progressModeLabel(mode, persian)) },
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (persian) "نمایش در صفحه قفل" else "Lockscreen visibility",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NotificationLockVisibility.entries.forEach { vis ->
                            FilterChip(
                                selected = config.lockVisibility == vis,
                                onClick = { onUpdate { it.copy(lockVisibility = vis) } },
                                label = { Text(lockVisLabel(vis, persian)) },
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp))

                    NotifSwitchRow(
                        label = context.getString(R.string.notif_action_recheck),
                        description = if (persian) "دکمه‌ی بررسی فوری اپراتور روی اعلان" else "Quick button to re-check current ISP",
                        checked = config.actionRecheckIsp,
                    ) { v -> onUpdate { it.copy(actionRecheckIsp = v) } }
                    NotifSwitchRow(
                        label = context.getString(R.string.notif_action_reports),
                        description = if (persian) "باز کردن مستقیم تب گزارش ۳۰ روز" else "Open 30-day reports tab directly",
                        checked = config.actionOpenReports,
                    ) { v -> onUpdate { it.copy(actionOpenReports = v) } }
                    NotifSwitchRow(
                        label = context.getString(R.string.notif_action_stop),
                        description = if (persian) "دکمه‌ی توقف سرویس پایش از روی اعلان" else "Stop background monitoring from the notification",
                        checked = config.actionStop,
                    ) { v -> onUpdate { it.copy(actionStop = v) } }

                    TextButton(
                        onClick = { UsageAccess.openNotificationSettings(context) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(context.getString(R.string.notif_system_settings))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPreviewCard(
    settings: AppSettings,
    tick: LiveTick?,
    dashboard: DashboardUi,
    usage: AppUsageUi,
    persian: Boolean,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val config = settings.notification
    val net = tick?.network
    val sampleOp = when {
        net != null && net.type != ConnectionType.OFFLINE -> net.operator.name(persian)
        else -> OperatorCatalog.byId("irancell").name(persian)
    }
    val topApp = usage.mobile.firstOrNull() ?: usage.wifi.firstOrNull()
    val topAppName = topApp?.let { OperatorCatalog.specialUidLabel(it.uid, persian) ?: it.label }.orEmpty()
    val topAppBytes = topApp?.bytes ?: (280L * 1024L * 1024L)

    val input = NotificationInput(
        downBytesPerSec = tick?.downBytesPerSecond ?: (1_450_000.0),
        upBytesPerSec = tick?.upBytesPerSecond ?: (215_000.0),
        averageSpeedBps = tick?.averageSpeed ?: (840_000.0),
        peakSpeedBps = tick?.peakSpeed ?: (3_200_000.0),
        operatorName = sampleOp,
        connectionType = net?.type ?: ConnectionType.WIFI,
        wifiSourceKind = net?.wifiSourceKind ?: IspResolver.WifiSourceKind.MODEM_ROUTER,
        wifiStatus = net?.wifiLookupStatus ?: IspResolver.LookupStatus.RESOLVED,
        mobileTech = net?.mobileTech.orEmpty(),
        vpnActive = net?.vpnActive ?: false,
        ipAddress = net?.ipAddress ?: "192.168.1.12",
        publicIp = net?.publicIp ?: "5.112.42.10",
        todayMobileBytes = dashboard.todayMobile.total.takeIf { it > 0 } ?: (420L * 1024L * 1024L),
        todayWifiBytes = dashboard.todayWifi.total.takeIf { it > 0 } ?: (1_300L * 1024L * 1024L),
        sessionRxBytes = tick?.sessionRxBytes ?: (95L * 1024L * 1024L),
        sessionTxBytes = tick?.sessionTxBytes ?: (18L * 1024L * 1024L),
        sessionStartMillis = tick?.sessionStart ?: (System.currentTimeMillis() - 45 * 60_000L),
        capTotalBytes = settings.dataCapBytes.takeIf { it > 0 } ?: (30L * 1024L * 1024L * 1024L),
        capUsedBytes = dashboard.monthMobileBytes.takeIf { it > 0 } ?: (12L * 1024L * 1024L * 1024L),
        topAppName = topAppName.ifBlank { if (persian) "تلگرام" else "Telegram" },
        topAppBytes = topAppBytes,
    )

    val preview = NotificationFormatter.format(
        input = input,
        config = config,
        persian = persian,
        appMonitoringTitle = context.getString(R.string.notif_title),
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MIcons.Bell, null, Modifier.size(12.dp), tint = scheme.onPrimary)
                }
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                )
                if (!preview.subText.isNullOrBlank()) {
                    Text(
                        text = "• ${preview.subText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (persian) "پیش‌نمایش اعلان" else "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Text(
                text = preview.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = preview.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = if (config.style == NotificationStyle.COMPACT) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (preview.expandedLines.isNotEmpty()) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                preview.expandedLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (preview.progressPercent != null) {
                LinearProgressIndicator(
                    progress = { (preview.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            }

            val actionLabels = buildList {
                if (config.actionRecheckIsp) add(context.getString(R.string.notif_action_recheck))
                if (config.actionOpenReports) add(context.getString(R.string.notif_action_reports))
                if (config.actionStop) add(context.getString(R.string.notif_action_stop))
            }
            if (actionLabels.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
                    actionLabels.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = scheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizerGroup(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun NotifSwitchRow(
    label: String,
    checked: Boolean,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun presetLabel(preset: NotificationPreset, persian: Boolean): String = when (preset) {
    NotificationPreset.FULL_DASHBOARD -> if (persian) "داشبورد کامل" else "Full dashboard"
    NotificationPreset.MINIMAL_SPEED -> if (persian) "سرعت مینیمال" else "Minimal speed"
    NotificationPreset.DATA_CAP_FOCUS -> if (persian) "تمرکز بر بسته" else "Data cap focus"
    NotificationPreset.OPERATOR_FOCUS -> if (persian) "اپراتور و شبکه" else "Operator & net"
}

private fun styleLabel(style: NotificationStyle, persian: Boolean): String = when (style) {
    NotificationStyle.COMPACT -> if (persian) "فشرده" else "Compact"
    NotificationStyle.BALANCED -> if (persian) "متعادل" else "Balanced"
    NotificationStyle.EXPANDED -> if (persian) "گسترده" else "Expanded"
}

private fun titleModeLabel(mode: NotificationTitleMode, persian: Boolean): String = when (mode) {
    NotificationTitleMode.SPEED -> if (persian) "سرعت زنده" else "Live speed"
    NotificationTitleMode.OPERATOR -> if (persian) "نام اپراتور" else "Operator"
    NotificationTitleMode.TODAY_USAGE -> if (persian) "مصرف امروز" else "Today's usage"
    NotificationTitleMode.APP_NAME -> if (persian) "پیش‌فرض برنامه" else "App name"
    NotificationTitleMode.CUSTOM -> if (persian) "الگوی دلخواه" else "Custom"
}

private fun progressModeLabel(mode: NotificationProgressMode, persian: Boolean): String = when (mode) {
    NotificationProgressMode.NONE -> if (persian) "خاموش" else "None"
    NotificationProgressMode.DATA_CAP -> if (persian) "سقف بسته" else "Data cap"
    NotificationProgressMode.MOBILE_SHARE -> if (persian) "سهم موبایل" else "Mobile share"
}

private fun lockVisLabel(vis: NotificationLockVisibility, persian: Boolean): String = when (vis) {
    NotificationLockVisibility.PUBLIC -> if (persian) "کامل" else "Public"
    NotificationLockVisibility.PRIVATE -> if (persian) "خلاصه" else "Private"
    NotificationLockVisibility.HIDDEN -> if (persian) "پنهان" else "Hidden"
}
