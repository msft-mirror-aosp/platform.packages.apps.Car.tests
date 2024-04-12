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


import static com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey.MEDIA_ID;
import static com.android.car.media.testmediaapp.TmaMediaItem.TREE_PATH_SEPARATOR;

import static java.util.Collections.emptyList;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.media.testmediaapp.TmaMediaItem.TmaMetadata;
import com.android.car.media.testmediaapp.loader.TmaLoader;
import com.android.car.media.testmediaapp.media1.TmaBrowser1;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Delegates the loading of {@link TmaMediaItem}s to {@link TmaLoader} and caches the results
 * for {@link TmaBrowser1}.
 */
public class TmaLibrary {

    private static final String TAG = "TmaLibrary";

    public static final String ROOT_MEDIA_ID = "_ROOT_";
    public static final String ROOT_PATH = ROOT_MEDIA_ID + TREE_PATH_SEPARATOR;

    private final TmaLoader mLoader;
    private final Map<TmaBrowseNodeType, String> mRootAssetPaths = new HashMap<>(5);

    /** Stores the root item of each loaded media asset file, keyed by the file's path. */
    private final Map<String, TmaMediaItem> mCachedFilesByPath = new HashMap<>(50);

    private TmaMediaItem mBrowseRoot;

    TmaLibrary(TmaLoader loader) {
        mLoader = loader;
        mRootAssetPaths.put(TmaBrowseNodeType.NULL, null);
        mRootAssetPaths.put(TmaBrowseNodeType.EMPTY, "media_items/empty.json");
        mRootAssetPaths.put(TmaBrowseNodeType.QUEUE_ONLY, "media_items/empty.json");
        mRootAssetPaths.put(TmaBrowseNodeType.SINGLE_TAB, "media_items/single_node.json");
        mRootAssetPaths.put(TmaBrowseNodeType.NODE_CHILDREN, "media_items/only_nodes.json");
        mRootAssetPaths.put(TmaBrowseNodeType.LEAF_CHILDREN, "media_items/simple_leaves.json");
        mRootAssetPaths.put(TmaBrowseNodeType.MIXED_CHILDREN, "media_items/mixed.json");
        mRootAssetPaths.put(TmaBrowseNodeType.UNTAGGED, "media_items/untagged.json");
        // Preload favorites into cache, as it's not necessarily accessed via the browse tree
        loadAssetFile("media_items/favorites.json");
    }

    private static TmaMediaItem newRootItem(String include) {
        TmaMetadata metaMap = new TmaMetadata();
        metaMap.putString(MEDIA_ID, ROOT_MEDIA_ID);
        return new TmaMediaItem(true, false, TmaMediaItem.ContentStyle.NONE,
                TmaMediaItem.ContentStyle.NONE, TmaMediaItem.ContentStyle.NONE, metaMap, 0,
                emptyList(), emptyList(), emptyList(), emptyList(), include);
    }

    public String getPath(TmaBrowseNodeType rootType) {
        return mRootAssetPaths.get(rootType);
    }

    void setBrowseRoot(TmaBrowseNodeType rootType) {
        String filePath = mRootAssetPaths.get(rootType);
        mBrowseRoot = (filePath != null) ? newRootItem(filePath) : null;
    }

    TmaMediaItem getRoot() {
        return mBrowseRoot;
    }

    public TmaMediaItem getBrowseRoot() {
        return mBrowseRoot;
    }

    public String getParentPath(String mediaId) {
        return staticGetParentPath(mediaId);
    }

    @VisibleForTesting
    public static String staticGetParentPath(String mediaId) {
        int len = mediaId.length();
        if (len <= 2) {
            return "";
        }
        return mediaId.substring(0, mediaId.lastIndexOf(TREE_PATH_SEPARATOR, len - 2) + 1);
    }

    /** Returns all the children of the node (explicit and included ones). */
    public List<TmaMediaItem> getAllChildren(TmaMediaItem item) {
        return getAllChildren(item, null);
    }

    public List<TmaMediaItem> getAllChildren(@Nullable TmaMediaItem item,
            @Nullable Predicate<TmaMediaItem> itemSelector) {
        if (item == null) {
            return emptyList();
        }
        // Processing includes only on request allows recursive structures :-)
        ArrayList<TmaMediaItem> children = new ArrayList<>(item.getChildren());
        if (!TextUtils.isEmpty(item.mInclude)) {
            children.addAll(loadAssetFile(item.mInclude).getChildren());
        }

        return (itemSelector == null) ? children :
                children.stream()
                        .filter(itemSelector)
                        .collect(Collectors.toList());
    }

    @Nullable
    public TmaMediaItem getMediaItemById(@Nullable String mediaId) {
        if (mediaId == null) return null;

        String[] nodeIds = mediaId.split(String.valueOf(TREE_PATH_SEPARATOR));
        if (nodeIds.length <= 0) {
            return null;
        }
        TmaMediaItem item = (ROOT_MEDIA_ID.equals(nodeIds[0])) ? mBrowseRoot :
                loadAssetFile(nodeIds[0]);
        int level = 1;
        while (item != null && level < nodeIds.length) {
            item = getChild(getAllChildren(item), nodeIds[level]);
            level++;
        }
        return item;
    }

    @Nullable
    private TmaMediaItem getChild(List<TmaMediaItem> children, String shortId) {
        for (TmaMediaItem child: children) {
            if (Objects.equals(shortId, child.getMediaId())) {
                return child;
            }
        }
        return null;
    }

    private TmaMediaItem loadAssetFile(String filePath) {
        TmaMediaItem result = mCachedFilesByPath.get(filePath);
        if (result == null) {
            result = mLoader.loadAssetFile(filePath);
            if (result != null) {
                mCachedFilesByPath.put(filePath, result);
            } else {
                Log.e(TAG, "Unable to load: " + filePath);
            }
        }
        return result;
    }
}
