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
 */
enum class ThemePalette(
    val fa: String,
    val en: String,
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
) {
    MARBLE("مرمر آبی", "Marble Blue", 0xFF2F6FED, 0xFF00A99D, 0xFF7B5CFA),
    EMERALD("زمرد", "Emerald", 0xFF0E9F6E, 0xFF2E86DE, 0xFFE0A100),
    SUNSET("غروب", "Sunset", 0xFFF2622E, 0xFFE8336D, 0xFFF5A623),
    GRAPE("انگور", "Grape", 0xFF7C3AED, 0xFFDB2777, 0xFF0EA5E9),
    ROSE("گل سرخ", "Rose", 0xFFE11D62, 0xFF9333EA, 0xFFF59E0B),
    OCEAN("اقیانوس", "Ocean", 0xFF0891B2, 0xFF2563EB, 0xFF14B8A6),
    SAFFRON("زعفران", "Saffron", 0xFFD97706, 0xFFC2410C, 0xFF65A30D),
    FIROOZEH("فیروزه", "Turquoise", 0xFF06B6A4, 0xFF0284C7, 0xFFEC4899),
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
)

/** مقیاس اندازه‌ی متن فعلی (۱ = متوسط)؛ برای تیتر بزرگ صفحه‌ها لازم است. */
val LocalTextScale = staticCompositionLocalOf { 1f }

val LocalMarbleColors = staticCompositionLocalOf {
    MarbleColors(
        chart = listOf(Color(0xFF2F6FED), Color(0xFF7B5CFA), Color(0xFF00A99D)),
        download = Color(0xFF2F6FED),
        upload = Color(0xFF7B5CFA),
        mobile = Color(0xFF2F6FED),
        wifi = Color(0xFF00A99D),
        heroStart = Color(0xFF2F6FED),
        heroEnd = Color(0xFF7B5CFA),
        onHero = Color.White,
        dark = false,
    )
}

private val ExtraChartColors = listOf(
    Color(0xFFFF8A3D), Color(0xFFE5488A), Color(0xFF3DB8F5), Color(0xFF9ACD32),
    Color(0xFFB57BFF), Color(0xFFFFC233), Color(0xFF26C6A6), Color(0xFFFF6B6B),
)

private fun mix(a: Color, b: Color, t: Float): Color = lerp(a, b, t.coerceIn(0f, 1f))

private fun paletteScheme(palette: ThemePalette, dark: Boolean): ColorScheme {
    val p = Color(palette.primary)
    val s = Color(palette.secondary)
    val t = Color(palette.tertiary)
    val white = Color.White
    val black = Color.Black
    return if (!dark) {
        val background = mix(Color(0xFFF7F8FC), p, 0.035f)
        lightColorScheme(
            primary = p,
            onPrimary = white,
            primaryContainer = mix(p, white, 0.84f),
            onPrimaryContainer = mix(p, black, 0.66f),
            secondary = s,
            onSecondary = white,
            secondaryContainer = mix(s, white, 0.86f),
            onSecondaryContainer = mix(s, black, 0.66f),
            tertiary = t,
            onTertiary = white,
            tertiaryContainer = mix(t, white, 0.86f),
            onTertiaryContainer = mix(t, black, 0.66f),
            background = background,
            onBackground = Color(0xFF191B22),
            surface = background,
            onSurface = Color(0xFF191B22),
            surfaceVariant = mix(Color(0xFFE8EAF1), p, 0.055f),
            onSurfaceVariant = Color(0xFF555A66),
            outline = Color(0xFF747986),
            outlineVariant = Color(0xFFC8CBD4),
            surfaceContainerLowest = white,
            surfaceContainerLow = mix(Color(0xFFF3F4F8), p, 0.055f),
            surfaceContainer = mix(Color(0xFFEFF1F6), p, 0.055f),
            surfaceContainerHigh = mix(Color(0xFFE9ECF3), p, 0.055f),
            surfaceContainerHighest = mix(Color(0xFFE2E5ED), p, 0.055f),
            surfaceDim = mix(Color(0xFFDADDE6), p, 0.055f),
            surfaceBright = mix(Color(0xFFFAFBFF), p, 0.055f),
        )
    } else {
        val base = mix(Color(0xFF101218), p, 0.045f)
        darkColorScheme(
            primary = mix(p, white, 0.42f),
            onPrimary = mix(p, black, 0.72f),
            primaryContainer = mix(p, black, 0.52f),
            onPrimaryContainer = mix(p, white, 0.84f),
            secondary = mix(s, white, 0.42f),
            onSecondary = mix(s, black, 0.72f),
            secondaryContainer = mix(s, black, 0.52f),
            onSecondaryContainer = mix(s, white, 0.84f),
            tertiary = mix(t, white, 0.42f),
            onTertiary = mix(t, black, 0.72f),
            tertiaryContainer = mix(t, black, 0.52f),
            onTertiaryContainer = mix(t, white, 0.84f),
            background = base,
            onBackground = Color(0xFFE6E8EF),
            surface = mix(Color(0xFF13161D), p, 0.07f),
            onSurface = Color(0xFFE6E8EF),
            surfaceVariant = mix(Color(0xFF2B303A), p, 0.07f),
            onSurfaceVariant = Color(0xFFC1C5D0),
            outline = Color(0xFF8B909D),
            outlineVariant = Color(0xFF424752),
            surfaceContainerLowest = mix(Color(0xFF0C0E13), p, 0.07f),
            surfaceContainerLow = mix(Color(0xFF171A22), p, 0.07f),
            surfaceContainer = mix(Color(0xFF1C2028), p, 0.07f),
            surfaceContainerHigh = mix(Color(0xFF232731), p, 0.07f),
            surfaceContainerHighest = mix(Color(0xFF2A2F3A), p, 0.07f),
            surfaceDim = Color(0xFF101218),
            surfaceBright = mix(Color(0xFF363B46), p, 0.07f),
        )
    }
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
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = androidx.compose.runtime.remember(textScale) { scaledTypography(textScale) }

    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme: ColorScheme = when {
        useDynamic -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> paletteScheme(palette, darkTheme)
    }

    val marble = MarbleColors(
        chart = listOf(scheme.primary, scheme.tertiary, scheme.secondary) + ExtraChartColors,
        download = scheme.primary,
        upload = scheme.tertiary,
        mobile = scheme.primary,
        wifi = scheme.secondary,
        heroStart = if (useDynamic) scheme.primary else if (darkTheme) mix(Color(palette.primary), Color.Black, 0.25f) else Color(palette.primary),
        heroEnd = if (useDynamic) scheme.tertiary else if (darkTheme) mix(Color(palette.tertiary), Color.Black, 0.30f) else Color(palette.tertiary),
        onHero = if (useDynamic) scheme.onPrimary else Color.White,
        dark = darkTheme,
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
