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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment that demos rotary interactions directly manipulating the state of UI widgets such as a
 * {@link android.widget.SeekBar}, {@link android.widget.DatePicker}, and
 * {@link android.widget.RadialTimePickerView}.
 */
public class RotaryDirectManipulationWidgets extends Fragment {
    // TODO(agathaman): refactor a common class that takes in a fragment xml id and inflates it, to
    //  share between this and RotaryCards.
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rotary_direct_manipulation, container, false);
    }
}
