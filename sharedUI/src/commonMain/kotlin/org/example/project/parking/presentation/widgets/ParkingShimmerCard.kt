package org.example.project.parking.presentation.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp

@Composable
internal fun ParkingShimmerCard() {
    val transition = rememberInfiniteTransition(label = "Parking shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "Shimmer position",
    )
    val placeholder = Modifier.clip(RoundedCornerShape(8.dp))
        .background(Color(0xFFE8ECEA))
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                val start = size.width * progress
                drawRect(Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent),
                    start = Offset(start, 0f),
                    end = Offset(start + size.width, size.height),
                ))
            }
        }
    Card(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "Loading parking locations"
            liveRegion = LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(140.dp).height(24.dp).then(placeholder))
            Box(Modifier.fillMaxWidth(0.7f).height(22.dp).then(placeholder))
            Box(Modifier.fillMaxWidth(0.9f).height(16.dp).then(placeholder))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.width(80.dp).height(42.dp).then(placeholder))
                Box(Modifier.width(140.dp).height(42.dp).then(placeholder))
            }
        }
    }
}
