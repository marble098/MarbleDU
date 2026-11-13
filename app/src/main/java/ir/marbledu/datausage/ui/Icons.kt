package ir.marbledu.datausage.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * آیکون‌های سبک متریال، بدون وابستگی به کتابخانه‌ی سنگین icons-extended.
 */
object MIcons {

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
            ) {
                addPathNodes(androidx.compose.ui.graphics.vector.PathParser().parsePathString(pathData).toNodes())
            }
        }.build()

    val Home = icon("home", "M4 21V9l8-6 8 6v12h-6v-7h-4v7z")
    val Live = icon("live", "M3 12h3.5l2.5-7 4 15 2.5-6H21v-2h-3.2l-3.6 8.6L9.6 5 7 12H3z")
    val Apps = icon("apps", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z")
    val Reports = icon("reports", "M5 3h9l5 5v13H5zM9 11h6v2H9zM9 15h6v2H9zM9 7h4v2H9z")
    val Settings = icon(
        "settings",
        "M12 8a4 4 0 100 8 4 4 0 000-8zM12 10.2a1.8 1.8 0 110 3.6 1.8 1.8 0 010-3.6z" +
            "M10.6 2h2.8l.4 2.2 1.7.9 2-1 2 2-1.1 1.9.6 1.9 2.1 1v2.6l-2.1 1-.6 1.9 1.1 1.9-2 2-2-1-1.7.9L13.4 22h-2.8l-.4-2.2-1.7-.9-2 1-2-2 1.1-1.9-.6-1.9-2.1-1v-2.6l2.1-1 .6-1.9-1.1-1.9 2-2 2 1 1.7-.9z",
    )
    val Download = icon("download", "M11 3h2v9.2l3.1-3.1 1.4 1.4L12 16l-5.5-5.5 1.4-1.4L11 12.2zM5 18h14v2H5z")
    val Upload = icon("upload", "M11 21h2v-9.2l3.1 3.1 1.4-1.4L12 8l-5.5 5.5 1.4 1.4L11 11.8zM5 4h14v2H5z")
    val Wifi = icon("wifi", "M12 20l-7-7a10 10 0 0114 0zM12 8a13 13 0 019.5 4.2L12 22 2.5 12.2A13 13 0 0112 8z")
    val Signal = icon("signal", "M3 20h3v-6H3zM8 20h3V9H8zM13 20h3V4h-3zM18 20h3v-9h-3z")
    val Speed = icon("speed", "M12 4a10 10 0 00-8.9 14.5l1.8-1a8 8 0 1114.2 0l1.8 1A10 10 0 0012 4zM13 7v5.3l3.4 2 1-1.7-2.4-1.4V7z")
    val Timer = icon("timer", "M9 1h6v2H9zM12 5a9 9 0 100 18 9 9 0 000-18zm1 4v5.4l3.5 2-1 1.8-4.5-2.6V9z")
    val Chart = icon("chart", "M4 20V4h2v14h14v2zM8 16V9h3v7zM13 16V6h3v10zM18 16v-4h3v4z")
    val Carrier = icon("carrier", "M12 3a10 10 0 100 20 10 10 0 000-20zm0 2a8 8 0 018 8h-4a4 4 0 00-4-4V5zM4 13h16a8 8 0 01-16 0z")
    val Share = icon("share", "M18 16a3 3 0 00-2.2 1l-6.9-4a3 3 0 000-2l6.9-4A3 3 0 1015 4l-6.9 4a3 3 0 100 8l6.9 4A3 3 0 1018 16z")
    val Globe = icon(
        "globe",
        "M12 2a10 10 0 100 20 10 10 0 000-20zM12 4c1.7 0 3.3 3 3.7 7H8.3C8.7 7 10.3 4 12 4z" +
            "M4.3 11h2.4c.1-2 .4-3.7 1-5A8 8 0 004.3 11zm0 2h3.4c.6 1.3 1.5 2.3 2.5 3a8 8 0 01-5.9-3z" +
            "M12 18.6c-1.7 0-3.3-3-3.7-7h7.4c-.4 4-2 7-3.7 7zM15.9 18c1-.7 1.9-1.7 2.5-3h3.4a8 8 0 01-5.9 3z",
    )
    val Shield = icon("shield", "M12 2l8 3v7c0 5-3.4 8.9-8 10-4.6-1.1-8-5-8-10V5z")
    val Clock = icon("clock", "M12 2a10 10 0 100 20 10 10 0 000-20zm1 5v5.3l4 2.4-1 1.7-5-3V7z")
    val Refresh = icon("refresh", "M12 5V2L7 7l5 5V9a5 5 0 11-5 5H5a7 7 0 107-9z")
    val Close = icon("close", "M18.3 5.7L12 12l6.3 6.3-1.4 1.4L10.6 13.4 4.3 19.7 2.9 18.3 9.2 12 2.9 5.7 4.3 4.3l6.3 6.3 6.3-6.3z")
    val ArrowForward = icon("arrow", "M12 4l8 8-8 8-1.4-1.4L16.2 13H4v-2h12.2L10.6 5.4z")
}
