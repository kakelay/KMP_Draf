package org.example.project.parking.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private val ParkingCategories = listOf(
    "all" to "All places",
    "mall" to "Malls",
    "office" to "Offices",
    "hotel" to "Hotels",
    "restaurant" to "Restaurants",
    "hospital" to "Hospitals",
)

@Composable
internal fun ParkingSearchForm(
    query: String,
    category: String,
    loading: Boolean,
    searchEnabled: Boolean,
    onQuery: (String) -> Unit,
    onCategory: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search parking by name or place") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColor,
                focusedLabelColor = AppColor,
                cursorColor = AppColor,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),

        )
        ParkingCategoryFilters(
            selectedCategory = category,
            onCategory = onCategory,
        )
        Button(
            onClick = onSearch,
            enabled = searchEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColor),
        ) {
            Text(if (loading) "Search again" else "Find parking")
        }
    }
}

@Composable
private fun ParkingCategoryFilters(selectedCategory: String, onCategory: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ParkingCategories) { (value, label) ->
            FilterChip(
                selected = selectedCategory == value,
                onClick = { onCategory(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColor,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}
