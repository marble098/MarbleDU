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
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage

// رنگ‌های برند MarbleDU – آبی مرمری با لهجه‌ی فیروزه‌ای
private val MarblePrimary = Color(0xFF2F6FED)
private val MarbleSecondary = Color(0xFF00A99D)
private val MarbleTertiary = Color(0xFF7B5CFA)
private val MarbleSurfaceLight = Color(0xFFF7F9FE)
private val MarbleSurfaceDark = Color(0xFF0F1218)

private val LightColors = lightColorScheme(
    primary = MarblePrimary,
    secondary = MarbleSecondary,
    tertiary = MarbleTertiary,
    background = MarbleSurfaceLight,
    surface = MarbleSurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EB8FF),
    secondary = Color(0xFF6FE3D6),
    tertiary = Color(0xFFC9B8FF),
    background = MarbleSurfaceDark,
    surface = MarbleSurfaceDark,
)

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
    language: AppLanguage,
    darkTheme: Boolean = isSystemInDarkTheme(),
    expressive: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val persian = language == AppLanguage.PERSIAN
    val layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr

    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    // در حالت روشن و بدون رنگ پویا از پالت Expressive خودمان استفاده می‌کنیم.
    val scheme: ColorScheme = if (expressive && !darkTheme && !dynamicColor) {
        expressiveLightColorScheme().copy(
            primary = MarblePrimary,
            secondary = MarbleSecondary,
            tertiary = MarbleTertiary,
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        if (expressive) {
            MaterialExpressiveTheme(
                colorScheme = scheme,
                motionScheme = MotionScheme.expressive(),
                typography = VazirTypography,
                content = content,
            )
        } else {
            MaterialTheme(
                colorScheme = scheme,
                typography = VazirTypography,
                content = content,
            )
        }
    }
}
