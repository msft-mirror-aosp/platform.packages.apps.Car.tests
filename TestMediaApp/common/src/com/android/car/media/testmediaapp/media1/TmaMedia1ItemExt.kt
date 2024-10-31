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

package com.android.car.media.testmediaapp.media1

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.car.app.mediaextensions.MetadataExtras
import androidx.media.utils.MediaConstants
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE
import com.android.car.media.testmediaapp.TmaMediaItem
import com.android.car.media.testmediaapp.TmaMediaItem.ContentStyle
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
import com.android.car.media.testmediaapp.loader.TmaMetaDataKeys
import com.android.car.media.testmediaapp.MediaConstants as TmaMediaConstants


private object Statics {

    // const val TAG = "TmaMediaItemExt"

    val keyMap : HashMap<MetadataKey, String> = HashMap(MetadataKey.values().size)
    init {
        keyMap[TITLE] = MediaMetadataCompat.METADATA_KEY_TITLE
        keyMap[ARTIST] = MediaMetadataCompat.METADATA_KEY_ARTIST
        keyMap[DURATION] = MediaMetadataCompat.METADATA_KEY_DURATION
        keyMap[ALBUM] = MediaMetadataCompat.METADATA_KEY_ALBUM
        keyMap[AUTHOR] = MediaMetadataCompat.METADATA_KEY_AUTHOR
        keyMap[WRITER] = MediaMetadataCompat.METADATA_KEY_WRITER
        keyMap[COMPOSER] = MediaMetadataCompat.METADATA_KEY_COMPOSER
        keyMap[COMPILATION] = MediaMetadataCompat.METADATA_KEY_COMPILATION
        keyMap[DATE] = MediaMetadataCompat.METADATA_KEY_DATE
        keyMap[YEAR] = MediaMetadataCompat.METADATA_KEY_YEAR
        keyMap[GENRE] = MediaMetadataCompat.METADATA_KEY_GENRE
        keyMap[TRACK_NUMBER] = MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER
        keyMap[NUM_TRACKS] = MediaMetadataCompat.METADATA_KEY_NUM_TRACKS
        keyMap[DISC_NUMBER] = MediaMetadataCompat.METADATA_KEY_DISC_NUMBER
        keyMap[ALBUM_ARTIST] = MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST
        keyMap[ART_URI] = MediaMetadataCompat.METADATA_KEY_ART_URI
        keyMap[ALBUM_ART_URI] = MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
        keyMap[DISPLAY_TITLE] = MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE
        keyMap[DISPLAY_SUBTITLE] = MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE
        keyMap[DISPLAY_DESCRIPTION] = MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
        keyMap[DISPLAY_ICON_URI] = MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
        keyMap[GROUP_TITLE] = DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE
        keyMap[MEDIA_ID] = MediaMetadataCompat.METADATA_KEY_MEDIA_ID
        keyMap[BT_FOLDER_TYPE] = MediaMetadataCompat.METADATA_KEY_BT_FOLDER_TYPE
        keyMap[MEDIA_URI] = MediaMetadataCompat.METADATA_KEY_MEDIA_URI
        keyMap[ADVERTISEMENT] = MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT
        keyMap[DOWNLOAD_STATUS] = MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS
        keyMap[PLAYBACK_PROGRESS] = MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE
        keyMap[PLAYBACK_STATUS] = MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
        keyMap[EXPLICIT] = MediaConstants.METADATA_KEY_IS_EXPLICIT
        keyMap[SUBTITLE_LINK_MEDIA_ID] = TmaMediaConstants.KEY_SUBTITLE_LINK_MEDIA_ID
        keyMap[DESCRIPTION_LINK_MEDIA_ID] = TmaMediaConstants.KEY_DESCRIPTION_LINK_MEDIA_ID
        keyMap[IMMERSIVE_AUDIO] = TmaMediaConstants.KEY_IMMERSIVE_AUDIO
        keyMap[FORMAT_TINTABLE_LARGE_ICON] =
            TmaMediaConstants.KEY_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI
        keyMap[FORMAT_TINTABLE_SMALL_ICON] =
            TmaMediaConstants.KEY_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI
        keyMap[EXCLUDE_ITEM_IN_MIXED_LIST] =
            MetadataExtras.KEY_EXCLUDE_MEDIA_ITEM_FROM_MIXED_APP_LIST
    }

