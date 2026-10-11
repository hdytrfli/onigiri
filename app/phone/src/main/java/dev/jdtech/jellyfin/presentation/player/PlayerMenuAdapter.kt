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

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): PlayerMenuItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_player_menu, parent, false)

        val item = items[position]

        view.findViewById<ImageView>(R.id.item_check).visibility =
            if (item.selected) View.VISIBLE else View.INVISIBLE

        view.findViewById<TextView>(R.id.item_title).text = item.title

        val subtitleView = view.findViewById<TextView>(R.id.item_subtitle)
        if (item.subtitle.isNullOrBlank()) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.visibility = View.VISIBLE
            subtitleView.text = item.subtitle
        }

        view.setOnClickListener { onClick(position) }

        return view
    }
}
