package dev.jdtech.jellyfin.presentation.player

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import coil3.load
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidEpisode
import java.util.UUID

/** A single episode row in the player episodes sheet. */
class PlayerEpisodeAdapter(
    private val context: Context,
    private val onClick: (FindroidEpisode) -> Unit,
) : BaseAdapter() {
    private val items = mutableListOf<FindroidEpisode>()
    private var currentEpisodeId: UUID? = null

    fun submit(newItems: List<FindroidEpisode>, currentId: UUID?) {
        items.clear()
        items.addAll(newItems)
        currentEpisodeId = currentId
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): FindroidEpisode = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_player_episode, parent, false)

        val episode = items[position]
        val isCurrent = episode.id == currentEpisodeId

        view.findViewById<View>(R.id.episode_current_indicator).visibility =
            if (isCurrent) View.VISIBLE else View.INVISIBLE

        val titleView = view.findViewById<TextView>(R.id.episode_title)
        titleView.text =
            context.getString(CoreR.string.episode_name, episode.indexNumber, episode.name)
        titleView.setTypeface(null, if (isCurrent) Typeface.BOLD else Typeface.NORMAL)

        val overviewView = view.findViewById<TextView>(R.id.episode_overview)
        if (episode.overview.isBlank()) {
            overviewView.visibility = View.GONE
        } else {
            overviewView.visibility = View.VISIBLE
            overviewView.text = episode.overview
        }

        view.findViewById<ImageView>(R.id.episode_played).visibility =
            if (episode.played) View.VISIBLE else View.GONE

        val thumbnail = view.findViewById<ImageView>(R.id.episode_thumbnail)
        var imageUri = episode.images.primary ?: episode.images.showPrimary

        // Ugly workaround to append the files directory when loading local images
        if (imageUri?.scheme == null) {
            imageUri =
                Uri.Builder()
                    .appendEncodedPath("${context.filesDir}")
                    .appendEncodedPath(imageUri?.path)
                    .build()
        }
        thumbnail.load(imageUri)

        view.setOnClickListener { onClick(episode) }

        return view
    }
}
