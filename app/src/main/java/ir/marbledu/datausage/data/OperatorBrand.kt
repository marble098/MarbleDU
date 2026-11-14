package ir.marbledu.datausage.data

/** A readable, shared three-stop treatment for an active network operator. */
data class OperatorBrandGradient(val start: Int, val middle: Int, val end: Int)

/**
 * Keeps the operator's visual identity consistent in Compose, RemoteViews, and
 * notifications. Explicit entries preserve the recognised mobile brands; every
 * other provider gets a safe gradient derived from its catalog colours.
 */
object OperatorBrand {
    fun gradient(operator: OperatorCatalog.Operator): OperatorBrandGradient = when (operator.id) {
        "mci" -> OperatorBrandGradient(0xFF006AAE.toInt(), 0xFF00A6C8.toInt(), 0xFF26D7C7.toInt())
        "irancell" -> OperatorBrandGradient(0xFFFF8F00.toInt(), 0xFFFFC107.toInt(), 0xFFFFD54F.toInt())
        "rightel" -> OperatorBrandGradient(0xFF6A1B9A.toInt(), 0xFFB5179E.toInt(), 0xFFE0218A.toInt())
        "shatel_mobile", "shatel" -> OperatorBrandGradient(0xFF102A63.toInt(), 0xFF174EA6.toInt(), 0xFF2979FF.toInt())
        else -> OperatorBrandGradient(
            start = deepen(operator.brandColor.toInt(), 0.30f),
            middle = operator.brandColor.toInt(),
            end = operator.accentColor.toInt(),
        )
    }

    private fun deepen(color: Int, amount: Float): Int {
        fun channel(shift: Int): Int = (((color shr shift) and 0xFF) * (1f - amount)).toInt()
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
