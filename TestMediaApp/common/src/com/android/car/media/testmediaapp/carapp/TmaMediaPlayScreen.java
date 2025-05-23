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
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.media3.common.MediaItem;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.media3.TmaMedia3ItemExtKt;

/** Displays a Play/Pause button for a {@link MediaItem} in a {@link MessageTemplate} */
public class TmaMediaPlayScreen extends Screen {

    private final TmaBrowsedMediaItem mMediaItem;
    private final MediaSessionController mMediaSessionController;
    private final TmaLibrary mLibrary;

    public TmaMediaPlayScreen(@NonNull CarContext carContext,
                                 @NonNull TmaBrowsedMediaItem mediaItem,
                                 @NonNull MediaSessionController mediaSessionController,
                                 @NonNull TmaLibrary library) {
        super(carContext);
        mMediaItem = mediaItem;
        mMediaSessionController = mediaSessionController;
        mLibrary = library;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        MediaItem m = TmaMedia3ItemExtKt.toMediaItem(mMediaItem.mItem, mLibrary,
                mMediaItem.mParentId);
        return new MessageTemplate.Builder("PLAYBACK").setHeaderAction(Action.BACK)
                .setTitle(MediaScreenUtils.getMediaItemTitle(mMediaItem))
                .addAction(new Action.Builder().setTitle("PLAY").setOnClickListener(
                        () -> mMediaSessionController.play(m)).build())
                .addAction(new Action.Builder().setTitle("Pause").setOnClickListener(
                        mMediaSessionController::pause).build()).build();
    }
}
