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

package com.android.car.media.testmediaapp.media1;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PREPARE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_APP_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_CONCURRENT_STREAM_LIMIT;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_CONTENT_ALREADY_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_END_OF_QUEUE;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_NOT_AVAILABLE_IN_REGION;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_PARENTAL_CONTROL_RESTRICTED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_SKIP_LIMIT_REACHED;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

import static androidx.media3.session.MediaConstants.EXTRAS_KEY_COMMAND_BUTTON_ICON_COMPAT;

import static com.android.car.media.testmediaapp.MediaConstants.KEY_DESCRIPTION_LINK_MEDIA_ID;
import static com.android.car.media.testmediaapp.MediaConstants.KEY_SUBTITLE_LINK_MEDIA_ID;
import static com.android.car.media.testmediaapp.media1.TmaMedia1ItemExtKt.buildMetadata;
import static com.android.car.media.testmediaapp.media1.TmaMedia1ItemExtKt.toDescription;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.CustomAction;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.utils.MediaConstants;

import com.android.car.media.testmediaapp.R;
import com.android.car.media.testmediaapp.TmaBrowserDelegate;
import com.android.car.media.testmediaapp.TmaCustomAction;
import com.android.car.media.testmediaapp.TmaLibrary;
import com.android.car.media.testmediaapp.TmaMediaEvent;
import com.android.car.media.testmediaapp.TmaMediaEvent.EventState;
import com.android.car.media.testmediaapp.TmaMediaEvent.ResolutionIntent;
import com.android.car.media.testmediaapp.TmaMediaItem;
import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.TmaPlayer;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * This class simulates all media interactions (no sound is actually played).
 */
public class TmaPlayer1 extends MediaSessionCompat.Callback implements TmaPlayer.PlayerDelegate {

    private static final String TAG = "TmaPlayer1";

    private final MediaSessionCompat mSession;
    private final TmaPrefs mPrefs;
    private final TmaPlayer mFakePlayer;
    private final Handler mHandler;

    private TmaMediaItem mDelayedMetaData;
    private final Runnable mDelayedMetaDataRunnable = this::updateSessionMetadataDelayed;

    TmaPlayer1(TmaBrowserDelegate browser, TmaLibrary library, AudioManager audioManager,
            Handler handler, MediaSessionCompat session) {
        mSession = session;
        mSession.setCallback(this);
        mPrefs = TmaPrefs.getInstance(browser.getContext());
        mFakePlayer = new TmaPlayer(this, browser, library, audioManager, handler);
        mHandler = handler;
    }

    private int toM1State(EventState tmaState) {
        switch (tmaState) {
            case NONE                   : return STATE_NONE;
            case STOPPED                : return STATE_STOPPED;
            case PAUSED                 : return STATE_PAUSED;
            case PLAYING                : return STATE_PLAYING;
            case FAST_FORWARDING        : return STATE_FAST_FORWARDING;
            case REWINDING              : return STATE_REWINDING;
            case BUFFERING              : return STATE_BUFFERING;
            case ERROR                  : return STATE_ERROR;
            case CONNECTING             : return STATE_CONNECTING;
            case SKIPPING_TO_PREVIOUS   : return STATE_SKIPPING_TO_PREVIOUS;
            case SKIPPING_TO_NEXT       : return STATE_SKIPPING_TO_NEXT;
            case SKIPPING_TO_QUEUE_ITEM : return STATE_SKIPPING_TO_QUEUE_ITEM;
            default:
                throw new IllegalArgumentException("Unexpected state");
        }
    }

    private int toM1ErrorCode(TmaMediaEvent.StateErrorCode tmaCode) {
        switch (tmaCode) {
            case UNKNOWN_ERROR               : return ERROR_CODE_UNKNOWN_ERROR;
            case APP_ERROR                   : return ERROR_CODE_APP_ERROR;
            case NOT_SUPPORTED               : return ERROR_CODE_NOT_SUPPORTED;
            case AUTHENTICATION_EXPIRED      : return ERROR_CODE_AUTHENTICATION_EXPIRED;
            case PREMIUM_ACCOUNT_REQUIRED    : return ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED;
            case CONCURRENT_STREAM_LIMIT     : return ERROR_CODE_CONCURRENT_STREAM_LIMIT;
            case PARENTAL_CONTROL_RESTRICTED : return ERROR_CODE_PARENTAL_CONTROL_RESTRICTED;
            case NOT_AVAILABLE_IN_REGION     : return ERROR_CODE_NOT_AVAILABLE_IN_REGION;
            case CONTENT_ALREADY_PLAYING     : return ERROR_CODE_CONTENT_ALREADY_PLAYING;
            case SKIP_LIMIT_REACHED          : return ERROR_CODE_SKIP_LIMIT_REACHED;
            case ACTION_ABORTED              : return ERROR_CODE_ACTION_ABORTED;
            case END_OF_QUEUE                : return ERROR_CODE_END_OF_QUEUE;
            default:
                throw new IllegalArgumentException("Unexpected error code");
        }
    }

