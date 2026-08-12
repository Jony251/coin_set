package com.example.coinset.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Standardized gap/padding scale - replaces the scattered hand-typed dp
// literals (4/6/8/12/16/20/24/32) found across every screen, where the same
// visual pattern (e.g. a status badge) had independently drifted to
// different padding values in different files.
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

// Component-specific sizes - separate from Spacing (gaps) since these are
// dimensions of things, not space between things.
object Dimens {
    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 48.dp

    val avatarSmall: Dp = 40.dp
    val avatarMedium: Dp = 64.dp
    val avatarLarge: Dp = 120.dp

    val logoMark: Dp = 56.dp

    val carouselCardWidth: Dp = 140.dp
    val carouselImageHeight: Dp = 100.dp
    val detailImageHeight: Dp = 200.dp

    val primaryButtonHeight: Dp = 56.dp

    val badgeHorizontalPadding: Dp = 8.dp
    val badgeVerticalPadding: Dp = 4.dp

    val bottomNavRaisedCircle: Dp = 56.dp
    val bottomNavRaisedCircleOffset: Dp = (-20).dp
}
