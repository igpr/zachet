package com.pokeguide.app.repository

import com.pokeguide.app.model.PokemonDetails
import com.pokeguide.app.model.PokemonSummary

/** Интерфейс репозитория — абстрагирует источник данных от UI */
interface PokemonRepository {
    suspend fun getAll(): Result<List<PokemonSummary>>
    suspend fun getById(id: Int): Result<PokemonDetails>
}
