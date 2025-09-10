/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.ADD_TO_QUEUE;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOAD;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOADED;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOADING;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.FAVORITE;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.FAVORITED;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.REMOVE_FROM_QUEUE;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaLoginEventOrder.PLAYBACK_STATE_UPDATE_FIRST;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsParser;

import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.analytics.AnalyticsHandler;
import com.android.car.media.testmediaapp.loader.TmaLoader;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAnalyticsState;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaReplyDelay;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaSearchMode;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Base class that shares code to browse TMA data independently of the api media api (v1 / v3) used.
 */
@OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
public abstract class TmaBrowserDelegate {
    private static final String TAG = "TmaBrowserDelegate";

    private static final int MAX_SEARCH_DEPTH = 4;

    private Context mContext;
    protected TmaPrefs mPrefs;
    protected Handler mHandler;
    protected TmaLibrary mLibrary;
    protected AnalyticsHandler mAnalyticsHandler;

    public TmaBrowserDelegate(@NonNull Context context) {
        mContext = context;
        mPrefs = TmaPrefs.getInstance(context);
        mHandler = new Handler(context.getMainLooper());
        mAnalyticsHandler = new AnalyticsHandler(context.getApplicationContext());

        mLibrary = new TmaLibrary(new TmaLoader(context));
    }

    protected void addPrefsListeners() {
        mPrefs.mAccountType.registerChangeListener(mOnAccountChanged);
        mPrefs.mRootNodeType.registerChangeListener(mOnRootNodeTypeChanged);
        mPrefs.mRootReplyDelay.registerChangeListener(mOnReplyDelayChanged);
        mPrefs.mSearchMode.registerChangeListener(mOnSearchModeChanged);
        mPrefs.mAnalyticsState.registerChangeListener(mOnAnalyticsChanged);
    }

    public Context getContext() {
        return mContext;
    }

    protected boolean canSearch() {
        return (mPrefs.mSearchMode.getValue() == TmaSearchMode.ENABLED);
    }

    public void onDestroy() {
        mPrefs.mAccountType.unregisterChangeListener(mOnAccountChanged);
        mPrefs.mRootNodeType.unregisterChangeListener(mOnRootNodeTypeChanged);
        mPrefs.mRootReplyDelay.unregisterChangeListener(mOnReplyDelayChanged);
        mPrefs.mSearchMode.unregisterChangeListener(mOnSearchModeChanged);
        mPrefs.mAnalyticsState.unregisterChangeListener(mOnAnalyticsChanged);

        mHandler = null;
        mPrefs = null;
    }

    private final TmaPrefs.PrefValueChangedListener<TmaAccountType> mOnAccountChanged =
            (oldValue, newValue) -> {
                if (PLAYBACK_STATE_UPDATE_FIRST.equals(mPrefs.mLoginEventOrder.getValue())) {
                    setAccountType(newValue);
                } else {
                    (new Handler()).postDelayed(() -> setAccountType(newValue), 3000);
                }
                invalidateRoot();
            };

    private final TmaPrefs.PrefValueChangedListener<TmaBrowseNodeType> mOnRootNodeTypeChanged =
            (oldValue, newValue) -> {
                invalidateRoot();
                mLibrary.setBrowseRoot(newValue);
            };

    private final TmaPrefs.PrefValueChangedListener<TmaReplyDelay> mOnReplyDelayChanged =
            (oldValue, newValue) -> invalidateRoot();

    private final TmaPrefs.PrefValueChangedListener<TmaSearchMode> mOnSearchModeChanged =
            this::onSearchModeChanged;

    private final TmaPrefs.PrefValueChangedListener<TmaAnalyticsState> mOnAnalyticsChanged =
            this::onAnalyticsChanged;

    protected abstract void setAccountType(@NonNull TmaAccountType accountType);

    protected abstract void invalidateRoot();

    /**
     * Notifies that the children of the media1/media3 node with the given id has changed.
     * @param parentId : the TMA (full) path for that node.
     */
    protected abstract void notifyChildrenChanged(@NonNull String parentId);

