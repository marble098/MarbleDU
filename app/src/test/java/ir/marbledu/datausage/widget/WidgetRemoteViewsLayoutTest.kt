package ir.marbledu.datausage.widget

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نگهبان «قوانین RemoteViews» برای چیدمان‌های ویجت.
 *
 * چرا این تست وجود دارد؟ ویجت صفحه‌ی اصلی با inflater خودِ لانچر ساخته می‌شود،
 * نه با inflater برنامه. اندروید فقط کلاس‌هایی را اجازه می‌دهد که
 * `@RemoteView` دارند و بعضی ویژگی‌ها/منابع را هم در این مسیر پشتیبانی نمی‌کند.
 * هر تخلف، مستقیم به جعبه‌ی قرمز «Can't load widget» تبدیل می‌شود. این تست
 * دقیقاً همان دو موردی را می‌گیرد که در نسخه‌ی قبل باعث خطا می‌شدند:
 *
 *  ۱) ویوهای غیرمجاز (مثل `<View>` خام) در چیدمان،
 *  ۲) فونت سفارشی `@font/...` (اندروید از میانه‌ی ۲۰۲۱ در RemoteViews ممنوع کرده).
 *
 * همچنین هر ارجاع `@drawable/...` باید واقعاً وجود داشته باشد تا خطای inflation
 * فقط در گوشی کاربر دیده نشود.
 */
class WidgetRemoteViewsLayoutTest {

    private val allowedViews = setOf(
        "LinearLayout",
        "FrameLayout",
        "RelativeLayout",
        "GridLayout",
        "TextView",
        "ImageView",
        "ImageButton",
        "Button",
        "ProgressBar",
        "Chronometer",
        "ViewFlipper",
        "AdapterViewFlipper",
        "ListView",
        "GridView",
        "StackView",
        "ViewStub",
    )

    /** چیدمان‌های ویجت و اعلان پایش؛ هر دو با inflater سیستم (لانچر / نوار اعلان) ساخته می‌شوند. */
    private val layouts = listOf(
        "widget_usage.xml",
        "widget_preview.xml",
        "widget_fallback.xml",
        "notif_monitor_small.xml",
        "notif_monitor_big.xml",
    )

    @Test
    fun `widget layouts only use RemoteViews-safe views`() {
        layouts.forEach { name ->
            val xml = readLayout(name)
            val tags = Regex("<([A-Za-z][\\w.]*)").findAll(xml)
                .map { it.groupValues[1] }
                .filterNot { it.startsWith("?") || it == "!--" }
                .toSet()
            tags.forEach { tag ->
                assertTrue(
                    "$name از ویوی «$tag» استفاده می‌کند که در RemoteViews مجاز نیست",
                    tag in allowedViews,
                )
            }
            assertTrue("$name نباید از <View> خام استفاده کند", "<View" !in xml)
        }
    }

    @Test
    fun `widget layouts avoid custom fonts and theme attributes`() {
        layouts.forEach { name ->
            val xml = readLayout(name)
            assertEquals("$name نباید از فونت سفارشی استفاده کند", 0, Regex("@font/").findAll(xml).count())
            assertEquals("$name نباید از ویژگی تم استفاده کند", 0, Regex("\\?attr/").findAll(xml).count())
            assertEquals(
                "$name نباید از ویژگی تم اندروید استفاده کند",
                0,
                Regex("\\?android:attr/").findAll(xml).count(),
            )
            assertEquals("$name نباید از ویوهای AppCompat استفاده کند", 0, Regex("app:").findAll(xml).count())
        }
    }

    @Test
    fun `every drawable referenced by widget layouts exists`() {
        val resDir = layoutDir().parentFile
        layouts.forEach { name ->
            val xml = readLayout(name)
            Regex("@drawable/([a-z0-9_]+)").findAll(xml).map { it.groupValues[1] }.toSet().forEach { d ->
                val found = resDir.listFiles().orEmpty().any { dir ->
                    dir.isDirectory && File(dir, "$d.xml").exists()
                } || resDir.listFiles().orEmpty().any { dir ->
                    dir.isDirectory && (File(dir, "$d.webp").exists() || File(dir, "$d.png").exists())
                }
                assertTrue("$name به drawable ناموجود «$d» اشاره می‌کند", found)
            }
        }
    }

    /**
     * متن چیدمان بدون توضیحات XML.
     *
     * توضیحات خودشان اسم چیزهای ممنوعه را می‌آورند (مثلاً «این فایل `<View>` ندارد»)،
     * پس اگر حذف نشوند تست به‌اشتباه قرمز می‌شود.
     */
    private fun readLayout(name: String): String =
        File(layoutDir(), name).readText()
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    /** پوشه‌ی `res/layout` را از مسیر اجرای تست پیدا می‌کند (Gradle از پوشه‌ی ماژول اجرا می‌کند). */
    private fun layoutDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            val current = dir ?: return@repeat
            listOf("src/main/res/layout", "app/src/main/res/layout").forEach { rel ->
                val candidate = File(current, rel)
                if (candidate.isDirectory) return candidate
            }
            dir = current.parentFile
        }
        throw IllegalStateException(
            "پوشه‌ی res/layout پیدا نشد؛ مسیر شروع: ${System.getProperty("user.dir")}",
        )
    }
}
