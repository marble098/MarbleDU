package ir.marbledu.datausage.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.BuildConfig
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationPreset
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.theme.ThemePalette
import ir.marbledu.datausage.util.UsageAccess

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

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = FloatingBarClearance),
    ) {
        item { SectionTitle(text = context.getString(R.string.settings)) }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 30).forEach { seconds ->
                        OptionChip(
                            label = if (persian) "${Formats.integer(seconds.toLong(), true)} ثانیه"
                            else "$seconds s",
                            selected = settings.monitorIntervalSeconds == seconds,
                        ) {
                            vm.setMonitorInterval(seconds)
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
            }
        }

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

        // --------------------------------------------------------- درباره
        item {
            val versionLabel = if (persian) {
                Formats.toPersianDigits(BuildConfig.VERSION_NAME)
            } else BuildConfig.VERSION_NAME
            SettingsCard(
                title = context.getString(R.string.about),
                description = context.getString(R.string.about_text),
                icon = MIcons.Shield,
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
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
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
                Icon(MIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
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
