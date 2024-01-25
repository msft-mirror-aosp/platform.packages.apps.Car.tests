/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.car.media.testmediaapp.analytics;

import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.ANALYTICS_ON;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsBroadcastReceiver;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsCallback;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.car.app.mediaextensions.analytics.event.BrowseChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.ErrorEvent;
import androidx.car.app.mediaextensions.analytics.event.MediaClickedEvent;
import androidx.car.app.mediaextensions.analytics.event.ViewChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.VisibleItemsEvent;
import androidx.lifecycle.MutableLiveData;

import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.ArrayDeque;
import java.util.Deque;

@OptIn(markerClass = androidx.car.app.annotations2.ExperimentalCarApi.class)
public class TmaAnalyticsBroadcastReceiver extends AnalyticsBroadcastReceiver {

    public static final String TAG = TmaAnalyticsBroadcastReceiver.class.getSimpleName();

    public static final int SESSION_ID = 0;

    static AnalyticsCallback callback = new AnalyticsCallback() {
        @Override
        public void onBrowseNodeChangeEvent(@NonNull BrowseChangeEvent event) {
            handleEvent(event);
        }

        @Override
        public void onMediaClickedEvent(@NonNull MediaClickedEvent event) {
            handleEvent(event);
        }

        @Override
        public void onViewChangeEvent(@NonNull ViewChangeEvent event) {
            handleEvent(event);
        }

        @Override
        public void onVisibleItemsEvent(@NonNull VisibleItemsEvent event) {
            handleEvent(event);
        }

        @Override
        public void onErrorEvent(@NonNull ErrorEvent event) {
            handleEvent(event);
        }

        private void handleEvent(@NonNull AnalyticsEvent event) {
            Deque<AnalyticsEvent> queue = analyticsEventLiveData.getValue();
            if (!queue.offer(event)) {
                queue.poll();
                queue.offer(event);
            }
            analyticsEventLiveData.setValue(analyticsEventLiveData.getValue());
        }
    };

    //Optionally bounded dequeue. Bounded to 20 here.
    public static final MutableLiveData<Deque<AnalyticsEvent>>
            analyticsEventLiveData = new MutableLiveData<>(new ArrayDeque<>(20));

    private TmaPrefs mPrefs;

    public TmaAnalyticsBroadcastReceiver() {
        super(callback);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        mPrefs = TmaPrefs.getInstance(context);

        if (!isAnalyticsEnabled()) {
            Log.e(TAG, "Analytics sent when not enabled!");
        }
    }

    private boolean isAnalyticsEnabled() {
        if (mPrefs == null
                || mPrefs.mAnalyticsState == null
                || mPrefs.mAnalyticsState.getValue() == null) {
            return false;
        }
        return mPrefs.mAnalyticsState.getValue().getFlags().contains(ANALYTICS_ON.getId());
    }
}
