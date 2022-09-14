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

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PREPARE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_APP_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.utils.MediaConstants;

import com.android.car.media.testmediaapp.TmaMediaEvent.Action;
import com.android.car.media.testmediaapp.TmaMediaEvent.EventState;
import com.android.car.media.testmediaapp.TmaMediaEvent.ResolutionIntent;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaCustomAction;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * This class simulates all media interactions (no sound is actually played).
 */
public class TmaPlayer extends MediaSessionCompat.Callback {

    private static final String TAG = "TmaPlayer";

    private final TmaBrowser mBrowser;
    private final TmaPrefs mPrefs;
    private final TmaLibrary mLibrary;
    private final AudioManager mAudioManager;
    private final Handler mHandler;
    private final Runnable mTrackTimer = this::onStop;
    private final Runnable mEventTrigger = this::onProcessMediaEvent;
    private final MediaSessionCompat mSession;
    private final AudioFocusRequest mAudioFocusRequest;

    /** Only updated when the state changes. */
    private long mCurrentPositionMs = 0;
    private float mPlaybackSpeed = 1.0f; // TODO: make variable.
    private long mPlaybackStartTimeMs;
    private boolean mIsPlaying;
    private List<TmaMediaItem> mQueue = Collections.emptyList();
    private List<QueueItem> mSessionQueue = Collections.emptyList();
    private int mActiveItemIndex = -1;
    private int mNextEventIndex = -1;
    private boolean mResumeOnFocusGain;

