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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.android.car.rotaryplayground.L.TAG

/**
 * An Activity to demonstrate rotary support for Jetpack Compose on Android S and newer.
 * 1. It uses a [com.android.car.ui.FocusParkingView] to hide focus highlight when rotary navigates
 *    out of this Activity.
 * 2. It uses a [com.android.car.ui.FocusArea] to wrap the
 *    [androidx.compose.ui.platform.ComposeView] to support rotary nudging into this Activity.
 * 3. Because there are focusable Views before and after the ComposeView, in order to support rotary
 *    controller rotations between the surrounding Views and embedded Composables, it registers an
 *    [ViewTreeObserver.OnGlobalFocusChangeListener] to handle focus transitions and manage
 *    ComposeView focusability. **Notes**:
 *     1. The OnGlobalFocusChangeListener is unnecessary if no focusable Views exist before or after
 *        the ComposeView within that FocusArea.
 *     2. Dynamically changing focusability of the ComposeView is unnecessary if no focusable Views
 *        exists before the ComposeView within that FocusArea.
 */
class ViewComposeActivity : ComponentActivity() {
    private val ANDROID_COMPOSE_VIEW_CLASS_NAME = "androidx.compose.ui.platform.AndroidComposeView"
    private val focusMoveDirection = mutableStateOf(0)
    private lateinit var composeView: ComposeView
    private lateinit var button2: Button

    private val focusChangeListener: ViewTreeObserver.OnGlobalFocusChangeListener =
        ViewTreeObserver.OnGlobalFocusChangeListener { oldFocus, newFocus ->
            Log.d(TAG, "old $oldFocus, new: $newFocus")
            if (newFocus == composeView) {
                // The focus is moving onto the ComposeView.
                if (oldFocus == button2) {
                    // rotating from button2 (counter-clockwise rotation)
                    Log.d(TAG, "moving focus backward into Compose")
                    focusMoveDirection.value = 2
                } else {
                    // any other cases, such as rotating from button1, or nudging from system bars
                    Log.d(TAG, "moving focus forward into Compose")
                    focusMoveDirection.value = 1
                }
            }

            if (ANDROID_COMPOSE_VIEW_CLASS_NAME.equals(newFocus::class.qualifiedName)) {
                // Focus just moved to a Composable from ComposeView because of
                // focusManager.moveFocus()
                Log.d(TAG, "setting ComposeView not focusable")
                composeView.isFocusable = false
            } else {
                Log.d(TAG, "setting ComposeView focusable")
                composeView.isFocusable = true
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(this.window, true)
        setContentView(R.layout.view_compose_activity)
        composeView = findViewById<ComposeView>(R.id.compose_view)
        button2 = findViewById<Button>(R.id.button2)
        setUpComposeView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        composeView.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)
    }

    override fun onDetachedFromWindow() {
        composeView.viewTreeObserver.removeOnGlobalFocusChangeListener(focusChangeListener)
        super.onDetachedFromWindow()
    }

    private fun setUpComposeView() {
        composeView.setContent {
            InitFocusManager(focusMoveDirection)
            SetUpComposables()
        }
    }
}

@Composable
fun InitFocusManager(focusMoveDirection: MutableState<Int>) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(focusMoveDirection.value) {
        if (focusMoveDirection.value == 1) {
            Log.d(TAG, "move focus to next")
            focusManager.moveFocus(FocusDirection.Next)
            focusMoveDirection.value = 0
        } else if (focusMoveDirection.value == 2) {
            Log.d(TAG, "move focus to previous")
            focusManager.moveFocus(FocusDirection.Previous)
            focusMoveDirection.value = 0
        }
    }
}

@Composable
fun SetUpComposables() {
    val context = LocalContext.current
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotaryButton(
                    onClick = {
                        val intent = Intent(context, RotaryActivity::class.java)
                        context.startActivity(intent)
                    },
                    text = "Back",
                    shape = RectangleShape,
                )

                Text(
                    text = "focusable text",
                    modifier =
                        Modifier.clickableFocusHighlight(shape = RectangleShape, onClick = {})
                            .padding(8.dp),
                )

                // Because there is a focusable View (button2) AFTER this Composable, this
                // Composable must not  use Modifier.clickableFocusHighlight(). Otherwise,
                // rotating counter-clockwise  from button2 will not move focus to this
                // Composable on Android builds earlier than 25Q4.
                RotaryButton(onClick = {}, text = "Last Composable", shape = RectangleShape)
            }
        }
    }
}

@Composable
fun RotaryButton(onClick: () -> Unit, text: String, shape: Shape, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier =
            modifier
                .focusHighlight(interactionSource = interactionSource, shape = shape)
                .padding(8.dp),
    ) {
        Text(text)
    }
}
