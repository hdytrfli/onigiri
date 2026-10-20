package dev.jdtech.jellyfin.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets

/**
 * The top bar of every secondary page that only needs a way back: the back button on the leading
 * edge, no title and no other actions. The page's content and the navigation context say what the
 * page is, so settings submenus, identify, edit images and subtitle search all look identical.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            AppIconButton(
                onClick = onBack,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_left),
                        contentDescription = null,
                    )
                },
            )
        },
        windowInsets = topBarWindowInsets(),
        scrollBehavior = scrollBehavior,
    )
}
