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

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.car.ui.utils.DirectManipulationHelper;

/**
 * Fragment that demos rotary interactions directly manipulating the state of UI widgets such as a
 * {@link android.widget.SeekBar}, {@link android.widget.DatePicker}, and
 * {@link android.widget.RadialTimePickerView}.
 */
public class RotaryDirectManipulationWidgets extends Fragment {
    // TODO(agathaman): refactor a common class that takes in a fragment xml id and inflates it, to
    //  share between this and RotaryCards.

    /** How many pixels do we want to move the {@link DirectManipulationView} for nudge. */
    private static final float DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE = 10f;

    /** How many pixels do we want to zoom the {@link DirectManipulationView} for a rotation. */
    private static final float DIRECT_MANIPULATION_VIEW_PX_PER_ROTATION = 10f;

    /** Background color of {@link DirectManipulationView} when it's in direct manipulation mode. */
    private static final int BACKGROUND_COLOR_IN_DIRECT_MANIPULATION_MODE = Color.BLUE;

    /**
     * Background color of {@link DirectManipulationView} when it's not in direct manipulation
     * mode.
     */
    private static final int BACKGROUND_COLOR_NOT_IN_DIRECT_MANIPULATION_MODE = Color.TRANSPARENT;

    /** Whether any view in this Fragment is in direct manipulation mode. */
    private boolean mInDirectManipulationMode;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rotary_direct_manipulation, container, false);
        DirectManipulationView directManipulationView =
                view.findViewById(R.id.direct_manipulation_view);
        initDirectManipulationView(directManipulationView);
        return view;
    }

    /**
     * Initializes the {@link DirectManipulationView} so that it can enter/exit direct manipulation
     * mode and interact with the rotary controller directly. In direct manipulation mode, the
     * circle of the DirectManipulationView can move when the controller nudges, and the circle of
     * the DirectManipulationView can zoom when the controller rotates.
     */
    private void initDirectManipulationView(@NonNull DirectManipulationView dmv) {
        dmv.setOnKeyListener((view, keyCode, keyEvent) -> {
            boolean isActionUp = keyEvent.getAction() == KeyEvent.ACTION_UP;
            switch (keyCode) {
                // Always consume KEYCODE_DPAD_CENTER and KEYCODE_BACK event.
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (!mInDirectManipulationMode && isActionUp) {
                        mInDirectManipulationMode = true;
                        dmv.setBackgroundColor(BACKGROUND_COLOR_IN_DIRECT_MANIPULATION_MODE);
                        dmv.invalidate();
                        DirectManipulationHelper.enableDirectManipulationMode(dmv, true);
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (mInDirectManipulationMode && isActionUp) {
                        mInDirectManipulationMode = false;
                        dmv.setBackgroundColor(BACKGROUND_COLOR_NOT_IN_DIRECT_MANIPULATION_MODE);
                        dmv.invalidate();
                        DirectManipulationHelper.enableDirectManipulationMode(dmv, false);
                    }
                    return true;
                // Consume controller nudge event (KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN,
                // KEYCODE_DPAD_LEFT, or KEYCODE_DPAD_RIGHT) only when in direct manipulation mode.
                // When handling nudge event, move the circle of the DirectManipulationView.
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!mInDirectManipulationMode) {
                        return false;
                    }
                    if (isActionUp) {
                        dmv.move(0f, -DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!mInDirectManipulationMode) {
                        return false;
                    }
                    if (isActionUp) {
                        dmv.move(0f, DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!mInDirectManipulationMode) {
                        return false;
                    }
                    if (isActionUp) {
                        dmv.move(-DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE, 0f);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!mInDirectManipulationMode) {
                        return false;
                    }
                    if (isActionUp) {
                        dmv.move(DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE, 0f);
                    }
                    return true;
                // Don't consume other key events.
                default:
                    return false;
            }
        });

        // When in direct manipulation mode, zoom the circle of the DirectManipulationView on
        // controller rotate event.
        dmv.setOnGenericMotionListener(((view, motionEvent) -> {
            if (!mInDirectManipulationMode) {
                return false;
            }
            float scroll = motionEvent.getAxisValue(MotionEvent.AXIS_SCROLL);
            dmv.zoom(DIRECT_MANIPULATION_VIEW_PX_PER_ROTATION * scroll);
            return true;
        }));
    }
}