    protected abstract void onSearchModeChanged(@NonNull TmaSearchMode oldValue,
            @NonNull TmaSearchMode newValue);

    protected abstract void onAnalyticsChanged(
            @NonNull TmaAnalyticsState oldValue, @NonNull TmaAnalyticsState newValue);

    public String stringify(@Nullable Bundle bundle) {
        if (bundle == null) {
            return "null bundle!";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : bundle.keySet()) {
            String shortKey = key.substring(key.lastIndexOf('.'));
            builder.append("\n").append(shortKey).append(": ").append(bundle.get(key));
        }
        return builder.toString();
    }

    private void addSearchResults(
            @NonNull String mediaPath, @Nullable TmaMediaItem node, Matcher matcher,
            List<TmaBrowsedMediaItem> results, int currentDepth) {
        if (node == null || currentDepth <= 0) {
            return;
        }

        for (TmaMediaItem child : mLibrary.getAllChildren(node)) {
            if (child.mIsHidden) {
                continue;
            }
            String title = child.getTitle();
            if (title != null) {
                matcher.reset(title);
                if (matcher.find()) {
                    results.add(new TmaBrowsedMediaItem(results.size(), child, mediaPath));
                }
            }
            addSearchResults(child.getPath(mediaPath), child, matcher, results, currentDepth - 1);
        }
    }

    void toggleItem(@NonNull String mediaId) {
        TmaMediaItem item = mLibrary.getMediaItemById(mediaId);
        if (item == null) {
            Log.e(TAG, "toggleItem can't find: " + mediaId);
            return;
        }
        item.mIsHidden = !item.mIsHidden;

        String parentId = (item == mLibrary.getRoot()) ? mediaId : mLibrary.getParentPath(mediaId);
        notifyChildrenChanged(parentId);
    }

