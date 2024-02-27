/*
 * Copyright (c) 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.media.testmediaapp;

import static androidx.media.utils.MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED;
import static com.android.car.media.testmediaapp.TmaLibrary.ROOT_PATH;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.ADD_TO_QUEUE;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOAD;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOADED;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.DOWNLOADING;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.FAVORITE;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.FAVORITED;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.REMOVE_FROM_QUEUE;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.getActionById;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.values;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_ICON;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_ID;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_LABEL;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ROOT_LIST;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.ANALYTICS_ON;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.SHARE_GOOGLE;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.SHARE_OEM;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType.LEAF_CHILDREN;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType.QUEUE_ONLY;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaLoginEventOrder.PLAYBACK_STATE_UPDATE_FIRST;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsParser;
import androidx.car.app.mediaextensions.analytics.client.RootHintsPopulator;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction;
import com.android.car.media.testmediaapp.analytics.AnalyticsHandler;
import com.android.car.media.testmediaapp.loader.TmaLoader;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAnalyticsState;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaReplyDelay;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Implementation of {@link MediaBrowserServiceCompat} that delivers {@link MediaItem}s based on
 * json configuration files stored in the application's assets. Those assets combined with a few
 * preferences (see: {@link TmaPrefs}), allow to create a variety of use cases (including error
 * states) to stress test the Car Media application. <p/>
 * The media items are cached in the {@link TmaLibrary}, and can be virtually played with
 * {@link TmaPlayer}.
 */
@OptIn(markerClass = androidx.car.app.annotations2.ExperimentalCarApi.class)
public class TmaBrowser extends MediaBrowserServiceCompat {
    private static final String TAG = "TmaBrowser";

    private static final int MAX_SEARCH_DEPTH = 4;
    private static final String MEDIA_SESSION_TAG = "TEST_MEDIA_SESSION";

    // TODO(b/235362454): remove this once it's available in MediaConstants
    private static final String FAVORITES_MEDIA_ITEM =
            "androidx.media.BrowserRoot.Extras.FAVORITES_MEDIA_ITEM";
    /** Extras key to allow Android Auto to identify the browse service from the media session. */
    private static final String BROWSE_SERVICE_FOR_SESSION_KEY =
            "android.media.session.BROWSE_SERVICE";

    private TmaPrefs mPrefs;
    private Handler mHandler;
    private MediaSessionCompat mSession;
    private TmaLibrary mLibrary;
    private TmaPlayer mPlayer;
    private BrowserRoot mRoot;
    private AnalyticsHandler mAnalyticsHandler;

    public TmaBrowser() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = TmaPrefs.getInstance(this);
        mHandler = new Handler(getMainLooper());
        mAnalyticsHandler = new AnalyticsHandler(getApplicationContext());

