package dev.hdytrfli.onigiri.ui.preview

import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.HomeItem
import dev.hdytrfli.onigiri.models.HomeSection
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.models.View
import java.util.UUID

val dummyHomeSuggestions = HomeItem.Suggestions(id = UUID.randomUUID(), items = dummyMovies)

val dummyHomeSection =
    HomeItem.Section(
        HomeSection(
            id = UUID.randomUUID(),
            name = UiText.DynamicString("Continue watching"),
            items = dummyMovies + dummyEpisodes,
        )
    )

val dummyHomeView =
    HomeItem.ViewItem(
        View(
            id = UUID.randomUUID(),
            name = "Movies",
            items = dummyMovies,
            type = CollectionType.Movies,
        )
    )