    protected @Nullable List<TmaBrowsedMediaItem> getMediaItems(
            @NonNull String parentId, @Nullable String filter) {
        if (TmaAccountType.NONE.equals(mPrefs.mAccountType.getValue())) {
            Log.w(TAG, "getMediaItems: no account selected. " + parentId);
            return null;
        }

        List<TmaBrowsedMediaItem> result = null;
        TmaMediaItem node = mLibrary.getMediaItemById(parentId);
        if (node == null) {
            Log.e(TAG, "Node not found: " + parentId);
        } else if (node.mIsHidden) {
            Log.w(TAG, "getMediaItems: node is hidden. " + parentId);
        } else if (filter != null) {
            result = new ArrayList<>();
            Pattern pat = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE);
            addSearchResults(parentId, node, pat.matcher(""), result, MAX_SEARCH_DEPTH);
        } else {
            result = new ArrayList<>();
            List<TmaMediaItem> children = mLibrary.getAllChildren(node);
            int childrenCount = children.size();
            if (childrenCount > 0) {
                int selfUpdateDelay = node.getSelfUpdateDelay();
                int childrenToShow = (selfUpdateDelay > 0) ? node.mRevealCounter : childrenCount;
                for (int childIndex = 0; childIndex < childrenToShow; childIndex++) {
                    TmaMediaItem child = children.get(childIndex);
                    if (child.mIsHidden) {
                        continue;
                    }
                    result.add(new TmaBrowsedMediaItem(result.size(), child, parentId));
                }
            }
        }
        return result;
    }

    public void onSubscriptionDelta(@NonNull String parentId, int delta) {
        TmaMediaItem node = mLibrary.getMediaItemById(parentId);
        if ((node == null) || (node.getSelfUpdateDelay() <= 0)) {
            return;
        }
        node.mSubscribeCount = node.mSubscribeCount + delta;
        if ((node.mUpdateNodeTask == null) && (node.mSubscribeCount > 0)) {
            node.mUpdateNodeTask = new UpdateNodeTask(parentId, node);
            mHandler.postDelayed(node.mUpdateNodeTask, node.getSelfUpdateDelay());
        }
        if ((node.mUpdateNodeTask != null) && (node.mSubscribeCount <= 0)) {
            mHandler.removeCallbacks(node.mUpdateNodeTask);
            node.mUpdateNodeTask = null;
        }
    }

    public class UpdateNodeTask implements Runnable {

        private final String mNodePath;
        private final TmaMediaItem mItem;

        public UpdateNodeTask(@NonNull String nodePath, @NonNull TmaMediaItem item) {
            mNodePath = nodePath;
            mItem = item;
        }

        @Override
        public void run() {
            notifyChildrenChanged(mNodePath);

            int selfUpdateDelay = mItem.getSelfUpdateDelay();
            int childrenCount = mLibrary.getAllChildren(mItem).size();
            if (selfUpdateDelay > 0 && childrenCount > 0) {
                mHandler.postDelayed(this, selfUpdateDelay);
                mItem.mRevealCounter = (mItem.mRevealCounter + 1) % (childrenCount + 1);
            }
        }
    }

    protected abstract void addItemToQueue(String mediaId);
    protected abstract void removeItemFromQueue(String mediaId);

    protected interface CustomActionResultHelper {
        void sendResult(@NonNull Bundle extras);
        void sendProgressUpdate(@NonNull Bundle extras);
        void sendError(@NonNull Bundle extras);
        default void detach() {}
    }

    /** Base code to handle custom actions. */
    public void handleCustomAction(String action, Bundle extras, CustomActionResultHelper result) {
        // Handle analytics.
        if (AnalyticsParser.isAnalyticsAction(action)) {
            AnalyticsParser.parseAnalyticsAction(action, extras, mAnalyticsHandler);
            result.sendResult(Bundle.EMPTY);
            return;
        }

        // Handle non-analytics actions.
        String mediaId = extras.getString(BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID);
        TmaBrowseAction browseAction = TmaBrowseAction.getActionById(action);
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (browseAction == null || node == null) {
            Bundle resultBundle = new Bundle();
            Log.e(TAG, "onCustomAction invalid action or node");
            result.sendError(resultBundle);
            return;
        }
        result.detach();
        Context context = getContext();
        switch (browseAction) {
            case DOWNLOAD:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .sendTo(DOWNLOAD, result::sendProgressUpdate)
                        .onComplete(() -> node.replaceAction(DOWNLOAD, DOWNLOADING))
                        .send();
                new ActionResultSender(context, mHandler).setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_download_complete)
                        .sendToDelayed(DOWNLOADING, 5_000, result::sendResult)
                        .onComplete(() -> node.replaceAction(DOWNLOADING, DOWNLOADED))
                        .send();
                break;
            case DOWNLOADING:
            case DOWNLOADED:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_download_removed)
                        .sendTo(DOWNLOADING, result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, DOWNLOAD))
                        .send();
                break;
            case FAVORITE:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_added_favorite)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, FAVORITED))
                        .send();
                break;
            case FAVORITED:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_removed_favorite)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, FAVORITE))
                        .send();
                break;
            case ADD_TO_QUEUE:
                // show PBV with mediaItem
                addItemToQueue(mediaId);

                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(ADD_TO_QUEUE, REMOVE_FROM_QUEUE))
                        .send();
                break;
            case REMOVE_FROM_QUEUE:
                // Show PBV without Media Item
                removeItemFromQueue(mediaId);

                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(REMOVE_FROM_QUEUE, ADD_TO_QUEUE))
                        .send();
                break;
            case ERROR_ACTION:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_error)
                        .sendTo(result::sendError)
                        .send();
                break;
            case BROWSE_ACTION:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setBrowseNode(mediaId)
                        .sendTo(result::sendResult)
                        .send();
                break;
            case PBV_ACTION:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .send();
                break;
            default:
                new ActionResultSender(context, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_invalid)
                        .sendTo(result::sendError)
                        .send();
        }
    }
}