    public void setAccountType(@NonNull TmaEnumPrefs.TmaAccountType accountType) {
        if (accountType == TmaEnumPrefs.TmaAccountType.NONE) {
            mSession.setMetadata(null);

            Context context = mFakePlayer.getBrowser().getContext();
            stopAndUpdateState();
            setPlaybackState(
                    new TmaMediaEvent(TmaMediaEvent.EventState.ERROR,
                            TmaMediaEvent.StateErrorCode.AUTHENTICATION_EXPIRED,
                            context.getResources().getString(R.string.no_account),
                            context.getResources().getString(R.string.select_account),
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

    public @NonNull TmaPlayer getImpl() {
        return mFakePlayer;
    }

    /** Sets custom action, queue id, etc. */
    @SuppressLint("UnsafeOptInUsageError")
    private void setActiveItemState(PlaybackStateCompat.Builder state) {
        TmaBrowsedMediaItem activeItem = mFakePlayer.getActiveItem();
        if (activeItem != null) {
            Resources res = mFakePlayer.getBrowser().getContext().getResources();
            for (TmaCustomAction action : activeItem.mItem.mCustomActions) {
                String name = res.getString(action.mNameId);
                Bundle extras = new Bundle();
                if (action.mIconId != 0) {
                    extras.putInt(EXTRAS_KEY_COMMAND_BUTTON_ICON_COMPAT, action.mIconId);
                }
                CustomAction custom = new CustomAction.Builder(action.mId, name, action.mIcon)
                        .setExtras(extras)
                        .build();
                state.addCustomAction(custom);
            }
            state.setActiveQueueItemId(mFakePlayer.getActiveItemIndex());
        }
    }

    private void updateSessionMetadata(@NonNull TmaMediaItem item) {
        mHandler.removeCallbacks(mDelayedMetaDataRunnable);
        TmaEnumPrefs.TmaReplyDelay delay = mPrefs.mRootReplyDelay.getValue();
        if (delay == TmaEnumPrefs.TmaReplyDelay.NONE) {
            updateSessionMetadataImpl(item);
        } else {
            updateSessionMetadataImpl(null);
            mDelayedMetaData = item;
            mHandler.postDelayed(mDelayedMetaDataRunnable, delay.mReplyDelayMs);
        }
    }

    private void updateSessionMetadataImpl(@Nullable TmaMediaItem item) {
        if (item == null) {
            mSession.setMetadata(null);
            return;
        }
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder(buildMetadata(item));

        TmaLibrary library = mFakePlayer.getLibrary();
        String subtitleLink = item.selectLink(library, MetadataKey.SUBTITLE_LINK_MEDIA_ID);
        if (subtitleLink != null) {
            builder.putString(KEY_SUBTITLE_LINK_MEDIA_ID, subtitleLink);
        }

        String descLink = item.selectLink(library, MetadataKey.DESCRIPTION_LINK_MEDIA_ID);
        if (descLink != null) {
            builder.putString(KEY_DESCRIPTION_LINK_MEDIA_ID, descLink);
        }

        mSession.setMetadata(builder.build());
    }

    private void updateSessionMetadataDelayed() {
        updateSessionMetadataImpl(mDelayedMetaData);
    }

    private void sendStopPlaybackState() {
        float speed = mFakePlayer.getPlaybackSpeed();
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(STATE_STOPPED, mFakePlayer.getPositionMs(), speed)
                // TODO(media3) revert the use of ACTION_PLAY_PAUSE required b/369442714.
                .setActions(addActions(ACTION_PLAY | ACTION_PLAY_PAUSE));
        setActiveItemState(state);
        mSession.setPlaybackState(state.build());
    }

    private long addActions(long actions) {
        actions |= ACTION_PLAY_FROM_MEDIA_ID | ACTION_SEEK_TO
                | ACTION_PREPARE;

        if (!mFakePlayer.getQueue().isEmpty()) {
            actions |= ACTION_SKIP_TO_QUEUE_ITEM;
            if (mFakePlayer.getActiveItemIndex() < mFakePlayer.getQueue().size() - 2) {
                actions |= ACTION_SKIP_TO_NEXT;
            }
            if (0 < mFakePlayer.getActiveItemIndex()) {
                actions |= ACTION_SKIP_TO_PREVIOUS;
            }
        }

        return actions;
    }

    /////////////////////////// MediaSessionCompat.Callback overrides ///////////////////////////

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        mFakePlayer.playFromMediaId(mediaId);
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
        mFakePlayer.prepareMediaItem(mediaId);
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
        mFakePlayer.setActiveItemIndex((int) id);
        mFakePlayer.playActiveQueueItem();
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        mFakePlayer.setActiveItemIndex(mFakePlayer.getActiveItemIndex() + 1);
        mFakePlayer.playActiveQueueItem();
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        mFakePlayer.setActiveItemIndex(mFakePlayer.getActiveItemIndex() - 1);
        mFakePlayer.playActiveQueueItem();
    }

    @Override
    public void onPlay() {
        super.onPlay();
        mFakePlayer.startPlayBack(true);
    }

    @Override
    public void onSeekTo(long pos) {
        super.onSeekTo(pos);
        mFakePlayer.seekTo(pos);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFakePlayer.pausePlayback();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAndUpdateState();
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        super.onCustomAction(action, extras);
        mFakePlayer.onCustomAction(action, extras);
    }

    /////////////////////////// TmaPlayer.PlayerDelegate overrides ///////////////////////////

    /** Updates the state in the media session based on the given {@link TmaMediaEvent}. */
    @Override
    public void setPlaybackState(TmaMediaEvent event) {
        Log.i(TAG, "setPlaybackState " + event);

        float speed = mFakePlayer.getPlaybackSpeed();
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(toM1State(event.mState), mFakePlayer.getPositionMs(), speed)
                .setErrorMessage(toM1ErrorCode(event.mErrorCode), event.mErrorMessage)
                // TODO(media3) revert the use of ACTION_PLAY_PAUSE required b/369442714.
                .setActions(addActions(ACTION_PAUSE | ACTION_PLAY_PAUSE));
        if (ResolutionIntent.PREFS.equals(event.mResolutionIntent)) {
            Context  context = mFakePlayer.getBrowser().getContext();
            PendingIntent pendingIntent = TmaPrefsActivity.getPendingIntent(context);

            Bundle extras = new Bundle();
            extras.putString(MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL,
                    event.mActionLabel);
            extras.putParcelable(
                    MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT,
                    pendingIntent);
            state.setExtras(extras);
        }

        setActiveItemState(state);
        mSession.setPlaybackState(state.build());
    }

    @Override
    public void setQueue() {
        List<TmaBrowsedMediaItem> tmaQueue = mFakePlayer.getQueue();
        List<QueueItem> m1Queue = new ArrayList<>(tmaQueue.size());
        for (int i = 0; i < tmaQueue.size(); i++) {
            TmaBrowsedMediaItem it = tmaQueue.get(i);
            MediaDescriptionCompat desc = toDescription(it.mItem, it.mParentId);
            m1Queue.add(new QueueItem(desc, i));
        }
        mSession.setQueue(m1Queue);
    }

    @Override
    public void prepareActiveItem() {
        TmaBrowsedMediaItem activeItem = mFakePlayer.getActiveItem();
        if (activeItem != null) {
            mFakePlayer.stopPlayback();
            updateSessionMetadata(activeItem.mItem);

            float speed = mFakePlayer.getPlaybackSpeed();
            PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                    .setState(STATE_PAUSED, mFakePlayer.getPositionMs(), speed)
                    // TODO(media3) revert the use of ACTION_PLAY_PAUSE required b/369442714.
                    .setActions(addActions(ACTION_PLAY | ACTION_PLAY_PAUSE));
            setActiveItemState(state);
            mSession.setPlaybackState(state.build());
        }
    }

    @Override
    public void updateActiveItemMetadata() {
        TmaBrowsedMediaItem activeItem = mFakePlayer.getActiveItem();
        if (activeItem != null) {
            updateSessionMetadata(activeItem.mItem);
        }
    }

    @Override
    public void resetMetadata() {
        mSession.setMetadata(mSession.getController().getMetadata());
    }

    @Override
    public void maybeActivateSession() {
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    @Override
    public void setErrorState(String message) {
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setState(STATE_ERROR, mFakePlayer.getPositionMs(), mFakePlayer.getPlaybackSpeed())
                .setErrorMessage(ERROR_CODE_APP_ERROR, message)
                .build();
        mSession.setPlaybackState(state);
    }

    @Override
    public void setCurrentPlayingItem(@NonNull TmaBrowsedMediaItem activeItem) {
        updateSessionMetadata(activeItem.mItem);
    }

    @Override
    public void sendPausePlaybackState() {
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(STATE_PAUSED, mFakePlayer.getPositionMs(), mFakePlayer.getPlaybackSpeed())
                // TODO(media3) revert the use of ACTION_PLAY_PAUSE required b/369442714.
                .setActions(addActions(ACTION_PLAY | ACTION_PLAY_PAUSE));
        setActiveItemState(state);
        mSession.setPlaybackState(state.build());
    }

    @Override
    public void stopAndUpdateState() {
        mFakePlayer.stopPlayback();
        sendStopPlaybackState();
    }
}
