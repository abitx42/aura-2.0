package com.example.ui.anim

import androidx.compose.ui.unit.dp

object AuraCornerRadius {
    val Hero = 24.dp      // Feature/overview cards
    val Section = 20.dp   // Settings/section containers
    val Card = 16.dp      // Standard content cards
    val Flow = 14.dp      // Timeline/kanban/habit cards
    val Row = 12.dp       // List row items
    val Input = 12.dp     // Text fields, dropdowns
    val Chip = 8.dp       // Filter chips, day cells, pills
}

object AuraAnimTiming {
    val PressDown = 60    // ms, near-instant
    val PressRelease = 280 // ms, elastic bounce-back
    val ScreenTransition = 300 // ms, enter/exit
    val ShimmerCycle = 1100 // ms, one full sweep
    val TabSlide = 320    // ms, indicator travel
}
