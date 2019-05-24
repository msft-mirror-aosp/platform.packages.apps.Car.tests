/*
 * Copyright 2019 The Android Open Source Project
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

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;

import static com.android.car.media.common.MediaConstants.CONTENT_STYLE_BROWSABLE_HINT;
import static com.android.car.media.common.MediaConstants.CONTENT_STYLE_GRID_ITEM_HINT_VALUE;
import static com.android.car.media.common.MediaConstants.CONTENT_STYLE_LIST_ITEM_HINT_VALUE;
import static com.android.car.media.common.MediaConstants.CONTENT_STYLE_PLAYABLE_HINT;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Our internal representation of media items. */
public class TmaMediaItem {

    /** The name of each entry is the value used in the json file. */
    public enum ContentStyle {
        NONE (0),
        LIST (CONTENT_STYLE_LIST_ITEM_HINT_VALUE),
        GRID (CONTENT_STYLE_GRID_ITEM_HINT_VALUE);
        final int mBundleValue;
        ContentStyle(int value) {
            mBundleValue = value;
        }
    }

    private final @MediaItem.Flags int mFlags;
    private final MediaMetadataCompat mMediaMetadata;
    private final ContentStyle mPlayableStyle;
    private final ContentStyle mBrowsableStyle;

    /** Read only list. */
    final List<TmaMediaItem> mChildren;
    /** Read only list. Events triggered when starting the playback. */
    final List<TmaMediaEvent> mMediaEvents;
    /** References another json file where to get extra children from. */
    final String mInclude;


    public TmaMediaItem(@MediaItem.Flags int flags, ContentStyle playableStyle,
            ContentStyle browsableStyle, MediaMetadataCompat metadata,
            List<TmaMediaEvent> mediaEvents, List<TmaMediaItem> children, String include) {
        mFlags = flags;
        mPlayableStyle = playableStyle;
        mBrowsableStyle = browsableStyle;
        mMediaMetadata = metadata;
        mChildren = Collections.unmodifiableList(children);
        mMediaEvents = Collections.unmodifiableList(mediaEvents);
        mInclude = include;
    }

    public String getMediaId() {
        return mMediaMetadata.getString(METADATA_KEY_MEDIA_ID);
    }

    /** Returns -1 if the duration key is unspecified or <= 0. */
    public long getDuration() {
        long result = mMediaMetadata.getLong(METADATA_KEY_DURATION);
        if (result <= 0) return -1;
        return result;
    }

    public TmaMediaItem append(List<TmaMediaItem> children) {
        List<TmaMediaItem> allChildren = new ArrayList<>(mChildren.size() + children.size());
        allChildren.addAll(mChildren);
        allChildren.addAll(children);
        return new TmaMediaItem(mFlags, mPlayableStyle, mBrowsableStyle, mMediaMetadata,
                mMediaEvents, allChildren, null);
    }

    void updateSessionMetadata(MediaSessionCompat session) {
        session.setMetadata(mMediaMetadata);
    }

    MediaItem toMediaItem() {
        return new MediaItem(buildDescription(), mFlags);
    }

    private MediaDescriptionCompat buildDescription() {

        // Use the default media description but add our extras.
        MediaDescriptionCompat metadataDescription = mMediaMetadata.getDescription();

        MediaDescriptionCompat.Builder bob = new MediaDescriptionCompat.Builder();
        bob.setMediaId(metadataDescription.getMediaId());
        bob.setTitle(metadataDescription.getTitle());
        bob.setSubtitle(metadataDescription.getSubtitle());
        bob.setDescription(metadataDescription.getDescription());
        bob.setIconBitmap(metadataDescription.getIconBitmap());
        bob.setIconUri(metadataDescription.getIconUri());
        bob.setMediaUri(metadataDescription.getMediaUri());

        Bundle extras = new Bundle();
        if (metadataDescription.getExtras() != null) {
            extras.putAll(metadataDescription.getExtras());
        }

        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, mPlayableStyle.mBundleValue);
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, mBrowsableStyle.mBundleValue);

        bob.setExtras(extras);
        return bob.build();
    }
}
