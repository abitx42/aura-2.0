package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// SEMANTIC COLORS & ACCENTS (Fintech Reference)
// ==========================================
val SemanticGreen = Color(0xFF00D287)          // Available, Incoming, +₹, Completed
val SemanticGreenContainer = Color(0x2600D287) // 15% alpha green container
val SemanticGreenDark = Color(0xFF004D31)

val SemanticRed = Color(0xFFFF4D4D)            // Used, Outgoing, -₹, Debt Owed, Deletions
val SemanticRedContainer = Color(0x26FF4D4D)   // 15% alpha red container
val SemanticRedDark = Color(0xFF4A1414)

val SemanticGold = Color(0xFFFFB800)           // ✨ PRO badge, Default star, Highlights
val SemanticGoldContainer = Color(0x33FFB800)  // 20% alpha gold container

val RadiantOrange = Color(0xFFFA6438)          // Signature Brand Coral Accent (Screenshots 1-8)
val RadiantOrangeMuted = Color(0xFF8A3018)
val RadiantOrangeGlow = Color(0x33FA6438)

val AxioElectricLime = Color(0xFFA8F020)       // Axio Electric Lime Accent (Screenshots 9-11)
val AxioElectricLimeGlow = Color(0x33A8F020)

// ==========================================
// BASE & SURFACE COLORS
// ==========================================
val DarkBackground = Color(0xFF121315)         // Deep matte dark background
val DarkSurface = Color(0xFF18191D)            // Dock / top bar surface
val DarkCard = Color(0xFF1E2024)               // Layered content cards
val DarkCardBorder = Color(0xFF2A2C33)         // Thin subtle card outline
val DarkPillActive = Color(0xFF2F3138)         // Active capsule in bottom nav

val AmoledBackground = Color(0xFF000000)       // Pure pitch black
val AmoledSurface = Color(0xFF0D0E10)
val AmoledCard = Color(0xFF141518)
val AmoledCardBorder = Color(0xFF22242A)
val AmoledPillActive = Color(0xFF22242B)

val LightBackground = Color(0xFFF6F7FB)        // Soft bone white background
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightCardBorder = Color(0xFFE5E7EB)
val LightPillActive = Color(0xFFF0F1F5)

// Text Colors
val TextWhitePrimary = Color(0xFFFFFFFF)
val TextBoneSecondary = Color(0xFF9EA3B0)
val TextMutedSteel = Color(0xFF6B7280)

val TextDarkPrimary = Color(0xFF111827)
val TextDarkSecondary = Color(0xFF4B5563)
val TextDarkMuted = Color(0xFF9CA3AF)

// ==========================================
// MUTABLE THEME VARS (For compatibility)
// ==========================================
var AuraObsidian = DarkBackground
var AuraCharcoalBase = DarkSurface
var AuraSlateCard = DarkCard
var AuraSlateLight = DarkCardBorder

var AuraCyanNeon = RadiantOrange
var AuraCyanMuted = RadiantOrangeMuted
var AuraPurpleAccent = Color(0xFF7C4DFF)
var AuraCopperWarm = SemanticGold

var AuraWhiteMedium = TextBoneSecondary
var AuraWhiteMuted = TextMutedSteel

// Mood category tags
val MoodHappy = Color(0xFF00E676)
val MoodCalm = Color(0xFF29B6F6)
val MoodContent = Color(0xFFBA68C8)
val MoodNeutral = Color(0xFFECEFF1)
val MoodCreative = Color(0xFFFFD54F)
val MoodTired = Color(0xFF8D6E63)
val MoodSad = Color(0xFFEF5350)
