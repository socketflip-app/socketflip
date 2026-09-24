package app.socketflip

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: the same single flip as the floating button.
 * Lit while the tunnel is up, so the next tap is visibly the one that takes it down.
 */
class FlipTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = if (FlipVpnService.isUp) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        // FlipVpnService asks for a listening refresh once the flip has landed.
        FlipVpnService.flip(this)
    }
}