    TmaPlayer(TmaBrowser browser, TmaLibrary library, AudioManager audioManager, Handler handler,
            MediaSessionCompat session) {
        mBrowser = browser;
        mPrefs = TmaPrefs.getInstance(mBrowser);
        mLibrary = library;
        mAudioManager = audioManager;
        mHandler = handler;
        mSession = session;

        mAudioFocusRequest = new AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this::onAudioFocusChange, mHandler)
            .build();
    }

    /** Updates the state in the media session based on the given {@link TmaMediaEvent}. */
    void setPlaybackState(TmaMediaEvent event) {
        Log.i(TAG, "setPlaybackState " + event);

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(event.mState.mValue, mCurrentPositionMs, mPlaybackSpeed)
                .setErrorMessage(event.mErrorCode.mValue, event.mErrorMessage)
                .setActions(addActions(ACTION_PAUSE));
        if (ResolutionIntent.PREFS.equals(event.mResolutionIntent)) {
            Intent prefsIntent = new Intent();
            prefsIntent.setClass(mBrowser, TmaPrefsActivity.class);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mBrowser, 0, prefsIntent,
                    PendingIntent.FLAG_IMMUTABLE);

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

    @Nullable
    private TmaMediaItem getActiveItem() {
        if ((0 <= mActiveItemIndex) && (mActiveItemIndex < mQueue.size())) {
            return mQueue.get(mActiveItemIndex);
        }
        return null;
    }

    void buildQueue(String parentPath) {
        TmaMediaItem parentItem = mLibrary.getMediaItemById(parentPath);
        List<TmaMediaItem> playables = mLibrary.getAllChildren(parentItem, FLAG_PLAYABLE);
        mQueue = playables;

        int queueSize = playables.size();
        mSessionQueue = new ArrayList<>(queueSize);
        for (int i = 0 ; i < queueSize; i++) {
            TmaMediaItem child = mQueue.get(i);
            mSessionQueue.add(new QueueItem(child.buildDescription(parentPath), i));
        }
        mSession.setQueue(mSessionQueue);
    }

    void addItemToQueue(String mediaId) {
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (node != null && node.testFlag(FLAG_PLAYABLE)) {
            mQueue.add(node);
            String parentPath = mLibrary.getParentPath(mediaId);
            MediaDescriptionCompat desc = node.buildDescription(parentPath);
            mSessionQueue.add(new QueueItem(desc, mQueue.size()));
            mSession.setQueue(mSessionQueue);
        }
    }

    void removeItemFromQueue(String mediaId) {
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (node != null && node.testFlag(FLAG_PLAYABLE)) {
            int queueSize = mQueue.size();
            List<TmaMediaItem> newQueue = new ArrayList<>(queueSize);
            List<QueueItem> newSessionQueue = new ArrayList<>(queueSize);
            for (int i = 0; i < queueSize; i++) {
                if (!Objects.equals(node, mQueue.get(i))) {
                    newQueue.add(mQueue.get(i));
                    MediaDescriptionCompat description = mSessionQueue.get(i).getDescription();
                    newSessionQueue.add(new QueueItem(description, newQueue.size()));
                }
            }
            mQueue = newQueue;
            mSessionQueue = newSessionQueue;
            mSession.setQueue(mSessionQueue);
        }
    }

    /** Sets custom action, queue id, etc. */
    private void setActiveItemState(PlaybackStateCompat.Builder state) {
        TmaMediaItem activeItem = getActiveItem();
        if (activeItem != null) {
            for (TmaCustomAction action : activeItem.mCustomActions) {
                String name = mBrowser.getResources().getString(action.mNameId);
                state.addCustomAction(action.mId, name, action.mIcon);
            }
            state.setActiveQueueItemId(mActiveItemIndex);
        }
    }

    private void playActiveQueueItem() {
        if (getActiveItem() != null) {
            if (mIsPlaying) {
                stopPlayback();
            }
            startPlayBack(true);
        }
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        buildQueue(mLibrary.getParentPath(mediaId));
        setActiveQueueItem(mLibrary.getMediaItemById(mediaId));
        playActiveQueueItem();
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
        prepareMediaItem(mediaId);
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
    }

    /** If the given item is in the queue, make it the active one, otherwise activate the first. */
    void setActiveQueueItem(@Nullable TmaMediaItem item) {
        if (item == null) {
            mActiveItemIndex = 0;
            return;
        }
        mActiveItemIndex = Math.max(0, mQueue.indexOf(item));
    }

    void prepareMediaItem(String mediaId) {
        buildQueue(mLibrary.getParentPath(mediaId));
        setActiveQueueItem(mLibrary.getMediaItemById(mediaId));
        prepareActiveItem();
    }

    void prepareActiveItem() {
        TmaMediaItem activeItem = getActiveItem();
        if (activeItem != null) {
            if (mIsPlaying) {
                stopPlayback();
            }

            activeItem.updateSessionMetadata(mLibrary, mSession);

            PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, mCurrentPositionMs, mPlaybackSpeed)
                    .setActions(addActions(ACTION_PLAY));
            setActiveItemState(state);
            mSession.setPlaybackState(state.build());
        }
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
        mActiveItemIndex = (int) id;
        playActiveQueueItem();
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        mActiveItemIndex++;
        playActiveQueueItem();
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        mActiveItemIndex--;
        playActiveQueueItem();
    }

    @Override
    public void onPlay() {
        super.onPlay();
        startPlayBack(true);
    }

    @Override
    public void onSeekTo(long pos) {
        super.onSeekTo(pos);
        boolean wasPlaying = mIsPlaying;
        if (wasPlaying) {
            mHandler.removeCallbacks(mTrackTimer);
        }
        mCurrentPositionMs = pos;
        boolean requestAudioFocus = !wasPlaying;
        startPlayBack(requestAudioFocus);
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePlayback();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlayback();
        sendStopPlaybackState();
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        super.onCustomAction(action, extras);
        TmaMediaItem activeItem = getActiveItem();
        if (activeItem != null) {
            if (TmaCustomAction.HEART_PLUS_PLUS.mId.equals(action)) {
                activeItem.mHearts++;
                toast("" + activeItem.mHearts);
            } else if (TmaCustomAction.HEART_LESS_LESS.mId.equals(action)) {
                activeItem.mHearts--;
                toast("" + activeItem.mHearts);
            } else if (TmaCustomAction.REQUEST_LOCATION.mId.equals(action)) {
                mBrowser.startService(new Intent(mBrowser, TmaForegroundService.class));
            }
        }
    }

    /** Note: this is for quick feedback implementation, media apps should avoid toasts... */
    private void toast(String message) {
        Toast.makeText(mBrowser, message, Toast.LENGTH_LONG).show();
    }

    private boolean audioFocusGranted() {
        return mAudioManager.requestAudioFocus(mAudioFocusRequest) == AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void onProcessMediaEvent() {
        TmaMediaItem activeItem = getActiveItem();
        if (activeItem == null) return;

        TmaMediaEvent event = activeItem.mMediaEvents.get(mNextEventIndex);
        event.maybeThrow();
        if (!TextUtils.isEmpty(event.mMediaItemIdToToggle)) {
            mBrowser.toggleItem(event.mMediaItemIdToToggle);
        }

        if (event.premiumAccountRequired() &&
                TmaAccountType.PAID.equals(mPrefs.mAccountType.getValue())) {
            Log.i(TAG, "Ignoring even for paid account");
            return;
        } else if (Action.RESET_METADATA.equals(event.mAction)) {
            mSession.setMetadata(mSession.getController().getMetadata());
        } else {
            setPlaybackState(event);
        }

        if (event.mState == EventState.PLAYING) {
            if (!mSession.isActive()) {
                mSession.setActive(true);
            }

            long trackDurationMs = activeItem.getDuration();
            if (trackDurationMs > 0) {
                mPlaybackStartTimeMs = System.currentTimeMillis();
                long remainingMs = (long) ((trackDurationMs - mCurrentPositionMs) / mPlaybackSpeed);
                mHandler.postDelayed(mTrackTimer, remainingMs);
            }
            mIsPlaying = true;
        } else if (mIsPlaying) {
            stopPlayback();
        }

        mNextEventIndex++;
        if (mNextEventIndex < activeItem.mMediaEvents.size()) {
            mHandler.postDelayed(mEventTrigger,
                    activeItem.mMediaEvents.get(mNextEventIndex).mPostDelayMs);
        }
    }

    private void startPlayBack(boolean requestAudioFocus) {
        if (requestAudioFocus && !audioFocusGranted()) return;

        TmaMediaItem activeItem = getActiveItem();
        if (activeItem == null || activeItem.mMediaEvents.size() <= 0) {
            PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                    .setState(STATE_ERROR, mCurrentPositionMs, mPlaybackSpeed)
                    .setErrorMessage(ERROR_CODE_APP_ERROR, "null mActiveItem or empty events")
                    .build();
            mSession.setPlaybackState(state);
            return;
        }

        activeItem.updateSessionMetadata(mLibrary, mSession);

        mHandler.removeCallbacks(mEventTrigger);
        mNextEventIndex = 0;
        mHandler.postDelayed(mEventTrigger, activeItem.mMediaEvents.get(0).mPostDelayMs);
    }

    private void pausePlayback() {
        mCurrentPositionMs += (System.currentTimeMillis() - mPlaybackStartTimeMs) / mPlaybackSpeed;
        mHandler.removeCallbacks(mTrackTimer);
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, mCurrentPositionMs, mPlaybackSpeed)
                .setActions(addActions(ACTION_PLAY));
        setActiveItemState(state);
        mSession.setPlaybackState(state.build());
        mIsPlaying = false;
    }

    /** Doesn't change the playback state. */
    private void stopPlayback() {
        mCurrentPositionMs = 0;
        mHandler.removeCallbacks(mTrackTimer);
        mIsPlaying = false;
    }

    private void sendStopPlaybackState() {
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, mCurrentPositionMs, mPlaybackSpeed)
                .setActions(addActions(ACTION_PLAY));
        setActiveItemState(state);
        mSession.setPlaybackState(state.build());
    }

    private long addActions(long actions) {
        actions |= ACTION_PLAY_FROM_MEDIA_ID | ACTION_SEEK_TO
                | ACTION_PREPARE;

        if (!mQueue.isEmpty()) {
            actions |= ACTION_SKIP_TO_QUEUE_ITEM;
            if (mActiveItemIndex < mQueue.size()) {
                actions |= ACTION_SKIP_TO_NEXT;
            }
            if (0 < mActiveItemIndex) {
                actions |= ACTION_SKIP_TO_PREVIOUS;
            }
        }

        return actions;
    }

    private void onAudioFocusChange(int focusChange) {
        // Adapted from samples at https://developer.android.com/guide/topics/media-apps/audio-focus
        // Android Auto emulator tests rely on the app pausing and resuming in response to focus
        // transient loss and focus gain, respectively.
        switch (focusChange) {
            case AUDIOFOCUS_GAIN:
                if (mResumeOnFocusGain) {
                    mResumeOnFocusGain = false;
                    startPlayBack(/* requestAudioFocus= */ false);
                }
                break;
            case AUDIOFOCUS_LOSS:
                mResumeOnFocusGain = false;
                pausePlayback();
                break;
            case AUDIOFOCUS_LOSS_TRANSIENT:
            case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mResumeOnFocusGain = mIsPlaying;
                pausePlayback();
                break;
            default:
                Log.w(TAG, "Unknown audio focus change " + focusChange);
        }
    }
}
