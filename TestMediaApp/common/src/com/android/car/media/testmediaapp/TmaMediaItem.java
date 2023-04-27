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
import static com.android.car.media.testmediaapp.MediaConstants.KEY_DESCRIPTION_LINK_MEDIA_ID;
import static com.android.car.media.testmediaapp.MediaConstants.KEY_SUBTITLE_LINK_MEDIA_ID;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ITEM_LIST;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.METADATA_KEY_PLAYBACK_PROGRESS;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.METADATA_KEY_PLAYBACK_STATUS;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media.utils.MediaConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Our internal representation of media items. */
public class TmaMediaItem {

    private static final String CUSTOM_ACTION_PREFIX = "com.android.car.media.testmediaapp.";

    /**
     * The character used to separate short media ids (returned by {@link #getMediaId} from
     * different {@link TmaMediaItem} instances to form a full path. See {@link #getPath}.
     */
    public static final char TREE_PATH_SEPARATOR = '#';

    /** Separates multiple media ids (eg: in links). See {@link #selectLink} */
    public static final char MULTI_ID_SEPARATOR = '|';

    /** The name of each entry is the value used in the json file. */
    public enum ContentStyle {
        NONE (0),
        LIST (MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM),
        GRID (MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
        LIST_CATEGORY(MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM),
        GRID_CATEGORY(MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM);
        final int mBundleValue;
        ContentStyle(int value) {
            mBundleValue = value;
        }
    }

    public enum TmaCustomAction {
        HEART_PLUS_PLUS(CUSTOM_ACTION_PREFIX + "heart_plus_plus", R.string.heart_plus_plus,
                R.drawable.ic_heart_plus_plus),
        HEART_LESS_LESS(CUSTOM_ACTION_PREFIX + "heart_less_less", R.string.heart_less_less,
                R.drawable.ic_heart_less_less),
        REQUEST_LOCATION(CUSTOM_ACTION_PREFIX + "location", R.string.location,
                R.drawable.ic_location);

        final String mId;
        final int mNameId;
        final int mIcon;

        TmaCustomAction(String id, int name, int icon) {
            mId = id;
            mNameId = name;
            mIcon = icon;
        }
    }

    public enum TmaBrowseAction {
        DOWNLOAD("DOWNLOAD", R.string.download,
                "drawable/ic_download_for_offline"),
        DOWNLOADING("DOWNLOADING", R.string.downloading,
                "drawable/ic_downloading"),
        DOWNLOADED("DOWNLOAD-COMPLETE", R.string.downloaded,
                "drawable/ic_done_outline"),
        FAVORITE("FAVORITE", R.string.favorite,
                "drawable/ic_favorite"),
        FAVORITED("FAVORITED", R.string.favorited,
                "drawable/ic_favorited"),
        ADD_TO_QUEUE("ADD_TO_QUEUE", R.string.add_to_queue,
                "drawable/ic_playlist_add_check"),
        REMOVE_FROM_QUEUE("REMOVE_FROM_QUEUE", R.string.remove_from_queue,
                "drawable/ic_playlist_remove"),
        ERROR_ACTION("ERROR_ACTION", R.string.error_action,
                "drawable/ic_close"),
        BROWSE_ACTION("BROWSE_ACTION", R.string.browse_action,
                "drawable/ic_subdirectory_arrow_left"),
        PBV_ACTION("PBV_ACTION", R.string.pbv_action,
                "drawable/ic_queue_music");


        public final String mId;
        public final int mLabelResId;
        public final String mIcon;

        TmaBrowseAction(String id, int labelResId, String icon) {
            mId = CUSTOM_ACTION_PREFIX + id;
            mLabelResId = labelResId;
            mIcon = TmaPublicProvider.buildUriString(icon);
        }

