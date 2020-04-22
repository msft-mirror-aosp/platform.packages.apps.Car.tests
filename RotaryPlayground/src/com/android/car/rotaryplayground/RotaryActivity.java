/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.car.rotaryplayground;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * The main activity for Rotary Playground
 *
 * This activity starts the menu fragment and handles event calls (e.g. android:onClick) from
 * elements in fragments.
 */
public class RotaryActivity extends FragmentActivity {

    private static final String TAG = "RotaryActivity";
    private static final String MESSAGE = "Hello there!";

    private Fragment mMenuFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rotary_activity);
        showMenuFragment();
    }

    /** Event handler for buttons in res/layout/rotary_cards.xml */
    public void onRotaryCardsButtonClick(View v) {
        showToast(MESSAGE);
    }

    private void showMenuFragment() {
        if (mMenuFragment == null) {
            mMenuFragment = new RotaryMenu();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rotary_menu, mMenuFragment)
                .commit();
    }

    private void showToast(String message) {
        Log.d(TAG, "showToast -> " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}