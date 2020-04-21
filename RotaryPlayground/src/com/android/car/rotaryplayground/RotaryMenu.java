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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment for the menu.
 *
 * On focus of a menu item, the associated fragment will start in the R.id.rotary_content container.
 */
public class RotaryMenu extends Fragment {

    private Fragment mRotaryCards = null;

    private Button mCardExamplesButton;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rotary_menu, container, false);

        mCardExamplesButton = view.findViewById(R.id.cards_example);
        mCardExamplesButton.setOnFocusChangeListener((v, hasFocus) -> showRotaryCards(hasFocus));
        mCardExamplesButton.setOnClickListener(v -> showRotaryCards(/* hasFocus= */ true));

        return view;
    }

    private void showRotaryCards(boolean hasFocus) {
        if (!hasFocus) {
            return; // do nothing if no focus.
        }
        if (mRotaryCards == null) {
            mRotaryCards = new RotaryCards();
        }
        showContent(mRotaryCards);
    }

    private void showContent(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.rotary_content, fragment)
                .commit();
    }
}
