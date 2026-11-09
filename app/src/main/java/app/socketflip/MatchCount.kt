package app.socketflip

/**
 * Reconnects this game. SocketFlip cannot see games, so a quiet spell with no
 * reconnect starts a new count. Pure, so the unit tests cover it.
 */
object MatchCount {
    /** The count as of [now]: [count] still stands unless [gapMs] has passed since [lastAt]. */
    fun current(count: Int, lastAt: Long, now: Long, gapMs: Long): Int =
        if (lastAt == 0L || now - lastAt > gapMs) 0 else count

    /** Milliseconds until the quiet spell resets the count, never negative. */
    fun resetIn(lastAt: Long, now: Long, gapMs: Long): Long = maxOf(0L, lastAt + gapMs - now)

    /** Badge shade: 0 grey, 1 amber (one before the limit), 2 red (at or past it). */
    fun level(count: Int, limit: Int): Int = when {
        count >= limit -> 2
        count == limit - 1 -> 1
        else -> 0
    }
}