        public static TmaBrowseAction getActionById(String actionId){
            for(TmaBrowseAction action: TmaBrowseAction.values()){
                if(action.mId.equals(actionId)){
                    return action;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "TmaBrowseActions{" +
                    "mId='" + mId + '\'' +
                    ", mNameId=" + mLabelResId +
                    ", mIcon='" + mIcon + '\'' +
                    '}';
        }
    }

    private final int mFlags;
    private final MediaMetadataCompat mMediaMetadata;
    private final ContentStyle mPlayableStyle;
    private final ContentStyle mBrowsableStyle;
    private final ContentStyle mSingleItemStyle;
    private final int mSelfUpdateMs;

    /** Read only list, doesn't contain the children from {@link #mInclude}. */
    private final List<TmaMediaItem> mChildren;

    /** Read only list. */
    final List<TmaCustomAction> mCustomActions;
    /** Read only list. Events triggered when starting the playback. */
    final List<TmaMediaEvent> mMediaEvents;
    /** References another json file where to get extra children from. */
    final String mInclude;
    /** List of browse custom actions */
    final List<String> mBrowseActions;

    int mHearts;
    int mRevealCounter;
    boolean mIsHidden = false;


    public TmaMediaItem(int flags, ContentStyle playableStyle, ContentStyle browsableStyle,
            ContentStyle singleItemStyle, MediaMetadataCompat metadata, int selfUpdateMs,
            List<TmaCustomAction> customActions, List<String> browseActions,
            List<TmaMediaEvent> mediaEvents, List<TmaMediaItem> children, String include) {
        mFlags = flags;
        mPlayableStyle = playableStyle;
        mBrowsableStyle = browsableStyle;
        mSingleItemStyle = singleItemStyle;
        mMediaMetadata = metadata;
        mSelfUpdateMs = selfUpdateMs;
        mCustomActions = Collections.unmodifiableList(customActions);
        mBrowseActions = browseActions;
        mMediaEvents = Collections.unmodifiableList(mediaEvents);
        mInclude = include;
        mChildren = Collections.unmodifiableList(children);
    }

    int getSelfUpdateDelay() {
        return mSelfUpdateMs;
    }

    boolean testFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    int getFlags() {
        return mFlags;
    }

    List<TmaMediaItem> getChildren() {
        return mChildren;
    }

    String getMediaId() {
        return mMediaMetadata.getString(METADATA_KEY_MEDIA_ID);
    }

    String getPath(String parentPath) {
        return parentPath + getMediaId() + TREE_PATH_SEPARATOR;
    }

    /** Returns -1 if the duration key is unspecified or <= 0. */
    long getDuration() {
        long result = mMediaMetadata.getLong(METADATA_KEY_DURATION);
        if (result <= 0) return -1;
        return result;
    }

    void updateSessionMetadata(TmaLibrary lib, MediaSessionCompat session) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder(mMediaMetadata);
        selectLink(lib, builder, KEY_SUBTITLE_LINK_MEDIA_ID);
        selectLink(lib, builder, KEY_DESCRIPTION_LINK_MEDIA_ID);
        session.setMetadata(builder.build());
    }

    @SuppressLint("WrongConstant")
    MediaBrowserCompat.MediaItem toSessionItem(@NonNull String mediaPath) {
        return new MediaBrowserCompat.MediaItem(buildDescription(mediaPath), getFlags());
    }

    /**
     * Replace old action with new action if old actions exists in actions list, if old action not
     * found, add new action to front of list.
     */
    public void replaceAction(TmaBrowseAction oldAction, TmaBrowseAction newAction) {
        int oldActionIndex = mBrowseActions.indexOf(oldAction.mId);
        if (oldActionIndex > -1) {
            mBrowseActions.remove(oldActionIndex);
            mBrowseActions.add(oldActionIndex, newAction.mId);
        } else {
            mBrowseActions.add(newAction.mId);
        }
    }

    MediaDescriptionCompat buildDescription(@NonNull String parentPath) {

        // Use the default media description but add our extras.
        MediaDescriptionCompat metadataDescription = mMediaMetadata.getDescription();

        MediaDescriptionCompat.Builder bob = new MediaDescriptionCompat.Builder();
        bob.setMediaId(getPath(parentPath));
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

        extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                mPlayableStyle.mBundleValue);
        extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                mBrowsableStyle.mBundleValue);
        extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM,
                mSingleItemStyle.mBundleValue);

        int playbackStatus = (int) mMediaMetadata.getBundle().getLong(METADATA_KEY_PLAYBACK_STATUS,
                2);
        double playbackProgress = mMediaMetadata.getBundle().getLong(METADATA_KEY_PLAYBACK_PROGRESS,
                -1) / 100.0;

        extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS, playbackStatus);
        extras.putDouble(MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE,
                playbackProgress);
        if (mMediaMetadata.containsKey(MediaConstants.METADATA_KEY_IS_EXPLICIT)) {
            extras.putLong(MediaConstants.METADATA_KEY_IS_EXPLICIT,
                    mMediaMetadata.getLong(MediaConstants.METADATA_KEY_IS_EXPLICIT));
        }

        if(mBrowseActions != null && !mBrowseActions.isEmpty()){
            extras.putStringArrayList(BROWSE_CUSTOM_ACTIONS_ITEM_LIST,
                    new ArrayList<>(mBrowseActions));
        }

        bob.setExtras(extras);
        return bob.build();
    }

    /**
     * TLdr: selects the first link that works with the current config.
     *
     * The same file can be included in multiple places, so most links start with _ROOT_ and will
     * resolved to the current root node (eg: _ROOT_#advanced#art nodes#art_nature_files#).
     *
     * However with Queue Only, the current root is empty, so links need to point explicitly to a
     * specific file (eg: media_items/album_art/art_nodes.json#art_nature_files#).
     *
     * However, an absolutely linked item and its "corresponding item in a non empty browse tree
     * are not recognised as being the same because their ids are different, which lets CarMediaApp
     * push the node twice on the stack...
     *
     * Soo.. the json files usually include both link forms separated by {@link #MULTI_ID_SEPARATOR}
     * and this method selects the fist form that works.
     */
    private void selectLink(@NonNull TmaLibrary lib, MediaMetadataCompat.Builder bob, String key) {
        String value = mMediaMetadata.getString(key);
        if (!TextUtils.isEmpty(value)) {
            String[] idsList = value.split("\\" + MULTI_ID_SEPARATOR);
            for (String mediaId : idsList) {
                if (lib.getMediaItemById(mediaId) != null) {
                    bob.putString(key, mediaId);
                    return;
                }
            }
        }
    }
}