    fun mapStyle(style : ContentStyle) : Int {
        return when (style) {
            ContentStyle.NONE -> 0
            ContentStyle.LIST -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            ContentStyle.GRID -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            ContentStyle.LIST_CATEGORY
            -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM
            ContentStyle.GRID_CATEGORY
            -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        }
    }
}

fun TmaMediaItem.toMediaItem(mediaPath : String) : MediaItem {
    var flags = 0
    if (mIsBrowsable) flags = flags or MediaItem.FLAG_BROWSABLE
    if (mIsPlayable) flags = flags or MediaItem.FLAG_PLAYABLE
    return MediaItem(toDescription(mediaPath), flags)
}

fun TmaMediaItem.buildMetadata() : MediaMetadataCompat {
    val builder = MediaMetadataCompat.Builder()
    for (key in mMediaMetadata.keys) {
        val key2 = Statics.keyMap[key]
        when (key.mKeyType) {
            ValueType.INT,
            ValueType.LONG,
            -> builder.putLong(key2, mMediaMetadata.getLong(key))
            ValueType.TEXT,
            ValueType.URI,
            -> builder.putString(key2, mMediaMetadata.getString(key))
            ValueType.DOUBLE,
            -> builder.putString(key2, mMediaMetadata.getDouble(key).toString())
        }
    }
    return builder.build()
}

fun TmaMediaItem.toDescription(parentPath : String) : MediaDescriptionCompat {
    val metadataCompat = buildMetadata();

    // Use the default media description but add our extras.
    val metadataDescription: MediaDescriptionCompat = metadataCompat.description

    val bob = MediaDescriptionCompat.Builder()
    bob.apply {
        setMediaId(getPath(parentPath))
        setTitle(metadataDescription.title)
        setSubtitle(metadataDescription.subtitle)
        setDescription(metadataDescription.description)
        setIconUri(metadataDescription.iconUri)
        setMediaUri(metadataDescription.mediaUri)
    }

    val extras = Bundle()
    if (metadataDescription.extras != null) {
        extras.putAll(metadataDescription.extras)
    }

    extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            Statics.mapStyle(mPlayableStyle))
    extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            Statics.mapStyle(mBrowsableStyle))
    extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM,
            Statics.mapStyle(mSingleItemStyle))

    val playbackStatus : Int = metadataCompat.bundle.getLong(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS, 2).toInt()
    val progress: Double? = metadataCompat.bundle.getString(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE)?.toDoubleOrNull()

    extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS, playbackStatus)
    if (progress != null) {
        extras.putDouble(MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE, progress)
    }

    if (metadataCompat.containsKey(MediaConstants.METADATA_KEY_IS_EXPLICIT)) {
        extras.putLong(MediaConstants.METADATA_KEY_IS_EXPLICIT,
                metadataCompat.getLong(MediaConstants.METADATA_KEY_IS_EXPLICIT))
    }

    if (metadataCompat.containsKey(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE)) {
        extras.putString(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                metadataCompat.getString(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE));
    }

    if (mBrowseActions != null && mBrowseActions.isNotEmpty()) {
        extras.putStringArrayList(TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ITEM_LIST,
                ArrayList(mBrowseActions))
    }

    if (mIconsUriList != null && mIconsUriList.isNotEmpty()) {
        extras.putStringArrayList(MetadataExtras.KEY_TINTABLE_INDICATOR_ICON_URI_LIST,
            ArrayList(mIconsUriList))
    }

    bob.setExtras(extras)
    return bob.build()
}