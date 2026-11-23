package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.ui.components.AtariFourStripes
import com.example.ui.components.AtariGameCoverCard
import com.example.ui.components.AtariHeader
import com.example.ui.components.AtariSearchBar
import com.example.ui.components.TelegramChannelsCard
import com.example.ui.components.formatPlayDuration
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun HomeScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    lastPlayedGame: GameEntity?,
    recentlyAdded: List<GameEntity>,
    mostPlayed: List<GameEntity>,
    searchResults: List<GameEntity>,
    onPlayGame: (GameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onImportRomClick: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AtariHeader(title = strings.navHome)

        val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                AtariSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = strings.searchPlaceholder,
                    onImportClick = onImportRomClick
                )
            }

            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "${strings.navLibrary} (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = AtariAmber
                    )
                }
                items(searchResults.take(15), key = { it.id }) { game ->
                    val sys = AtariSystem.fromCode(game.system)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayGame(game) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.title,
                                    fontFamily = ChakraPetchFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${sys.displayName} • ${game.year} • ${game.genre}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AtariTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = strings.playNow,
                                tint = AtariOrange
                            )
                        }
                    }
                }
            } else {
                // Hero Continue Playing Card matching mockup Screen 1
                item {
                    val heroGame = lastPlayedGame ?: mostPlayed.firstOrNull()
                    if (heroGame != null) {
                        ContinuePlayingHeroCard(
                            game = heroGame,
                            continueLabel = strings.continuePlaying,
                            playLabel = strings.playNow,
                            onPlay = { onPlayGame(heroGame) }
                        )
                    }
                }

                // Recently Added Carousel
                item {
                    SectionRowHeader(
                        title = strings.recentlyAdded,
                        onSeeAll = onOpenLibrary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyAdded, key = { "rec_${it.id}" }) { game ->
                            AtariGameCoverCard(
                                game = game,
                                onClick = { onPlayGame(game) },
                                onToggleFavorite = { onToggleFavorite(game.id) },
                                modifier = Modifier.width(124.dp),
                                showMetadataBelow = false
                            )
                        }
                    }
                }

                // Most Played Carousel
                item {
                    SectionRowHeader(
                        title = strings.mostPlayed,
                        onSeeAll = onOpenLibrary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mostPlayed, key = { "most_${it.id}" }) { game ->
                            AtariGameCoverCard(
                                game = game,
                                onClick = { onPlayGame(game) },
                                onToggleFavorite = { onToggleFavorite(game.id) },
                                modifier = Modifier.width(124.dp),
                                showMetadataBelow = false
                            )
                        }
                    }
                }

                // Quick Import ROM Banner
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onImportRomClick)
                            .testTag("home_import_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = strings.importRom,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.importRom,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "ملفات متعددة أو مجلد كامل • a26 • bin • a78 • zip"
                                        } else {
                                            "Multiple files or full folder • a26 • bin • a78 • zip"
                                        },
                                        fontFamily = ChakraPetchFontFamily,
                                        fontSize = 12.sp,
                                        color = AtariTextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "+",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AtariAmber
                            )
                        }
                    }
                }

                // Official Telegram Channels Card
                item {
                    TelegramChannelsCard(isArabic = isArabic)
                }
            }
        }
    }
}

@Composable
private fun ContinuePlayingHeroCard(
    game: GameEntity,
    continueLabel: String,
    playLabel: String,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AtariOrange, Color(0xFFE34812))
                )
            )
            .clickable(onClick = onPlay)
            .padding(18.dp)
            .testTag("continue_playing_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = continueLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${game.title} — ${formatPlayDuration(game.playTimeSeconds)}",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AtariFourStripes(
                    modifier = Modifier.width(74.dp),
                    stripeHeight = 4f
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AtariSystem.fromCode(game.system).displayName,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AtariOrange
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("hero_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = playLabel,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = playLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = AtariOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRowHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSeeAll),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AtariAmber,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "‹",
            fontSize = 20.sp,
            color = AtariTextSecondary
        )
    }
}
