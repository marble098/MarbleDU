package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppListFilter
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppSurgeDetector
import ir.marbledu.datausage.data.AppUsage
import ir.marbledu.datausage.data.BackgroundDrainAnalyzer
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.DonutChart
import ir.marbledu.datausage.ui.DonutLegend
import ir.marbledu.datausage.ui.DonutSlice
import ir.marbledu.datausage.ui.EmptyState
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.LargeTitleScaffold
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.SplitBar
import ir.marbledu.datausage.ui.UsageBar
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.theme.accessibleColor
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun AppsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val usage by vm.usage.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val surges by vm.appSurges.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    var sliceSelection by remember(usage.range) { mutableStateOf<Int?>(null) }

    // فقط یک بار در ورود به صفحه لود می‌شود.
    LaunchedEffect(Unit) {
        vm.loadUsage()
    }

    var query by rememberSaveable { mutableStateOf("") }
    var network by rememberSaveable { mutableStateOf(0) }
    var sortMode by rememberSaveable { mutableStateOf(0) } // 0 = کل، 1 = پس‌زمینه، 2 = موبایل
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) vm.loadAppSurges()
    }

    val appsTabs = listOf(
        if (persian) "نمودار" else "Chart",
        if (persian) "همه‌ی اپ‌ها" else "All apps",
        context.getString(R.string.apps_tab_drain),
    )
    val listState = rememberLazyListState()
    LargeTitleScaffold(
        title = if (persian) "اپ‌ها" else "Apps",
        listState = listState,
        subtitle = if (persian) "مصرف هر اپ در بازه‌ی انتخاب‌شده" else "Per-app usage for the selected range",
        tabs = appsTabs,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeRange.entries.toList()) { range ->
                    FilterChip(
                        selected = usage.range == range,
                        onClick = { vm.setRange(range) },
                        label = { Text(if (persian) range.labelFa else range.labelEn) },
                    )
                }
            }
        }

        if (!dashboard.hasUsageAccess) {
            item {
                MarbleCard(padding = 14.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.error.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                MIcons.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = scheme.error,
                            )
                        }
                        Text(
                            context.getString(R.string.no_apps),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FilledTonalButton(
                        onClick = { UsageAccess.openSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (persian) "فعال کردن دسترسی" else "Enable access")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = "${context.getString(R.string.cellular)}: ${
                        Formats.bytes(usage.mobile.sumOf { it.bytes }, persian)
                    }",
                    color = scheme.primary,
                )
                InfoChip(
                    text = "${context.getString(R.string.wifi)}: ${
                        Formats.bytes(usage.wifi.sumOf { it.bytes }, persian)
                    }",
                    color = scheme.secondary,
                )
            }
        }

        val combined = mergeByPackage(usage.mobile, usage.wifi)
        val drainSummary = usage.drainSummary
            ?: BackgroundDrainAnalyzer.summarize(usage.mobile, usage.wifi)

        val baseFiltered = mergeByPackage(
            if (network == 2) emptyList() else usage.mobile,
            if (network == 1) emptyList() else usage.wifi,
        ).filter { (_, app) ->
            AppListFilter.matches(
                OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label,
                app.packageName,
                query,
            )
        }

        val filteredApps = when (sortMode) {
            1 -> baseFiltered.sortedByDescending { (_, app) ->
                drainSummary.entriesByPackage[app.packageName]?.backgroundBytes ?: 0L
            }
            2 -> baseFiltered.sortedByDescending { (_, app) ->
                drainSummary.entriesByPackage[app.packageName]?.mobileTotalBytes ?: 0L
            }
            else -> baseFiltered
        }

        if (selectedTab == 1) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(context.getString(R.string.apps_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(R.string.apps_network_all, R.string.cellular, R.string.wifi).withIndex().toList()) { (index, label) ->
                        FilterChip(
                            selected = network == index,
                            onClick = { network = index },
                            label = { Text(context.getString(label)) },
                        )
                    }
                }
                val sortLabels = listOf(
                    if (persian) "بیشترین کل" else "Sort: Total",
                    if (persian) "بیشترین پس‌زمینه" else "Sort: Background",
                    if (persian) "بیشترین موبایل" else "Sort: Mobile",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortLabels.withIndex().toList()) { (index, label) ->
                        FilterChip(
                            selected = sortMode == index,
                            onClick = { sortMode = index },
                            label = { Text(label) },
                        )
                    }
                }
                if (!usage.loading && filteredApps.isEmpty()) {
                    Text(context.getString(R.string.apps_no_match))
                }
            }
        }

        if (usage.loading) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (combined.isEmpty() && selectedTab != 2) {
            item { EmptyState(context.getString(R.string.no_data_yet)) }
        } else {
            when (selectedTab) {
                0 -> {
                    item {
                        val top = combined.take(5)
                        val rest = combined.drop(5).fold(0L) { acc, pair -> acc + pair.first }
                        val slices = top.mapIndexed { i, (bytes, app) ->
                            val displayName = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                            DonutSlice(
                                displayName,
                                bytes.toFloat(),
                                marble.chart[i % marble.chart.size],
                                Formats.bytes(bytes, persian),
                            )
                        } + if (rest > 0) listOf(
                            DonutSlice(
                                if (persian) "بقیه" else "Others",
                                rest.toFloat(),
                                scheme.outline,
                                Formats.bytes(rest, persian),
                            ),
                        ) else emptyList()
                        val grandTotal = combined.fold(0L) { acc, pair -> acc + pair.first }
                        MarbleCard {
                            CardHeader(
                                title = if (persian) "سهم اپ‌ها" else "App share",
                                icon = MIcons.Apps,
                                trailing = Formats.integer(combined.size.toLong(), persian) +
                                    (if (persian) " اپ" else " apps"),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(slices, diameter = 128.dp, stroke = 17.dp, selected = sliceSelection) {
                                    val picked = sliceSelection?.let { slices.getOrNull(it) }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = picked?.detail ?: Formats.bytes(grandTotal, persian),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = picked?.label ?: context.getString(R.string.total),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(78.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                DonutLegend(slices, persian, Modifier.weight(1f), sliceSelection) {
                                    sliceSelection = it
                                }
                            }
                            SplitBar(
                                listOf(
                                    usage.mobile.sumOf { it.bytes }.toFloat() to marble.mobile,
                                    usage.wifi.sumOf { it.bytes }.toFloat() to marble.wifi,
                                ),
                                height = 10.dp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                LegendDot(context.getString(R.string.cellular), marble.mobile)
                                LegendDot(context.getString(R.string.wifi), marble.wifi)
                            }
                        }
                    }
                }

                1 -> {
                    val listMax = (filteredApps.maxOfOrNull { it.first } ?: 1L).coerceAtLeast(1L)
                    items(filteredApps, key = { it.second.packageName }) { (bytes, app) ->
                        val displayName = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                        val subLabel = if (app.packageName.startsWith("uid:")) {
                            if (persian) "سرویس سیستم" else "System service"
                        } else app.packageName
                        val drainEntry = drainSummary.entriesByPackage[app.packageName]
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = scheme.surfaceContainerLow,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(app.packageName.substringBefore(':'), displayName, size = 34.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = Formats.bytes(bytes, persian),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.primary,
                                    )
                                }
                                Text(
                                    text = "${LiveMonitor.formatDate(app.firstTimestamp, persian)} • " +
                                        context.getString(
                                            R.string.from_to,
                                            LiveMonitor.formatHourMinute(app.firstTimestamp, persian),
                                            LiveMonitor.formatHourMinute(app.lastTimestamp, persian),
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                                if (drainEntry != null && (drainEntry.backgroundBytes > 0L || drainEntry.mobileTotalBytes > 0L)) {
                                    Spacer(Modifier.height(5.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (drainEntry.mobileTotalBytes > 0L) {
                                            InfoChip(
                                                text = "${context.getString(R.string.cellular)}: ${Formats.bytes(drainEntry.mobileTotalBytes, persian)}",
                                                color = marble.mobile,
                                            )
                                        }
                                        if (drainEntry.backgroundBytes > 0L) {
                                            InfoChip(
                                                text = "${context.getString(R.string.background_label)} ${Formats.integer(drainEntry.backgroundSharePercent.toLong(), persian)}${if (persian) "٪" else "%"}",
                                                color = if (drainEntry.backgroundSharePercent >= 50) scheme.error else scheme.tertiary,
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (drainEntry.isRestrictableApp && drainEntry.backgroundBytes >= 1024L * 1024L) {
                                            TextButton(
                                                onClick = { UsageAccess.openAppDataUsageSettings(context, app.packageName) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            ) {
                                                Text(
                                                    if (persian) "محدودکردن" else "Restrict",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                UsageBar(
                                    label = subLabel,
                                    bytes = bytes,
                                    fraction = bytes.toFloat() / listMax,
                                    persian = persian,
                                    color = marble.chart[(combined.indexOfFirst { it.second.packageName == app.packageName }
                                        .coerceAtLeast(0)) % marble.chart.size],
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        ForegroundBackgroundSummaryCard(
                            summary = drainSummary,
                            range = usage.range,
                            persian = persian,
                        )
                    }
                    item {
                        AppSurgesCard(
                            surges = surges,
                            persian = persian,
                            onRefresh = { vm.loadAppSurges(force = true) },
                            onOpenAppSettings = { pkg -> UsageAccess.openAppDataUsageSettings(context, pkg) },
                        )
                    }
                    item {
                        SectionTitle(
                            text = context.getString(R.string.top_background_drainers),
                        )
                        Text(
                            text = context.getString(R.string.top_background_drainers_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (drainSummary.topDrainers.isEmpty()) {
                        item { EmptyState(context.getString(R.string.no_background_drain)) }
                    } else {
                        items(drainSummary.topDrainers, key = { "bg_${it.packageName}" }) { entry ->
                            BackgroundDrainerRow(
                                entry = entry,
                                persian = persian,
                                onRestrict = {
                                    UsageAccess.openAppDataUsageSettings(context, entry.packageName)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForegroundBackgroundSummaryCard(
    summary: BackgroundDrainAnalyzer.Summary,
    range: TimeRange,
    persian: Boolean,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val bgTint = accessibleColor(Color(0xFFEF4444), scheme.surfaceContainerLow)

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = context.getString(R.string.drain_summary_title),
            icon = MIcons.Shield,
            tint = scheme.primary,
            subtitle = if (persian) range.labelFa else range.labelEn,
            trailing = "${Formats.integer(summary.backgroundSharePercent.toLong(), persian)}${if (persian) "٪ پس‌زمینه" else "% BG"}",
        )
        Text(
            text = context.getString(R.string.drain_summary_desc),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        SplitBar(
            listOf(
                summary.foregroundBytes.toFloat() to scheme.primary,
                summary.backgroundBytes.toFloat() to bgTint,
            ),
            height = 10.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                label = context.getString(R.string.foreground_label),
                value = Formats.bytes(summary.foregroundBytes, persian),
                color = scheme.primary,
                modifier = Modifier.weight(1f),
            )
            Metric(
                label = context.getString(R.string.background_label),
                value = Formats.bytes(summary.backgroundBytes, persian),
                color = bgTint,
                modifier = Modifier.weight(1f),
            )
        }
        if (summary.backgroundBytes > 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = "${context.getString(R.string.cellular)}: ${Formats.bytes(summary.mobileBackgroundBytes, persian)}",
                    color = marble.mobile,
                )
                InfoChip(
                    text = "${context.getString(R.string.wifi)}: ${Formats.bytes(summary.wifiBackgroundBytes, persian)}",
                    color = marble.wifi,
                )
            }
        }
    }
}

@Composable
private fun AppSurgesCard(
    surges: ir.marbledu.datausage.ui.AppSurgesUi,
    persian: Boolean,
    onRefresh: () -> Unit,
    onOpenAppSettings: (String) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val warnTint = accessibleColor(Color(0xFFF59E0B), scheme.surfaceContainerLow)

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = context.getString(R.string.surge_title),
            icon = MIcons.Speed,
            tint = warnTint,
            subtitle = context.getString(R.string.surge_subtitle),
            trailing = if (surges.items.isNotEmpty()) {
                Formats.integer(surges.items.size.toLong(), persian) + (if (persian) " اپ" else " apps")
            } else null,
        )

        when {
            surges.loading -> {
                Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                }
            }
            surges.items.isEmpty() -> {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = context.getString(R.string.surge_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) {
                        Text(context.getString(R.string.refresh_label))
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    surges.items.forEach { item ->
                        SurgeItemRow(
                            item = item,
                            persian = persian,
                            warnTint = warnTint,
                            onRestrict = { onOpenAppSettings(item.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurgeItemRow(
    item: AppSurgeDetector.SurgeItem,
    persian: Boolean,
    warnTint: Color,
    onRestrict: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val displayName = OperatorCatalog.specialUidLabel(item.uid, persian) ?: item.label
    val badge = if (item.isNewSpike) {
        context.getString(R.string.surge_new_spike)
    } else {
        "${Formats.number(item.multiplier, persian, 1)}×"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(item.packageName.substringBefore(':'), displayName, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                InfoChip(text = badge, color = warnTint)
            }
            Text(
                text = if (item.baselineDailyBytes > 0L) {
                    if (persian) {
                        "امروز ${Formats.bytes(item.todayTotalBytes, true)} · میانگین ${Formats.bytes(item.baselineDailyBytes, true)}"
                    } else {
                        "Today ${Formats.bytes(item.todayTotalBytes, false)} · avg ${Formats.bytes(item.baselineDailyBytes, false)}/day"
                    }
                } else {
                    if (persian) "امروز ${Formats.bytes(item.todayTotalBytes, true)}"
                    else "Today ${Formats.bytes(item.todayTotalBytes, false)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.isRestrictableApp) {
            TextButton(
                onClick = onRestrict,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    if (persian) "تنظیم" else "Manage",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun BackgroundDrainerRow(
    entry: BackgroundDrainAnalyzer.AppEntry,
    persian: Boolean,
    onRestrict: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val displayName = OperatorCatalog.specialUidLabel(entry.uid, persian) ?: entry.label
    val bgTint = accessibleColor(Color(0xFFEF4444), scheme.surfaceContainerLow)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(entry.packageName.substringBefore(':'), displayName, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (persian) {
                            "${Formats.bytes(entry.backgroundBytes, true)} پس‌زمینه از ${Formats.bytes(entry.totalBytes, true)} (${Formats.integer(entry.backgroundSharePercent.toLong(), true)}٪)"
                        } else {
                            "${Formats.bytes(entry.backgroundBytes, false)} background of ${Formats.bytes(entry.totalBytes, false)} (${entry.backgroundSharePercent}%)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                InfoChip(
                    text = "${Formats.integer(entry.backgroundSharePercent.toLong(), persian)}${if (persian) "٪" else "%"}",
                    color = if (entry.backgroundSharePercent >= 50) bgTint else scheme.primary,
                )
            }

            SplitBar(
                listOf(
                    entry.foregroundBytes.toFloat() to scheme.primary,
                    entry.backgroundBytes.toFloat() to bgTint,
                ),
                height = 7.dp,
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.mobileBackgroundBytes > 0L) {
                        InfoChip(
                            text = "${context.getString(R.string.cellular)}: ${Formats.bytes(entry.mobileBackgroundBytes, persian)}",
                            color = marble.mobile,
                        )
                    }
                    if (entry.wifiBackgroundBytes > 0L) {
                        InfoChip(
                            text = "${context.getString(R.string.wifi)}: ${Formats.bytes(entry.wifiBackgroundBytes, persian)}",
                            color = marble.wifi,
                        )
                    }
                }
                if (entry.isRestrictableApp) {
                    FilledTonalButton(
                        onClick = onRestrict,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            context.getString(R.string.restrict_bg_action),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

/** ادغام مصرف موبایل و وای‌فای بر اساس پکیج. */
private fun mergeByPackage(
    mobile: List<AppUsage>,
    wifi: List<AppUsage>,
): List<Pair<Long, AppUsage>> {
    val map = LinkedHashMap<String, Pair<Long, AppUsage>>()
    (mobile + wifi).forEach { usage ->
        val existing = map[usage.packageName]
        map[usage.packageName] = if (existing == null) {
            usage.bytes to usage
        } else {
            (existing.first + usage.bytes) to existing.second.copy(
                firstTimestamp = minOf(existing.second.firstTimestamp, usage.firstTimestamp),
                lastTimestamp = maxOf(existing.second.lastTimestamp, usage.lastTimestamp),
                foregroundBytes = existing.second.foregroundBytes + usage.foregroundBytes,
                backgroundBytes = existing.second.backgroundBytes + usage.backgroundBytes,
            )
        }
    }
    return map.values.sortedByDescending { it.first }
}
