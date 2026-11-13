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
import ir.marbledu.datausage.ui.MIcons
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
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
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

        item {
            SettingsCard(
                title = context.getString(R.string.appearance),
                description = context.getString(R.string.expressive_desc),
                icon = MIcons.Speed,
            ) {
                SwitchRow(
                    label = context.getString(R.string.expressive_design),
                    description = context.getString(R.string.expressive_desc),
                    checked = settings.expressiveDesign,
                    onCheckedChange = { vm.setExpressive(it) },
                )
                SwitchRow(
                    label = context.getString(R.string.dynamic_color),
                    description = context.getString(R.string.dynamic_desc),
                    checked = settings.dynamicColor,
                    onCheckedChange = { vm.setDynamicColor(it) },
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

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private const val CHANGELOG = "۱.۰.۰ – اولین نسخه\n" +
    "• مصرف اینترنت بدون VPN، به تفکیک اپ\n" +
    "• بازه‌های مصرف از ساعت تا ساعت\n" +
    "• شناسایی اپراتورهای ایرانی\n" +
    "• سرعت، میانگین سرعت و پینگ\n" +
    "• گزارش CSV و اشتراک‌گذاری\n" +
    "• فارسی/انگلیسی با چیدمان راست‌چین و چپ‌چین\n" +
    "• طراحی Material 3 Expressive"
