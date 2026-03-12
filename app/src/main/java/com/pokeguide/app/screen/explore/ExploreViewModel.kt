package com.pokeguide.app.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeguide.app.model.PokemonSummary
import com.pokeguide.app.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ID популярных покемонов для кнопки «Популярные» в пустом состоянии
private val POPULAR_IDS = setOf(1, 6, 9, 25, 37, 39)

/** Состояния экрана каталога */
sealed interface ExploreState {
    data object Loading : ExploreState
    data class Content(
        val pokemon: List<PokemonSummary>,
        val searchText: String
    ) : ExploreState
    data class Failed(val reason: String) : ExploreState
}

/**
 * ViewModel каталога покемонов.
 * Загружает список, фильтрует по имени, поддерживает MOD_A3 (пустое состояние).
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: PokemonRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ExploreState>(ExploreState.Loading)
    val state: StateFlow<ExploreState> = _state

    // Полный список покемонов из API (кэш на время жизни VM)
    private var catalogue = emptyList<PokemonSummary>()
    private var currentQuery = ""

    init {
        loadCatalogue()
    }

    /** Загрузить каталог покемонов с сервера */
    fun loadCatalogue() {
        viewModelScope.launch {
            _state.value = ExploreState.Loading
            repo.getAll()
                .onSuccess { list ->
                    catalogue = list
                    currentQuery = ""
                    _state.value = ExploreState.Content(list, "")
                }
                .onFailure { err ->
                    _state.value = ExploreState.Failed(err.localizedMessage ?: "Неизвестная ошибка")
                }
        }
    }

    /** Фильтровать по имени при вводе в поиск */
    fun onSearchChanged(query: String) {
        currentQuery = query
        applyFilter()
    }

    /** MOD_A3: показать только популярных покемонов */
    fun showPopular() {
        currentQuery = ""
        val popular = catalogue.filter { it.id in POPULAR_IDS }
        _state.value = ExploreState.Content(popular, "")
    }

    /** Сбросить поиск и показать весь каталог */
    fun resetSearch() {
        currentQuery = ""
        _state.value = ExploreState.Content(catalogue, "")
    }

    /** Локальная фильтрация по текущему запросу */
    private fun applyFilter() {
        val visible = if (currentQuery.isBlank()) {
            catalogue
        } else {
            catalogue.filter { it.name.contains(currentQuery, ignoreCase = true) }
        }
        _state.value = ExploreState.Content(visible, currentQuery)
    }
}
