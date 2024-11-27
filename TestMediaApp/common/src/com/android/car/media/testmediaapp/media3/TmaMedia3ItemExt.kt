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

package com.android.car.media.testmediaapp.media3

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.car.app.mediaextensions.MetadataExtras
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
import androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE
import androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM
import com.android.car.media.testmediaapp.TmaLibrary
import com.android.car.media.testmediaapp.TmaMediaItem
import com.android.car.media.testmediaapp.TmaMediaItem.ContentStyle
import com.android.car.media.testmediaapp.TmaMediaItem.TmaMetadata
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ADVERTISEMENT
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ALBUM
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ALBUM_ARTIST
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ALBUM_ART_URI
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ARTIST
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.ART_URI
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.AUTHOR
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.BT_FOLDER_TYPE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.COMPILATION
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.COMPOSER
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DATE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DESCRIPTION_LINK_MEDIA_ID
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DISC_NUMBER
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DISPLAY_DESCRIPTION
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DISPLAY_ICON_URI
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DISPLAY_SUBTITLE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DISPLAY_TITLE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DOWNLOAD_STATUS
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.DURATION
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.EXCLUDE_ITEM_IN_MIXED_LIST
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.EXPLICIT
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.FORMAT_TINTABLE_LARGE_ICON
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.FORMAT_TINTABLE_SMALL_ICON
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.GENRE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.GROUP_TITLE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.IMMERSIVE_AUDIO
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.MEDIA_ID
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.MEDIA_URI
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.NUM_TRACKS
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.PLAYBACK_PROGRESS
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.PLAYBACK_STATUS
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.SUBTITLE_LINK_MEDIA_ID
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.TITLE
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.TRACK_NUMBER
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.WRITER
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.YEAR
import com.android.car.media.testmediaapp.TmaMediaItem.ValueType
import com.android.car.media.testmediaapp.MediaConstants as TmaMediaConstants


@OptIn(UnstableApi::class)
private object Statics {
    const val TAG = "TmaMedia3ItemExt"

    val extrasMap : HashMap<MetadataKey, String> = HashMap(MetadataKey.values().size)
    init {
        extrasMap[EXPLICIT] = MediaConstants.EXTRAS_KEY_IS_EXPLICIT
        extrasMap[GROUP_TITLE] = MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE
        extrasMap[SUBTITLE_LINK_MEDIA_ID] = TmaMediaConstants.KEY_SUBTITLE_LINK_MEDIA_ID
        extrasMap[DESCRIPTION_LINK_MEDIA_ID] = TmaMediaConstants.KEY_DESCRIPTION_LINK_MEDIA_ID
        extrasMap[IMMERSIVE_AUDIO] = TmaMediaConstants.KEY_IMMERSIVE_AUDIO
        extrasMap[FORMAT_TINTABLE_LARGE_ICON] =
                TmaMediaConstants.KEY_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI
        extrasMap[FORMAT_TINTABLE_SMALL_ICON] =
                TmaMediaConstants.KEY_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI
        extrasMap[EXCLUDE_ITEM_IN_MIXED_LIST] =
            MetadataExtras.KEY_EXCLUDE_MEDIA_ITEM_FROM_MIXED_APP_LIST
        extrasMap[PLAYBACK_PROGRESS] = MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE
        extrasMap[PLAYBACK_STATUS] = MediaConstants.EXTRAS_KEY_COMPLETION_STATUS
    }

    fun mapStyle(style : ContentStyle) : Int {
        return when (style) {
            ContentStyle.NONE -> 0
            ContentStyle.LIST -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            ContentStyle.GRID -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            ContentStyle.LIST_CATEGORY
            -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM
            ContentStyle.GRID_CATEGORY
            -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        }
    }

