package ir.marbledu.datausage.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.marbledu.datausage.ui.theme.LocalTextScale

/**
 * اسکلت مشترک صفحه‌ها، مشابه رابط iOS:
 *  - بالای صفحه یک تیتر خیلی بزرگ و پررنگ نمایش داده می‌شود؛
 *  - با اسکرول به پایین، تیتر بزرگ نرم «چروک» می‌شود و در نوار فشرده‌ی بالای
 *    صفحه جا می‌گیرد (همیشه همان‌جا می‌ماند)؛
 *  - تب‌های ثانویه‌ی صفحه (در صورت وجود) زیر نوار فشرده همیشه دیده می‌شوند.
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
    // میزان «چروک شدن» تیتر: ۰ = تیتر بزرگ کامل، ۱ = تیتر بزرگ کاملاً رفته زیر نوار فشرده.
    val collapse by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 96f).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        CompactBar(title = title, collapse = collapse, actions = actions)
        if (tabs != null && onTabSelected != null) {
            TopTabs(tabs = tabs, selected = selectedTab, onSelect = onTabSelected)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 2.dp,
                bottom = FloatingBarClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "large_title") {
                LargeTitleBlock(title = title, subtitle = subtitle, collapse = collapse)
            }
            content()
        }
    }
}

/** نوار فشرده‌ی بالای صفحه؛ وقتی تیتر بزرگ اسکرول شد، اینجا ظاهر می‌شود. */
@Composable
private fun CompactBar(
    title: String,
    collapse: Float,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val titleAlpha by animateFloatAsState(
        targetValue = collapse,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "compact-title-alpha",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clipToBounds()
            .background(scheme.surfaceContainer.copy(alpha = 0.96f * collapse)),
    ) {
        // خط مویی زیر نوار، مثل iOS
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(0.6.dp)
                .background(scheme.outlineVariant.copy(alpha = 0.85f * collapse)),
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // تعادل چپ/راست تا تیتر دقیقاً وسط بماند
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                // در فارسی/انگلیسی هر دو، فضای خالیِ همان وزن آیکون‌ها را می‌گیرد
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(2.2f)
                    .graphicsLayer { alpha = titleAlpha },
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (actions != null) {
                        actions()
                    }
                }
            }
        }
    }
}

/** تیتر بزرگ صفحه؛ با اسکرول به بالا می‌لغزد و محو می‌شود. */
@Composable
private fun LargeTitleBlock(
    title: String,
    subtitle: String?,
    collapse: Float,
) {
    val scheme = MaterialTheme.colorScheme
    val scale = LocalTextScale.current
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // کمی کوچک‌تر می‌شود و محو می‌شود تا زیر نوار فشرده جا شود
                val shrink = 1f - 0.18f * collapse
                scaleX = shrink
                scaleY = shrink
                alpha = (1f - collapse * 0.92f).coerceIn(0f, 1f)
                translationY = -10f * collapse * density
            }
            .padding(top = 6.dp, bottom = 4.dp),
    ) {
        Text(
            text = title,
            fontSize = (30f * scale).sp,
            lineHeight = (36f * scale).sp,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

/**
 * تب‌های ثانویه‌ی صفحه: مینیمال، کپسولی و با انیمیشن نرم؛
 * همیشه زیر نوار فشرده دیده می‌شود تا کاربر یک‌ضربه‌ای به بخش خودش برود.
 */
@Composable
fun TopTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.9f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) scheme.primaryContainer else Color.Transparent,
                label = "top-tab-container",
            )
            val tint by animateColorAsState(
                targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                label = "top-tab-tint",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "top-tab-scale",
            )
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(container)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 15.dp, vertical = 8.dp),
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}
