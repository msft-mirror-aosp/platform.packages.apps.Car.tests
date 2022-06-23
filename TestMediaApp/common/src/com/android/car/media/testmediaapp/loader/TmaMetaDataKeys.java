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
    public static final String METADATA_KEY_PLAYBACK_STATUS =
            "android.media.metadata.PLAYBACK_STATUS";
    public static final String METADATA_KEY_PLAYBACK_PROGRESS =
            "android.media.metadata.PLAYBACK_PROGRESS";
    public static final String METADATA_KEY_SINGLE_ITEM_STYLE =
            "android.media.metadata.SINGLE_ITEM_STYLE";
}
