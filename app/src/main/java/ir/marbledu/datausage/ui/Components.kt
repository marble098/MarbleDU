package ir.marbledu.datausage.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.core.graphics.drawable.toBitmap
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.data.Formats

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        action?.invoke()
    }
}

/** کارت آمار با اعداد درشت و نمودار موج‌دار متریال اکسپرسیو. */
@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    progress: Float? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress != null) {
                val animated by animateFloatAsState(progress, label = "progress")
                LinearWavyProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                )
            }
        }
    }
}

/** ردیف اپ با آیکون، نام، حجم و نوار نسبت مصرف. */
@Composable
fun UsageBar(
    label: String,
    bytes: Long,
    fraction: Float,
    persian: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trailing: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = Formats.bytes(bytes, persian),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                Text(trailing, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.14f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

/** چیپ کوچک برای برچسب‌ها (اپراتور، نوع شبکه و …). */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------
// اجزای نسخه‌ی ۱.۱
// ---------------------------------------------------------------------------

/**
 * نشان اپراتور — «کاشی لوگو».
 *
 *  - کاشی گرد سفید (در تم تیره: سطح روشنِ خنثی) تا نشان‌های رنگی و پرکنتراست
 *    روی هر پوسته‌ای خوانا بمانند؛
 *  - حلقه‌ی نازکِ رنگ برند دور کاشی، و یک هاله‌ی بسیار محو از همان رنگ، تا هر
 *    اپراتور «امضای رنگی» خودش را داشته باشد بدون این‌که لوگو را بپوشاند؛
 *  - لوگو با مقیاس اختصاصی خودش (پهن‌ها بزرگ‌تر، مربعی‌ها کوچک‌تر) وسط‌چین
 *    می‌شود تا وزن بصری همه‌ی نشان‌ها یکسان دیده شود؛
 *  - اگر لوگویی موجود نباشد، نشان حرف‌دار با گرادیان رنگ برند رسم می‌شود.
 */
@Composable
fun OperatorBadge(
    operator: ir.marbledu.datausage.data.OperatorCatalog.Operator,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    transportIcon: ImageVector? = null,
) {
    val brand = Color(operator.brandColor)
    val accent = Color(operator.accentColor)
    // متن روی کاشی برند با کنتراست تضمینی انتخاب می‌شود (سیاه یا سفید، هرکدام خواناتر).
    val onBrand = ir.marbledu.datausage.ui.theme.bestOnColor(brand)
    val readableAccent = ir.marbledu.datausage.ui.theme.accessibleColor(accent, onBrand)
    val logo = OperatorLogos.logoFor(operator.id)
    val marble = LocalMarbleColors.current
    val shape = RoundedCornerShape(percent = 30)
    Box(modifier.size(size)) {
        // هاله‌ی رنگی برند، خیلی کم‌رنگ و بیرون از کاشی؛ عمق ملایمی به نشان می‌دهد.
        Box(
            Modifier
                .fillMaxSize()
                .padding(size * 0.02f)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(brand.copy(alpha = if (marble.dark) 0.30f else 0.18f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(size * 0.055f)
                .clip(shape)
                .background(if (marble.dark) Color(0xFFF4F6FA) else Color.White)
                .border(
                    width = if (size.value >= 34f) 1.dp else 0.7.dp,
                    color = brand.copy(alpha = if (marble.dark) 0.55f else 0.30f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                Image(
                    painter = painterResource(logo),
                    contentDescription = operator.en,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(OperatorLogos.fitScale(operator.id)),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(brand, readableAccent))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = operator.mark,
                        color = onBrand,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * (if (operator.mark.length > 3) 0.26f else 0.32f)).sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        if (transportIcon != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = size * 0.06f, y = size * 0.06f)
                    .size(size * 0.46f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    transportIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(size * 0.27f),
                )
            }
        }
    }
}

/** آیکون واقعی اپ از روی نام پکیج؛ اگر پیدا نشد یک دایره با حرف اول. */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    fallbackColor: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size).clip(RoundedCornerShape(percent = 26)),
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(percent = 26))
                .background(fallbackColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.trim().take(1).uppercase().ifEmpty { "?" },
                color = fallbackColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/**
 * کارت استاندارد اپ.
 *
 * در تم AMOLED سطح‌ها کاملاً تیره‌اند، بنابراین یک خط موییِ روشن دور کارت
 * کشیده می‌شود تا مرز کارت‌ها دیده شود و کارت‌ها «شناور» به‌نظر برسند.
 */
@Composable
fun MarbleCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val marble = LocalMarbleColors.current
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (marble.cardBorder != null) {
                    Modifier.border(1.dp, marble.cardBorder, shape)
                } else Modifier,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/**
 * کارت بزرگ گرادیانی بالای صفحه‌ها.
 *
 * گرادیان سه‌رنگ + دو هاله‌ی نوری محو، عمق و زنده‌بودن بیشتری می‌دهد؛
 * لبه‌ی بالا هم بازتاب نور دارد تا شبیه شیشه‌ی رنگی دیده شود.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.heroStart, colors.heroEnd, colors.heroStart.copy(alpha = 0.92f)),
                ),
            ),
    ) {
        // هاله‌های نوری
        Canvas(Modifier.fillMaxSize().clip(shape)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = if (colors.dark) 0.16f else 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.05f),
                    radius = size.width * 0.6f,
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.88f, size.height * 0.05f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(scheme.secondary.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.95f),
                    radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.05f, size.height * 0.95f),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (colors.dark) 0.10f else 0.22f), Color.Transparent),
                    endY = size.height * 0.42f,
                ),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.42f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    Color.White.copy(alpha = if (colors.dark) 0.14f else 0.30f),
                    shape,
                ),
        )
        CompositionLocalProvider(LocalContentColor provides colors.onHero) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/** عنوان کارت با آیکون رنگی. */
@Composable
fun CardHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: String? = null,
    subtitle: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}

/** یک عدد با برچسب و نقطه‌ی رنگی (دانلود / آپلود / مجموع). */
@Composable
fun Metric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            } else {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** راهنمای رنگ نمودار. */
@Composable
fun LegendDot(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** فضای خالی پایین لیست‌ها تا محتوا زیر نوار شناور پنهان نماند. */
val FloatingBarClearance = 94.dp
