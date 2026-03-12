package com.pokeguide.app.screen.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokeguide.app.model.PokemonDetails
import com.pokeguide.app.model.StatValue
import com.pokeguide.app.screen.components.ErrorOverlay
import com.pokeguide.app.screen.components.LoadingOverlay
import com.pokeguide.app.screen.components.TypeChip
import com.pokeguide.app.theme.TypeColor
import java.util.Locale

private const val MAX_STAT = 255f

/**
 * Экран деталей покемона.
 * Показывает арт, типы, характеристики и способности.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    vm: DetailsViewModel = hiltViewModel()
) {
    val uiState by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Подробнее") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = uiState) {
                is ProfileState.Loading -> LoadingOverlay()
                is ProfileState.Failed -> ErrorOverlay(s.reason, onRetry = vm::load)
                is ProfileState.Ready -> ProfileBody(s.pokemon)
            }
        }
    }
}

/** Тело экрана: арт + инфо + статы + способности */
@Composable
private fun ProfileBody(p: PokemonDetails) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ArtworkHeader(p)
        InfoSection(p)
        StatsSection(p.stats, p.types.firstOrNull())
        AbilitiesSection(p.abilities)
        Spacer(Modifier.height(32.dp))
    }
}

/** Шапка с артворком покемона на цветном фоне типа */
@Composable
private fun ArtworkHeader(p: PokemonDetails) {
    val typeColor = TypeColor.of(p.types.firstOrNull() ?: "Normal")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(typeColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = p.imageUrl,
            contentDescription = p.name,
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )
        // Полупрозрачный номер в углу
        Text(
            text = String.format(Locale.US, "#%03d", p.id),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

/** Имя, типы и основные параметры */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoSection(p: PokemonDetails) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(p.name, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            p.types.forEach { TypeChip(it) }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MeasureItem("Рост", String.format(Locale.US, "%.1f м", p.heightDm / 10f))
            MeasureItem("Вес", String.format(Locale.US, "%.1f кг", p.weightHg / 10f))
            MeasureItem("Опыт", p.baseXp.toString())
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}

@Composable
private fun MeasureItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

/** Секция базовых характеристик с полосками прогресса */
@Composable
private fun StatsSection(stats: List<StatValue>, primaryType: String?) {
    val barColor = TypeColor.of(primaryType ?: "Normal")
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Характеристики", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        stats.forEach { stat ->
            StatBar(stat, barColor)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Одна полоска стата: название — значение — прогресс */
@Composable
private fun StatBar(stat: StatValue, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stat.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = stat.base.toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(36.dp)
        )
        LinearProgressIndicator(
            progress = { (stat.base / MAX_STAT).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

/** Список способностей покемона */
@Composable
private fun AbilitiesSection(abilities: List<com.pokeguide.app.model.Ability>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Способности", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        abilities.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("• ", style = MaterialTheme.typography.bodyMedium)
                Text(a.name, style = MaterialTheme.typography.bodyMedium)
                if (a.hidden) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(скрытая)",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