        ComponentName mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(this);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mbrComponent);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE);

        mSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG, mbrComponent, mbrIntent);
        setSessionToken(mSession.getSessionToken());

        mLibrary = new TmaLibrary(new TmaLoader(this));
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlayer = new TmaPlayer(this, mLibrary, audioManager, mHandler, mSession);

        mSession.setCallback(mPlayer);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Bundle mediaSessionExtras = new Bundle();
        mediaSessionExtras.putString(BROWSE_SERVICE_FOR_SESSION_KEY, TmaBrowser.class.getName());
        mSession.setExtras(mediaSessionExtras);

        mPrefs.mAccountType.registerChangeListener(mOnAccountChanged);
        mPrefs.mRootNodeType.registerChangeListener(mOnRootNodeTypeChanged);
        mPrefs.mRootReplyDelay.registerChangeListener(mOnReplyDelayChanged);
        mPrefs.mAnalyticsState.registerChangeListener(mOnAnalyticsChanged);

        updateRootExtras();
    }

    private void updateRootExtras() {
        Bundle browserRootExtras = new Bundle();
        browserRootExtras.putParcelableArrayList(
                BROWSE_CUSTOM_ACTIONS_ROOT_LIST, new ArrayList<>(createCustomActionsList()));

        browserRootExtras.putBoolean(BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true);

        browserRootExtras.putParcelable(FAVORITES_MEDIA_ITEM, getFavoritesMediaItem());

        Set<String> flags = mPrefs.mAnalyticsState.getValue().getFlags();
        new RootHintsPopulator(browserRootExtras)
            .setAnalyticsOptIn(flags.contains(ANALYTICS_ON.getId()))
            .setSharePlatform(flags.contains(SHARE_GOOGLE.getId()))
            .setShareOem(flags.contains(SHARE_OEM.getId()));

        mRoot = new BrowserRoot(ROOT_PATH, browserRootExtras);
        updatePlaybackState(mPrefs.mAccountType.getValue());
    }

    private List<Bundle> createCustomActionsList() {
        ArrayList<Bundle> browseActionsBundle = new ArrayList<>();
        for (TmaBrowseAction browseAction : values()) {
            Bundle action = new Bundle();
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_ID, browseAction.mId);
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_LABEL,
                    getString(browseAction.mLabelResId));
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_ICON, browseAction.mIcon);
            Bundle bundle = new Bundle();
            action.putBundle(BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS, bundle);
            browseActionsBundle.add(action);
        }
        return browseActionsBundle;
    }

    @Override
    public void onDestroy() {
        mPrefs.mAccountType.unregisterChangeListener(mOnAccountChanged);
        mPrefs.mRootNodeType.unregisterChangeListener(mOnRootNodeTypeChanged);
        mPrefs.mRootReplyDelay.unregisterChangeListener(mOnReplyDelayChanged);
        mPrefs.mAnalyticsState.unregisterChangeListener(mOnAnalyticsChanged);
        mSession.release();
        mHandler = null;
        mPrefs = null;
        super.onDestroy();
    }

    private final TmaPrefs.PrefValueChangedListener<TmaAccountType> mOnAccountChanged =
            (oldValue, newValue) -> {
                if (PLAYBACK_STATE_UPDATE_FIRST.equals(mPrefs.mLoginEventOrder.getValue())) {
                    updatePlaybackState(newValue);
                } else {
                    (new Handler()).postDelayed(() -> updatePlaybackState(newValue), 3000);
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

    private final TmaPrefs.PrefValueChangedListener<TmaAnalyticsState> mOnAnalyticsChanged =
            (oldValue, newValue) -> {
                updateRootExtras();
                invalidateRoot();
                Log.v(TAG, "AnalyticsMode: " + newValue.toString());
            };


    private void updatePlaybackState(TmaAccountType accountType) {
        if (accountType == TmaAccountType.NONE) {
            mSession.setMetadata(null);
            mPlayer.onStop();
            mPlayer.setPlaybackState(
                    new TmaMediaEvent(TmaMediaEvent.EventState.ERROR,
                            TmaMediaEvent.StateErrorCode.AUTHENTICATION_EXPIRED,
                            getResources().getString(R.string.no_account),
                            getResources().getString(R.string.select_account),
                            TmaMediaEvent.ResolutionIntent.PREFS,
                            TmaMediaEvent.Action.NONE, 0, null, null));
        } else {
            // TODO don't reset error in all cases...
            PlaybackStateCompat.Builder playbackState = new PlaybackStateCompat.Builder();
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
            playbackState.setActions(PlaybackStateCompat.ACTION_PREPARE);
            mSession.setPlaybackState(playbackState.build());
        }
    }

    private void invalidateRoot() {
        if (mRoot != null && mRoot.getExtras() != null) {
            mSession.setExtras(mRoot.getExtras());
        }
        notifyChildrenChanged(ROOT_PATH);
    }

    private String stringify(Bundle bundle) {
        StringBuilder builder = new StringBuilder();
        for (String key : bundle.keySet()) {
            String shortKey = key.substring(key.lastIndexOf('.'));
            builder.append("\n").append(shortKey).append(": ").append(bundle.get(key));
        }
        return builder.toString();
    }

    @Override
    public BrowserRoot onGetRoot(
            @NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        if (rootHints == null) {
            Log.w(TAG, "Client " + clientPackageName + " didn't set rootHints.");
        }
        Log.i(TAG, "onGetRoot client: " + clientPackageName + " Hints: " + stringify(rootHints));
        return mRoot;
    }


    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result) {
        onLoadChildren(parentId, result, new Bundle());
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result,
            @NonNull Bundle options) {
        Log.i(TAG, "onLoadChildren parentId: " + parentId + " Options: " + stringify(options));

        getMediaItemsWithDelay(parentId, result, null);

        if (QUEUE_ONLY.equals(mPrefs.mRootNodeType.getValue()) && ROOT_PATH.equals(parentId)) {
            mPlayer.buildQueue(mLibrary.getPath(LEAF_CHILDREN));
            mPlayer.setActiveQueueItem(null);
            mPlayer.prepareActiveItem();
        }
    }

    @Override
    public void onLoadItem(String itemId, @NonNull Result<MediaBrowserCompat.MediaItem> result) {
        Runnable task = () -> {
            TmaMediaItem node = mLibrary.getMediaItemById(itemId);
            if (node == null) {
                result.sendResult(null);
            } else {
                result.sendResult(node.toSessionItem(mLibrary.getParentPath(itemId)));
            }
        };
        runTaskAndSendResultWithDelay(task, result);
    }

    @Override
    public void onSearch(@NonNull String query, Bundle extras,
            @NonNull Result<List<MediaItem>> result) {
        Log.i(TAG, "onSearch query: " + query + " Extras: " + stringify(extras));
        getMediaItemsWithDelay(ROOT_PATH, result, query);
    }

    private void getMediaItemsWithDelay(@NonNull String parentId,
            @NonNull Result<List<MediaItem>> result, @Nullable String filter) {
        // TODO: allow per item override of the delay ?
        Runnable task = () -> {
            TmaMediaItem node = TmaAccountType.NONE.equals(mPrefs.mAccountType.getValue()) ? null :
                    mLibrary.getMediaItemById(parentId);

            if (node == null) {
                result.sendResult(null);
            } else if (filter != null) {
                List<MediaItem> hits = new ArrayList<>(50);
                Pattern pat = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE);
                addSearchResults(parentId, node, pat.matcher(""), hits, MAX_SEARCH_DEPTH);
                result.sendResult(hits);
            } else {
                List<TmaMediaItem> children = mLibrary.getAllChildren(node);
                int childrenCount = children.size();
                List<MediaItem> items = new ArrayList<>(childrenCount);
                if (childrenCount <= 0) {
                    result.sendResult(items);
                } else {
                    int selfUpdateDelay = node.getSelfUpdateDelay();
                    int toShow = (selfUpdateDelay > 0) ? node.mRevealCounter : childrenCount;
                    for (int childIndex = 0; childIndex < toShow; childIndex++) {
                        TmaMediaItem child = children.get(childIndex);
                        if (child.mIsHidden) {
                            continue;
                        }
                        items.add(child.toSessionItem(parentId));
                    }
                    result.sendResult(items);

                    if (selfUpdateDelay > 0) {
                        mHandler.postDelayed(new UpdateNodeTask(parentId), selfUpdateDelay);
                        node.mRevealCounter = (node.mRevealCounter + 1) % (childrenCount);
                    }
                }
            }
        };
        runTaskAndSendResultWithDelay(task, result);
    }

    private void runTaskAndSendResultWithDelay(Runnable task, @NonNull Result<?> result) {
        TmaReplyDelay delay = mPrefs.mRootReplyDelay.getValue();
        if (delay == TmaReplyDelay.NONE) {
            task.run();
        } else {
            result.detach();
            mHandler.postDelayed(task, delay.mReplyDelayMs);
        }
    }

    private void addSearchResults(@NonNull String mediaPath, @Nullable TmaMediaItem node,
            Matcher matcher, List<MediaItem> hits, int currentDepth) {
        if (node == null || currentDepth <= 0) {
            return;
        }

        for (TmaMediaItem child : mLibrary.getAllChildren(node)) {
            if (child.mIsHidden) {
                continue;
            }
            MediaItem item = child.toSessionItem(mediaPath);
            CharSequence title = item.getDescription().getTitle();
            if (title != null) {
                matcher.reset(title);
                if (matcher.find()) {
                    hits.add(item);
                }
            }
            addSearchResults(child.getPath(mediaPath), child, matcher, hits, currentDepth - 1);
        }
    }

    void toggleItem(@NonNull String mediaId) {
        TmaMediaItem item = mLibrary.getMediaItemById(mediaId);
        if (item == null) {
            Log.e(TAG, "toggleItem can't find: " + mediaId);
            return;
        }
        item.mIsHidden = !item.mIsHidden;

        notifyChildrenChanged(mLibrary.getParentPath(mediaId));
    }

    private MediaBrowser.MediaItem getFavoritesMediaItem() {
        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setMediaId("favorites"); // corresponds to "favorites.json"
        builder.setTitle(getResources().getString(R.string.favorites_title));

        return new MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE);
    }

    private class UpdateNodeTask implements Runnable {

        private final String mNodeId;

        UpdateNodeTask(@NonNull String nodeId) {
            mNodeId = nodeId;
        }

        @Override
        public void run() {
            notifyChildrenChanged(mNodeId);
        }
    }

    @Override
    public void onCustomAction(
            @NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
        handleCustomAction(action, extras, result);
    }

    private void handleCustomAction(String action, Bundle extras, Result<Bundle> result) {
        // Handle analytics.
        if (AnalyticsParser.isAnalyticsAction(action)) {
            AnalyticsParser.parseAnalyticsAction(action, extras, mAnalyticsHandler);
            result.sendResult(null);
            return;
        }

        // Handle non-analytics actions.
        String mediaId = extras.getString(BROWSE_CUSTOM_ACTIONS_MEDIA_ITEM_ID);
        TmaBrowseAction browseAction = getActionById(action);
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (browseAction == null || node == null) {
            Bundle resultBundle = new Bundle();
            Log.e(TAG, "onCustomAction invalid action or node");
            result.sendError(resultBundle);
            return;
        }
        result.detach();
        switch (browseAction) {
            case DOWNLOAD:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .sendTo(DOWNLOAD, result::sendProgressUpdate)
                        .onComplete(() -> node.replaceAction(DOWNLOAD, DOWNLOADING))
                        .send();
                new ActionResultSender(this, mHandler).setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_download_complete)
                        .sendToDelayed(DOWNLOADING, 5_000, result::sendResult)
                        .onComplete(() -> node.replaceAction(DOWNLOADING, DOWNLOADED))
                        .send();
                break;
            case DOWNLOADING:
            case DOWNLOADED:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_download_removed)
                        .sendTo(DOWNLOADING, result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, DOWNLOAD))
                        .send();
                break;
            case FAVORITE:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_added_favorite)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, FAVORITED))
                        .send();
                break;
            case FAVORITED:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_removed_favorite)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(browseAction, FAVORITE))
                        .send();
                break;
            case ADD_TO_QUEUE:
                // show PBV with mediaItem
                mPlayer.addItemToQueue(mediaId);

                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(ADD_TO_QUEUE, REMOVE_FROM_QUEUE))
                        .send();
                break;
            case REMOVE_FROM_QUEUE:
                // Show PBV without Media Item
                mPlayer.removeItemFromQueue(mediaId);

                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .onComplete(() -> node.replaceAction(REMOVE_FROM_QUEUE, ADD_TO_QUEUE))
                        .send();
                break;
            case ERROR_ACTION:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_error)
                        .sendTo(result::sendError)
                        .send();
                break;
            case BROWSE_ACTION:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setBrowseNode(mediaId)
                        .sendTo(result::sendResult)
                        .send();
                break;
            case PBV_ACTION:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setShowPlaybackView(true)
                        .sendTo(result::sendResult)
                        .send();
                break;
            default:
                new ActionResultSender(this, mHandler)
                        .setRefreshMediaId(mediaId)
                        .setMessage(R.string.action_result_string_invalid)
                        .sendTo(result::sendError)
                        .send();
        }
    }
}
