package ir.marbledu.datausage.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.ui.screens.AppsScreen
import ir.marbledu.datausage.ui.screens.DashboardScreen
import ir.marbledu.datausage.ui.screens.LiveScreen
import ir.marbledu.datausage.ui.screens.OnboardingScreen
import ir.marbledu.datausage.ui.screens.ReportsScreen
import ir.marbledu.datausage.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_dashboard, MIcons.Home),
    LIVE(R.string.nav_live, MIcons.Live),
    APPS(R.string.nav_apps, MIcons.Apps),
    REPORTS(R.string.nav_reports, MIcons.Reports),
    SETTINGS(R.string.nav_settings, MIcons.Settings),
}

@Composable
fun AppRoot(vm: MainViewModel, settings: AppSettings) {
    val persian = settings.language == AppLanguage.PERSIAN
    if (!settings.onboarded) {
        OnboardingScreen(persian = persian, onFinish = { vm.completeOnboarding() })
        return
    }

    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AnimatedContent(
                targetState = Tab.entries[pagerState.currentPage],
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab-content",
                modifier = Modifier.weight(1f),
            ) { tab ->
                when (tab) {
                    Tab.HOME -> DashboardScreen(vm, settings, persian)
                    Tab.LIVE -> LiveScreen(vm, settings, persian)
                    Tab.APPS -> AppsScreen(vm, settings, persian)
                    Tab.REPORTS -> ReportsScreen(vm, settings, persian)
                    Tab.SETTINGS -> SettingsScreen(vm, settings, persian)
                }
            }
            MarbleNavigationBar(
                current = pagerState.currentPage,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                persian = persian,
            )
        }
    }
}

/**
 * نوار ناوبری شناور با ظاهر Material 3 Expressive: قرص‌شکل،
 * گزینه‌ی انتخاب‌شده با پس‌زمینه‌ی رنگی و آیکون پر.
 */
@Composable
private fun MarbleNavigationBar(current: Int, onSelect: (Int) -> Unit, persian: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    val selected = index == current
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent,
                            modifier = Modifier.fillMaxWidth(if (selected) 0.95f else 0.7f),
                            onClick = { onSelect(index) },
                        ) {
                            Row(
                                Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (selected) 22.dp else 21.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (selected) {
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        text = label(tab),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun label(tab: Tab): String = androidx.compose.ui.platform.LocalContext.current
    .getString(tab.labelRes)
