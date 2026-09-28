package com.example.smartstudent.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Reference UI uses fully-rounded pill buttons and softly-rounded cards/inputs.
val StudentShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PillShape = RoundedCornerShape(50)
val CardShape = RoundedCornerShape(16.dp)
val InputShape = RoundedCornerShape(12.dp)
