package com.pegion.services

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.pegion.MainActivity
import com.pegion.R
import com.pegion.ui.screens.share.ShareTargetActivity

@RequiresApi(Build.VERSION_CODES.N)
class PegionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val url = getClipboardUrl()
        if (url != null) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.quick_settings_tile_ready)
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.quick_settings_tile_idle)
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val url = getClipboardUrl()
        val targetIntent = if (url != null) {
            Intent(this, ShareTargetActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Toast.makeText(this, R.string.quick_settings_no_clipboard, Toast.LENGTH_SHORT).show()
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(targetIntent)
        }
    }

    private fun getClipboardUrl(): String? {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboard?.primaryClip ?: return null
            if (clipData.itemCount <= 0) return null
            val item = clipData.getItemAt(0)
            val text = item.text?.toString()?.trim() ?: item.uri?.toString()?.trim() ?: return null
            if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
                text
            } else {
                val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
                urlRegex.find(text)?.value
            }
        } catch (_: Exception) {
            null
        }
    }
}
