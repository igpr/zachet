package com.pokeguide.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pokeguide.app.screen.details.DetailsScreen
import com.pokeguide.app.screen.explore.ExploreScreen

/** Маршруты навигации */
object Destination {
    const val EXPLORE = "explore"
    const val DETAILS = "details/{pokemonId}"

    fun details(id: Int): String = "details/$id"
}

/** Граф навигации приложения: каталог → детали */
@Composable
fun AppRouter(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destination.EXPLORE) {

        composable(Destination.EXPLORE) {
            ExploreScreen(
                onPokemonClick = { id -> navController.navigate(Destination.details(id)) }
            )
        }

        composable(
            route = Destination.DETAILS,
            arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
        ) {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
    }
}
