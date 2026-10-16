package dev.jdtech.jellyfin.presentation.player

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import dev.jdtech.jellyfin.R

/** A single selectable row in the combined player options dialog. */
data class PlayerMenuItem(
    val title: String,
    val subtitle: String?,
    val selected: Boolean,
    val iconRes: Int? = null,
    val badge: String? = null,
)

class PlayerMenuAdapter(
    private val context: Context,
    private val onClick: (Int) -> Unit,
) : BaseAdapter() {
    private val items = mutableListOf<PlayerMenuItem>()

    fun submit(newItems: List<PlayerMenuItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * Marks the row at [position] as the only selected one without waiting for the player to apply
     * the change. Used to keep the sheet open while reflecting the choice immediately.
     */
    fun select(position: Int) {
        items.indices.forEach { index ->
            items[index] = items[index].copy(selected = index == position)
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): PlayerMenuItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_player_menu, parent, false)

        val item = items[position]

        val iconView = view.findViewById<ImageView>(R.id.item_icon)
        if (item.iconRes == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(item.iconRes)
        }

        view.findViewById<ImageView>(R.id.item_check).visibility =
            if (item.selected) View.VISIBLE else View.INVISIBLE

        view.findViewById<TextView>(R.id.item_title).text = item.title

        val badgeView = view.findViewById<TextView>(R.id.item_badge)
        if (item.badge.isNullOrBlank()) {
            badgeView.visibility = View.GONE
        } else {
            badgeView.visibility = View.VISIBLE
            badgeView.text = item.badge
        }

        val subtitleView = view.findViewById<TextView>(R.id.item_subtitle)
        if (item.subtitle.isNullOrBlank()) {
            // Track rows always carry a second line (label, placeholder, or "No audio/subtitle"),
            // so collapsing here only affects lists that are meant to be one line, like speeds.
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.visibility = View.VISIBLE
            subtitleView.text = item.subtitle
        }

        view.setOnClickListener { onClick(position) }

        return view
    }
}
