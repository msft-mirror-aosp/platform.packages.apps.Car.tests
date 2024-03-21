/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.car.media.testmediaapp;

import static androidx.media3.session.CommandButton.ICON_SKIP_BACK_15;
import static androidx.media3.session.CommandButton.ICON_SKIP_FORWARD_30;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
@OptIn(markerClass = UnstableApi.class)
public enum TmaCustomAction {

    HEART_PLUS_PLUS("heart_plus_plus", R.string.heart_plus_plus, R.drawable.ic_heart_plus_plus, 0),
    HEART_LESS_LESS("heart_less_less", R.string.heart_less_less, R.drawable.ic_heart_less_less, 0),
    REQUEST_LOCATION("location", R.string.location, R.drawable.ic_location, 0),
    // We use the close icon drawable to make sure the displayed icon comes from CarMediaApp.
    SKIP_FORWARD_30("skip_forward_30", R.string.skip_forward_30, R.drawable.ic_close,
            ICON_SKIP_FORWARD_30),
    SKIP_BACK_15("skip_back_15", R.string.skip_back_15, R.drawable.ic_close, ICON_SKIP_BACK_15);

    final String mId;
    final int mNameId;
    final int mIcon;
    final int mIconId;

    TmaCustomAction(String id, int name, int icon, int iconId) {
        mId = TmaMediaItem.CUSTOM_ACTION_PREFIX + id;
        mNameId = name;
        mIcon = icon;
        mIconId = iconId;
    }

}
