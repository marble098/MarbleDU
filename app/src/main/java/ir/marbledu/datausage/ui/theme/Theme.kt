package ir.marbledu.datausage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import ir.marbledu.datausage.R

/**
 * پالت‌های رنگی اپ. هر پالت سه رنگ اصلی دارد و بقیه‌ی رنگ‌ها (سطح‌ها، کارت‌ها،
 * حالت تیره) از روی همان‌ها ساخته می‌شود تا کل اپ یک‌دست و رنگی بماند.
 *
 * رنگ‌ها در نسخه‌ی ۱.۵ عمداً شادتر و زنده‌تر شده‌اند: اشباع بالاتر،
 * روشناییِ متعادل و کنتراست کافی برای متن.
 */
enum class ThemePalette(
    val fa: String,
    val en: String,
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
) {
    MARBLE("مرمر آبی", "Marble Blue", 0xFF2F6FED, 0xFF00C2B2, 0xFF8B5CF6),
    EMERALD("زمرد", "Emerald", 0xFF00B87A, 0xFF2E86DE, 0xFFFFB300),
    SUNSET("غروب", "Sunset", 0xFFFF5A1F, 0xFFFF2D78, 0xFFFFB020),
    GRAPE("انگور", "Grape", 0xFF8B2FE8, 0xFFF02C86, 0xFF00B4F0),
    ROSE("گل سرخ", "Rose", 0xFFF5136B, 0xFFA227F0, 0xFFFFB020),
    OCEAN("اقیانوس", "Ocean", 0xFF00A8D8, 0xFF2D6BFF, 0xFF14D8B4),
    SAFFRON("زعفران", "Saffron", 0xFFF59E00, 0xFFE04E00, 0xFF7CC400),
    FIROOZEH("فیروزه", "Turquoise", 0xFF00C9B7, 0xFF0A84FF, 0xFFFF4FA8),
    CANDY("آب‌نبات", "Candy", 0xFFFF3D8B, 0xFF9B5CFF, 0xFF00D4FF),
    NEON("نئون", "Neon", 0xFF9BE600, 0xFF00D9FF, 0xFFFF2BD6),
    AURORA("شفق", "Aurora", 0xFF22D98B, 0xFF7A5CFF, 0xFF00C8FF),
    ;

    fun name(persian: Boolean): String = if (persian) fa else en

    companion object {
        fun of(name: String?): ThemePalette = entries.firstOrNull { it.name == name } ?: MARBLE
    }
}

/** رنگ‌های نمودارها و گرادیان‌ها که از پالت فعلی می‌آیند. */
data class MarbleColors(
    val chart: List<Color>,
    val download: Color,
    val upload: Color,
    val mobile: Color,
    val wifi: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val dark: Boolean,
    /** تم سیاه مطلق؛ سطح‌ها کاملاً مشکی‌اند و کارت‌ها با خط نازک جدا می‌شوند. */
    val amoled: Boolean = false,
    /** در تم AMOLED یک خط موییِ روشن دور کارت‌ها کشیده می‌شود. */
    val cardBorder: Color? = null,
)

/** مقیاس اندازه‌ی متن فعلی (۱ = متوسط)؛ برای تیتر بزرگ صفحه‌ها لازم است. */
val LocalTextScale = staticCompositionLocalOf { 1f }

val LocalMarbleColors = staticCompositionLocalOf {
    MarbleColors(
        chart = listOf(Color(0xFF2F6FED), Color(0xFF8B5CF6), Color(0xFF00C2B2)),
        download = Color(0xFF2F6FED),
        upload = Color(0xFF8B5CF6),
        mobile = Color(0xFF2F6FED),
        wifi = Color(0xFF00C2B2),
        heroStart = Color(0xFF2F6FED),
        heroEnd = Color(0xFF8B5CF6),
        onHero = Color.White,
        dark = false,
    )
}

private val ExtraChartColors = listOf(
    Color(0xFFFF8A3D), Color(0xFFE5488A), Color(0xFF3DB8F5), Color(0xFF9ACD32),
    Color(0xFFB57BFF), Color(0xFFFFC233), Color(0xFF26C6A6), Color(0xFFFF6B6B),
)

private fun mix(a: Color, b: Color, t: Float): Color = lerp(a, b, t.coerceIn(0f, 1f))

