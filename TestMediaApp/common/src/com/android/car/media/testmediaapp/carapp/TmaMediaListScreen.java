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
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Template;
import androidx.media3.common.MediaItem;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;

/** Displays {@link MediaItem}s in a {@link ListTemplate} */
public class TmaMediaListScreen extends Screen {

    private final TmaBrowsedMediaItem mMediaItem;
    private final TmaLibrary mLibrary;
    private final MediaSessionController mMediaSessionController;

    public TmaMediaListScreen(@NonNull CarContext carContext, @NonNull TmaLibrary library,
                              @NonNull TmaBrowsedMediaItem item,
                              @NonNull MediaSessionController mediaSessionController) {
        super(carContext);
        mMediaItem = item;
        mLibrary = library;
        mMediaSessionController = mediaSessionController;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return createForPath();
    }

    private ListTemplate createForPath() {
        ListTemplate.Builder listTemplate = new ListTemplate.Builder();
        listTemplate.setHeaderAction(Action.BACK);
        listTemplate.setTitle(MediaScreenUtils.getMediaItemTitle(mMediaItem));

        ItemList itemList = MediaScreenUtils.createMediaItemList(getCarContext(),
                getScreenManager(), mMediaItem, mLibrary,
                mMediaSessionController);
        listTemplate.setSingleList(itemList);
        return listTemplate.build();
    }

}
