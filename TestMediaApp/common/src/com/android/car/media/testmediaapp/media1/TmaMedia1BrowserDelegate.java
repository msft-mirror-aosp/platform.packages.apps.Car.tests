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

package com.android.car.media.testmediaapp.media1;

import static androidx.media.utils.MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_FAVORITES_MEDIA_ITEM;
import static androidx.media.utils.MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED;

import static com.android.car.media.testmediaapp.TmaLibrary.ROOT_PATH;
import static com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction.values;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_ICON;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_ID;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ACTION_LABEL;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ROOT_LIST;
import static com.android.car.media.testmediaapp.media1.TmaMedia1ItemExtKt.toMediaItem;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.ANALYTICS_ON;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.SHARE_GOOGLE;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.SHARE_OEM;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType.QUEUE_ONLY;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.mediaextensions.analytics.client.RootHintsPopulator;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.media.session.MediaButtonReceiver;

import com.android.car.media.testmediaapp.R;
import com.android.car.media.testmediaapp.TmaBrowserDelegate;
import com.android.car.media.testmediaapp.TmaMediaItem;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaReplyDelay;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaSearchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Handles browsing TMA data from a media1 browser.
 */
@OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
public class TmaMedia1BrowserDelegate extends TmaBrowserDelegate {
    private static final String TAG = "TMA1-BrowserDelegate";

    private static final String MEDIA_SESSION_TAG = "TEST_MEDIA1_SESSION";

    // TODO(b/235362454): remove this once it's available in MediaConstants
    private static final String FAVORITES_MEDIA_ITEM =
            "androidx.media.BrowserRoot.Extras.FAVORITES_MEDIA_ITEM";
    /** Extras key to allow Android Auto to identify the browse service from the media session. */
    private static final String BROWSE_SERVICE_FOR_SESSION_KEY =
            "android.media.session.BROWSE_SERVICE";


    private final TmaPlayer1 mPlayer;
    private MediaBrowserServiceCompat mBrowser;
    private MediaSessionCompat mSession;
    private BrowserRoot mRoot;

    public TmaMedia1BrowserDelegate(MediaBrowserServiceCompat browser) {
        super(browser);
        mBrowser = browser;

        ComponentName mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(mBrowser);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mbrComponent);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(mBrowser, 0, mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE);

        mSession = new MediaSessionCompat(mBrowser, MEDIA_SESSION_TAG, mbrComponent, mbrIntent);
        mBrowser.setSessionToken(mSession.getSessionToken());

        AudioManager audioManager = browser.getSystemService(AudioManager.class);
        mPlayer = new TmaPlayer1(this, mLibrary, audioManager, mHandler, mSession);

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Bundle mediaSessionExtras = new Bundle();
        mediaSessionExtras.putString(BROWSE_SERVICE_FOR_SESSION_KEY, TmaBrowser1.class.getName());
        mSession.setExtras(mediaSessionExtras);

