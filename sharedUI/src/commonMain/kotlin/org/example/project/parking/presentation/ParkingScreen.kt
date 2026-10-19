package org.example.project.parking.presentation

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.parking.presentation.widgets.ParkingBackground
import org.example.project.parking.presentation.widgets.ParkingEmptyState
import org.example.project.parking.presentation.widgets.ParkingErrorCard
import org.example.project.parking.presentation.widgets.ParkingHeader
import org.example.project.parking.presentation.widgets.ParkingLoadingIndicator
import org.example.project.parking.presentation.widgets.ParkingLocationCard
import org.example.project.parking.presentation.widgets.ParkingResultsHeader
import org.example.project.parking.presentation.widgets.ParkingSearchForm
import org.example.project.parking.presentation.widgets.ParkingWelcomeCard
import org.example.project.parking.presentation.widgets.ParkingShimmerCard

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun ParkingScreen(
    state: ParkingUiState,
    onQuery: (String) -> Unit,
    onCategory: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFavorite: (String) -> Unit,
    onRefresh: () -> Unit = onSearch,
) {
    val actionsEnabled = !state.loading && state.favoritePending == null
    val listState = rememberLazyListState()
    val currentState by rememberUpdatedState(state)
    val loadMore by rememberUpdatedState(onLoadMore)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState) {
        snapshotFlow {
            val current = currentState
            val lastId = current.locations.lastOrNull()?.id
            lastId != null && current.hasNext && !current.loading &&
                current.favoritePending == null && current.error == null &&
                listState.layoutInfo.visibleItemsInfo.any { it.key == lastId }
        }.collect { reachedEnd ->
            if (reachedEnd) loadMore()
        }
    }

    LaunchedEffect(listState, keyboard, focusManager) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                keyboard?.hide()
                focusManager.clearFocus()
            }
        }
    }

    Surface(color = ParkingBackground, modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { if (actionsEnabled) onRefresh() },
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stickyHeader(key = "parking_header") {
                Surface(color = ParkingBackground) {
                    Box(Modifier.padding(vertical = 8.dp)) {
                        ParkingHeader()
                    }
                }
            }
            item { ParkingWelcomeCard() }
            item {
                ParkingSearchForm(
                    query = state.query,
                    category = state.category,
                    loading = state.loading,
                    searchEnabled = state.favoritePending == null,
                    onQuery = onQuery,
                    onCategory = onCategory,
                    onSearch = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        onSearch()
                    },
                )
            }
            if (state.locations.isNotEmpty() || (!state.loading && state.error == null)) {
                item { ParkingResultsHeader(count = state.locations.size) }
            }
            state.error?.let { message ->
                item {
                    ParkingErrorCard(
                        message = message,
                        retryEnabled = actionsEnabled,
                        onRetry = onRetry,
                    )
                }
            }
            if (!state.loading && state.error == null && state.locations.isEmpty()) {
                item { ParkingEmptyState() }
            }
            items(state.locations, key = { it.id }) { location ->
                ParkingLocationCard(
                    location = location,
                    savingFavorite = state.favoritePending == location.id,
                    favoriteEnabled = actionsEnabled,
                    onFavorite = { onFavorite(location.id) },
                )
            }
            if (state.loading && !state.refreshing && state.locations.isEmpty()) {
                items(3) { ParkingShimmerCard() }
            } else if (state.loading && !state.refreshing) {
                item { ParkingLoadingIndicator() }
            }
        }
        }
    }
}
