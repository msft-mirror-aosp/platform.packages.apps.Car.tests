/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.media.testmediaapp.loader;

public class TmaMetaDataKeys {
    /** Keys for the {@link TmaMediaMetadataReader}
     *  We set them to correct androidx keys in
     *  {@link com.android.car.media.testmediaapp.TmaMediaItem}
     *  This is because the reader is using MetaDataCompat keys
     *      and the keys we want to use are in androidx
     */
    //TODO(b/263524275): Convert to androidx constants
    public static final String METADATA_KEY_PLAYBACK_STATUS =
            "android.media.metadata.PLAYBACK_STATUS";
    public static final String METADATA_KEY_PLAYBACK_PROGRESS =
            "android.media.metadata.PLAYBACK_PROGRESS";
    public static final String BROWSE_CUSTOM_ACTIONS_ROOT_LIST =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ROOT_LIST";
    public static final String BROWSE_CUSTOM_ACTIONS_ITEM_LIST =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ITEM_LIST";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_ID =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ACTION_ID";
    public static final String BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_LABEL =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ACTION_LABEL";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_ICON =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ACTION_ICON";
    public static final String BROWSE_ROOT_CUSTOM_ACTIONS_ACTION_ALLOWED_TYPES =
            "androidx.media.MediaBrowserCompat.BROWSE_ROOT_CUSTOM_ACTIONS_ACTION_ACTIONS_TYPES";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_BROWSE_NODE =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_EXTRA_NEW_BROWSE_NODE";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_PROGRESS_UPDATE =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_EXTRA_PROGRESS_UPDATE";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_OPEN_PLAYBACK =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_EXTRA_OPEN_PLAYBACK";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_REFRESH_ITEM =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_REFRESH_ITEM";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_MESSAGE =
            "android.car.media.common.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_MESSAGE";
}
