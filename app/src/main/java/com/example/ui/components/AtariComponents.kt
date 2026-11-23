package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariStripeColors
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceBorder
import com.example.ui.theme.AtariTextMuted
import com.example.ui.theme.AtariTextPrimary
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun AtariHeader(
    title: String,
    modifier: Modifier = Modifier,
    leadingAction: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (leadingAction != null) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    leadingAction()
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )
            if (trailingAction != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    trailingAction()
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun AtariFourStripes(
    modifier: Modifier = Modifier,
    stripeHeight: Float = 4f
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AtariStripeColors.forEach { color ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stripeHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun AtariSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onImportClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AtariSurfaceBorder, RoundedCornerShape(25.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = placeholder,
            tint = AtariTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtariTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AtariTextPrimary),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )
        }
        if (onImportClick != null) {
            IconButton(
                onClick = onImportClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("quick_import_rom_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Import ROM",
                    tint = AtariAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AtariGameCoverCard(
    game: GameEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showMetadataBelow: Boolean = true
) {
    val baseColor = parseHexColor(game.colorHex)
    val darkShade = Color(
        red = baseColor.red * 0.56f,
        green = baseColor.green * 0.56f,
        blue = baseColor.blue * 0.56f,
        alpha = 1f
    )
    val sys = AtariSystem.fromCode(game.system)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("game_card_${game.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, darkShade),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                val diag = Path().apply {
                    moveTo(size.width * 0.12f, size.height)
                    lineTo(size.width, size.height * 0.22f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path = diag, color = darkShade.copy(alpha = 0.85f))
            }

            // System chip top-start
            Surface(
                color = Color.Black.copy(alpha = 0.38f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
            ) {
                Text(
                    text = sys.shortName,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Favorite Star button top-end
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .testTag("favorite_toggle_${game.id}")
            ) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (game.isFavorite) AtariAmber else Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Centered bold game title inside cover matching style guide
            Text(
                text = game.title,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp)
            )
        }

        if (showMetadataBelow) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = game.title,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${sys.displayName} • ${game.year}",
                style = MaterialTheme.typography.labelSmall,
                color = AtariTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class MainTab(val route: String) {
    HOME("home"),
    LIBRARY("library"),
    FAVORITES("favorites"),
    BROWSER("browser"),
    SETTINGS("settings")
}

@Composable
fun AtariBottomBar(
    currentRoute: String?,
    onNavigate: (MainTab) -> Unit,
    activeDownloadCount: Int
) {
    val strings = LocalAppStrings.current
    val tabs = listOf(
        Triple(MainTab.HOME, strings.navHome, Icons.Filled.Home to Icons.Outlined.Home),
        Triple(MainTab.LIBRARY, strings.navLibrary, Icons.Filled.Games to Icons.Outlined.Games),
        Triple(MainTab.FAVORITES, strings.navFavorites, Icons.Filled.Star to Icons.Outlined.StarBorder),
        Triple(MainTab.BROWSER, strings.navBrowser, Icons.Filled.Download to Icons.Outlined.Download),
        Triple(MainTab.SETTINGS, strings.navSettings, Icons.Filled.Settings to Icons.Outlined.Settings)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AtariSurfaceBorder,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label, icons) ->
                val selected = currentRoute == tab.route
                val tint = if (selected) MaterialTheme.colorScheme.primary else AtariTextSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(tab) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_${tab.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) icons.first else icons.second,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        if (tab == MainTab.BROWSER && activeDownloadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    AtariOrange
}

fun formatPlayDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}
