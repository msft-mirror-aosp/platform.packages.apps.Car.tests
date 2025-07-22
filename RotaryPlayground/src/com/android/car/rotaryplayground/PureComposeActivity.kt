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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.accessibilityClassName
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.car.ui.FocusArea
import com.android.car.ui.FocusParkingView

class PureComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        Modifier.semantics {
                                accessibilityClassName = FocusParkingView::class.qualifiedName!!
                            }
                            .focusable(true)
                    )
                    Column {
                        Text(text = "This Activity is supported by rotary on 25Q4 and newer builds")
                        Row {
                            FocusAreaWithBackButton()
                            FocusAreaWithTwoButtons()
                        }
                        Row {
                            FocusAreaWithTwoButtons()
                            FocusAreaWithTwoButtons()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusAreaWithBackButton() {
    val context = LocalContext.current
    Row(
        Modifier.semantics { accessibilityClassName = FocusArea::class.qualifiedName!! }
            .padding(horizontal = 100.dp, vertical = 100.dp)
            .background(color = MaterialTheme.colorScheme.outline)
    ) {
        RotaryButton(
            onClick = {
                val intent = Intent(context, RotaryActivity::class.java)
                context.startActivity(intent)
            },
            text = "Back",
            shape = RectangleShape,
        )
        RotaryButton(onClick = {}, text = "Button2", shape = RectangleShape)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusAreaWithTwoButtons() {
    Row(
        Modifier.semantics { accessibilityClassName = FocusArea::class.qualifiedName!! }
            .padding(horizontal = 100.dp, vertical = 100.dp)
            .background(color = MaterialTheme.colorScheme.outline)
    ) {
        RotaryButton(onClick = {}, text = "Button1", shape = RectangleShape)
        RotaryButton(onClick = {}, text = "Button2", shape = RectangleShape)
    }
}
