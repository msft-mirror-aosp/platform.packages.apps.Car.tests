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
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.media.MediaPlaybackManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.media3.common.util.UnstableApi;

import com.android.car.media.testmediaapp.carapp.MediaSessionController;
import com.android.car.media.testmediaapp.carapp.TmaMediaPlayScreen;
import com.android.car.media.testmediaapp.carapp.TmaMediaTabScreen;
import com.android.car.media.testmediaapp.carapp.TmaSettingsScreen;
import com.android.car.media.testmediaapp.carapp.TmaSignInScreen;
import com.android.car.media.testmediaapp.media3.TmaBrowser3;

import kotlin.Unit;

/**
 * {@link Session} for Test Media App car screens.
 */
public final class TmaCalSession extends Session {

    public static final String SIGN_IN_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SIGN_IN";
    public static final String SETTINGS_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SETTINGS";
    private static final String SHOW_MEDIA_PLAYBACK =
            "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK";
    private MediaSessionController mMediaSessionController = null;

    @OptIn(markerClass = {ExperimentalCarApi.class, UnstableApi.class})
    // TODO b/461579151: 3p apps should not follow this pattern of obtaining the media1 token.
    //  Refactor this class to bind directly to the TmaBrowser3 and use its Player
    //  (for play/pause commands) and platformToken (for MediaPlaybackTemplate creation) directly
    public TmaCalSession() {
        getLifecycle().addObserver((LifecycleEventObserver) (lifecycleOwner, event) -> {
            if (event == Lifecycle.Event.ON_CREATE) {
                if (mMediaSessionController == null) {
                    mMediaSessionController = new MediaSessionController(
                            getCarContext(),
                            TmaBrowser3.class,
                            token -> {
                                MediaPlaybackManager manager = getCarContext().getCarService(
                                        MediaPlaybackManager.class);
                                manager.registerMediaPlaybackToken(
                                        MediaSessionCompat.Token.fromToken(token));
                                return Unit.INSTANCE;
                            });
                }
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    @Override
    public Screen onCreateScreen(@NonNull Intent intent) {
        if (SIGN_IN_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSignInScreen(getCarContext());
        } else if (SETTINGS_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSettingsScreen(getCarContext());
        } else {
            return new TmaMediaTabScreen(getCarContext(), mMediaSessionController);
        }
    }

    @Override
    public void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (mMediaSessionController != null && getCarContext().getCarAppApiLevel() >= 8
                && SHOW_MEDIA_PLAYBACK.equals(intent.getAction())) {
            ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);
            if (screenManager.getTop() instanceof TmaMediaPlayScreen) {
                return;
            }
            screenManager.push(TmaMediaPlayScreen.createScreenFromPlaying(
                    getCarContext(), mMediaSessionController));
        }
    }
}
