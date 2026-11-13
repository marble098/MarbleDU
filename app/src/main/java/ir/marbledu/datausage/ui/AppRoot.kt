package ir.marbledu.datausage.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import ir.marbledu.datausage.MainActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.ui.screens.AppsScreen
import ir.marbledu.datausage.ui.screens.DashboardScreen
import ir.marbledu.datausage.ui.screens.LiveScreen
import ir.marbledu.datausage.ui.screens.OnboardingScreen
import ir.marbledu.datausage.ui.screens.OperatorDetailScreen
import ir.marbledu.datausage.ui.screens.ReportsScreen
import ir.marbledu.datausage.ui.screens.SettingsScreen
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.util.UsageAccess
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_dashboard, MIcons.Home),
    LIVE(R.string.nav_live, MIcons.Live),
    APPS(R.string.nav_apps, MIcons.Apps),
    REPORTS(R.string.nav_reports, MIcons.Calendar),
    SETTINGS(R.string.nav_settings, MIcons.Settings),
}

@Composable
fun AppRoot(
    vm: MainViewModel,
    settings: AppSettings,
    persian: Boolean,
    requestedTab: String? = null,
    onConsumeRequestedTab: () -> Unit = {},
) {
    val context = LocalContext.current
    val dashboard by vm.dashboard.collectAsState()
    val scope = rememberCoroutineScope()

    if (!settings.onboarded) {
        OnboardingScreen(
            persian = persian,
            needsUsageAccess = !dashboard.hasUsageAccess,
            onGrantUsageAccess = { UsageAccess.openSettings(context) },
            onFinish = { vm.completeOnboarding() },
        )
        return
    }

    // صفحه‌ی کامل جزئیات اپراتور؛ همه‌جا تمام‌صفحه است و روی تب‌ها می‌نشیند.
    var operatorDetailTarget by remember { mutableStateOf<Pair<String, SimIdentity?>?>(null) }
    operatorDetailTarget?.let { target ->
        OperatorDetailScreen(
            vm = vm,
            operatorId = target.first,
            sim = target.second,
            persian = persian,
            onBack = { operatorDetailTarget = null },
        )
        return
    }

    // بارگیری و پایش فقط بعد از راه‌اندازی آغاز می‌شود؛ بازگشت از تنظیمات سیستم
    // هم وضعیت دسترسی‌ها و داده‌ها را تازه می‌کند.
    LaunchedEffect(settings.onboarded) {
        vm.startLive()
        vm.refreshAll()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.startLive()
        vm.refreshAll()
        vm.refreshPermissions()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vm.stopLive()
    }

    val tabs = Tab.entries
    // صفحه‌ها داخل یک Pager هستند: هم با کشیدن انگشت جابه‌جا می‌شوند و هم با
    // لمس تب؛ نشانگرِ نوار پایین دقیقاً همراه انگشت حرکت می‌کند.
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(requestedTab) {
        if (requestedTab == MainActivity.TAB_REPORTS) {
            pagerState.animateScrollToPage(Tab.REPORTS.ordinal)
            onConsumeRequestedTab()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabs[page]) {
                Tab.HOME -> DashboardScreen(
                    vm,
                    settings,
                    persian,
                    onOpenOperatorDetail = { operatorId, sim ->
                        operatorDetailTarget = operatorId to sim
                    },
                )
                Tab.LIVE -> LiveScreen(vm, settings, persian)
                Tab.APPS -> AppsScreen(vm, settings, persian)
                Tab.REPORTS -> ReportsScreen(
                    vm,
                    settings,
                    persian,
                    onOpenOperatorDetail = { operatorId, sim ->
                        operatorDetailTarget = operatorId to sim
                    },
                )
                Tab.SETTINGS -> SettingsScreen(vm, settings, persian)
            }
        }
        // نوار شناورِ شیشه‌ای روی محتوا می‌نشیند؛ محتوا از زیر شیشه رد می‌شود.
        FloatingNavBar(
            pagerState = pagerState,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * نوار ناوبری شناورِ شیشه‌ای (Glass Navigation Bar).
 *
 *  - بدنه‌ی شیشه‌ای نیمه‌شفاف با گرادیان، هاله‌ی رنگی پالت و هایلایت نور در لبه؛
 *  - محتوای صفحه از زیر شیشه رد می‌شود و رنگِ پس‌زمینه از آن عبور می‌کند؛
 *  - نشانگرِ کپسولیِ گرادیانی بین تب‌ها «سُر می‌خورد» و وسط راه کمی کش می‌آید؛
 *  - آیکون تبِ انتخاب‌شده با حرکت فنری بالا می‌پرد و برچسبش پررنگ می‌شود.
 */
@Composable
private fun FloatingNavBar(
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = Tab.entries
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val shape = RoundedCornerShape(28.dp)
    val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (tabs.size - 1).toFloat())

    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(62.dp),
        ) {
            val barShape = shape
            // maxWidth را قبل از ورود به scope شیشه می‌گیریم تا با BoxScope
            // داخل GlassBar قاطی نشود.
            val tabSlotWidth = maxWidth / tabs.size
            GlassBar(
                modifier = Modifier.fillMaxSize(),
                shape = barShape,
                tint = scheme.primary,
            ) {
                val tabWidth = tabSlotWidth
                // درخشش دنبال‌کننده‌ی تب فعال، پشت آیکون‌ها
                Box(
                    Modifier
                        .offset { IntOffset((tabWidth.toPx() * position).roundToInt(), 0) }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 5.dp, vertical = 7.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (marble.amoled) scheme.primary.copy(alpha = 0.34f)
                                    else scheme.primary.copy(alpha = 0.22f),
                                    scheme.tertiary.copy(alpha = 0.22f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (marble.dark) 0.16f else 0.55f),
                            shape = RoundedCornerShape(21.dp),
                        ),
                )
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                        val pop by animateFloatAsState(
                            targetValue = if (pagerState.currentPage == index) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "tab-pop",
                        )
                        val tint = lerp(scheme.onSurfaceVariant, scheme.primary, closeness)
                        val text = label(tab)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSelect(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = text,
                                    tint = tint,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .graphicsLayer {
                                            val scale = 1f + 0.18f * pop
                                            scaleX = scale
                                            scaleY = scale
                                            translationY = -2.dp.toPx() * pop
                                        },
                                )
                                Text(
                                    text = text,
                                    color = tint,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = if (closeness > 0.5f) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun label(tab: Tab): String = LocalContext.current.getString(tab.labelRes)
