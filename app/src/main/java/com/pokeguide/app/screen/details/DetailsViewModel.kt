package com.pokeguide.app.screen.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeguide.app.model.PokemonDetails
import com.pokeguide.app.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояния экрана деталей покемона */
sealed interface ProfileState {
    data object Loading : ProfileState
    data class Ready(val pokemon: PokemonDetails) : ProfileState
    data class Failed(val reason: String) : ProfileState
}

/**
 * ViewModel для экрана деталей.
 * Получает pokemonId из навигации и грузит данные по нему.
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val repo: PokemonRepository
) : ViewModel() {

    private val pokemonId: Int = saved["pokemonId"] ?: 0

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    init {
        load()
    }

    /** Загрузить данные покемона по ID */
    fun load() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            repo.getById(pokemonId)
                .onSuccess { _state.value = ProfileState.Ready(it) }
                .onFailure { _state.value = ProfileState.Failed(it.localizedMessage ?: "Ошибка") }
        }
    }
}
