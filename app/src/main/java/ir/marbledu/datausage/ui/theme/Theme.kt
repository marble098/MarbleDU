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
        lightColorScheme(
            primary = p,
            onPrimary = white,
            primaryContainer = mix(p, white, 0.82f),
            onPrimaryContainer = mix(p, black, 0.62f),
            secondary = s,
            onSecondary = white,
            secondaryContainer = mix(s, white, 0.82f),
            onSecondaryContainer = mix(s, black, 0.62f),
            tertiary = t,
            onTertiary = white,
            tertiaryContainer = mix(t, white, 0.82f),
            onTertiaryContainer = mix(t, black, 0.62f),
            background = mix(p, white, 0.955f),
            onBackground = mix(p, black, 0.88f),
            surface = mix(p, white, 0.955f),
            onSurface = mix(p, black, 0.88f),
            surfaceVariant = mix(p, white, 0.86f),
            onSurfaceVariant = mix(p, Color(0xFF3C3F46), 0.82f),
            outline = mix(p, Color(0xFF7A7D85), 0.75f),
            outlineVariant = mix(p, white, 0.74f),
            surfaceContainerLowest = white,
            surfaceContainerLow = mix(p, white, 0.975f),
            surfaceContainer = mix(p, white, 0.925f),
            surfaceContainerHigh = mix(p, white, 0.985f),
            surfaceContainerHighest = mix(p, white, 0.88f),
            surfaceDim = mix(p, white, 0.84f),
            surfaceBright = mix(p, white, 0.97f),
        )
    } else {
        val base = Color(0xFF0D0E13)
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
            background = mix(p, base, 0.94f),
            onBackground = Color(0xFFE7E8EE),
            surface = mix(p, base, 0.94f),
            onSurface = Color(0xFFE7E8EE),
            surfaceVariant = mix(p, base, 0.80f),
            onSurfaceVariant = Color(0xFFC3C6D0),
            outline = mix(p, Color(0xFF8C8F99), 0.80f),
            outlineVariant = mix(p, base, 0.70f),
            surfaceContainerLowest = mix(p, black, 0.96f),
            surfaceContainerLow = mix(p, base, 0.90f),
            surfaceContainer = mix(p, base, 0.87f),
            surfaceContainerHigh = mix(p, base, 0.84f),
            surfaceContainerHighest = mix(p, base, 0.79f),
            surfaceDim = mix(p, base, 0.94f),
            surfaceBright = mix(p, base, 0.74f),
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

private fun withVazirmatn(style: TextStyle): TextStyle =
    style.copy(fontFamily = Vazirmatn, lineHeight = style.fontSize * 1.75f)

private val VazirTypography = Typography().let { base ->
    Typography(
        displayLarge = withVazirmatn(base.displayLarge),
        displayMedium = withVazirmatn(base.displayMedium),
        displaySmall = withVazirmatn(base.displaySmall),
        headlineLarge = withVazirmatn(base.headlineLarge),
        headlineMedium = withVazirmatn(base.headlineMedium),
        headlineSmall = withVazirmatn(base.headlineSmall),
        titleLarge = withVazirmatn(base.titleLarge),
        titleMedium = withVazirmatn(base.titleMedium),
        titleSmall = withVazirmatn(base.titleSmall),
        bodyLarge = withVazirmatn(base.bodyLarge),
        bodyMedium = withVazirmatn(base.bodyMedium),
        bodySmall = withVazirmatn(base.bodySmall),
        labelLarge = withVazirmatn(base.labelLarge),
        labelMedium = withVazirmatn(base.labelMedium),
        labelSmall = withVazirmatn(base.labelSmall),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarbleDuTheme(
    persian: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: ThemePalette = ThemePalette.MARBLE,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr

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
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = VazirTypography,
            content = content,
        )
    }
}
