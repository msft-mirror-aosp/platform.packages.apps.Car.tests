/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.rotaryplayground

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Visual focus highlight that reacts to externally provided focus and press states. */
@Composable
fun Modifier.focusHighlight(interactionSource: MutableInteractionSource, shape: Shape): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fillColor =
        when {
            isFocused && isPressed -> Color(0x8A94CBFF)
            isFocused -> Color(0x3D94CBFF)
            else -> Color.Transparent
        }
    val strokeColor = if (isFocused) Color(0xFF94CBFF) else Color.Transparent
    val strokeWidth =
        when {
            isFocused && isPressed -> 4.dp
            isFocused -> 8.dp
            else -> 0.dp
        }

    return this.background(fillColor, shape)
        .border(border = BorderStroke(width = strokeWidth, color = strokeColor), shape = shape)
}

/**
 * Applies a visual highlight based on focus and press states, and makes the element clickable.
 *
 * **Intended Use Cases:**
 * - Applying to simple, non-interactive Composables (e.g., `Box`, `Row`) to make them interactive
 *   and visually indicate their focused/pressed state.
 * - When this modifier is intended to be the *primary* source of clickability and focusability for
 *   the element it's applied to.
 */
@Composable
fun Modifier.clickableFocusHighlight(shape: Shape, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fillColor =
        when {
            isFocused && isPressed -> Color(0x8A94CBFF)
            isFocused -> Color(0x3D94CBFF)
            else -> Color.Transparent
        }
    val strokeColor = if (isFocused) Color(0xFF94CBFF) else Color.Transparent
    val strokeWidth =
        when {
            isFocused && isPressed -> 4.dp
            isFocused -> 8.dp
            else -> 0.dp
        }
    return this.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
        .background(fillColor)
        .border(border = BorderStroke(width = strokeWidth, color = strokeColor), shape = shape)
}
