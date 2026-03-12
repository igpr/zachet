package com.pokeguide.app.repository

import com.pokeguide.app.api.PokeService
import com.pokeguide.app.api.PokemonEntry
import com.pokeguide.app.api.PokemonResponse
import com.pokeguide.app.model.Ability
import com.pokeguide.app.model.PokemonDetails
import com.pokeguide.app.model.PokemonSummary
import com.pokeguide.app.model.StatValue
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

// Шаблон URL для official-artwork по ID покемона
private const val ARTWORK_TEMPLATE =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/%d.png"

/**
 * Реализация репозитория.
 * Маппит DTO из API в доменные модели, не пропуская сырые данные в UI.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeService
) : PokemonRepository {

    override suspend fun getAll(): Result<List<PokemonSummary>> = safeCall {
        api.fetchList().entries.map { it.toSummary() }
    }

    override suspend fun getById(id: Int): Result<PokemonDetails> = safeCall {
        api.fetchById(id).toDetails()
    }

    /** Конвертация записи списка в доменную модель */
    private fun PokemonEntry.toSummary(): PokemonSummary {
        val pokemonId = url.trimEnd('/').substringAfterLast('/').toInt()
        return PokemonSummary(
            id = pokemonId,
            name = name.replaceFirstChar { it.uppercaseChar() },
            imageUrl = ARTWORK_TEMPLATE.format(pokemonId)
        )
    }

    /** Конвертация полного ответа API в доменную модель деталей */
    private fun PokemonResponse.toDetails(): PokemonDetails = PokemonDetails(
        id = id,
        name = name.replaceFirstChar { it.uppercaseChar() },
        imageUrl = sprites.other?.officialArtwork?.frontDefault
            ?: sprites.frontDefault
            ?: ARTWORK_TEMPLATE.format(id),
        heightDm = height,
        weightHg = weight,
        baseXp = baseExperience ?: 0,
        types = types.sortedBy { it.slot }.map { slot ->
            slot.type.name.replaceFirstChar { it.uppercaseChar() }
        },
        stats = stats.map { s ->
            StatValue(
                label = formatStatName(s.stat.name),
                base = s.baseStat
            )
        },
        abilities = abilities.map { a ->
            Ability(
                name = a.ability.name.replace('-', ' ')
                    .split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } },
                hidden = a.isHidden
            )
        }
    )

    /** Красивые названия характеристик вместо raw-строк из API */
    private fun formatStatName(raw: String): String = when (raw) {
        "hp" -> "HP"
        "attack" -> "Атака"
        "defense" -> "Защита"
        "special-attack" -> "Сп. Атк"
        "special-defense" -> "Сп. Защ"
        "speed" -> "Скорость"
        else -> raw.replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Безопасный вызов API с обработкой ошибок.
     * Пробрасывает CancellationException чтобы не ломать структурный concurrency.
     */
    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (ex: Exception) {
        Result.failure(ex)
    }
}
