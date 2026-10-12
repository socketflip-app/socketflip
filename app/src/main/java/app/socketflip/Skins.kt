package app.socketflip

/** A ready-made look with a name (a string resource, so it can be translated), shown in the Skins row in Settings. */
data class Skin(val name: Int, val look: Look)

object Skins {
    // Each one keeps the icon readable on both button colours and the two states
    // clearly different (checked for contrast and colour difference, including for
    // red-green colour blindness in Clear Pair).
    val ALL = listOf(
        Skin(R.string.skin_default, Look.DEFAULT),
        Skin(R.string.skin_tavern, Look(0xFF8B3A1A.toInt(), 0xFF1F5566.toInt(), 0xFFE0B048.toInt(), 0xFFF5E6C4.toInt(), 0.85f, 1.0f, 56, mirrorIcon = true)),
        Skin(R.string.skin_subtle, Look(0xFF757575.toInt(), 0xFF263238.toInt(), 0xFFE0E0E0.toInt(), 0xFFFFFFFF.toInt(), 0.35f, 0.9f, 44)),
        Skin(R.string.skin_high_contrast, Look(0xFF000000.toInt(), 0xFF4A148C.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFEB3B.toInt(), 1.0f, 1.0f, 64)),
        Skin(R.string.skin_midnight, Look(0xFF1A237E.toInt(), 0xFF004D40.toInt(), 0xFF90A4AE.toInt(), 0xFFCFD8DC.toInt(), 0.6f, 0.95f, 48)),
        Skin(R.string.skin_stealth, Look(0xFF1C1C1C.toInt(), 0xFF0D47A1.toInt(), 0xFF8A8A8A.toInt(), 0xFF9E9E9E.toInt(), 0.5f, 0.9f, 46)),
        Skin(R.string.skin_graphite, Look(0xFF000000.toInt(), 0xFF757575.toInt(), 0xFFBDBDBD.toInt(), 0xFFFFFFFF.toInt(), 0.75f, 1.0f, 52)),
        Skin(R.string.skin_candy, Look(0xFFEC407A.toInt(), 0xFF7E57C2.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFFFFF.toInt(), 0.85f, 1.0f, 56)),
        Skin(R.string.skin_citrus, Look(0xFFFB8C00.toInt(), 0xFF43A047.toInt(), 0xFFFFFFFF.toInt(), 0xFF212121.toInt(), 0.85f, 1.0f, 56)),
        Skin(R.string.skin_sherbet, Look(0xFFFFD54F.toInt(), 0xFF4DD0E1.toInt(), 0xFF6A1B9A.toInt(), 0xFF263238.toInt(), 0.8f, 1.0f, 54)),
        Skin(R.string.skin_ember, Look(0xFFD84315.toInt(), 0xFF6D4C41.toInt(), 0xFFFFCA28.toInt(), 0xFFFFF3E0.toInt(), 0.8f, 1.0f, 52)),
        Skin(R.string.skin_lagoon, Look(0xFF0288D1.toInt(), 0xFF5E35B1.toInt(), 0xFF80DEEA.toInt(), 0xFFFFFFFF.toInt(), 0.8f, 1.0f, 52)),
        Skin(R.string.skin_clear_pair, Look(0xFF0072B2.toInt(), 0xFFE69F00.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0.85f, 1.0f, 56)),
        Skin(R.string.skin_whisper, Look(0xFF424242.toInt(), 0xFF1565C0.toInt(), 0xFFBDBDBD.toInt(), 0xFFFFFFFF.toInt(), 0.2f, 0.7f, 36)),
    )
}
