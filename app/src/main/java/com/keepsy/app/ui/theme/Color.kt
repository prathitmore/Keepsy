package com.keepsy.app.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Design System Colors
val Background = Color(0xFF0B1220)
val SurfaceSecondary = Color(0xFF161E2E)
val SurfaceTertiary = Color(0xFF1E293B)

val PrimaryPurple = Color(0xFF6366F1)
val PrimaryAccent = Color(0xFF00E0D1) // Teal
val PrimaryGlow = PrimaryPurple.copy(alpha = 0.15f)

val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val BorderColor = Color(0xFF334155).copy(alpha = 0.4f)
val BorderHighlight = Color(0xFF475569).copy(alpha = 0.6f)

// Design System Aliases
val CardBackground = SurfaceSecondary
val Slate900DarkBackground = Background
val TextMutedDark = TextSecondary
val HighlightTeal = PrimaryAccent
val MutedRedDanger = ErrorRed
val DeepIndigoPrimary = PrimaryPurple
val SoftGreenSuccess = SuccessGreen
val WarmAmberWarning = WarningAmber
