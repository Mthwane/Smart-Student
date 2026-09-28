package com.example.smartstudent.theme

import androidx.compose.ui.graphics.Color

// Core neutrals — v9 palette matched to the Figma "SmartStudent" design system:
// near-pure black cards on a soft cream background, gold as the hero accent.
val StudentBlack = Color(0xFF15130F)          // warm near-black (ink, not pure #000)
val StudentCardBlack = Color(0xFF000000)       // pure black — the hero balance/vault cards
val StudentBeigeBg = Color(0xFFF7F5F0)         // primary app background (matches Figma bg)
val StudentCream = Color(0xFFFFFFFF)           // card/surface, lighter than bg
val StudentBeige100 = Color(0xFFF0ECE4)        // subtle fills, chips, nav bar
val StudentBeige200 = Color(0xFFE7E1D5)        // borders, dividers
val StudentTaupe400 = Color(0xFFA79E8E)        // muted secondary text/icons
val StudentBrown600 = Color(0xFF6E6656)        // body text on beige
val StudentBrown800 = Color(0xFF211E19)        // headings on beige
val StudentWhite = Color(0xFFFFFFFF)

// Brand accent — Figma's warm metallic gold, replacing the old muted mustard.
val StudentGold = Color(0xFFD4AF37)
val StudentGoldLight = Color(0xFFF9ECC3)
val StudentGoldDark = Color(0xFF8F651E)

// Semantic (money states) — kept legible against beige
val StudentGreen = Color(0xFF3F7D51)
val StudentGreenLight = Color(0xFFE1EDE1)
val StudentRed = Color(0xFFB5473A)
val StudentRedLight = Color(0xFFF3E1DE)
val StudentBlue = Color(0xFF3E6DF0)
val StudentAmber = Color(0xFFE8A93B)

// Legacy aliases kept so existing gray-named references still compile;
// mapped onto the new warm-neutral scale instead of cool grays.
val StudentGray50 = StudentCream
val StudentGray100 = StudentBeige100
val StudentGray200 = StudentBeige200
val StudentGray400 = StudentTaupe400
val StudentGray600 = StudentBrown600
val StudentGray800 = StudentBrown800

// Category / chart accent palette — earthy, muted, distinguishable in a pie chart
val CategoryTerracotta = Color(0xFFC97B54)
val CategoryOlive = Color(0xFF8A9155)
val CategoryMustard = Color(0xFFD9A441)
val CategoryClay = Color(0xFFA85C4B)
val CategorySage = Color(0xFF7C9070)
val CategoryPlum = Color(0xFF8C6A7A)
val CategoryChartPalette = listOf(
    StudentGold, CategoryTerracotta, CategoryOlive, CategoryMustard, CategoryClay, CategorySage, CategoryPlum
)

// Kept for illustration/onboarding art already built in v1 (softened to fit the new palette)
val CategoryPurple = Color(0xFFB9AEFB)
val CategoryMint = Color(0xFFA6E3C4)
val CategorySky = Color(0xFFAFD9F0)
val CategoryPeach = Color(0xFFF2D2B8)
val CategoryCoral = Color(0xFFF2846B)
val CategoryAmberBadge = Color(0xFFF7D8A6)
