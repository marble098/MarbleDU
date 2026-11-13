package ir.marbledu.datausage.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun everyPaletteKeepsAccentAndButtonTextReadable() {
        ThemePalette.entries.forEach { palette ->
            listOf(paletteScheme(palette, false), paletteScheme(palette, true), amoledScheme(palette))
                .map { it.withAccessibleAccents() }.forEach { scheme ->
                    listOf(scheme.primary to scheme.onPrimary, scheme.secondary to scheme.onSecondary,
                        scheme.tertiary to scheme.onTertiary).forEach { (accent, label) ->
                        assertTrue("${palette.name}: button label", contrastRatio(accent, label) >= 4.499f)
                        listOf(scheme.background, scheme.surface, scheme.surfaceContainerLow,
                            scheme.surfaceContainer, scheme.surfaceContainerHigh,
                            scheme.surfaceContainerHighest).forEach { bg ->
                            assertTrue("${palette.name}: accent on card", contrastRatio(accent, bg) >= 4.499f)
                        }
                    }
                    assertTrue(contrastRatio(scheme.onSurface, scheme.surface) >= 4.5f)
                }
        }
    }

    @Test fun colorfulHeroEndpointsSupportWhiteText() {
        ThemePalette.entries.forEach { palette ->
            listOf(palette.primary, palette.tertiary).forEach {
                assertTrue(contrastRatio(accessibleColor(Color(it), Color.White), Color.White) >= 4.499f)
            }
        }
    }

    @Test fun choosesBlackForBrightOperatorBrand() {
        assertTrue(bestOnColor(Color(0xFF00A6C8)) == Color.Black)
    }
}
