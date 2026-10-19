package org.example.project.parking.presentation

import org.example.project.parking.domain.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



class ParkingViewModel(private val repository: ParkingRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ParkingUiState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var autoSearchJob: Job? = null
    private var searchAfterFavorite = false
    private var loadedPage = -1
    private var retryPage = 0
    private var activeSearch = ParkingSearch()
    private var generation = 0

    init { search() }
    fun editQuery(value: String) {
        val query = value.take(160)
        if (query == state.value.query) return
        mutableState.update { it.copy(query = query) }
        autoSearchJob?.cancel()
        autoSearchJob = viewModelScope.launch {
            delay(400)
            autoSearchJob = null
            search()
        }
    }

    fun editCategory(value: String) {
        if (value == state.value.category) return
        mutableState.update { it.copy(category = value) }
        search()
    }

    fun search() {
        autoSearchJob?.cancel()
        autoSearchJob = null
        if (state.value.favoritePending != null) {
            searchAfterFavorite = true
            return
        }
        searchAfterFavorite = false
        val snapshot = state.value
        activeSearch = ParkingSearch(query = snapshot.query, category = snapshot.category)
        loadedPage = -1
        load(0)
    }
    fun loadMore() {
        if (!state.value.loading && state.value.favoritePending == null && state.value.hasNext && state.value.error == null) load(loadedPage + 1)
    }
    fun refresh() {
        if (state.value.loading || state.value.favoritePending != null) return
        autoSearchJob?.cancel()
        autoSearchJob = null
        activeSearch = ParkingSearch(query = state.value.query, category = state.value.category)
        load(0, refreshing = true)
    }
    fun retry() {
        if (!state.value.loading && state.value.favoritePending == null) load(retryPage)
    }
    private fun load(page: Int, refreshing: Boolean = false) {
        retryPage = page
        searchJob?.cancel()
        val requestGeneration = ++generation
        mutableState.update {
            it.copy(loading = true, refreshing = refreshing, error = null, locations = if (page == 0 && !refreshing) emptyList() else it.locations,
                hasNext = if (page == 0) false else it.hasNext)
        }
        searchJob = viewModelScope.launch {
            try {
                val result = repository.search(activeSearch.copy(page = page))
                if (requestGeneration != generation) return@launch
                loadedPage = result.page
                mutableState.update {
                    it.copy(loading = false, refreshing = false, locations = ((if (page == 0) emptyList() else it.locations) + result.locations).distinctBy { item -> item.id },
                        hasNext = result.hasNext)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (requestGeneration == generation) mutableState.update { it.copy(loading = false, refreshing = false, error = error.userMessage()) }
            }
        }
    }
    // Serialize writes against list requests so an older GET cannot overwrite a saved favorite.
    // Update only after the backend confirms success; failures leave the existing value intact.
    fun toggleFavorite(id: String) {
        if (state.value.loading || state.value.favoritePending != null) return
        val location = state.value.locations.find { it.id == id } ?: return
        mutableState.update { it.copy(favoritePending = id, error = null) }
        viewModelScope.launch {
            try {
                val saved = repository.setFavorite(id, !location.isFavorite)
                mutableState.update { current -> current.copy(locations = current.locations.map {
                    if (it.id == id) saved ?: it.copy(isFavorite = false) else it
                }) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = error.userMessage()) } }
            finally {
                mutableState.update { it.copy(favoritePending = null) }
                if (searchAfterFavorite) search()
            }
        }
    }
}
private fun Exception.userMessage(): String {
    val failure = this as? ParkingException ?: return "Could not load parking. Please retry."
    return when (failure.reason) {
        ParkingFailureReason.SIGN_IN_REQUIRED -> "Sign in with Firebase to save favorites."
        ParkingFailureReason.SESSION_EXPIRED -> "Your session expired. Please sign in again."
        ParkingFailureReason.ACCESS_DENIED -> "You do not have access to this action."
        ParkingFailureReason.REQUEST_FAILED -> "Request failed. Please retry." +
            (failure.referenceNumber?.let { " Reference: $it" } ?: "")
        ParkingFailureReason.CONNECTION_FAILED -> "Could not load parking. Check your connection and retry."
    }
}
