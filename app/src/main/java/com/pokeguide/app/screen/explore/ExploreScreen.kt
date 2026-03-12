package com.pokeguide.app.screen.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokeguide.app.model.PokemonSummary
import com.pokeguide.app.screen.components.EmptyView
import com.pokeguide.app.screen.components.ErrorOverlay
import com.pokeguide.app.screen.components.LoadingOverlay
import java.util.Locale

private const val VARIANT_CODE = "POKE-POKEMON-MOD_A3_EMPTY_STATE_ACTIONS"
private const val GRID_COLUMNS = 2
private const val MAX_RANDOM_ID = 898

/**
 * Главный экран — каталог покемонов в виде сетки.
 * Сверху поиск, при пустом результате — EmptyView с тремя действиями.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPokemonClick: (Int) -> Unit,
    vm: ExploreViewModel = hiltViewModel()
) {
    val uiState by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PokéGuide", style = MaterialTheme.typography.titleLarge)
                        Text(
                            VARIANT_CODE,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Строка поиска
            SearchField(
                value = query,
                onChange = { q ->
                    query = q
                    vm.onSearchChanged(q)
                },
                onClear = {
                    query = ""
                    vm.resetSearch()
                    keyboard?.hide()
                }
            )

            Box(Modifier.weight(1f)) {
                when (val s = uiState) {
                    is ExploreState.Loading -> LoadingOverlay()
                    is ExploreState.Failed -> ErrorOverlay(s.reason, onRetry = vm::loadCatalogue)
                    is ExploreState.Content -> {
                        if (s.pokemon.isEmpty() && s.searchText.isNotEmpty()) {
                            // MOD_A3: пустое состояние с тремя действиями
                            EmptyView(
                                query = s.searchText,
                                onPopular = {
                                    query = ""
                                    vm.showPopular()
                                },
                                onSurprise = { onPokemonClick((1..MAX_RANDOM_ID).random()) },
                                onReset = {
                                    query = ""
                                    vm.resetSearch()
                                }
                            )
                        } else {
                            PokemonGrid(s.pokemon, onPokemonClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("Найти покемона…") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {}),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/** Сетка покемонов 2×N */
@Composable
private fun PokemonGrid(
    pokemon: List<PokemonSummary>,
    onClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = pokemon, key = { it.id }) { poke ->
            PokemonCell(poke) { onClick(poke.id) }
        }
    }
}

/** Ячейка одного покемона: картинка + имя + номер */
@Composable
private fun PokemonCell(pokemon: PokemonSummary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .aspectRatio(0.85f)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.US, "#%03d", pokemon.id),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 4.dp)
            )
            AsyncImage(
                model = pokemon.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier
                    .size(96.dp)
                    .weight(1f),
                contentScale = ContentScale.Fit
            )
            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
