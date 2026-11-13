package ir.marbledu.datausage.ui.screens
import android.content.Intent
import android.Manifest
import android.provider.Settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.BuildConfig
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppTextSize
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationPreset
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.data.UiMode
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.LargeTitleScaffold
import ir.marbledu.datausage.ui.JalaliDatePickerDialog
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.theme.ThemePalette
import ir.marbledu.datausage.util.SettingsBackup
import ir.marbledu.datausage.util.UsageAccess
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val tick by vm.tick.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val usage by vm.usage.collectAsState()

    var showCapDialog by remember { mutableStateOf(false) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var showCarrierDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showNotifSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTariffDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showEpochPicker by remember { mutableStateOf(false) }
    var showRestorePicker by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val settingsTabs = if (persian) listOf("ظاهری", "پایش و اعلان‌ها", "داده‌ها و درباره")
    else listOf("Appearance", "Monitoring", "Data & about")
    val listState = rememberLazyListState()
    LargeTitleScaffold(
        title = if (persian) "تنظیمات" else "Settings",
        listState = listState,
        tabs = settingsTabs,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    ) {
        if (selectedTab == 0) {
        // ------------------------------------------------------------- زبان
        item {
            SettingsCard(
                title = context.getString(R.string.language),
                description = context.getString(R.string.language_desc),
                icon = MIcons.Globe,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionChip(context.getString(R.string.persian), settings.language == AppLanguage.PERSIAN) {
                        vm.setLanguage(AppLanguage.PERSIAN)
                    }
                    OptionChip(context.getString(R.string.english), settings.language == AppLanguage.ENGLISH) {
                        vm.setLanguage(AppLanguage.ENGLISH)
                    }
                    OptionChip(context.getString(R.string.system_language), settings.language == AppLanguage.SYSTEM) {
                        vm.setLanguage(AppLanguage.SYSTEM)
                    }
                }
            }
        }

        // ------------------------------------------------------------- پوسته
        item {
            SettingsCard(
                title = context.getString(R.string.theme),
                icon = MIcons.Settings,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionChip(context.getString(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) {
                        vm.setTheme(ThemeMode.SYSTEM)
                    }
                    OptionChip(context.getString(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) {
                        vm.setTheme(ThemeMode.LIGHT)
                    }
                    OptionChip(context.getString(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) {
                        vm.setTheme(ThemeMode.DARK)
                    }
                }
                // تم کامل AMOLED: سیاه مطلق، مناسب پنل‌های OLED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (settings.themeMode == ThemeMode.AMOLED) Color(0xFF0B0B0F) else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { vm.setTheme(ThemeMode.AMOLED) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(ThemePalette.of(settings.palette).primary),
                                        Color.Black,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            MIcons.Palette,
                            null,
                            Modifier.size(17.dp),
                            tint = ir.marbledu.datausage.ui.theme.bestOnColor(
                                Color(ThemePalette.of(settings.palette).primary),
                            ),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.theme_amoled),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.themeMode == ThemeMode.AMOLED) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = context.getString(R.string.theme_amoled_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.themeMode == ThemeMode.AMOLED) Color(0xFFB7BBC6) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (settings.themeMode == ThemeMode.AMOLED) {
                        Icon(
                            MIcons.Check,
                            null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------ پالت رنگی
        item {
            SettingsCard(
                title = if (persian) "تم رنگی" else "Color theme",
                description = if (persian) "یک رنگ انتخاب کن؛ کل اپ، نمودارها و نوار پایین همان رنگ می‌شوند"
                else "Pick a color – the whole app, charts and the bottom bar follow it",
                icon = MIcons.Palette,
            ) {
                val current = ThemePalette.of(settings.palette)
                val dynamicAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                val dynamicOn = settings.dynamicColor && dynamicAvailable
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(ThemePalette.entries.toList()) { palette ->
                        PaletteSwatch(
                            name = palette.name(persian),
                            colors = listOf(Color(palette.primary), Color(palette.tertiary), Color(palette.secondary)),
                            selected = !dynamicOn && current == palette,
                            onClick = { vm.setPalette(palette.name, dynamic = false) },
                        )
                    }
                    if (dynamicAvailable) {
                        item {
                            PaletteSwatch(
                                name = if (persian) "والپیپر" else "Wallpaper",
                                colors = listOf(Color(0xFF8AB4F8), Color(0xFFF28B82), Color(0xFF81C995)),
                                selected = dynamicOn,
                                onClick = { vm.setPalette(settings.palette, dynamic = true) },
                            )
                        }
                    }
                }
                Text(
                    text = (if (persian) "انتخاب فعلی: " else "Current: ") +
                        (if (dynamicOn) context.getString(R.string.dynamic_color) else current.name(persian)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ------------------------------------------------------- حالت نمایش
        item {
            SettingsCard(
                title = context.getString(R.string.ui_mode),
                description = context.getString(R.string.ui_mode_desc),
                icon = MIcons.Layout,
                onClick = {},
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionChip(
                        context.getString(R.string.ui_mode_minimal),
                        settings.uiMode == UiMode.MINIMAL,
                    ) { vm.setUiMode(UiMode.MINIMAL) }
                    OptionChip(
                        context.getString(R.string.ui_mode_advanced),
                        settings.uiMode == UiMode.ADVANCED,
                    ) { vm.setUiMode(UiMode.ADVANCED) }
                }
                Text(
                    text = if (persian) {
                        when (settings.uiMode) {
                            UiMode.MINIMAL -> "رابطی آرام، مرتب و ساده‌تر برای مبتدیان و کاربران عادی"
                            UiMode.ADVANCED -> "جزئیات کامل، گزارش‌های پیشرفته و امکانات حرفه‌ای"
                        }
                    } else {
                        when (settings.uiMode) {
                            UiMode.MINIMAL -> "Calm, neat and simple UI for beginners and regular users"
                            UiMode.ADVANCED -> "Full details, advanced reports and professional features"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ------------------------------------------------------ اندازه‌ی متن
        item {
            SettingsCard(
                title = if (persian) "اندازه‌ی متن" else "Text size",
                description = if (persian) "روی همه‌ی متن‌های برنامه اثر می‌گذارد" else "Scales text across the app",
                icon = MIcons.Tune,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(if (persian) "کوچک" else "Small", settings.textSize == AppTextSize.SMALL) {
                        vm.setTextSize(AppTextSize.SMALL)
                    }
                    OptionChip(if (persian) "معمولی" else "Medium", settings.textSize == AppTextSize.MEDIUM) {
                        vm.setTextSize(AppTextSize.MEDIUM)
                    }
                    OptionChip(if (persian) "بزرگ" else "Large", settings.textSize == AppTextSize.LARGE) {
                        vm.setTextSize(AppTextSize.LARGE)
                    }
                }
            }
        }

        }
        if (selectedTab == 1) {
        // ------------------------------------------- پایش پس‌زمینه و اعلان
        item {
            SettingsCard(
                title = context.getString(R.string.monitor_background),
                description = context.getString(R.string.monitor_background_desc),
                icon = MIcons.Live,
            ) {
                SwitchRow(
                    label = context.getString(R.string.monitor_background),
                    checked = settings.monitorInBackground,
                    onCheckedChange = { vm.setMonitorBackground(it) },
                )
                Text(
                    text = if (persian) "فاصله‌ی به‌روزرسانی" else "Update interval",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(listOf(1, 3, 5, 10, 30)) { seconds ->
                        OptionChip(
                            label = if (persian) "${Formats.integer(seconds.toLong(), true)} ثانیه"
                            else "$seconds s",
                            selected = settings.monitorIntervalSeconds == seconds,
                        ) {
                            vm.setMonitorInterval(seconds)
                        }
                    }
                }
                Text(
                    text = if (persian) {
                        "فاصله‌ی کوتاه‌تر مرز جلسه‌ها را دقیق‌تر ثبت می‌کند، اما مصرف باتری و اعلان پایش را بیشتر می‌کند."
                    } else {
                        "A shorter interval captures session boundaries more closely, but uses more battery and keeps monitoring active."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ------------------------------------------- دسترسی‌های رن‌تایم
        item {
            SettingsCard(
                title = context.getString(R.string.permissions_title),
                description = context.getString(R.string.permissions_desc),
                icon = MIcons.Shield,
            ) {
                val perms by vm.permissions.collectAsState()
                val notifGranted = perms.contains(android.Manifest.permission.POST_NOTIFICATIONS)
                val phoneGranted = perms.contains(android.Manifest.permission.READ_PHONE_STATE)
                PermissionStatusRow(
                    label = context.getString(R.string.perm_notifications),
                    granted = notifGranted,
                    onOpen = { UsageAccess.openNotificationSettings(context) },
                )
                PermissionStatusRow(
                    label = context.getString(R.string.perm_phone_state),
                    granted = phoneGranted,
                    onOpen = { UsageAccess.openAppDetails(context) },
                )
                if (!notifGranted || !phoneGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                ).setData(
                                    android.net.Uri.parse("package:${context.packageName}"),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                        ) {
                            Text(context.getString(R.string.perm_open_system_settings))
                        }
                    }
                }
            }
        }

        // ------------------------------------------- شخصی‌سازی اعلان پایش
        item {
            SettingsCard(
                title = context.getString(R.string.notif_customization),
                description = context.getString(R.string.notif_customization_desc),
                icon = MIcons.Bell,
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
                NotificationPreviewCard(
                    settings = settings,
                    tick = tick,
                    dashboard = dashboard,
                    usage = usage,
                    persian = persian,
                )

                Text(
                    text = if (persian) "چیدمان‌های سریع" else "Quick presets",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NotificationPreset.entries.toList()) { preset ->
                        OptionChip(
                            label = presetLabel(preset, persian),
                            selected = settings.notification == NotificationConfig.fromPreset(preset),
                            onClick = { vm.applyNotificationPreset(preset) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = { showNotifSheet = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(MIcons.Tune, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(context.getString(R.string.notif_open_customizer), maxLines = 1)
                    }
                    TextButton(onClick = { UsageAccess.openNotificationSettings(context) }) {
                        Text(if (persian) "اعلان سیستم" else "System", maxLines = 1)
                    }
                }
            }
        }

        // --------------------------------------------- اپراتور سیم‌کارت
        item {
            val selectedCarrierLabel = if (settings.operatorOverride.isBlank()) {
                context.getString(R.string.carrier_auto)
            } else {
                OperatorCatalog.resolveName(settings.operatorOverride).name(persian)
            }
            SettingsCard(
                title = context.getString(R.string.carrier_settings),
                description = context.getString(R.string.ir_operator_hint),
                icon = MIcons.Signal,
            ) {
                Text(
                    text = selectedCarrierLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { showCarrierDialog = true }) {
                    Text(if (persian) "انتخاب اپراتور" else "Choose carrier")
                }
            }
        }

        // ----------------------------------------------------- سقف مصرف
        item {
            SettingsCard(
                title = context.getString(R.string.data_cap),
                description = if (settings.dataCapBytes > 0) {
                    Formats.bytes(settings.dataCapBytes, persian)
                } else context.getString(R.string.set_cap_hint),
                icon = MIcons.Shield,
            ) {
                TextButton(onClick = { showCapDialog = true }) {
                    Text(if (persian) "تنظیم سقف" else "Set limit")
                }
            }
        }

        // ----------------------------------------------------- هشدار مصرف
        item {
            SettingsCard(
                title = if (persian) "هشدار مصرف" else "Usage alerts",
                description = if (persian) {
                    "وقتی به سقف روزانه یا ۸۰٪ و ۱۰۰٪ بسته برسی، اعلان می‌گیری"
                } else "Get notified at your daily limit and at 80% / 100% of your plan",
                icon = MIcons.Bell,
            ) {
                SwitchRow(
                    label = if (persian) "هشدارها فعال باشند" else "Enable alerts",
                    checked = settings.usageAlerts,
                    description = if (persian) "به پایش پس‌زمینه نیاز دارد" else "Requires background monitoring",
                ) { vm.setUsageAlerts(it) }
                Text(
                    text = if (settings.dailyLimitMb > 0) {
                        (if (persian) "سقف روزانه‌ی دیتای موبایل: " else "Daily mobile-data limit: ") +
                            Formats.bytes(settings.dailyLimitBytes, persian)
                    } else {
                        if (persian) "سقف روزانه تنظیم نشده" else "No daily limit set"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showDailyLimitDialog = true }) {
                    Text(if (persian) "تنظیم سقف روزانه" else "Set daily limit")
                }
                Spacer(Modifier.height(2.dp))
                SwitchRow(
                    label = if (persian) "هشدار جهش مصرف ساعتی" else "Hourly spike alerts",
                    checked = settings.spikeAlertEnabled,
                    description = if (persian) {
                        "اگر مصرف این ساعت خیلی بیشتر از معمولِ همان ساعت باشد، به تو خبر می‌دهیم"
                    } else {
                        "Get notified when this hour's usage runs far above its usual level"
                    },
                ) { vm.setSpikeAlert(it) }
                SwitchRow(
                    label = if (persian) "خلاصه‌ی هفتگی" else "Weekly digest",
                    checked = settings.weeklyDigestEnabled,
                    description = if (persian) {
                        "هر هفته یک جمع‌بندی از مصرف‌ات به‌همراه مقایسه با هفته‌ی قبل می‌گیری"
                    } else {
                        "A weekly summary of your usage, compared with the week before"
                    },
                ) { vm.setWeeklyDigest(it) }
            }
        }

        // ------------------------------------------------------- ویجت صفحه‌ی اصلی
        item {
            SettingsCard(
                title = context.getString(R.string.widget_settings),
                description = context.getString(R.string.widget_settings_desc),
                icon = MIcons.Widget,
                onClick = null,
            ) {
                SwitchRow(
                    label = context.getString(R.string.widget_show_operator),
                    checked = settings.widgetShowOperator,
                    onCheckedChange = { vm.setWidgetOperator(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.widget_show_cap),
                    checked = settings.widgetShowCap,
                    onCheckedChange = { vm.setWidgetCap(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.widget_detailed),
                    checked = settings.widgetDetailed,
                    onCheckedChange = { vm.setWidgetDetailed(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.widget_big_text),
                    checked = settings.widgetBigText,
                    onCheckedChange = { vm.setWidgetBigText(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.widget_follow_theme),
                    description = context.getString(R.string.widget_follow_theme_desc),
                    checked = settings.widgetFollowTheme,
                    onCheckedChange = { vm.setWidgetFollowTheme(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.widget_quick_actions),
                    description = context.getString(R.string.widget_quick_actions_desc),
                    checked = settings.widgetQuickActions,
                    onCheckedChange = { vm.setWidgetQuickActions(it) },
                )
            }
        }

        // --------------------------------------------- نشست‌های اتصال در خانه
        item {
            SettingsCard(
                title = context.getString(R.string.sessions_settings),
                description = context.getString(R.string.sessions_settings_desc),
                icon = MIcons.Clock,
                accent = MaterialTheme.colorScheme.secondary,
            ) {
                SwitchRow(
                    label = context.getString(R.string.sessions_show_card),
                    checked = settings.sessionsCardEnabled,
                    onCheckedChange = { vm.setSessionsCard(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.sessions_tabs),
                    description = context.getString(R.string.sessions_tabs_desc),
                    checked = settings.sessionsTabsEnabled,
                    onCheckedChange = { vm.setSessionsTabs(it) },
                )
                Text(
                    text = context.getString(R.string.sessions_visible_count),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(2, 3, 4, 5, 6, 8)) { count ->
                        OptionChip(
                            label = Formats.integer(count.toLong(), persian),
                            selected = settings.sessionsVisibleCount == count,
                        ) { vm.setSessionsVisible(count) }
                    }
                }
                Text(
                    text = context.getString(R.string.sessions_visible_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ------------------------------------------------ عینک مصرف (overlay)
        item {
            SettingsCard(
                title = context.getString(R.string.overlay_title),
                description = context.getString(R.string.overlay_desc),
                icon = MIcons.Speed,
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
                val overlayGranted = UsageAccess.canDrawOverlays(context)
                SwitchRow(
                    label = context.getString(R.string.overlay_enable),
                    description = context.getString(R.string.overlay_enable_desc),
                    checked = settings.speedOverlay,
                    onCheckedChange = { enabled ->
                        if (enabled && !overlayGranted) {
                            // اول اجازه‌ی «نمایش روی برنامه‌های دیگر» را بخواه؛
                            // کاربر بعد از برگشتن، کلید را دوباره می‌زند.
                            UsageAccess.openOverlaySettings(context)
                        } else {
                            vm.setSpeedOverlay(enabled)
                        }
                    },
                )
                if (!overlayGranted) {
                    TextButton(onClick = { UsageAccess.openOverlaySettings(context) }) {
                        Text(
                            if (persian) "دادن اجازه‌ی نمایش روی برنامه‌های دیگر"
                            else "Grant “display over other apps”",
                        )
                    }
                }
                SwitchRow(
                    label = context.getString(R.string.overlay_chart),
                    checked = settings.speedOverlayChart,
                    onCheckedChange = { vm.setSpeedOverlayChart(it) },
                )
                Text(
                    text = context.getString(R.string.overlay_help),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        }
        if (selectedTab == 2) {
        // --------------------------------------------------- دوره‌ی صورتحساب
        item {
            SettingsCard(
                title = context.getString(R.string.billing_cycle),
                description = if (persian) "شروع دوره‌ی صورتحساب شما" else "Your cycle start day",
                icon = MIcons.Chart,
            ) {
                Text(
                    text = if (persian) "روز شروع دوره" else "Cycle start day",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 7, 15, 21, 28).forEach { day ->
                        OptionChip(
                            label = Formats.integer(day.toLong(), persian),
                            selected = settings.billingCycleDay == day,
                        ) {
                            vm.setBillingDay(day)
                        }
                    }
                }
            }
        }

        // ------------------------------------------------- قیمت هر گیگابایت
        item {
            SettingsCard(
                title = if (persian) "قیمت هر گیگابایت بسته" else "Price per GB",
                description = if (persian) "هزینه‌ی تقریبی مصرف این دوره را برآورد می‌کنیم" else "We estimate this cycle's cost for you",
                icon = MIcons.Chart,
            ) {
                Text(
                    text = if (settings.tariffPerGbToman > 0) {
                        (if (persian) "قیمت فعلی: " else "Current price: ") +
                            Formats.integer(settings.tariffPerGbToman, persian) +
                            (if (persian) " تومان به ازای هر گیگابایت" else " Toman per GB")
                    } else {
                        if (persian) "هنوز قیمتی وارد نشده" else "No price set yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (settings.tariffPerGbToman > 0 && dashboard.monthMobileBytes > 0) {
                    val gb = dashboard.monthMobileBytes / (1024.0 * 1024.0 * 1024.0)
                    val cost = (gb * settings.tariffPerGbToman).toLong()
                    Text(
                        text = (if (persian) "هزینه‌ی تقریبی این دوره: " else "Estimated cost this cycle: ") +
                            Formats.integer(cost, persian) +
                            (if (persian) " تومان" else " Toman"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = { showTariffDialog = true }) {
                    Text(if (persian) "تنظیم قیمت هر گیگ" else "Set price per GB")
                }
            }
        }

        // ----------------------------------------------------- داده‌ها و آمار
        item {
            SettingsCard(
                title = if (persian) "داده‌ها و آمار" else "Data & stats",
                description = if (persian) "آمار را صفر کن یا فقط از تاریخ انتخابی ببین"
                else "Reset stats or show them only from a chosen date",
                icon = MIcons.Trash,
            ) {
                if (settings.statsEpochMillis > 0) {
                    Text(
                        text = (if (persian) "آمار از " else "Stats shown from ") +
                            JalaliDate.fromMillis(settings.statsEpochMillis).long(persian) +
                            (if (persian) " به بعد محاسبه می‌شود" else " onwards"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { vm.clearStatsEpoch() }) {
                        Text(if (persian) "بازگشت به حالت پیش‌فرض (۳۰ روز اخیر)" else "Back to default (last 30 days)")
                    }
                }
                FilledTonalButton(onClick = { showEpochPicker = true }) {
                    Icon(MIcons.Calendar, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (persian) "شروع آمار از تاریخ مشخص…" else "Start stats from a date…", maxLines = 1)
                }
                Text(
                    text = if (persian) "روزهای ۳۰ روز گذشته قابل انتخاب‌اند" else "Any day within the last 30 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showResetDialog = true }) {
                    Text(
                        text = if (persian) "حذف همه‌ی آمار و شروع دوباره" else "Erase all stats and start over",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // ------------------------------------------- پشتیبان‌گیری تنظیمات
        item {
            SettingsCard(
                title = context.getString(R.string.backup_title),
                description = context.getString(R.string.backup_desc),
                icon = MIcons.Share,
                accent = MaterialTheme.colorScheme.secondary,
            ) {
                Text(
                    text = context.getString(R.string.backup_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    val uri = SettingsBackup.export(context, settings)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.backup_title))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, context.getString(R.string.backup_share)),
                                    )
                                }.onFailure { backupMessage = it.message }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(MIcons.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(context.getString(R.string.backup_export), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { showRestorePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(MIcons.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(context.getString(R.string.backup_import), maxLines = 1)
                    }
                }
                backupMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // --------------------------------------------------------- درباره
        item {
            val versionLabel = if (persian) {
                Formats.toPersianDigits(BuildConfig.VERSION_NAME)
            } else BuildConfig.VERSION_NAME
            SettingsCard(
                title = context.getString(R.string.about),
                description = context.getString(R.string.about_text),
                icon = MIcons.Shield,
                onClick = { showAboutDialog = true },
            ) {
                Text(
                    text = "${context.getString(R.string.version)}: $versionLabel",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { showChangelog = true }) {
                    Text(context.getString(R.string.changelog))
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showNotifSheet) {
        NotificationCustomizerSheet(
            settings = settings,
            tick = tick,
            dashboard = dashboard,
            usage = usage,
            persian = persian,
            onUpdate = { transform -> vm.updateNotification(transform) },
            onPreset = { preset -> vm.applyNotificationPreset(preset) },
            onDismiss = { showNotifSheet = false },
        )
    }

    if (showCapDialog) {
        var input by remember {
            mutableStateOf(
                if (settings.dataCapBytes > 0) (settings.dataCapBytes / 1024 / 1024 / 1024).toString() else "",
            )
        }
        AlertDialog(
            onDismissRequest = { showCapDialog = false },
            title = { Text(context.getString(R.string.data_cap)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        input = Formats.normalizeDigits(raw).filter { it.isDigit() }
                    },
                    label = { Text(context.getString(R.string.gb)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val gb = Formats.normalizeDigits(input).toLongOrNull() ?: 0L
                    vm.setDataCap(gb * 1024 * 1024 * 1024)
                    showCapDialog = false
                }) { Text(context.getString(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCapDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showDailyLimitDialog) {
        var input by remember {
            mutableStateOf(if (settings.dailyLimitMb > 0) settings.dailyLimitMb.toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showDailyLimitDialog = false },
            title = { Text(if (persian) "سقف مصرف روزانه" else "Daily limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (persian) {
                            "برای دیتای موبایل، بر حسب مگابایت. خالی یا صفر = خاموش."
                        } else "For mobile data, in megabytes. Empty or zero turns it off.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = input,
                        onValueChange = { raw ->
                            input = Formats.normalizeDigits(raw).filter { it.isDigit() }.take(6)
                        },
                        label = { Text(context.getString(R.string.mb)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDailyLimitMb(Formats.normalizeDigits(input).toIntOrNull() ?: 0)
                    showDailyLimitDialog = false
                }) { Text(context.getString(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDailyLimitDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showTariffDialog) {
        var input by remember {
            mutableStateOf(if (settings.tariffPerGbToman > 0) settings.tariffPerGbToman.toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showTariffDialog = false },
            title = { Text(if (persian) "قیمت هر گیگابایت بسته" else "Price per GB") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (persian) {
                            "قیمت بسته‌ات برای هر گیگابایت را به تومان وارد کن؛ برای برآورد هزینه‌ی این دوره استفاده می‌شود."
                        } else {
                            "Enter your plan's price per GB in Toman; used to estimate this cycle."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = input,
                        onValueChange = { raw ->
                            input = Formats.normalizeDigits(raw).filter { it.isDigit() }.take(9)
                        },
                        label = { Text(if (persian) "تومان هر گیگابایت" else "Toman per GB") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTariffPerGbToman(Formats.normalizeDigits(input).toLongOrNull() ?: 0L)
                    showTariffDialog = false
                }) { Text(context.getString(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTariffDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (persian) "حذف همه‌ی آمار" else "Erase all stats") },
            text = {
                Text(
                    if (persian) {
                        "همه‌ی آمار ثبت‌شده (نمونه‌های مصرف، جلسه‌ها و هیستوگرام ساعتی) برای همیشه حذف می‌شود و آمارگیری از همین لحظه از نو شروع می‌شود. این کار قابل بازگشت نیست."
                    } else {
                        "All recorded stats (usage samples, sessions and hourly histograms) will be permanently deleted, and tracking restarts from now. This cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetStats()
                    showResetDialog = false
                }) {
                    Text(
                        text = if (persian) "حذف همیشگی" else "Erase everything",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showEpochPicker) {
        JalaliDatePickerDialog(
            persian = persian,
            maxMillis = System.currentTimeMillis(),
            minMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
            initialMillis = settings.statsEpochMillis.takeIf { it > 0 },
            onPick = { dayStart ->
                vm.setStatsEpochFromDay(dayStart)
                showEpochPicker = false
            },
            onDismiss = { showEpochPicker = false },
        )
    }

    if (showCarrierDialog) {
        AlertDialog(
            onDismissRequest = { showCarrierDialog = false },
            title = { Text(context.getString(R.string.carrier_settings)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    OperatorCatalog.iranian.forEach { operator ->
                        val selected = settings.operatorOverride == operator.fa ||
                            settings.operatorOverride == operator.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected) {
                                    vm.setOperator(operator.fa)
                                    showCarrierDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    vm.setOperator(operator.fa)
                                    showCarrierDialog = false
                                },
                            )
                            OperatorBadge(operator, size = 28.dp)
                            Spacer(Modifier.size(10.dp))
                            Text(operator.name(persian))
                        }
                    }
                    TextButton(onClick = {
                        vm.setOperator("")
                        showCarrierDialog = false
                    }) { Text(context.getString(R.string.carrier_auto)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCarrierDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showRestorePicker) {
        val picker = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        ) { uri ->
            showRestorePicker = false
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val restored = SettingsBackup.readFrom(context, uri, settings)
                if (restored == null) {
                    backupMessage = context.getString(R.string.backup_invalid)
                } else {
                    vm.applyBackup(restored)
                    backupMessage = context.getString(R.string.backup_restored)
                }
            }
        }
        LaunchedEffect(Unit) {
            picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
    }

    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text(context.getString(R.string.changelog)) },
            text = {
                Box(
                    Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(changelogText(persian), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) { Text(if (persian) "باشه" else "OK") }
            },
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false }, persian = persian)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = accent)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (description != null) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = checked) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** وضعیتِ یک مجوز: لیبل و علامت (فعال / غیرفعال). */
@Composable
private fun PermissionStatusRow(label: String, granted: Boolean, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        // «وضعیت» به‌شکل یک نشانِ کپسولیِ رنگی؛ بدون آیکون سرگردان کنار تیتر.
        Surface(
            shape = RoundedCornerShape(50),
            color = if (granted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (granted) MIcons.Check else MIcons.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (granted) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = if (granted) ctx.getString(R.string.perm_status_granted)
                    else ctx.getString(R.string.perm_status_denied),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (granted) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** نمونه‌ی رنگیِ یک پالت: دایره‌ی گرادیانی با تیک برای انتخاب فعلی. */
@Composable
private fun PaletteSwatch(name: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                // تیک روی گرادیان رنگی: سیاه یا سفید، هرکدام خواناتر (روی سبز نئونی سفید گم می‌شد).
                val avg = colors.reduce { a, b -> androidx.compose.ui.graphics.lerp(a, b, 0.5f) }
                Icon(
                    MIcons.Check,
                    contentDescription = null,
                    tint = ir.marbledu.datausage.ui.theme.bestOnColor(avg),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private fun changelogText(persian: Boolean): String = if (persian) {
    "۱.۵.۰ – نوار شیشه‌ای، تم AMOLED، نشست‌های تب‌دار و چهار قابلیت تازه\n" +
        "• نوار تب شیشه‌ای و شفاف با هاله‌ی رنگی و انیمیشن فنری؛ سربرگ صفحه‌ها هم با اسکرول شیشه‌ای می‌شود\n" +
        "• کارت نشست‌ها با ارتفاع محدود، اسکرول داخلی و تب‌بندی «همه / داده موبایل / وای‌فای / هر اپراتور» (قابل خاموش کردن از تنظیمات)\n" +
        "• ۱۱ پالت رنگی شادتر + تم کامل AMOLED با پس‌زمینه‌ی مشکی مطلق\n" +
        "• رفع علامت نامرتب جلوی تیترهای کارت اجازه‌ها: نشان‌های کپسولی رنگی\n" +
        "• ویجت تازه: سربرگ، عدد بزرگ، دو ستون موبایل/وای‌فای، نوار بسته و دو میان‌بر\n" +
        "• اعلان پایش و هشدارها با ساختار و رنگ تازه: سربرگ، خطوط جدا، نوار پیشرفت و دکمه‌ی آیکون‌دار\n" +
        "• بینش مصرف: الگوی روزانه، سنگین‌ترین و سبک‌ترین روز هفته، ساعت اوج و بودجه‌ی امروز\n" +
        "• عینک مصرف: حباب شیشه‌ای سرعت و مصرف روی همه‌ی اپ‌ها (قابل جابه‌جایی)\n" +
        "• پشتیبان‌گیری و بازیابی تنظیمات در یک فایل JSON\n" +
        "• حالت سفر: نمایش ردیف‌های تخمینی و اپراتورهای ناشناخته\n\n" +
    "۱.۴.۰ – صفحه‌ی اپراتور تمام‌صفحه، هشدار جهش مصرف و خلاصه‌ی هفتگی\n" +
        "• صفحه‌ی کامل جزئیات هر اپراتور: روند ۳۰ روزه، ساعت‌های اوج و تفکیک موبایل/وای‌فای؛ روی هر ستون روز بزن تا ۲۴ ساعت همان روز باز شود\n" +
        "• کارت «اپراتورها و نوع اتصال» در خانه: امروز / ۷ روز اخیر / انتخاب تاریخ شمسی\n" +
        "• ویجت دقیق‌تر: ردیف اختیاری دانلود/آپلود و اعمال فوری تغییرات\n" +
        "• هشدار جهش مصرف ساعتی: اگر یک ساعت خیلی بیشتر از معمول همان ساعت مصرف کنی، خبرت می‌کنیم\n" +
        "• خلاصه‌ی هفتگی: هر هفته جمع‌بندی مصرف با مقایسه با هفته‌ی قبل\n" +
        "• قیمت هر گیگابایت بسته: هزینه‌ی تقریبی این دوره را نشان می‌دهیم\n" +
        "• اندازه‌ی متن: کوچک (پیش‌فرض)، معمولی یا بزرگ\n" +
        "• حذف همه‌ی آمار یا شروع آمار از تاریخ انتخابی (حداکثر ۳۰ روز پیش)\n" +
        "• تیترهای بزرگ مثل iOS با نوار فشرده و تب‌های بالای صفحه؛ صفحه‌ها به تب جدا تقسیم شدند\n" +
        "• دکمه‌های دسترسی مستقیم به صفحه‌ی خودشان در تنظیمات سیستم\n" +
        "• «جلسه‌های سیم‌کارت و تطبیق شمارنده» حذف شد (پیچیده و کم‌فایده بود)\n\n" +
    "۱.۳.۰ – آیکون جدید، پینگ واقعی، لوگوی اپراتورها و ۴ قابلیت تازه\n" +
        "• آیکون کاملاً بازطراحی‌شده با پشتیبانی از همه‌ی شکل‌ها و آیکون تم‌دار\n" +
        "• پینگ درست شد: تأخیر واقعی تا سرور به‌جای پاسخ محلی VPN (۳–۴ میلی‌ثانیه‌ی اشتباه)\n" +
        "• تست کیفیت اتصال: داخلی/بین‌الملل، نمره و مناسب بودن برای بازی، تماس و ویدیو\n" +
        "• لوگوی رسمی اپراتورها کنار نام‌شان\n" +
        "• هشدار مصرف: سقف روزانه و ۸۰٪ / ۱۰۰٪ بسته\n" +
        "• پیش‌بینی بسته: چند روز دوام می‌آورد و سهم هر روز\n" +
        "• ویجت «مصرف امروز» برای صفحه‌ی اصلی\n\n" +
    "۱.۲.۰ – شناسایی هوشمند وای‌فای، فارسی کامل و اعلان دلخواه\n" +
        "• تشخیص چندلایه‌ی اپراتور وای‌فای و هات‌اسپات (IP عمومی، DNS اپراتور، IPv6، دامنه DHCP و نام شبکه)\n" +
        "• تفکیک مودم/روتر از هات‌اسپات آیفون/اندروید و نمایش دلیل در صورت مسدود شدن توسط VPN\n" +
        "• شخصی‌سازی کامل اعلان پایش با ۴ چیدمان آماده، الگوهای دلخواه، نوار پیشرفت، صفحه قفل و دکمه‌های سریع\n" +
        "• فارسی‌سازی کامل در کل رابط کاربری و نوار اعلان مستقل از زبان سیستم\n" +
        "• فعال‌سازی همیشگی امکانات بومی ایران و طراحی Material 3 Expressive\n\n" +
        "۱.۱.۰ – گزارش ۳۰ روزه و تم‌های رنگی\n" +
        "• شناسایی اپراتور حتی روی وای‌فای + نشان هر اپراتور\n" +
        "• تب «۳۰ روز»: گزارش کامل هر روز با تاریخ شمسی و میلادی\n" +
        "• مصرف هر اپراتور، هر اپ و نمودار ۲۴ ساعتِ هر روز\n" +
        "• نمودارهای جدید، رنگی و قابل لمس\n" +
        "• ۸ تم رنگی + رنگ والپیپر\n" +
        "• نوار پایین شناور با انیمیشن\n\n" +
        "۱.۰.۱ – رفع باگ‌ها\n" +
        "• دسترسی مصرف اپ‌ها حالا در تنظیمات دیده می‌شود\n" +
        "• نوار تب پایین درست کار می‌کند\n" +
        "• «امروز» از نیمه‌شب و دوره‌ی صورتحساب از روز انتخابی\n" +
        "• ساعت‌های پرمصرف واقعی\n" +
        "• زبان «مثل سیستم» درست شد\n" +
        "• نمایش مصرف وای‌فای و خروجی CSV اصلاح‌شده\n\n" +
        "۱.۰.۰ – اولین نسخه\n" +
        "• مصرف اینترنت بدون VPN، به تفکیک اپ\n" +
        "• بازه‌های مصرف از ساعت تا ساعت\n" +
        "• شناسایی اپراتورهای ایرانی\n" +
        "• سرعت، میانگین سرعت و پینگ\n" +
        "• گزارش CSV و اشتراک‌گذاری\n" +
        "• فارسی/انگلیسی با چیدمان راست‌چین و چپ‌چین\n" +
        "• طراحی Material 3 Expressive"
} else {
    "1.5.0 – Glass tab bar, AMOLED theme, tabbed sessions & four new features\n" +
        "• Glassy translucent tab bar with a soft colour glow, springy pill indicator; page headers turn glassy as you scroll\n" +
        "• Sessions card is height-capped with inner scrolling and tabs: All / Mobile data / Wi-Fi / each operator (all switchable in Settings)\n" +
        "• 11 livelier colour palettes + a true-black AMOLED theme\n" +
        "• Fixed the stray glyph in front of the permissions card titles — statuses are now tidy coloured pills\n" +
        "• Redesigned home-screen widget: header, big number, mobile/Wi-Fi columns, plan bar and two quick actions\n" +
        "• Redesigned monitoring + alert notifications: structure, separators, progress bar and icon buttons\n" +
        "• Usage insights: daily pattern, heaviest & lightest weekday, peak hour and today's budget\n" +
        "• Speed lens: a draggable glass bubble showing live speed and today's usage over any app\n" +
        "• Settings backup & restore as a single JSON file\n" +
        "• Travel mode: show estimated rows and unrecognised carriers\n\n" +
    "1.4.0 – Full-screen operator page, spike alerts & weekly digest\n" +
        "• Full operator details page: 30-day trend, peak hours, mobile/Wi-Fi split; tap any day bar for its 24 hours\n" +
        "• \"Operators & transport\" card on Home: today / last 7 days / pick a Jalali date\n" +
        "• Richer widget: optional download/upload row; settings apply instantly\n" +
        "• Hourly spike alerts when a single hour runs far above its usual level\n" +
        "• Weekly digest: your week vs. the week before\n" +
        "• Price per GB: see this cycle's estimated cost\n" +
        "• Text size: small (default), medium or large\n" +
        "• Erase all stats or start tracking from a chosen day (up to 30 days back)\n" +
        "• iOS-style large collapsing titles with animated top tabs; pages split into tabs\n" +
        "• Permission rows jump straight to their own system pages\n" +
        "• Removed \"SIM sessions & counter audit\" (complex, low value)\n\n" +
    "1.3.0 – New icon, real ping, operator logos & 4 new features\n" +
        "• Fully redesigned icon for every launcher shape, plus a themed icon\n" +
        "• Ping fixed: real round-trip delay to the server instead of the VPN's local 3–4 ms reply\n" +
        "• Connection quality test: Iran/global delay, score, gaming / call / streaming verdict\n" +
        "• Official operator logos next to their names\n" +
        "• Usage alerts: daily limit and 80% / 100% of your plan\n" +
        "• Plan forecast: days left at your pace and a daily budget\n" +
        "• \"Today's usage\" home-screen widget\n\n" +
    "1.2.0 – Multi-tier Wi-Fi ISP detection, full Persian localization & rich notifications\n" +
        "• Multi-tier Wi-Fi & hotspot operator detection (parallel GeoIP, ISP DNS, IPv6 prefix, DHCP domain & SSID)\n" +
        "• Router vs. iPhone/Android hotspot detection and explicit VPN/captive-portal diagnostics\n" +
        "• Granular notification customization with live preview, 4 presets, custom templates, progress bars, lockscreen visibility & quick actions\n" +
        "• Complete Persian localization across service notifications and all screens regardless of system locale\n" +
        "• Always-on Iran features & Material 3 Expressive design\n\n" +
        "1.1.0 – 30-day reports & color themes\n" +
        "• Operator detection over Wi-Fi + carrier badges\n" +
        "• 30-day interactive reports with Jalali & Gregorian dates\n" +
        "• Per-operator, per-app and 24-hour breakdowns\n" +
        "• 8 color themes + wallpaper dynamic color\n\n" +
        "1.0.1 – Bug fixes & billing cycle improvements\n" +
        "1.0.0 – Initial release"
}
/** پنجره‌ی «درباره ما»: هدف اپ، آدرس ریپو، مجوز و حریم خصوصی. */
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    persian: Boolean,
) {
    val context = LocalContext.current
    val versionLabel = if (persian) Formats.toPersianDigits(BuildConfig.VERSION_NAME) else BuildConfig.VERSION_NAME
    val repoUrl = "https://github.com/marble098/MarbleDU"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (persian) "درباره MarbleDU" else "About MarbleDU", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopStart,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MIcons.Shield, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                text = context.getString(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${context.getString(R.string.version)}: $versionLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SectionTitle(text = if (persian) "هدف برنامه" else "Purpose")
                    Text(
                        text = if (persian) {
                            "نظارت دقیق و منظم بر سمت و سمتِ مصرف اینترنت شما، بدون نیاز به VPN یا روت. مصرف هر اپ، هر ساعت و هر اپراتور به شکل واضح قابل مشاهده است و همه داده‌ها روی خود گوشی شما باقی می‌مانند."
                        } else {
                            "Accurate and steady monitoring of where your mobile data goes — no VPN, no root. See per-app usage, per-operator attribution, hourly sessions and 30-day reports, all kept on your phone."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionTitle(text = if (persian) "آدرس کد منبع (ریپو)" else "Source repository")
                    Text(
                        text = repoUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse(repoUrl) },
                                    context.getString(R.string.about_view_repo),
                                ),
                            )
                        }),
                    )
                    SectionTitle(text = if (persian) "حریم خصوصی" else "Privacy")
                    Text(
                        text = if (persian) {
                            "هیچ آمار مصرفی از گوشی شما خارج نمی‌شود. تمام داده‌ها صرفاً روی دستگاه باقی می‌مانند و " +
                                "فقط برای شناسایی اپراتور اینترنت یک بررسی کم‌خطر روی شبکه‌ی محلی انجام می‌شود."
                        } else {
                            "No usage data ever leaves your phone. Everything stays on the device; only a light local " +
                                "network check is performed to identify your Wi-Fi ISP, and no user data is sent anywhere."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionTitle(text = if (persian) "ساخته‌شده با" else "Built with")
                    Text(
                        text = "Kotlin • Jetpack Compose • Material 3 Expressive • فونت Vazirmatn (SIL OFL 1.1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (persian) "بسته شود" else "Dismiss") } },
    )
}

