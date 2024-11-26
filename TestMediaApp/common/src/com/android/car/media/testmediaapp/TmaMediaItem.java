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

import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Our internal representation of media items. */
public class TmaMediaItem {

    public static final String CUSTOM_BROWSE_ACTION_PREFIX =
            TmaMediaItem.class.getName() + "_Action";

    public static final String CUSTOM_PLAYBACK_ACTION_PREFIX = TmaCustomAction.class.getName();

    /**
     * The character used to separate short media ids (returned by {@link #getMediaId} from
     * different {@link TmaMediaItem} instances to form a full path. See {@link #getPath}.
     */
    public static final char TREE_PATH_SEPARATOR = '#';

    /** Separates multiple media ids (eg: in links). See {@link #selectLink} */
    public static final char MULTI_ID_SEPARATOR = '|';

    /** Regroups a TmaMediaItem and the id (ie full path) of its parent in the browse tree. */
    public static class TmaBrowsedMediaItem {
        public final @NonNull TmaMediaItem mItem;
        public final @NonNull String mParentId;

        protected TmaBrowsedMediaItem(@NonNull TmaMediaItem item, @NonNull String parentId) {
            mItem = item;
            mParentId = parentId;
        }
    }

    /** The name of each entry is the value used in the json file. */
    public enum ContentStyle {
        NONE,
        LIST,
        GRID,
        LIST_CATEGORY,
        GRID_CATEGORY,
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
            mId = CUSTOM_BROWSE_ACTION_PREFIX + id;
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

    public enum ValueType {
        INT,
        LONG,
        TEXT,
        URI,
        DOUBLE,
    }

    /** The name of each entry is the key used in the json file. */
    public enum MetadataKey {
        TITLE(ValueType.TEXT),
        ARTIST(ValueType.TEXT),
        DURATION(ValueType.LONG),
        ALBUM(ValueType.TEXT),
        AUTHOR(ValueType.TEXT),
        WRITER(ValueType.TEXT),
        COMPOSER(ValueType.TEXT),
        COMPILATION(ValueType.TEXT),
        DATE(ValueType.TEXT),
        YEAR(ValueType.LONG),
        GENRE(ValueType.TEXT),
        TRACK_NUMBER(ValueType.LONG),
        NUM_TRACKS(ValueType.LONG),
        DISC_NUMBER(ValueType.LONG),
        ALBUM_ARTIST(ValueType.TEXT),
        ART_URI(ValueType.URI),
        ALBUM_ART_URI(ValueType.URI),
        DISPLAY_TITLE(ValueType.TEXT),
        DISPLAY_SUBTITLE(ValueType.TEXT),
        DISPLAY_DESCRIPTION(ValueType.TEXT),
        DISPLAY_ICON_URI(ValueType.URI),
        GROUP_TITLE(ValueType.TEXT),
        MEDIA_ID(ValueType.TEXT),
        BT_FOLDER_TYPE(ValueType.LONG),
        MEDIA_URI(ValueType.URI),
        ADVERTISEMENT(ValueType.LONG),
        DOWNLOAD_STATUS(ValueType.LONG),
        PLAYBACK_PROGRESS(ValueType.DOUBLE),
        PLAYBACK_STATUS(ValueType.INT),
        EXPLICIT(ValueType.LONG),
        SUBTITLE_LINK_MEDIA_ID(ValueType.TEXT),
        DESCRIPTION_LINK_MEDIA_ID(ValueType.TEXT),
        IMMERSIVE_AUDIO(ValueType.LONG),
        FORMAT_TINTABLE_LARGE_ICON(ValueType.URI),
        FORMAT_TINTABLE_SMALL_ICON(ValueType.URI),
        EXCLUDE_ITEM_IN_MIXED_LIST(ValueType.LONG),
        ;

        /** The type of the key's value in {@link MediaMetadataCompat}. */
        public final @NonNull ValueType mKeyType;

        MetadataKey(@NonNull ValueType valueType) {
            mKeyType = valueType;
        }
    }

    public static class TmaMetadata {
        private final Map<MetadataKey, Object> mMap = new HashMap<>();

        public Set<MetadataKey> getKeys() {
            return mMap.keySet();
        }

        public String getString(MetadataKey key) {
            return (String) mMap.get(key);
        }

