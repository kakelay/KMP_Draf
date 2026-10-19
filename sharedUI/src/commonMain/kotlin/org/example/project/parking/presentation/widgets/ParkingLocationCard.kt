package org.example.project.parking.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.parking.domain.ParkingLocation

@Composable
internal fun ParkingLocationCard(
    location: ParkingLocation,
    savingFavorite: Boolean,
    favoriteEnabled: Boolean,
    onFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ParkingAvailabilityBadge(availableSpaces = location.availableSpaces)
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = location.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = ParkingBackground)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = location.pricePerHour,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "per hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FavoriteButton(
                    isFavorite = location.isFavorite,
                    saving = savingFavorite,
                    enabled = favoriteEnabled,
                    onClick = onFavorite,
                )
            }
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    saving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = when {
        saving -> "Saving..."
        isFavorite -> "Remove favorite"
        else -> "Save favorite"
    }

    OutlinedButton(onClick = onClick, enabled = enabled) {
        Text(label)
    }
}

@Composable
private fun ParkingAvailabilityBadge(availableSpaces: Int) {
    val hasSpaces = availableSpaces > 0
    val background = if (hasSpaces) Color(0xFFE1F4E9) else Color(0xFFF0F1F2)
    val textColor = if (hasSpaces) AppColor else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (hasSpaces) "$availableSpaces spaces available" else "Currently full"

    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
