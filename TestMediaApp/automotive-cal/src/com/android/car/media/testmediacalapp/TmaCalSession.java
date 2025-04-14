/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.car.media.testmediacalapp;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

import com.android.car.media.testmediaapp.carapp.MediaSessionController;
import com.android.car.media.testmediaapp.carapp.TmaMediaTabScreen;
import com.android.car.media.testmediaapp.carapp.TmaSettingsScreen;
import com.android.car.media.testmediaapp.carapp.TmaSignInScreen;
import com.android.car.media.testmediaapp.media3.TmaBrowser3;

/**
 * {@link Session} for Test Media App car screens.
 */
public final class TmaCalSession extends Session {

    public static final String SIGN_IN_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SIGN_IN";
    public static final String SETTINGS_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SETTINGS";

    @NonNull
    @Override
    public Screen onCreateScreen(@NonNull Intent intent) {
        if (SIGN_IN_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSignInScreen(getCarContext());
        } else if (SETTINGS_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSettingsScreen(getCarContext());
        } else {
            return new TmaMediaTabScreen(getCarContext(),
                    new MediaSessionController(getCarContext(), TmaBrowser3.class));
        }
    }
}
