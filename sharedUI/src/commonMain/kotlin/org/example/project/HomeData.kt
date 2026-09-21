package org.example.project

internal data class QuickAction(val icon: String, val title: String)
internal data class Favorite(val initials: String, val name: String)
internal data class Service(val icon: String, val title: String, val color: Long)
internal data class GovernmentService(val icon: String, val title: String)
internal data class Discovery(val title: String, val icon: String, val color: Long)

internal val homeQuickActions = listOf(
    QuickAction("🧧", "Accounts"), QuickAction("福", "Cards"), QuickAction("福", "Cards"),
    QuickAction("⌘", "ABA Scan"), QuickAction("◈", "Favorites"), QuickAction("◈", "Favorites")
)
internal val favorites = listOf(
    Favorite("MS", "Meas\nSoksey"), Favorite("PV", "Pich Veasna"),
    Favorite("SC", "Sokdavy\nChhin"), Favorite("RT", "Ratha")
)
internal val exploreServices = listOf(
    Service("CBC", "Credit\nBureau", 0xFF007C4D), Service("🎟", "Cinema\nTicket", 0xFFFFD4C8),
    Service("metfone", "Metfone\nServices", 0xFFF21D2A), Service("MET", "VET\nExpress", 0xFFFF7C09),
    Service("▣", "Booking", 0xFF4BA4D7)
)
internal val governmentServices = listOf(
    GovernmentService("◎", "Digital\nPlatform"), GovernmentService("✺", "NSSF Self-\nEmployed"),
    GovernmentService("↗", "PPSHV\nExpressway"), GovernmentService("✺", "Foreigner\nWork Permit"),
    GovernmentService("◎", "Public\nService")
)
internal val discoveries = listOf(
    Discovery("One tap to all\nyour cashbacks", "▦", 0xFF0B2939),
    Discovery("Explore\nServices", "✦", 0xFF124B5B),
    Discovery("Customize\nyour app icon", "◇", 0xFF85D8D5),
    Discovery("Backup your\noptions", "↗", 0xFF31475B)
)
