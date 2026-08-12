package com.example.coinset.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Deliberate corner-radius language, slightly softer than stock M3 defaults
// (4/8/12/16/28) for a warmer, more considered collector-app feel. No
// Shapes() existed before this - every Card/Button corner radius was an
// unexamined Material3 default.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // badges, chips
    small = RoundedCornerShape(10.dp),       // buttons, text fields, small surfaces
    medium = RoundedCornerShape(16.dp),      // cards
    large = RoundedCornerShape(20.dp),       // hero images, bottom sheets
    extraLarge = RoundedCornerShape(28.dp)
)
