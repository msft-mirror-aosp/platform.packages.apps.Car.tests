/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.car.media.testmediaapp.media1;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media.MediaBrowserServiceCompat;

import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaPlayer;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.List;


/**
 * Implementation of {@link MediaBrowserServiceCompat} that delivers {@link MediaItem}s based on
 * json configuration files stored in the application's assets. Those assets combined with a few
 * preferences (see: {@link TmaPrefs}), allow to create a variety of use cases (including error
 * states) to stress test the Car Media application. <p/>
 * The media items are cached in the {@link TmaLibrary}, and can be virtually played with
 * {@link TmaPlayer}.
 */
@OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
public class TmaBrowser1 extends MediaBrowserServiceCompat {
    private static final String TAG = "TmaBrowser1";

    private TmaMedia1BrowserDelegate mDelegate;

    public TmaBrowser1() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDelegate = new TmaMedia1BrowserDelegate(this);
    }

    @Override
    public void onDestroy() {
        mDelegate.onDestroy();
        mDelegate = null;
        super.onDestroy();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackage, int clientUid, Bundle rootHints) {
        return mDelegate.onGetRoot(clientPackage, clientUid, rootHints);
    }

    @Override
    public void onSubscribe(String parentId, Bundle option) {
        mDelegate.onSubscriptionDelta(parentId, +1);
        super.onSubscribe(parentId, option);
    }

    @Override
    public void onUnsubscribe(String parentId) {
        mDelegate.onSubscriptionDelta(parentId, -1);
        super.onUnsubscribe(parentId);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result) {
        mDelegate.onLoadChildren(parentId, result, new Bundle());
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result,
            @NonNull Bundle options) {
        mDelegate.onLoadChildren(parentId, result, options);
    }

    @Override
    public void onLoadItem(String itemId, @NonNull Result<MediaBrowserCompat.MediaItem> result) {
        mDelegate.onLoadItem(itemId, result);
    }

    @Override
    public void onSearch(@NonNull String query, Bundle extras,
            @NonNull Result<List<MediaItem>> result) {
        mDelegate.onSearch(query, extras, result);
    }

    @Override
    public void onCustomAction(
            @NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
        mDelegate.handleCustomAction(action, extras, result);
    }
}
