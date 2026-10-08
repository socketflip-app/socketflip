package app.socketflip

import org.junit.Assert.assertTrue
import org.junit.Test

/** The restart question must read at a glance in light and dark mode. */
class RestartColorsTest {
    private fun check(c: RestartColors) {
        assertTrue(Look.contrast(c.text, c.background) >= 4.5)
        assertTrue(Look.contrast(c.restartText, c.restartFill) >= 4.5)
        assertTrue(Look.contrast(c.cancelText, c.cancelFill) >= 4.5)
        assertTrue(Look.contrast(c.cancelText, c.background) >= 4.5)
        // Button shapes stand out from the dialog (WCAG non-text contrast, 3:1).
        assertTrue(Look.contrast(c.cancelBorder, c.background) >= 3.0)
        // Restart and Cancel never look alike.
        assertTrue(Look.contrast(c.restartFill, c.cancelFill) >= 3.0)
    }

    @Test fun light() = check(RestartColors.LIGHT)

    @Test fun dark() = check(RestartColors.DARK)
}