        public Long getLong(MetadataKey key) {
            return (Long) mMap.get(key);
        }

        public Double getDouble(MetadataKey key) {
            return (Double) mMap.get(key);
        }

        public void putString(MetadataKey key, String value) {
            mMap.put(key, value);
        }

        public void putLong(MetadataKey key, Long value) {
            mMap.put(key, value);
        }

        public void putDouble(MetadataKey key, Double value) {
            mMap.put(key, value);
        }
    }

    public final boolean mIsBrowsable;
    public final boolean mIsPlayable;
    public final TmaMetadata mMediaMetadata;
    public final ContentStyle mPlayableStyle;
    public final ContentStyle mBrowsableStyle;
    public final ContentStyle mSingleItemStyle;
    private final int mSelfUpdateMs;

    /** Read only list, doesn't contain the children from {@link #mInclude}. */
    private final List<TmaMediaItem> mChildren;

    /** Read only list. */
    public final List<TmaCustomAction> mCustomActions;
    /** Read only list. Events triggered when starting the playback. */
    public final List<TmaMediaEvent> mMediaEvents;
    /** References another json file where to get extra children from. */
    final String mInclude;
    /** List of browse custom actions */
    public final List<String> mBrowseActions;
    /** List of icon uris. */
    public final ArrayList<Uri> mIndicatorIcons;

    private int mHearts;
    public int mSubscribeCount;
    public int mRevealCounter;
    public TmaBrowserDelegate.UpdateNodeTask mUpdateNodeTask;
    public boolean mIsHidden = false;


    public TmaMediaItem(boolean isBrowsable, boolean isPlayable, ContentStyle playableStyle,
            ContentStyle browsableStyle, ContentStyle singleItemStyle,
            TmaMetadata metadata, int selfUpdateMs,
            List<TmaCustomAction> customActions, List<String> browseActions,
            ArrayList<Uri> iconsUris, List<TmaMediaEvent> mediaEvents, List<TmaMediaItem> children,
            String include) {
        mIsBrowsable = isBrowsable;
        mIsPlayable = isPlayable;
        mPlayableStyle = playableStyle;
        mBrowsableStyle = browsableStyle;
        mSingleItemStyle = singleItemStyle;
        mMediaMetadata = metadata;
        mSelfUpdateMs = selfUpdateMs;
        mCustomActions = Collections.unmodifiableList(customActions);
        mBrowseActions = browseActions;
        mIndicatorIcons = iconsUris;
        mMediaEvents = Collections.unmodifiableList(mediaEvents);
        mInclude = include;
        mChildren = Collections.unmodifiableList(children);
    }

    public int getSelfUpdateDelay() {
        return mSelfUpdateMs;
    }

    List<TmaMediaItem> getChildren() {
        return mChildren;
    }

    public String getMediaId() {
        return mMediaMetadata.getString(MetadataKey.MEDIA_ID);
    }

    public String getTitle() {
        MetadataKey[] keys = {MetadataKey.DISPLAY_TITLE, MetadataKey.TITLE};
        for (MetadataKey key : keys) {
            String value = mMediaMetadata.getString(key);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return null;
    }

    public String getPath(String parentPath) {
        return parentPath + getMediaId() + TREE_PATH_SEPARATOR;
    }

    /** Returns -1 if the duration key is unspecified or <= 0. */
    public long getDuration() {
        Long result = mMediaMetadata.getLong(MetadataKey.DURATION);
        if ((result == null) || (result <= 0)) return -1;
        return result;
    }

    public void offsetHearts(int offset) {
        mHearts += offset;
        mMediaMetadata.putString(MetadataKey.DISPLAY_DESCRIPTION,
                (mHearts != 0) ? " " + mHearts + " ❤ " : "");
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
    public static String selectLink(@NonNull TmaLibrary lib, String value) {
        if (!TextUtils.isEmpty(value)) {
            String[] idsList = value.split("\\" + MULTI_ID_SEPARATOR);
            for (String mediaId : idsList) {
                if (lib.getMediaItemById(mediaId) != null) {
                    return mediaId;
                }
            }
        }
        return null;
    }

    public String selectLink(@NonNull TmaLibrary lib, MetadataKey key) {
        return selectLink(lib, mMediaMetadata.getString(key));
    }

}
