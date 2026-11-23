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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.ui.LibrarySortMode
import com.example.ui.components.AtariGameCoverCard
import com.example.ui.components.AtariHeader
import com.example.ui.components.AtariSearchBar
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.LocalAppStrings

@Composable
fun LibraryScreen(
    games: List<GameEntity>,
    totalCount: Int,
    searchQuery: String,
    selectedSystem: String,
    selectedGenre: String,
    selectedYear: Int,
    sortMode: LibrarySortMode,
    genres: List<String>,
    years: List<Int>,
    onSearchQueryChange: (String) -> Unit,
    onSystemSelect: (String) -> Unit,
    onGenreSelect: (String) -> Unit,
    onYearSelect: (Int) -> Unit,
    onSortChange: (LibrarySortMode) -> Unit,
    onPlayGame: (GameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onImportRomClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showGenreMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImportRomClick,
                containerColor = AtariOrange,
                contentColor = Color.White,
                modifier = Modifier.testTag("library_import_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.importRom)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AtariHeader(title = "${strings.libraryTitle} — $totalCount ${strings.gamesCountSuffix}")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AtariSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = strings.searchPlaceholder,
                    onImportClick = onImportRomClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                // System Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val allSelected = selectedSystem.isEmpty()
                        FilterChip(
                            selected = allSelected,
                            onClick = { onSystemSelect("") },
                            label = { Text(strings.filterAll) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AtariOrange,
                                selectedLabelColor = Color.White,
                                containerColor = AtariSurface,
                                labelColor = AtariTextSecondary
                            ),
                            border = null
                        )
                    }
                    items(AtariSystem.entries) { sys ->
                        val active = selectedSystem == sys.name
                        FilterChip(
                            selected = active,
                            onClick = { onSystemSelect(if (active) "" else sys.name) },
                            label = { Text(sys.displayName) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AtariOrange,
                                selectedLabelColor = Color.White,
                                containerColor = AtariSurface,
                                labelColor = AtariTextSecondary
                            ),
                            border = null
                        )
                    }
                }

                // Genre / Year / Sort Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box {
                            FilterChip(
                                selected = selectedGenre.isNotEmpty(),
                                onClick = { showGenreMenu = true },
                                leadingIcon = {
                                    Icon(Icons.Default.FilterList, contentDescription = null, tint = AtariAmber)
                                },
                                label = { Text(selectedGenre.ifEmpty { strings.allGenres }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtariAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = AtariAmber,
                                    containerColor = AtariSurface,
                                    labelColor = AtariTextSecondary
                                ),
                                border = null
                            )
                            DropdownMenu(expanded = showGenreMenu, onDismissRequest = { showGenreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.allGenres) },
                                    onClick = { onGenreSelect(""); showGenreMenu = false }
                                )
                                genres.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = { onGenreSelect(g); showGenreMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box {
                            FilterChip(
                                selected = selectedYear != 0,
                                onClick = { showYearMenu = true },
                                label = { Text(if (selectedYear == 0) strings.allYears else selectedYear.toString()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtariAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = AtariAmber,
                                    containerColor = AtariSurface,
                                    labelColor = AtariTextSecondary
                                ),
                                border = null
                            )
                            DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.allYears) },
                                    onClick = { onYearSelect(0); showYearMenu = false }
                                )
                                years.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = { onYearSelect(y); showYearMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    val sorts = listOf(
                        LibrarySortMode.MOST_PLAYED to strings.sortMostPlayed,
                        LibrarySortMode.TITLE to strings.sortTitle,
                        LibrarySortMode.NEWEST to strings.sortNewest
                    )
                    items(sorts) { (mode, label) ->
                        val active = sortMode == mode
                        FilterChip(
                            selected = active,
                            onClick = { onSortChange(mode) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = AtariSurface,
                                labelColor = AtariTextSecondary
                            ),
                            border = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_grid")
            ) {
                items(games, key = { it.id }) { game ->
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
