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
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Tab;
import androidx.car.app.model.TabContents;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;
import androidx.media3.common.MediaItem;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.loader.TmaLoader;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs;

import com.google.common.collect.ImmutableList;

import java.util.stream.Collectors;

/** Hosts the top level {@link MediaItem}s from the {@link MediaSessionController} */
public class TmaMediaTabScreen extends Screen {

    private final ImmutableList<TmaBrowsedMediaItem> mTopItems;
    private String mTabContentId = "";
    private final TmaLibrary mLibrary;
    private final MediaSessionController mMediaSessionController;


    public TmaMediaTabScreen(@NonNull CarContext carContext,
                             MediaSessionController mediaSessionController) {
        super(carContext);
        mLibrary = new TmaLibrary(new TmaLoader(carContext));
        mLibrary.setBrowseRoot(TmaEnumPrefs.TmaBrowseNodeType.NODE_CHILDREN);
        String rootpath = TmaLibrary.ROOT_PATH;
        mTopItems = ImmutableList.copyOf(mLibrary.getAllChildren(mLibrary.getBrowseRoot())
                .stream().map(it -> new TmaBrowsedMediaItem(it, rootpath))
                .collect(Collectors.toList()));
        mMediaSessionController = mediaSessionController;
    }


    private int indexForTabContentId() {
        for (int i = 0; i < mTopItems.size(); i++) {
            if (mTopItems.get(i).mItem.getMediaId().equals(mTabContentId)) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        TabTemplate.Builder tabTemplateBuilder = new TabTemplate.Builder(
                new TabTemplate.TabCallback() {
                    @Override
                    public void onTabSelected(@NonNull String tabContentId) {
                        TabTemplate.TabCallback.super.onTabSelected(tabContentId);
                        mTabContentId = tabContentId;
                        invalidate();
                    }});
        if (mTopItems == null) {
            tabTemplateBuilder.setLoading(true);
            return tabTemplateBuilder.build();
        }
        for (TmaBrowsedMediaItem mediaItem : mTopItems) {
            tabTemplateBuilder.addTab(
                    new Tab.Builder().setTitle(mediaItem.mItem.getTitle())
                            .setIcon(CarIcon.APP_ICON).setContentId(mediaItem.mItem.getMediaId())
                            .build());
        }
        TmaBrowsedMediaItem selectedItem = mTopItems.get(indexForTabContentId());
        tabTemplateBuilder.setHeaderAction(Action.APP_ICON);
        tabTemplateBuilder.setActiveTabContentId(selectedItem.mItem.getMediaId());
        tabTemplateBuilder.setTabContents(new TabContents.Builder(
                createForPath(selectedItem)).build());
        return tabTemplateBuilder.build();
    }

    private ListTemplate createForPath(TmaBrowsedMediaItem selectedItem) {
        ListTemplate.Builder listTemplate = new ListTemplate.Builder();
        listTemplate.setHeaderAction(Action.APP_ICON);

        ItemList itemList = MediaScreenUtils.createMediaItemList(
                getCarContext(), getScreenManager(), selectedItem,
                mLibrary, mMediaSessionController);
        listTemplate.setSingleList(itemList);
        return listTemplate.build();
    }
}
