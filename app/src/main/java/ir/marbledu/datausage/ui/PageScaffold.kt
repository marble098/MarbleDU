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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * اسکلت مشترک صفحه‌ها:
 *  - بدون هیچ پس‌زمینه‌ی مات در بالای صفحه؛
 *  - تیتر با اسکرول نرم جمع می‌شود و محو می‌گردد؛
 *  - نوار تب بالای شیشه‌ای (هم‌شکل نوار تب پایین) شناور می‌ماند و محتوا از زیر آن و نوار وضعیت عبور می‌کند.
 */
@Composable
fun LargeTitleScaffold(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    tabs: List<String>? = null,
    selectedTab: Int = 0,
    onTabSelected: ((Int) -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    var titleHeightPx by remember { mutableIntStateOf(0) }
    var tabsHeightPx by remember { mutableIntStateOf(0) }
    val hasTabs = !tabs.isNullOrEmpty() && onTabSelected != null

    val topPadding = with(density) {
        if (titleHeightPx == 0) {
            statusTopPx.toDp() + if (hasTabs) 114.dp else 54.dp
        } else {
            (statusTopPx + titleHeightPx + if (hasTabs) tabsHeightPx else 0).toDp() + 6.dp
        }
    }

    fun clampedScrollPx(): Float {
        val maxCollapse = titleHeightPx.toFloat()
        if (maxCollapse <= 0f) return 0f
        val raw = if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat()
        } else {
            maxCollapse
        }
        return raw.coerceIn(0f, maxCollapse)
    }

    Box(modifier.fillMaxSize().background(scheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topPadding,
                bottom = FloatingBarClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )

        // سربرگ شفاف و شناور بالای لیست؛ هیچ پس‌زمینه‌ی ماتی زیر تایتل و نوار تب قرار نمی‌گیرد.
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { titleHeightPx = it.height },
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, -clampedScrollPx().roundToInt()) }
                        .graphicsLayer {
                            val maxCollapse = titleHeightPx.toFloat().coerceAtLeast(1f)
                            val progress = (clampedScrollPx() / maxCollapse).coerceIn(0f, 1f)
                            alpha = (1f - progress * 1.45f).coerceIn(0f, 1f)
                        }
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                    if (actions != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                    }
                }
            }

            val tabList = tabs
            if (!tabList.isNullOrEmpty() && onTabSelected != null) {
                TopTabs(
                    tabs = tabList,
                    selected = selectedTab,
                    onSelect = onTabSelected,
                    modifier = Modifier
                        .onSizeChanged { tabsHeightPx = it.height }
                        .offset { IntOffset(0, -clampedScrollPx().roundToInt()) },
                )
            }
        }
    }
}

/**
 * نوار تب شیشه‌ای بالای صفحه:
 * دقیقاً هم‌جنس نوار پایین (GlassBar نیمه‌شفاف با هاله‌ی رنگی، بازتاب نور و کپسول لغزنده‌ی گرادیانی).
 */
@Composable
fun TopTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val clampedSelected = selected.coerceIn(0, tabs.lastIndex)
    val position by animateFloatAsState(
        targetValue = clampedSelected.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "top-tab-pill",
    )

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            val tabSlotWidth = maxWidth / tabs.size
            GlassBar(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                tint = scheme.primary,
            ) {
                // نشانگر کپسولی لغزنده هم‌شکل نوار ناوبری پایین
                Box(
                    Modifier
                        .offset { IntOffset((tabSlotWidth.toPx() * position).roundToInt(), 0) }
                        .width(tabSlotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(19.dp))
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
                            shape = RoundedCornerShape(19.dp),
                        ),
                )
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                        val pop by animateFloatAsState(
                            targetValue = if (clampedSelected == index) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "top-tab-pop",
                        )
                        val tint = lerp(scheme.onSurfaceVariant, scheme.primary, closeness)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .semantics { this.selected = clampedSelected == index }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab,
                                ) { onSelect(index) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab,
                                color = tint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (closeness > 0.5f) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.graphicsLayer {
                                    val scale = 1f + 0.05f * pop
                                    scaleX = scale
                                    scaleY = scale
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
