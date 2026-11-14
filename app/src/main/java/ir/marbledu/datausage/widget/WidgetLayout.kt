package ir.marbledu.datausage.widget

/**
 * چیزهایی که ویجت در یک اندازه‌ی مشخص نشان می‌دهد.
 * با بزرگ‌تر شدن ویجت، بخش‌های بیشتری روشن می‌شوند تا هیچ‌وقت متن بریده
 * یا روی هم نیفتد.
 */
data class WidgetVisibility(
    /** نام اپراتور زیر عنوان. */
    val operator: Boolean,
    /** نشان «در حال پایش / پایش خاموش». */
    val statusChip: Boolean,
    /** کارت‌های موبایل و وای‌فای. */
    val splitRow: Boolean,
    /** نوار سقف بسته. */
    val capRow: Boolean,
    /** درصد مصرف کنار نوار سقف. */
    val capPercent: Boolean,
    /** ردیف دانلود/آپلود (حالت کامل). */
    val detailRow: Boolean,
    /** دکمه‌های میان‌بر. */
    val actionsRow: Boolean,
    /** اندازه‌ی عدد بزرگ. */
    val heroTextSizeSp: Float,
    /** ویجت کوچک است و فقط سربرگ و عدد بزرگ را نشان می‌دهد. */
    val compact: Boolean,
)

/**
 * تصمیم‌گیری خالص درباره‌ی چیدمان ویجت بر اساس اندازه‌ی واقعی آن
 * (dp) و تنظیمات کاربر. هر بخش یک «حداقل ارتفاع» دارد و به ترتیب
 * اهمیت روشن می‌شود: اول اعداد، بعد سقف بسته، بعد میان‌برها.
 */
object WidgetLayout {

    /** سربرگ + عدد بزرگ + حاشیه‌ها. */
    const val BASE_HEIGHT_DP = 76

    /** حداقل ارتفاع برای کارت‌های موبایل/وای‌فای. */
    const val SPLIT_HEIGHT_DP = 148

    /** حداقل ارتفاع برای نوار سقف بسته. */
    const val CAP_HEIGHT_DP = 172

    /** حداقل ارتفاع برای دکمه‌های میان‌بر. */
    const val ACTIONS_HEIGHT_DP = 198

    /** حداقل ارتفاع برای ردیف دانلود/آپلود. */
    const val DETAIL_HEIGHT_DP = 222

    /** زیر این عرض، دو کارت کنار هم خوانا نیستند. */
    const val MIN_WIDTH_DP = 150

    /** زیر این عرض، نشان وضعیت و درصد جا نمی‌شوند. */
    const val WIDE_WIDTH_DP = 220

    /** عرض لازم برای عدد بزرگ‌تر. */
    const val BIG_WIDTH_DP = 280

    fun visibility(snapshot: WidgetSnapshot, widthDp: Int, heightDp: Int): WidgetVisibility {
        val width = widthDp.coerceAtLeast(0)
        val height = heightDp.coerceAtLeast(0)
        val roomyWidth = width >= MIN_WIDTH_DP
        val split = roomyWidth && height >= SPLIT_HEIGHT_DP
        val cap = snapshot.showCap && snapshot.hasCap && roomyWidth && height >= CAP_HEIGHT_DP
        val actions = snapshot.quickActions && roomyWidth && height >= ACTIONS_HEIGHT_DP
        val detail = snapshot.detailed && height >= DETAIL_HEIGHT_DP
        return WidgetVisibility(
            operator = snapshot.showOperator && snapshot.operatorName.isNotBlank(),
            statusChip = width >= WIDE_WIDTH_DP || !snapshot.monitoring,
            splitRow = split,
            capRow = cap,
            capPercent = cap && width >= WIDE_WIDTH_DP,
            detailRow = detail,
            actionsRow = actions,
            heroTextSizeSp = heroSize(snapshot, width, height),
            compact = height < SPLIT_HEIGHT_DP,
        )
    }

    private fun heroSize(snapshot: WidgetSnapshot, width: Int, height: Int): Float = when {
        height < SPLIT_HEIGHT_DP -> 22f
        snapshot.bigText -> 32f
        width >= BIG_WIDTH_DP -> 30f
        else -> 26f
    }
}
