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

import androidx.car.app.CarContext;
import androidx.car.app.ScreenManager;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaItem;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;

import java.util.ArrayList;
import java.util.List;

public class MediaScreenUtils {

    /** Creates an ItemList from a List of {@link TmaMediaItem}s */
    public static ItemList createMediaItemList(CarContext context, ScreenManager screenManager,
                                        TmaBrowsedMediaItem parent, TmaLibrary library,
                                        MediaSessionController mediaSessionController) {
        ItemList.Builder itemList = new ItemList.Builder();
        for (TmaBrowsedMediaItem item : getBrowsedTmaItems(library, parent)) {
            Row.Builder rowBuilder = new Row.Builder();
            if (Boolean.TRUE.equals(item.mItem.mIsBrowsable)) {
                rowBuilder.setOnClickListener(
                        () -> screenManager.push(new TmaMediaListScreen(
                                context, library, item, mediaSessionController)));
            } else if (Boolean.TRUE.equals(item.mItem.mIsPlayable)) {
                rowBuilder.setOnClickListener(
                        () -> screenManager.push(new TmaMediaPlayScreen(
                                context, item, mediaSessionController, library)));
            }
            rowBuilder.setTitle(getMediaItemTitle(item));
            itemList.addItem(rowBuilder.build());
        }
        return itemList.build();
    }

    /** Null-checks the TmaMediaItem's title */
    public static String getMediaItemTitle(TmaBrowsedMediaItem item) {
        return item.mItem.getTitle() != null ? item.mItem.getTitle() : "NO TITLE";
    }

    /** Get all children of a browsable [TmaBrowsedMediaItem] */
    private static List<TmaBrowsedMediaItem> getBrowsedTmaItems(TmaLibrary library,
                                                        TmaBrowsedMediaItem parent) {
        List<TmaMediaItem> items = library.getAllChildren(parent.mItem);
        List<TmaBrowsedMediaItem> result = new ArrayList<>(items.size());
        int index = 0;
        for (TmaMediaItem child : items) {
            String parentPath = parent.mItem.getPath(parent.mParentId);
            result.add(new TmaBrowsedMediaItem(index++, child, parentPath));
        }
        return result;
    }
}