/** رنگ‌های روشن و شادِ پالت؛ حداقل کنتراست برای متن حفظ می‌شود. */
private fun paletteScheme(palette: ThemePalette, dark: Boolean): ColorScheme {
    val p = Color(palette.primary)
    val s = Color(palette.secondary)
    val t = Color(palette.tertiary)
    val white = Color.White
    val black = Color.Black
    return if (!dark) {
        // پس‌زمینه‌ی روشن اما با رگه‌ی رنگیِ بیشتر از قبل تا اپ «شاد» دیده شود.
        val background = mix(Color(0xFFF6F8FF), p, 0.06f)
        lightColorScheme(
            primary = p,
            onPrimary = white,
            primaryContainer = mix(p, white, 0.80f),
            onPrimaryContainer = mix(p, black, 0.62f),
            secondary = s,
            onSecondary = white,
            secondaryContainer = mix(s, white, 0.82f),
            onSecondaryContainer = mix(s, black, 0.62f),
            tertiary = t,
            onTertiary = white,
            tertiaryContainer = mix(t, white, 0.82f),
            onTertiaryContainer = mix(t, black, 0.62f),
            background = background,
            onBackground = Color(0xFF14161D),
            surface = background,
            onSurface = Color(0xFF14161D),
            surfaceVariant = mix(Color(0xFFE7E9F3), p, 0.09f),
            onSurfaceVariant = Color(0xFF4E5361),
            outline = Color(0xFF6F7483),
            outlineVariant = mix(Color(0xFFC3C7D4), p, 0.10f),
            surfaceContainerLowest = white,
            surfaceContainerLow = mix(Color(0xFFF1F3FA), p, 0.075f),
            surfaceContainer = mix(Color(0xFFECEEF8), p, 0.085f),
            surfaceContainerHigh = mix(Color(0xFFE4E7F4), p, 0.10f),
            surfaceContainerHighest = mix(Color(0xFFDBDFEF), p, 0.11f),
            surfaceDim = mix(Color(0xFFD5D9E6), p, 0.09f),
            surfaceBright = mix(Color(0xFFFBFBFF), p, 0.06f),
        )
    } else {
        val base = mix(Color(0xFF0E1016), p, 0.055f)
        darkColorScheme(
            primary = mix(p, white, 0.38f),
            onPrimary = mix(p, black, 0.72f),
            primaryContainer = mix(p, black, 0.48f),
            onPrimaryContainer = mix(p, white, 0.88f),
            secondary = mix(s, white, 0.38f),
            onSecondary = mix(s, black, 0.72f),
            secondaryContainer = mix(s, black, 0.48f),
            onSecondaryContainer = mix(s, white, 0.88f),
            tertiary = mix(t, white, 0.38f),
            onTertiary = mix(t, black, 0.72f),
            tertiaryContainer = mix(t, black, 0.48f),
            onTertiaryContainer = mix(t, white, 0.88f),
            background = base,
            onBackground = Color(0xFFE9EBF2),
            surface = mix(Color(0xFF12151C), p, 0.08f),
            onSurface = Color(0xFFE9EBF2),
            surfaceVariant = mix(Color(0xFF2B303A), p, 0.09f),
            onSurfaceVariant = Color(0xFFC4C8D3),
            outline = Color(0xFF8F94A1),
            outlineVariant = mix(Color(0xFF424752), p, 0.10f),
            surfaceContainerLowest = mix(Color(0xFF0A0C11), p, 0.08f),
            surfaceContainerLow = mix(Color(0xFF161922), p, 0.09f),
            surfaceContainer = mix(Color(0xFF1B1F28), p, 0.09f),
            surfaceContainerHigh = mix(Color(0xFF232832), p, 0.09f),
            surfaceContainerHighest = mix(Color(0xFF2A303B), p, 0.09f),
            surfaceDim = Color(0xFF0E1016),
            surfaceBright = mix(Color(0xFF383E4A), p, 0.09f),
        )
    }
}

/**
 * تم «AMOLED»: پس‌زمینه و سطح‌های پایه کاملاً سیاه (#000) تا در پنل OLED
 * پیکسل‌ها خاموش بمانند و مصرف باتری کمتر شود؛ رنگ پالت فقط برای تاکیدها
 * می‌ماند تا اپ روی سیاه مطلق زنده و رنگی دیده شود.
 */
