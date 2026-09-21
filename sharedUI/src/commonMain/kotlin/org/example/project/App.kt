package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        var scannerVisible by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeColors.crimson)
                // .safeContentPadding()
                // .fillMaxWidth()
                // .padding(top = 24.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                .padding(
                    horizontal = 16.dp,
                    vertical = 34.dp
                )


        ) {
            DecorativePattern()
            if (scannerVisible) {
                ABAScanner(onClose = { scannerVisible = false })
            } else LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
//                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item { HomeHeader() }
                item { HomeGreeting() }
                item { BalanceCard() }
                item { QuickActions(homeQuickActions) { scannerVisible = true } }
                item { ShortcutRow() }
                item { PromoBanner() }
                item { HomeSection("Favorites", "VIEW PROFILE") { FavoritesRow(favorites) } }
                item { HomeSection("Explore Services", "VIEW PROFILE") { ServicesRow(exploreServices) } }
                item {
                    HomeSection(
                        "Government Services",
                        "VIEW PROFILE"
                    ) { GovernmentServicesRow(governmentServices) }
                }
                item { HomeSection("Discoveries") { DiscoveriesRow(discoveries) } }
                item { EditHomeButton() }
            }
        }
    }
}

internal object HomeColors {
    val crimson = Color(0xFFD80A3B)
    val deepCrimson = Color(0xFFA80632)
    val panel = Color(0xB8340828)
    val pale = Color(0xFFFFF7F5)
    val gold = Color(0xFFFFE13B)

}

 