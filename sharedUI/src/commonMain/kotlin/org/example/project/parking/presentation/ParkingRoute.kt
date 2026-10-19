package org.example.project.parking.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.parking.domain.ParkingRepository

/** Connects the screen to its ViewModel. Recreate after the Firebase user changes. */
@Composable
fun ParkingRoute(repository: ParkingRepository) {
    val model = viewModel { ParkingViewModel(repository) }
    val state by model.state.collectAsStateWithLifecycle()

    ParkingScreen(
        state = state,
        onQuery = model::editQuery,
        onCategory = model::editCategory,
        onSearch = model::search,
        onRetry = model::retry,
        onLoadMore = model::loadMore,
        onFavorite = model::toggleFavorite,
        onRefresh = model::refresh,
    )
}