private fun amoledScheme(palette: ThemePalette): ColorScheme {
    val p = Color(palette.primary)
    val s = Color(palette.secondary)
    val t = Color(palette.tertiary)
    val white = Color.White
    val black = Color.Black
    return darkColorScheme(
        primary = mix(p, white, 0.22f),
        onPrimary = black,
        primaryContainer = mix(p, black, 0.62f),
        onPrimaryContainer = mix(p, white, 0.92f),
        secondary = mix(s, white, 0.22f),
        onSecondary = black,
        secondaryContainer = mix(s, black, 0.62f),
        onSecondaryContainer = mix(s, white, 0.92f),
        tertiary = mix(t, white, 0.22f),
        onTertiary = black,
        tertiaryContainer = mix(t, black, 0.62f),
        onTertiaryContainer = mix(t, white, 0.92f),
        background = black,
        onBackground = Color(0xFFF2F4F9),
        surface = black,
        onSurface = Color(0xFFF2F4F9),
        surfaceVariant = Color(0xFF101014),
        onSurfaceVariant = Color(0xFFB7BBC6),
        outline = Color(0xFF6B6F7B),
        outlineVariant = Color(0xFF23232B),
        surfaceContainerLowest = black,
        surfaceContainerLow = Color(0xFF07070A),
        surfaceContainer = Color(0xFF0B0B0F),
        surfaceContainerHigh = Color(0xFF121217),
        surfaceContainerHighest = Color(0xFF191920),
        surfaceDim = black,
        surfaceBright = Color(0xFF2A2A33),
    )
}

/** فونت وزیرمتن – خوانا برای فارسی و انگلیسی. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/**
 * سلسله‌مراتب تایپوگرافی جمع‌وجورتر با فاصله‌ی خطوط مناسب فارسی؛ اعداد مهم‌اند، نه تیترهای غول‌آسا.
 * [scale] از تنظیمات «اندازه‌ی متن» می‌آید (۱ = متوسط؛ پیش‌فرض اپ «کوچک» است).
 */
private fun scaledTypography(scale: Float): Typography {
    val base = Typography()
    fun scaled(style: TextStyle, sizeScale: Float, lineScale: Float): TextStyle {
        val size = style.fontSize * sizeScale * scale
        return style.copy(fontFamily = Vazirmatn, fontSize = size, lineHeight = size * lineScale)
    }
    return Typography(
        displayLarge = scaled(base.displayLarge, 0.72f, 1.22f),
        displayMedium = scaled(base.displayMedium, 0.78f, 1.24f),
        displaySmall = scaled(base.displaySmall, 0.82f, 1.28f),
        headlineLarge = scaled(base.headlineLarge, 0.86f, 1.30f),
        headlineMedium = scaled(base.headlineMedium, 0.90f, 1.32f),
        headlineSmall = scaled(base.headlineSmall, 0.92f, 1.34f),
        titleLarge = scaled(base.titleLarge, 0.92f, 1.34f),
        titleMedium = scaled(base.titleMedium, 0.96f, 1.38f),
        titleSmall = scaled(base.titleSmall, 0.96f, 1.38f),
        bodyLarge = scaled(base.bodyLarge, 0.96f, 1.52f),
        bodyMedium = scaled(base.bodyMedium, 0.96f, 1.50f),
        bodySmall = scaled(base.bodySmall, 0.98f, 1.48f),
        labelLarge = scaled(base.labelLarge, 0.96f, 1.38f),
        labelMedium = scaled(base.labelMedium, 0.96f, 1.38f),
        labelSmall = scaled(base.labelSmall, 0.96f, 1.38f),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarbleDuTheme(
    persian: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: ThemePalette = ThemePalette.MARBLE,
    textScale: Float = 0.82f,
    /** تم سیاه مطلق (AMOLED)؛ رنگ والپیپر و حالت روشن را نادیده می‌گیرد. */
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = androidx.compose.runtime.remember(textScale) { scaledTypography(textScale) }

    val useDynamic = !amoled && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme: ColorScheme = when {
        amoled -> amoledScheme(palette)
        useDynamic -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> paletteScheme(palette, darkTheme)
    }

    val marble = MarbleColors(
        chart = listOf(scheme.primary, scheme.tertiary, scheme.secondary) + ExtraChartColors,
        download = scheme.primary,
        upload = scheme.tertiary,
        mobile = scheme.primary,
        wifi = scheme.secondary,
        heroStart = when {
            amoled -> mix(Color(palette.primary), Color.Black, 0.30f)
            useDynamic -> scheme.primary
            darkTheme -> mix(Color(palette.primary), Color.Black, 0.22f)
            else -> Color(palette.primary)
        },
        heroEnd = when {
            amoled -> mix(Color(palette.tertiary), Color.Black, 0.34f)
            useDynamic -> scheme.tertiary
            darkTheme -> mix(Color(palette.tertiary), Color.Black, 0.26f)
            else -> Color(palette.tertiary)
        },
        onHero = if (useDynamic) scheme.onPrimary else Color.White,
        dark = darkTheme || amoled,
        amoled = amoled,
        cardBorder = if (amoled) Color.White.copy(alpha = 0.075f) else null,
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalMarbleColors provides marble,
        LocalTextScale provides textScale,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
            content = content,
        )
    }
}
