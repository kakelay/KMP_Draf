package org.example.project.parking

import org.example.project.parking.domain.*
import org.example.project.parking.presentation.ParkingViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ParkingViewModelTest {
    @Test fun automaticPaginationWaitsForRequestAndStopsAfterFailureUntilRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<Int>()
            val pending = kotlinx.coroutines.CompletableDeferred<ParkingPage>()
            val repository = object : ParkingRepository {
                override suspend fun search(search: ParkingSearch): ParkingPage {
                    requests += search.page
                    return when (requests.size) {
                        1 -> ParkingPage(emptyList(), 40, 0, 20)
                        2 -> pending.await()
                        else -> ParkingPage(emptyList(), 40, 1, 20)
                    }
                }
                override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = null
            }
            val model = ParkingViewModel(repository)
            runCurrent()
            model.loadMore()
            model.loadMore()
            runCurrent()
            assertEquals(listOf(0, 1), requests)
            pending.completeExceptionally(ParkingException(ParkingFailureReason.CONNECTION_FAILED))
            runCurrent()
            model.loadMore()
            runCurrent()
            assertEquals(listOf(0, 1), requests)
            model.retry()
            runCurrent()
            assertEquals(listOf(0, 1, 1), requests)
            assertFalse(model.state.value.hasNext)
            model.loadMore()
            runCurrent()
            assertEquals(3, requests.size)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun refreshKeepsCardsThenReplacesThemAndRetriesFirstPageAfterFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<Int>()
            val old = ParkingLocation("old", "Old", "Street", 11.5, 104.9, "1.25", 8, false)
            val fresh = old.copy(id = "fresh", name = "Fresh")
            val response = kotlinx.coroutines.CompletableDeferred<ParkingPage>()
            val repository = object : ParkingRepository {
                override suspend fun search(search: ParkingSearch): ParkingPage {
                    requests += search.page
                    return when (requests.size) {
                        1 -> ParkingPage(listOf(old), 40, 0, 20)
                        2 -> ParkingPage(listOf(old.copy(id = "page2")), 40, 1, 20)
                        3 -> response.await()
                        else -> ParkingPage(listOf(fresh), 1, 0, 20)
                    }
                }
                override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = null
            }
            val model = ParkingViewModel(repository)
            runCurrent()
            model.loadMore()
            runCurrent()
            model.refresh()
            runCurrent()
            assertTrue(model.state.value.refreshing)
            assertEquals(2, model.state.value.locations.size)
            model.refresh()
            assertEquals(3, requests.size)
            response.completeExceptionally(ParkingException(ParkingFailureReason.CONNECTION_FAILED))
            runCurrent()
            assertFalse(model.state.value.refreshing)
            assertEquals(2, model.state.value.locations.size)
            model.retry()
            runCurrent()
            assertEquals(listOf(0, 1, 0, 0), requests)
            assertEquals(listOf(fresh), model.state.value.locations)
            model.refresh()
            runCurrent()
            assertFalse(model.state.value.refreshing)
            assertEquals(listOf(fresh), model.state.value.locations)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedFavoriteDoesNotChangeConfirmedServerState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = object : ParkingRepository {
                override suspend fun search(search: ParkingSearch) = ParkingPage(
                    listOf(ParkingLocation("p1", "Mall", "Street", 11.5, 104.9, "1.25", 8, false)), 1, 0, 20)
                override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = throw ParkingException(ParkingFailureReason.SIGN_IN_REQUIRED)
            }
            val model = ParkingViewModel(repository)
            advanceUntilIdle()
            model.toggleFavorite("p1")
            advanceUntilIdle()
            assertFalse(model.state.value.locations.single().isFavorite)
            assertNull(model.state.value.favoritePending)
            assertTrue(model.state.value.error!!.contains("Sign in"))
        } finally { Dispatchers.resetMain() }
    }

    @Test fun paginationUsesSubmittedSearchUntilAutoSearchRuns() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<ParkingSearch>()
            val repository = object : ParkingRepository {
                override suspend fun search(search: ParkingSearch): ParkingPage {
                    requests += search
                    return ParkingPage(emptyList(), 40, search.page, 20)
                }
                override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = null
            }
            val model = ParkingViewModel(repository)
            advanceUntilIdle()
            model.editQuery("mall")
            model.search()
            advanceUntilIdle()
            model.editQuery("unsubmitted draft")
            model.loadMore()
            runCurrent()
            assertEquals("mall", requests.last().query)
            assertEquals(1, requests.last().page)
            advanceUntilIdle()
            assertEquals("unsubmitted draft", requests.last().query)
            assertEquals(0, requests.last().page)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun typingSearchesOnlyAfterPauseAndUsesLatestQuery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<ParkingSearch>()
            val model = ParkingViewModel(recordingRepository(requests))
            runCurrent()
            model.editQuery("ma")
            advanceTimeBy(250)
            model.editQuery("mall")
            advanceTimeBy(399)
            runCurrent()
            assertEquals(1, requests.size)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("", "mall"), requests.map { it.query })
            model.editQuery("")
            advanceUntilIdle()
            assertEquals("", requests.last().query)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun categoryAndManualSearchCancelPendingTypingSearch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<ParkingSearch>()
            val model = ParkingViewModel(recordingRepository(requests))
            runCurrent()
            model.editQuery("mall")
            model.editCategory("mall")
            runCurrent()
            assertEquals(2, requests.size)
            assertEquals("mall", requests.last().category)
            advanceUntilIdle()
            assertEquals(2, requests.size)
            model.editQuery("office")
            model.search()
            advanceUntilIdle()
            assertEquals(3, requests.size)
            assertEquals("office", requests.last().query)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun autoSearchWaitsForFavoriteWrite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val saved = kotlinx.coroutines.CompletableDeferred<ParkingLocation?>()
            val requests = mutableListOf<ParkingSearch>()
            val location = ParkingLocation("p1", "Mall", "Street", 11.5, 104.9, "1.25", 8, false)
            val repository = object : ParkingRepository {
                override suspend fun search(search: ParkingSearch): ParkingPage {
                    requests += search
                    return ParkingPage(listOf(location), 1, 0, 20)
                }
                override suspend fun setFavorite(id: String, favorite: Boolean) = saved.await()
            }
            val model = ParkingViewModel(repository)
            runCurrent()
            model.toggleFavorite("p1")
            runCurrent()
            model.editQuery("new place")
            advanceTimeBy(400)
            runCurrent()
            assertEquals(1, requests.size)
            saved.complete(location.copy(isFavorite = true))
            runCurrent()
            assertEquals(2, requests.size)
            assertEquals("new place", requests.last().query)
        } finally { Dispatchers.resetMain() }
    }

    private fun recordingRepository(requests: MutableList<ParkingSearch>) = object : ParkingRepository {
        override suspend fun search(search: ParkingSearch): ParkingPage {
            requests += search
            return ParkingPage(emptyList(), 0, search.page, search.size)
        }
        override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = null
    }

}
