package com.wayside.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wayside.ui.navigation.WaysideDestination
import com.wayside.ui.theme.LocalWaysideColors

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val NAV_ITEMS = listOf(
    NavItem(WaysideDestination.HOME, "Explore", Icons.Rounded.Explore),
    NavItem(WaysideDestination.ROUTE, "Route", Icons.AutoMirrored.Rounded.AltRoute),
    NavItem(WaysideDestination.SAVED, "Saved", Icons.Rounded.Bookmarks),
    NavItem(WaysideDestination.PROFILE, "Profile", Icons.Rounded.Person),
)

@Composable
fun WaysideNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    NavigationBar(
        modifier = modifier,
        containerColor = scheme.surface,
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onPrimary,
                    selectedTextColor = scheme.primary,
                    indicatorColor = scheme.primary,
                    unselectedIconColor = wayside.textMuted,
                    unselectedTextColor = wayside.textMuted,
                ),
            )
        }
    }
}
