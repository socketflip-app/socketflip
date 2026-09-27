package app.socketflip

/** How the floating button looks. Colours are opaque ARGB; opacity is separate. */
data class Look(
    val downColor: Int,
    val upColor: Int,
    val ringColor: Int,
    val iconColor: Int,
    val idleAlpha: Float,
    val pressedAlpha: Float,
    val sizeDp: Int,
) {
    companion object {
        val DEFAULT = Look(
            downColor = 0xFF1E88E5.toInt(),
            upColor = 0xFF00897B.toInt(),
            ringColor = 0xFFFFA000.toInt(),
            iconColor = 0xFFFFFFFF.toInt(),
            idleAlpha = 0.8f,
            pressedAlpha = 1f,
            sizeDp = 52,
        )
    }
}