    fun mapExtra(lib: TmaLibrary, key : MetadataKey, metaExtras : Bundle, metadata: TmaMetadata) {
        val extraKey = extrasMap[key]
        if (extraKey == null) {
            Log.e(TAG, "Unsupported extra key")
            return
        }
        when (key.mKeyType) {
            ValueType.INT
            -> metaExtras.putInt(extraKey, metadata.getLong(key).toInt())
            ValueType.LONG,
            -> metaExtras.putLong(extraKey, metadata.getLong(key))
            ValueType.TEXT,
            ValueType.URI,
            -> {
                var value = metadata.getString(key)
                if (key == SUBTITLE_LINK_MEDIA_ID || key == DESCRIPTION_LINK_MEDIA_ID) {
                    value = TmaMediaItem.selectLink(lib, value)
                }
                metaExtras.putString(extraKey, value)
            }
            ValueType.DOUBLE,
            -> metaExtras.putDouble(extraKey, metadata.getDouble(key))
        }
    }
}

@OptIn(UnstableApi::class)
fun TmaMediaItem.toMediaItem(lib: TmaLibrary, parentPath : String) : MediaItem {
    val metaExtras = Bundle()
    val metaBuilder = MediaMetadata.Builder()
    metaBuilder.setIsBrowsable(mIsBrowsable)
    metaBuilder.setIsPlayable(mIsPlayable)

    // Process mMediaMetadata
    for (key in mMediaMetadata.keys) {
        when (key) {
            MEDIA_ID -> {} // Set on the MediaItem.
            TITLE -> metaBuilder.setTitle(mMediaMetadata.getString(key))
            ARTIST -> metaBuilder.setArtist(mMediaMetadata.getString(key))
            ALBUM -> metaBuilder.setAlbumTitle(mMediaMetadata.getString(key))
            DISPLAY_TITLE -> metaBuilder.setDisplayTitle(mMediaMetadata.getString(key))
            DISPLAY_SUBTITLE -> metaBuilder.setSubtitle(mMediaMetadata.getString(key))
            DISPLAY_DESCRIPTION -> metaBuilder.setDescription(
                    mMediaMetadata.getString(DISPLAY_DESCRIPTION))
            // TODO(media3) uncomment once the prebuilt has been updated
            // DURATION -> metaBuilder.setDurationMs(mMediaMetadata.getLong(key))
            ART_URI -> metaBuilder.setArtworkUri(Uri.parse(mMediaMetadata.getString(key)))

            AUTHOR,
            WRITER,
            COMPOSER,
            COMPILATION,
            DATE,
            YEAR,
            GENRE,
            TRACK_NUMBER,
            NUM_TRACKS,
            DISC_NUMBER,
            ALBUM_ARTIST,
            ALBUM_ART_URI,
            DISPLAY_ICON_URI,
            BT_FOLDER_TYPE,
            MEDIA_URI,
            ADVERTISEMENT,
            DOWNLOAD_STATUS,
            -> {
                TODO() // Not needed yet (not used in the json files).
            }
            else -> Statics.mapExtra(lib, key, metaExtras, mMediaMetadata)
        }
    }

    metaExtras.putInt(EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, Statics.mapStyle(mPlayableStyle))
    metaExtras.putInt(EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, Statics.mapStyle(mBrowsableStyle))
    metaExtras.putInt(EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, Statics.mapStyle(mSingleItemStyle))

//    TODO(media3) custom browse actions
//    if (mBrowseActions != null && mBrowseActions.isNotEmpty()) {
//        extras.putStringArrayList(TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ITEM_LIST,
//                ArrayList(mBrowseActions))
//    }

    if (mIndicatorIcons != null && mIndicatorIcons.isNotEmpty()) {
        metaExtras.putParcelableArrayList(MetadataExtras.KEY_TINTABLE_INDICATOR_ICON_URI_LIST,
            ArrayList(mIndicatorIcons))
    }

    metaBuilder.setExtras(metaExtras)

    return MediaItem.Builder()
            .setMediaId(getPath(parentPath))
            .setMediaMetadata(metaBuilder.build())
            .build()
}

