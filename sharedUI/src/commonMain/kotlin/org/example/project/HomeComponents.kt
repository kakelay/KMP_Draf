package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DecorativePattern() {
    Canvas(Modifier.fillMaxWidth().height(900.dp)) {
        val path = Path()
        for (row in 0..18) for (column in 0..9) {
            val x = column * 58f + if (row % 2 == 0) 0f else 28f
            val y = row * 52f
            path.reset()
            path.moveTo(x, y + 8f)
            path.quadraticTo(x + 8f, y, x + 16f, y + 8f)
            path.quadraticTo(x + 8f, y + 16f, x, y + 8f)
            drawPath(path, Color(0x14FFB6A0), style = Stroke(1.5f))
        }
    }
}

@Composable
internal fun HomeHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF4D291B)).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Hello, Kak Elay!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("View Profile  ›", color = Color(0xFFFFD7DD), fontSize = 12.sp)
        }
        IconButton("💬", "Messages")
        IconButton("♧", "Notifications")
        IconButton("▣", "Menu")
    }
}

@Composable
internal fun HomeGreeting() {
    Text(
        "Happy Chinese New Year, Sakirin!",
        modifier = Modifier.padding(top = 2.dp),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
}

@Composable
internal fun BalanceCard() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(HomeColors.deepCrimson)
            .border(7.dp, Color(0x66FF9C91), RoundedCornerShape(24.dp)).padding(16.dp)
    ) {
        Column {
            Text("Total Balance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$1,680.68", color = HomeColors.gold, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                Spacer(Modifier.weight(1f))
                Text("龍", color = HomeColors.gold, fontSize = 84.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Default",
                    modifier = Modifier.background(Color.White, RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = HomeColors.deepCrimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(9.dp))
                Text("Personal Expenses", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                MoneyAction("↙", "Receive Money")
                MoneyAction("↗", "Send Money")
            }
        }
    }
}

@Composable
private fun MoneyAction(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).background(HomeColors.gold, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                icon,
                color = HomeColors.deepCrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
internal fun QuickActions(actions: List<QuickAction>, onAction: (QuickAction) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0x33FFFFFF)).padding(8.dp)) {
        actions.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                row.forEach { action ->
                    if (action.title == "ABA Scan") {
                        ABAScanButton(action, Modifier.weight(1f), onAction)
                    } else {
                        QuickActionCard(action, Modifier.weight(1f), onAction)
                    }
                }
            }
            if (row != actions.chunked(3).last()) Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
internal fun ShortcutRow() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 42.dp)) {
        items(listOf("🏛  Government Services", "▣  Payment", "◉  More")) { label ->
            Box(
                Modifier.width(174.dp).height(48.dp).clip(RoundedCornerShape(24.dp))
                    .background(Color(0xD9FFF7F5)).clickable { }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text(label, color = HomeColors.deepCrimson, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

@Composable
internal fun ABAScanButton(action: QuickAction, modifier: Modifier, onClick: (QuickAction) -> Unit) {
    QuickActionCard(action, modifier, onClick)
}

@Composable
private fun QuickActionCard(action: QuickAction, modifier: Modifier, onAction: (QuickAction) -> Unit) {
    Column(
        modifier.aspectRatio(0.92f).clip(RoundedCornerShape(18.dp)).background(HomeColors.pale)
            .clickable { onAction(action) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(action.icon, color = HomeColors.deepCrimson, fontSize = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            action.title,
            color = HomeColors.deepCrimson,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun PromoBanner() {
    Column {
        SectionHeader("News & Promotions")
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF6D1AC7))
                .border(7.dp, Color(0x66FF9C91), RoundedCornerShape(22.dp)).padding(14.dp)
        ) {
            Column {
                Text("CARDS OFFER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("12%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 54.sp)
                Text("ON YOUR STAYS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
            Text("塔", modifier = Modifier.align(Alignment.CenterEnd), color = Color(0xFFFFD5C4), fontSize = 72.sp)
        }
    }
}

@Composable
internal fun HomeSection(title: String, action: String? = null, content: @Composable () -> Unit) {
    Column { SectionHeader(title, action); Spacer(Modifier.height(10.dp)); content() }
}

@Composable
internal fun SectionHeader(title: String, action: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (action != null) Text("$action  ›", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
internal fun FavoritesRow(items: List<Favorite>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 42.dp)) {
        items(
            items
        ) { FavoriteCard(it) }
    }
}

@Composable
private fun FavoriteCard(favorite: Favorite) {
    Column(
        Modifier.width(122.dp).height(122.dp).clip(RoundedCornerShape(20.dp)).background(HomeColors.panel)
            .padding(12.dp), verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.size(42.dp).background(Color(0xFFB66E7C), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(favorite.initials, color = Color.White, fontWeight = FontWeight.Bold) }
        Text(favorite.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
internal fun ServicesRow(items: List<Service>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 42.dp)) {
        items(
            items
        ) { ServiceCard(it) }
    }
}

@Composable
private fun ServiceCard(service: Service) {
    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(82.dp).clip(RoundedCornerShape(17.dp)).background(Color(service.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                service.icon,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(service.title, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
internal fun GovernmentServicesRow(items: List<GovernmentService>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 42.dp)) {
        items(
            items
        ) { GovernmentServiceCard(it) }
    }
}

@Composable
private fun GovernmentServiceCard(service: GovernmentService) {
    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(82.dp).clip(RoundedCornerShape(17.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) { Text(service.icon, color = Color(0xFF254D9D), fontSize = 38.sp) }
        Spacer(Modifier.height(6.dp))
        Text(service.title, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
internal fun DiscoveriesRow(items: List<Discovery>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 42.dp)) {
        items(
            items
        ) { DiscoveryCard(it) }
    }
}

@Composable
private fun DiscoveryCard(discovery: Discovery) {
    Box(
        Modifier.width(122.dp).height(146.dp).clip(RoundedCornerShape(19.dp)).background(Color(discovery.color))
            .border(2.dp, Color(0xAAFF9C91), RoundedCornerShape(19.dp)).padding(10.dp)
    ) {
        Text(discovery.icon, modifier = Modifier.align(Alignment.TopCenter), color = Color.White, fontSize = 52.sp)
        Text(
            discovery.title,
            modifier = Modifier.align(Alignment.BottomStart),
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
internal fun IconButton(icon: String, label: String) {
    Box(
        Modifier.size(38.dp).semantics { contentDescription = label }.clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon,
            color = Color.White,
            fontSize = 24.sp
        )
    }
}

@Composable
internal fun EditHomeButton() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = { },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HomeColors.deepCrimson)
        ) { Text("Edit Home", fontWeight = FontWeight.Bold) }
    }
}
