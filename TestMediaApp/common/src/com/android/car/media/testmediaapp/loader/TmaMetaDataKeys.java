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
    // TODO(b/222362032): Replace with androidx reference.
    public static final String BROWSE_CUSTOM_ACTIONS_ROOT_LIST =
            "androidx.media.utils.extras.CUSTOM_BROWSER_ACTION_ROOT_LIST";
    public static final String BROWSE_CUSTOM_ACTIONS_ITEM_LIST =
            "androidx.media.utils.extras.CUSTOM_BROWSER_ACTION_ID_LIST";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_ID =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_ID";
    public static final String BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_MEDIA_ITEM_ID";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_LABEL =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_LABEL";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_ICON =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_ICON_URI";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_LIMIT =
            "androidx.media.MediaBrowserCompat.BROWSE_CUSTOM_ACTIONS_ACTION_LIMIT";
    public static final String BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_EXTRAS";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_BROWSE_NODE =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_RESULT_BROWSE_NODE";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_OPEN_PLAYBACK =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_RESULT_SHOW_PLAYING_ITEM";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_REFRESH_ITEM =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_RESULT_REFRESH_ITEM";
    public static final String BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_MESSAGE =
            "androidx.media.utils.extras.KEY_CUSTOM_BROWSER_ACTION_RESULT_MESSAGE";
}
