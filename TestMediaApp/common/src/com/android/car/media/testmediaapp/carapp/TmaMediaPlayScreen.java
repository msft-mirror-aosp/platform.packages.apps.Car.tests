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
package com.android.car.media.testmediaapp.carapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.media.model.MediaPlaybackTemplate;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.media3.common.MediaItem;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.media3.TmaMedia3ItemExtKt;

/** Displays a Play/Pause button for a {@link MediaItem} in a {@link MessageTemplate} */
public class TmaMediaPlayScreen extends Screen {

    private final TmaBrowsedMediaItem mBrowsedMediaItem;
    private final MediaSessionController mMediaSessionController;
    private final TmaLibrary mLibrary;

    private TmaMediaPlayScreen(@NonNull CarContext carContext,
                              @Nullable TmaBrowsedMediaItem mediaItem,
                              @NonNull MediaSessionController mediaSessionController,
                              @Nullable TmaLibrary library) {
        super(carContext);
        mBrowsedMediaItem = mediaItem;
        mMediaSessionController = mediaSessionController;
        mLibrary = library;
    }

    /** Launch a TmaMediaPlayScreen for the currently playing item */
    public static TmaMediaPlayScreen createScreenFromBrowse(@NonNull CarContext carContext,
            @NonNull TmaBrowsedMediaItem mediaItem,
            @NonNull MediaSessionController mediaSessionController, @NonNull TmaLibrary library) {
        return new TmaMediaPlayScreen(carContext, mediaItem, mediaSessionController, library);
    }

    /** Launch the TmaMediaPlayScreen after playing a browsed item */
    public static TmaMediaPlayScreen createScreenFromPlaying(@NonNull CarContext carContext,
            @NonNull MediaSessionController mediaSessionController) {
        return new TmaMediaPlayScreen(carContext, /* mediaItem */ null, mediaSessionController,
                /* library */ null);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        if (getCarContext().getCarAppApiLevel() < 8) {
            if (mBrowsedMediaItem != null && mLibrary != null) {
                MediaItem m = TmaMedia3ItemExtKt.toMediaItem(mBrowsedMediaItem.mItem, mLibrary,
                        mBrowsedMediaItem.mParentId);
                return new MessageTemplate.Builder("PLAYBACK").setHeaderAction(Action.BACK)
                        .setTitle(MediaScreenUtils.getMediaItemTitle(mBrowsedMediaItem))
                        .addAction(new Action.Builder().setTitle("PLAY").setOnClickListener(
                                () -> mMediaSessionController.play(m)).build())
                        .addAction(new Action.Builder().setTitle("Pause").setOnClickListener(
                                mMediaSessionController::pause).build()).build();
            } else {
                // We never push a TmaMediaPlayScreen from non-browse when the host level is < 8
                return new MessageTemplate.Builder("Playback Screen not supported").build();
            }
        } else {
            return new MediaPlaybackTemplate.Builder().setHeader(
                    new Header.Builder().setStartHeaderAction(Action.APP_ICON).addEndHeaderAction(
                            new Action.Builder().setTitle("Button").build()
                    ).build()
            ).build();
        }
    }
}
