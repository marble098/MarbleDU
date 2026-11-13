package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.BuildConfig
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.ThemeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.theme.ThemePalette
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    var showCapDialog by remember { mutableStateOf(false) }
    var showCarrierDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = ir.marbledu.datausage.ui.FloatingBarClearance),
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
                HorizontalDivider()
                SwitchRow(
                    label = context.getString(R.string.expressive_design),
                    description = context.getString(R.string.expressive_desc),
                    checked = settings.expressiveDesign,
                    onCheckedChange = { vm.setExpressive(it) },
                )
            }
        }

        // ------------------------------------------------- گزینه‌های ایرانی
        item {
            SettingsCard(
                title = context.getString(R.string.iran_options),
                description = context.getString(R.string.iran_options_desc),
                icon = MIcons.Carrier,
                accent = MaterialTheme.colorScheme.tertiary,
            ) {
                val iran = settings.iran
                SwitchRow(context.getString(R.string.detect_operator), checked = iran.detectOperator) {
                    vm.updateIran { o -> o.copy(detectOperator = !o.detectOperator) }
                }
                SwitchRow(
                    label = if (persian) "شناسایی اپراتور روی وای‌فای" else "Identify operator on Wi-Fi",
                    description = if (persian) {
                        "برای فهمیدن اپراتورِ مودم، یک درخواست کوچک به سرویس «IP من» فرستاده می‌شود (ipwho.is و مشابه). " +
                            "هیچ آمار مصرفی ارسال نمی‌شود."
                    } else {
                        "Sends one small request to a “what is my IP” service (ipwho.is and similar) to learn the router's ISP. " +
                            "No usage data is sent."
                    },
                    checked = settings.wifiIspLookup,
                    onCheckedChange = { vm.setWifiIspLookup(it) },
                )
                HorizontalDivider()
                SwitchRow(context.getString(R.string.show_usage_report), checked = iran.showDataUsageReport) {
                    vm.updateIran { o -> o.copy(showDataUsageReport = !o.showDataUsageReport) }
                }
                SwitchRow(context.getString(R.string.show_average_speed), checked = iran.showAverageSpeed) {
                    vm.updateIran { o -> o.copy(showAverageSpeed = !o.showAverageSpeed) }
                }
                SwitchRow(context.getString(R.string.show_average_ping), checked = iran.showAveragePing) {
                    vm.updateIran { o -> o.copy(showAveragePing = !o.showAveragePing) }
                }
                SwitchRow(context.getString(R.string.show_timeline), checked = iran.showSessionTimeline) {
                    vm.updateIran { o -> o.copy(showSessionTimeline = !o.showSessionTimeline) }
                }
                SwitchRow(context.getString(R.string.show_per_app), checked = iran.showPerAppBreakdown) {
                    vm.updateIran { o -> o.copy(showPerAppBreakdown = !o.showPerAppBreakdown) }
                }
                SwitchRow(context.getString(R.string.show_peak), checked = iran.showPeakHours) {
                    vm.updateIran { o -> o.copy(showPeakHours = !o.showPeakHours) }
                }
                SwitchRow(context.getString(R.string.show_billing), checked = iran.showBillingCycle) {
                    vm.updateIran { o -> o.copy(showBillingCycle = !o.showBillingCycle) }
                }
            }
        }

        item {
            SettingsCard(
                title = context.getString(R.string.carrier_settings),
                description = context.getString(R.string.ir_operator_hint),
                icon = MIcons.Signal,
            ) {
                Text(
                    text = settings.operatorOverride.ifBlank { context.getString(R.string.carrier_auto) },
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { showCarrierDialog = true }) {
                    Text(if (persian) "انتخاب اپراتور" else "Choose carrier")
                }
            }
        }

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
                TextButton(onClick = { UsageAccess.openNotificationSettings(context) }) {
                    Text(if (persian) "تنظیمات اعلان‌ها" else "Notification settings")
                }
                // فاصله‌ی به‌روزرسانی اعلان سرعت در پس‌زمینه
                Text(
                    text = if (persian) "فاصله‌ی به‌روزرسانی" else "Update interval",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
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
                            label = Formats.integer(day.toLong(), persian).toString(),
                            selected = settings.billingCycleDay == day,
                        ) {
                            vm.setBillingDay(day)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = context.getString(R.string.about),
                description = context.getString(R.string.about_text),
                icon = MIcons.Shield,
            ) {
                Text(
                    text = "${context.getString(R.string.version)}: ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { showChangelog = true }) {
                    Text(context.getString(R.string.changelog))
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showCapDialog) {
        var input by remember { mutableStateOf(if (settings.dataCapBytes > 0) (settings.dataCapBytes / 1024 / 1024 / 1024).toString() else "") }
        AlertDialog(
            onDismissRequest = { showCapDialog = false },
            title = { Text(context.getString(R.string.data_cap)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text(context.getString(R.string.gb)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDataCap((input.toLongOrNull() ?: 0L) * 1024 * 1024 * 1024)
                    showCapDialog = false
                }) { Text(context.getString(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCapDialog = false }) { Text(context.getString(R.string.cancel)) }
            },
        )
    }

    if (showCarrierDialog) {
        AlertDialog(
            onDismissRequest = { showCarrierDialog = false },
            title = { Text(context.getString(R.string.carrier_settings)) },
            text = {
                Column {
                    OperatorCatalog.iranian.forEach { operator ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = settings.operatorOverride == operator.fa) {
                                    vm.setOperator(operator.fa)
                                    showCarrierDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.operatorOverride == operator.fa,
                                onClick = {
                                    vm.setOperator(operator.fa)
                                    showCarrierDialog = false
                                },
                            )
                            OperatorBadge(operator, size = 28.dp)
                            Spacer(Modifier.size(10.dp))
                            Text(if (persian) operator.fa else operator.en)
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
                Text(CHANGELOG, style = MaterialTheme.typography.bodySmall)
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
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
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

private const val CHANGELOG = "۱.۱.۰ – گزارش ۳۰ روزه و تم‌های رنگی\n" +
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