        addPrefsListeners();
        updateRootExtras();
    }

    private void updateRootExtras() {
        Bundle browserRootExtras = new Bundle();
        browserRootExtras.putParcelableArrayList(
                BROWSE_CUSTOM_ACTIONS_ROOT_LIST, new ArrayList<>(createCustomActionsList()));

        browserRootExtras.putBoolean(BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, canSearch());

        browserRootExtras.putParcelable(BROWSER_SERVICE_EXTRAS_KEY_FAVORITES_MEDIA_ITEM,
                getFavoritesMediaItem());

        Set<String> flags = mPrefs.mAnalyticsState.getValue().getFlags();
        new RootHintsPopulator(browserRootExtras)
            .setAnalyticsOptIn(flags.contains(ANALYTICS_ON.getId()))
            .setSharePlatform(flags.contains(SHARE_GOOGLE.getId()))
            .setShareOem(flags.contains(SHARE_OEM.getId()));

        mRoot = new BrowserRoot(ROOT_PATH, browserRootExtras);
        setAccountType(mPrefs.mAccountType.getValue());
    }

    private List<Bundle> createCustomActionsList() {
        ArrayList<Bundle> browseActionsBundle = new ArrayList<>();
        for (TmaBrowseAction browseAction : values()) {
            Bundle action = new Bundle();
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_ID, browseAction.mId);
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_LABEL,
                    getContext().getString(browseAction.mLabelResId));
            action.putString(BROWSE_CUSTOM_ACTIONS_ACTION_ICON, browseAction.mIcon);
            Bundle bundle = new Bundle();
            action.putBundle(BROWSE_CUSTOM_ACTIONS_ACTION_EXTRAS, bundle);
            browseActionsBundle.add(action);
        }
        return browseActionsBundle;
    }

    @Override
    public void onDestroy() {
        mSession.release();
        super.onDestroy();
    }

    @Override
    protected void setAccountType(@NonNull TmaAccountType accountType) {
        mPlayer.setAccountType(accountType);
    }

    @Override
    protected void invalidateRoot() {
        if (mRoot != null && mRoot.getExtras() != null) {
            mSession.setExtras(mRoot.getExtras());
        }
        notifyChildrenChanged(ROOT_PATH);
    }

    @Override
    protected void notifyChildrenChanged(@NonNull String parentId) {
        Log.i(TAG, "notifyChildrenChanged " + parentId);
        mBrowser.notifyChildrenChanged(parentId);
    }

    @Override
    protected void onSearchModeChanged(@NonNull TmaSearchMode oldValue,
            @NonNull TmaSearchMode newValue) {
    }

    @Override
    protected void onAnalyticsChanged(
            @NonNull TmaEnumPrefs.TmaAnalyticsState oldValue,
            @NonNull TmaEnumPrefs.TmaAnalyticsState newValue) {
        updateRootExtras();
        invalidateRoot();
        Log.v(TAG, "AnalyticsMode: " + newValue.toString());
    }

    public BrowserRoot onGetRoot(
            @NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        Log.i(TAG, "onGetRoot " + clientPackageName + " Hints: " + stringify(rootHints));
        return mRoot;
    }

    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result,
            @NonNull Bundle options) {
        Log.i(TAG, "onLoadChildren parentId: " + parentId + " Options: " + stringify(options));
        getMediaItemsWithDelay(parentId, result, null);

        if (QUEUE_ONLY.equals(mPrefs.mRootNodeType.getValue()) && ROOT_PATH.equals(parentId)) {
            mPlayer.getImpl().buildQueue(mLibrary.getPath(TmaBrowseNodeType.LEAF_CHILDREN));
            mPlayer.getImpl().setActiveQueueItem(null);
            mPlayer.prepareActiveItem();
        }
    }

    public void onLoadItem(String itemId, @NonNull Result<MediaBrowserCompat.MediaItem> result) {
        Runnable task = () -> {
            TmaMediaItem node = mLibrary.getMediaItemById(itemId);
            if (node == null) {
                result.sendResult(null);
            } else {
                result.sendResult(toMediaItem(node, mLibrary.getParentPath(itemId)));
            }
        };
        runTaskAndSendResultWithDelay(task, result);
    }

    public void onSearch(@NonNull String query, Bundle extras,
            @NonNull Result<List<MediaItem>> result) {
        Log.i(TAG, "onSearch query: " + query + " Extras: " + stringify(extras));
        getMediaItemsWithDelay(ROOT_PATH, result, query);
    }

    private void getMediaItemsWithDelay(@NonNull String parentId,
            @NonNull Result<List<MediaItem>> result, @Nullable String filter) {
        Runnable task = () -> {
            List<TmaMediaItem.TmaBrowsedMediaItem> tmaItems = getMediaItems(parentId, filter);
            if (tmaItems == null) {
                Log.i(TAG, "onLoadChildren has null result for: " + parentId);
                result.sendResult(null);
            } else {
                Log.i(TAG, "onLoadChildren has " + tmaItems.size() + " children for: " + parentId);
                result.sendResult(tmaItems.stream()
                        .map(it -> toMediaItem(it.mItem, it.mParentId))
                        .collect(Collectors.toList()));
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

    private MediaBrowser.MediaItem getFavoritesMediaItem() {
        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setMediaId("favorites"); // corresponds to "favorites.json"
        builder.setTitle(getContext().getString(R.string.favorites_title));

        return new MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE);
    }

    @Override
    protected void addItemToQueue(String mediaId) {
        mPlayer.getImpl().addItemToQueue(mediaId);
    }

    @Override
    protected void removeItemFromQueue(String mediaId) {
        mPlayer.getImpl().removeItemFromQueue(mediaId);
    }

    /** Handles a media1 custom action. */
    public void handleCustomAction(String action, Bundle extras, @NonNull Result<Bundle> result) {
        handleCustomAction(action, extras, new CustomActionResultHelper() {

            @Override
            public void sendResult(@NonNull Bundle extras) {
                result.sendResult(extras);
            }

            @Override
            public void sendProgressUpdate(@NonNull Bundle extras) {
                result.sendProgressUpdate(extras);
            }

            @Override
            public void sendError(@NonNull Bundle extras) {
                result.sendError(extras);
            }

            @Override
            public void detach() {
                result.detach();
            }
        });
    }
}
