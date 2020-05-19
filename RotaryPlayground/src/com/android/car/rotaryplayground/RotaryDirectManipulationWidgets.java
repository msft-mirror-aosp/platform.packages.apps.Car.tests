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
 * {@link android.widget.SeekBar}, {@link android.widget.DatePicker},
 * {@link android.widget.RadialTimePickerView}, and {@link DirectManipulationView}.
 */
public class RotaryDirectManipulationWidgets extends Fragment {
    // TODO(agathaman): refactor a common class that takes in a fragment xml id and inflates it, to
    //  share between this and RotaryCards.

    /** How many pixels do we want to move the {@link DirectManipulationView} for nudge. */
    private static final float DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE = 10f;

    /** How many pixels do we want to zoom the {@link DirectManipulationView} for a rotation. */
    private static final float DIRECT_MANIPULATION_VIEW_PX_PER_ROTATION = 10f;

    /** Background color of a view when it's in direct manipulation mode. */
    private static final int BACKGROUND_COLOR_IN_DIRECT_MANIPULATION_MODE = Color.BLUE;

    /** Background color of a view when it's not in direct manipulation mode. */
    private static final int BACKGROUND_COLOR_NOT_IN_DIRECT_MANIPULATION_MODE = Color.TRANSPARENT;

    /** The view that is in direct manipulation mode, or null if none. */
    private View mViewInDirectManipulationMode;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rotary_direct_manipulation, container, false);
        DirectManipulationView dmv = view.findViewById(R.id.direct_manipulation_view);
        initDirectManipulationMode(dmv, /* handleNudge= */ true, /* handleRotate= */ true);
        return view;
    }

    @Override
    public void onPause() {
        if (mViewInDirectManipulationMode != null) {
            // To ensure that the user doesn't get stuck in direct manipulation mode, disable direct
            // manipulation mode when the fragment is not interactive (e.g., a dialog shows up).
            enableDirectManipulationMode(mViewInDirectManipulationMode, false);
        }
        super.onPause();
    }

    /**
     * Initializes the given view so that it can enter/exit direct manipulation mode and interact
     * with the rotary controller directly.
     *
     * @param dmv          the view to enable direct manipulation mode
     * @param handleNudge  whether to handle controller nudge in direct manipulation mode
     * @param handleRotate whether to handle controller rotate in direct manipulation mode
     */
    private void initDirectManipulationMode(
            @NonNull View dmv, boolean handleNudge, boolean handleRotate) {
        dmv.setOnKeyListener((view, keyCode, keyEvent) -> {
            boolean isActionUp = keyEvent.getAction() == KeyEvent.ACTION_UP;
            switch (keyCode) {
                // Always consume KEYCODE_DPAD_CENTER and KEYCODE_BACK event.
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (mViewInDirectManipulationMode == null && isActionUp) {
                        enableDirectManipulationMode(dmv, true);
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (mViewInDirectManipulationMode != null && isActionUp) {
                        enableDirectManipulationMode(dmv, false);
                    }
                    return true;
                // Consume nudge event if the view handles controller nudge in direct manipulation
                // mode.
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return handleNudge? handleNudgeEvent(keyEvent) : false;
                // Don't consume other key events.
                default:
                    return false;
            }
        });

        // Consume rotate event if the view handles controller rotate in direct manipulation mode.
        if (handleRotate) {
            dmv.setOnGenericMotionListener(((view, motionEvent) -> {
                float scroll = motionEvent.getAxisValue(MotionEvent.AXIS_SCROLL);
                return handleRotateEvent(scroll);
            }));
        }
    }

    private void enableDirectManipulationMode(@NonNull View view, boolean enable) {
        view.setBackgroundColor(enable
                ? BACKGROUND_COLOR_IN_DIRECT_MANIPULATION_MODE
                : BACKGROUND_COLOR_NOT_IN_DIRECT_MANIPULATION_MODE);
        view.invalidate();
        DirectManipulationHelper.enableDirectManipulationMode(view, enable);
        mViewInDirectManipulationMode = enable ? view : null;
    }

    /** Handles controller nudge event. Returns whether the event was consumed. */
    private boolean handleNudgeEvent(KeyEvent keyEvent) {
        if (mViewInDirectManipulationMode == null) {
            return false;
        }
        if (keyEvent.getAction() != KeyEvent.ACTION_UP) {
            return true;
        }
        int keyCode = keyEvent.getKeyCode();
        if (mViewInDirectManipulationMode instanceof DirectManipulationView) {
            DirectManipulationView dmv = (DirectManipulationView) mViewInDirectManipulationMode;
            handleNudgeEvent(dmv, keyCode);
            return true;
        }

        // TODO: support other views.

        return true;
    }

    /** Handles controller rotate event. Returns whether the event was consumed. */
    private boolean handleRotateEvent(float scroll) {
        if (mViewInDirectManipulationMode == null) {
            return false;
        }
        if (mViewInDirectManipulationMode instanceof DirectManipulationView) {
            DirectManipulationView dmv = (DirectManipulationView) mViewInDirectManipulationMode;
            handleRotateEvent(dmv, scroll);
            return true;
        }

        // TODO: support other views.

        return true;
    }

    /** Moves the circle of the DirectManipulationView when the controller nudges. */
    private void handleNudgeEvent(@NonNull DirectManipulationView dmv, int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                dmv.move(0f, -DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE);
                return;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                dmv.move(0f, DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE);
                return;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                dmv.move(-DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE, 0f);
                return;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                dmv.move(DIRECT_MANIPULATION_VIEW_PX_PER_NUDGE, 0f);
                return;
            default:
                throw new IllegalArgumentException("Invalid keycode :" + keyCode);
        }
    }

    /** Zooms the circle of the DirectManipulationView when the controller rotates. */
    private void handleRotateEvent(@NonNull DirectManipulationView dmv, float scroll) {
        dmv.zoom(DIRECT_MANIPULATION_VIEW_PX_PER_ROTATION * scroll);
    }
}
