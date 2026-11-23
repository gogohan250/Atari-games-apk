package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.GameEntity
import com.example.ui.components.AtariGameCoverCard
import com.example.ui.components.AtariHeader
import com.example.ui.components.formatPlayDuration
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun FavoritesScreen(
    favorites: List<GameEntity>,
    onPlayGame: (GameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val totalPlaySec = favorites.sumOf { it.playTimeSeconds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AtariHeader(title = "${strings.favoritesTitle} (${favorites.size})")

        if (favorites.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AtariAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${favorites.size} ${strings.gamesCountSuffix}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = formatPlayDuration(totalPlaySec),
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = strings.favoritesEmpty,
                        tint = AtariAmber.copy(alpha = 0.45f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.favoritesEmpty,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.favoritesEmptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtariTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_grid")
            ) {
                items(favorites, key = { it.id }) { game ->
                    AtariGameCoverCard(
                        game = game,
                        onClick = { onPlayGame(game) },
                        onToggleFavorite = { onToggleFavorite(game.id) }
                    )
                }
            }
        }
    }
}
