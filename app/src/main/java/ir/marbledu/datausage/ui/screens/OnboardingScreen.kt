package ir.marbledu.datausage.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.ui.MIcons
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

@Composable
fun OnboardingScreen(persian: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    val slides = listOf(
        Slide(MIcons.Chart, R.string.onboarding_title_1, R.string.onboarding_body_1),
        Slide(MIcons.Shield, R.string.onboarding_title_2, R.string.onboarding_body_2),
        Slide(MIcons.Reports, R.string.onboarding_title_3, R.string.onboarding_body_3),
    )
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinish) { Text(context.getString(R.string.skip)) }
            }
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { index ->
                val slide = slides[index]
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(132.dp)
                            .clip(RoundedCornerShape(44.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            slide.icon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = context.getString(slide.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = context.getString(slide.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(slides.size) { index ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (index == pager.currentPage) 26.dp else 9.dp, 9.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pager.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (pager.currentPage == slides.lastIndex) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    if (pager.currentPage == slides.lastIndex) context.getString(R.string.get_started)
                    else context.getString(R.string.next),
                )
            }
        }
    }
}
