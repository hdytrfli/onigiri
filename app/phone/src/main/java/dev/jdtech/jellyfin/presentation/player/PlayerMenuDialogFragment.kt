package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ListView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.lang.IllegalStateException

/**
 * Combined player options dialog: audio, subtitle and speed selection in one dialog with tabs.
 * Audio/subtitle rows show the track label plus language/codec, and the selected row is checked.
 */
class PlayerMenuDialogFragment(private val viewModel: PlayerViewModel) : DialogFragment() {
    private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    private val speedTexts = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity ?: throw IllegalStateException("Activity cannot be null")

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_player_menu, null)
        val tabs = view.findViewById<TabLayout>(R.id.menu_tabs)
        val list = view.findViewById<ListView>(R.id.menu_list)

        val adapter =
            PlayerMenuAdapter(activity) { position ->
                onItemClick(tabs.selectedTabPosition, position)
            }
        list.adapter = adapter

        tabs.addTab(tabs.newTab().setText(R.string.player_menu_tab_audio))
        tabs.addTab(tabs.newTab().setText(R.string.player_menu_tab_subtitles))
        tabs.addTab(tabs.newTab().setText(R.string.player_menu_tab_speed))
        tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    adapter.submit(itemsFor(tab.position))
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
        adapter.submit(itemsFor(0))

        return MaterialAlertDialogBuilder(activity).setView(view).create()
    }

    private fun itemsFor(tab: Int): List<PlayerMenuItem> =
        when (tab) {
            0 -> trackItems(C.TRACK_TYPE_AUDIO)
            1 -> trackItems(C.TRACK_TYPE_TEXT)
            else -> speedItems()
        }

    private fun trackItems(type: @C.TrackType Int): List<PlayerMenuItem> {
        val groups =
            viewModel.player.currentTracks.groups.filter { it.type == type && it.isSupported }

        val items = mutableListOf<PlayerMenuItem>()
        items.add(
            PlayerMenuItem(
                title = getString(CoreR.string.none),
                subtitle = null,
                selected = groups.none { it.isSelected },
            )
        )
        groups.forEachIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            items.add(
                PlayerMenuItem(
                    title = format.label ?: format.language ?: "Track ${index + 1}",
                    subtitle = format.codecs,
                    selected = group.isSelected,
                )
            )
        }
        return items
    }

    private fun speedItems(): List<PlayerMenuItem> = speeds.mapIndexed { index, speed ->
        PlayerMenuItem(
            title = speedTexts[index],
            subtitle = null,
            selected = speed == viewModel.playbackSpeed,
        )
    }

    private fun onItemClick(tab: Int, position: Int) {
        when (tab) {
            0 -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, position - 1)
            1 -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, position - 1)
            else -> viewModel.selectSpeed(speeds[position])
        }
        dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fix for hiding the system bars on API < 30
        activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
