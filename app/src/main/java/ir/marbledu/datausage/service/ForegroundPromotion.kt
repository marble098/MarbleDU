package ir.marbledu.datausage.service

/** Handles only platform start restrictions; programming errors must not be hidden. */
internal fun tryForegroundPromotion(
    promote: () -> Unit,
    isStartNotAllowed: (IllegalStateException) -> Boolean,
): Boolean = try {
    promote()
    true
} catch (error: IllegalStateException) {
    if (!isStartNotAllowed(error)) throw error
    false
}
