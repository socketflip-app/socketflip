package app.socketflip

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: the same single flip as the floating button. */
class FlipTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        FlipVpnService.flip(this)
    }
}
