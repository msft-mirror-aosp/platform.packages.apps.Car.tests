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

import android.content.Context;
import android.content.Intent;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.android.car.media.testmediaapp.TmaMediaEvent.Action;
import com.android.car.media.testmediaapp.TmaMediaEvent.EventState;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * This class simulates all media interactions (no sound is actually played).
 */
public final class TmaPlayer {

    private static final String TAG = "TmaPlayer";

    private final TmaBrowserDelegate mBrowser;
    private final TmaPrefs mPrefs;
    private final TmaLibrary mLibrary;
    private final AudioManager mAudioManager;
    private final Handler mHandler;
    private final Runnable mTrackTimer = this::stopAndUpdateState;
    private final Runnable mEventTrigger = this::onProcessMediaEvent;
    private final AudioFocusRequest mAudioFocusRequest;
    private PlayerDelegate mPlayerDelegate;

    /** Only updated when the state changes. */
    private long mCurrentPositionMs = 0;
    private final float mPlaybackSpeed = 1.0f; // TODO: make variable.
    private long mPlaybackStartTimeMs;
    private boolean mIsPlaying;
    private List<TmaBrowsedMediaItem> mQueue = Collections.synchronizedList(new ArrayList<>());
    private int mActiveItemIndex = -1;
    private int mNextEventIndex = -1;
    private boolean mResumeOnFocusGain;
    private TmaBrowsedMediaItem mPlaybackItem;

    public interface PlayerDelegate {
        @NonNull TmaPlayer getImpl();
        void setPlaybackState(TmaMediaEvent event);
        void setQueue();
        void updateActiveItemMetadata();
        void maybeActivateSession();
        void resetMetadata();
        void setErrorState(String message);
        void setCurrentPlayingItem(@NonNull TmaBrowsedMediaItem activeItem);
        void sendPausePlaybackState();
        void stopAndUpdateState();
    }

    public TmaPlayer(@NonNull PlayerDelegate playerDelegate,
            @NonNull TmaBrowserDelegate browser, @NonNull TmaLibrary library,
            @NonNull AudioManager audioManager, @NonNull Handler handler) {
        mPlayerDelegate = playerDelegate;
        mBrowser = browser;
        mPrefs = TmaPrefs.getInstance(mBrowser.getContext());
        mLibrary = library;
        mAudioManager = audioManager;
        mHandler = handler;

        mAudioFocusRequest = new AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this::onAudioFocusChange, mHandler)
            .build();
    }

    public TmaBrowserDelegate getBrowser() {
        return mBrowser;
    }

    public TmaLibrary getLibrary() {
        return mLibrary;
    }

    public long getPositionMs() {
        return mCurrentPositionMs;
    }

    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    public List<TmaBrowsedMediaItem> getQueue() {
        return mQueue;
    }

    public int getActiveItemIndex() {
        return mActiveItemIndex;
    }

    public void setActiveItemIndex(int index) {
        mActiveItemIndex = MathUtils.clamp(index, 0, mQueue.size() - 1);
    }

    @Nullable
    public TmaBrowsedMediaItem getActiveItem() {
        if ((0 <= mActiveItemIndex) && (mActiveItemIndex < mQueue.size())) {
            return mQueue.get(mActiveItemIndex);
        }
        return null;
    }

    public void playFromMediaId(String mediaId) {
        buildQueue(mLibrary.getParentPath(mediaId));
        setActiveQueueItem(mLibrary.getMediaItemById(mediaId));
        playActiveQueueItem();
    }

    public void buildQueue(String parentPath) {
        TmaMediaItem parentItem = mLibrary.getMediaItemById(parentPath);
        List<TmaMediaItem> playables = mLibrary.getAllChildren(parentItem, it -> it.mIsPlayable);
        mQueue.clear();

        for (TmaMediaItem child : playables) {
            mQueue.add(new TmaBrowsedMediaItem(child, parentPath));
        }
        mPlayerDelegate.setQueue();
    }

    public void addItemToQueue(String mediaId) {
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (node != null && node.mIsPlayable) {
            String parentPath = mLibrary.getParentPath(mediaId);
            mQueue.add(new TmaBrowsedMediaItem(node, parentPath));
            mPlayerDelegate.setQueue();
        }
    }

    public void removeItemFromQueue(String mediaId) {
        TmaMediaItem node = mLibrary.getMediaItemById(mediaId);
        if (node != null && node.mIsPlayable) {
            int queueSize = mQueue.size();
            List<TmaBrowsedMediaItem> newQueue = new ArrayList<>(queueSize);
            for (int i = 0; i < queueSize; i++) {
                if (!Objects.equals(node, mQueue.get(i).mItem)) {
                    newQueue.add(mQueue.get(i));
                }
            }
            mQueue = newQueue;
            mPlayerDelegate.setQueue();
        }
    }

    public void playActiveQueueItem() {
        if (getActiveItem() != null) {
            stopPlayback();
            startPlayBack(true);
        }
    }

    public void playQueueItem(int index) {
        if (0 <= index && index < mQueue.size()) {
            mActiveItemIndex = index;
            playActiveQueueItem();
        }
    }

    /** If the given item is in the queue, make it the active one, otherwise activate the first. */
    public void setActiveQueueItem(@Nullable TmaMediaItem item) {
        if (item == null) {
            mActiveItemIndex = 0;
            return;
        }
        for (int i = 0; i < mQueue.size(); i++) {
            if (Objects.equals(item, mQueue.get(i).mItem)) {
                mActiveItemIndex = i;
                break;
            }
        }
    }

    public void seekTo(long pos) {
        boolean wasPlaying = mIsPlaying;
        if (wasPlaying) {
            mHandler.removeCallbacks(mTrackTimer);
        }
        mCurrentPositionMs = pos;
        boolean requestAudioFocus = !wasPlaying;
        startPlayBack(requestAudioFocus);
    }

    public void onCustomAction(String action, Bundle extras) {
        TmaBrowsedMediaItem queueItem = getActiveItem();
        if (queueItem != null) {
            TmaMediaItem activeItem = queueItem.mItem;
            if (TmaCustomAction.HEART_PLUS_PLUS.mId.equals(action)) {
                activeItem.offsetHearts(+1);
                mPlayerDelegate.updateActiveItemMetadata();
            } else if (TmaCustomAction.HEART_LESS_LESS.mId.equals(action)) {
                activeItem.offsetHearts(-1);
                mPlayerDelegate.updateActiveItemMetadata();
            } else if (TmaCustomAction.REQUEST_LOCATION.mId.equals(action)) {
                Context context = mBrowser.getContext();
                context.startService(new Intent(context, TmaForegroundService.class));
            }
        }
    }

    private boolean audioFocusGranted() {
        return mAudioManager.requestAudioFocus(mAudioFocusRequest) == AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void stopAndUpdateState() {
        mPlayerDelegate.stopAndUpdateState();
    }

    private void onProcessMediaEvent() {
        TmaBrowsedMediaItem activeItem = getActiveItem();
        if (activeItem == null) return;

        TmaMediaEvent event = activeItem.mItem.mMediaEvents.get(mNextEventIndex);
        event.maybeThrow();
        if (!TextUtils.isEmpty(event.mMediaItemIdToToggle)) {
            mBrowser.toggleItem(event.mMediaItemIdToToggle);
        }

        if (event.premiumAccountRequired() &&
                TmaAccountType.PAID.equals(mPrefs.mAccountType.getValue())) {
            Log.i(TAG, "Ignoring event for paid account");
            return;
        } else if (Action.RESET_METADATA.equals(event.mAction)) {
            mPlayerDelegate.resetMetadata();
        } else {
            mPlayerDelegate.setPlaybackState(event);
        }

        if (event.mState == EventState.PLAYING) {
            mPlayerDelegate.maybeActivateSession();

            long trackDurationMs = activeItem.mItem.getDuration();
            if (trackDurationMs > 0) {
                mPlaybackStartTimeMs = System.currentTimeMillis();
                long remainingMs = (long) ((trackDurationMs - mCurrentPositionMs) / mPlaybackSpeed);
                mHandler.postDelayed(mTrackTimer, remainingMs);
            }
            mIsPlaying = true;
        } else {
            stopPlayback();
            // Don't call mPlayerDelegate.stopAndUpdateState() here as it breaks error handling by
            // resetting the state right after sending an error (incorrectly added in ag/29780977).
        }

        mNextEventIndex++;
        if (mNextEventIndex < activeItem.mItem.mMediaEvents.size()) {
            mHandler.postDelayed(mEventTrigger,
                    activeItem.mItem.mMediaEvents.get(mNextEventIndex).mPostDelayMs);
        }
    }

    public void startPlayBack(boolean requestAudioFocus) {
        if (requestAudioFocus && !audioFocusGranted()) return;

        TmaBrowsedMediaItem activeItem = getActiveItem();
        if (activeItem == null || activeItem.mItem.mMediaEvents.size() == 0) {
            mPlayerDelegate.setErrorState("null mActiveItem or empty events");
            return;
        }

        if (mPlaybackItem != activeItem) {
            mPlaybackItem = activeItem;

            mPlayerDelegate.setCurrentPlayingItem(activeItem);

            mHandler.removeCallbacks(mEventTrigger);
            mNextEventIndex = 0;
            mHandler.postDelayed(mEventTrigger, activeItem.mItem.mMediaEvents.get(0).mPostDelayMs);
        }
    }


    public void pausePlayback() {
        mPlaybackItem = null;
        mCurrentPositionMs += (System.currentTimeMillis() - mPlaybackStartTimeMs) / mPlaybackSpeed;
        mIsPlaying = false;
        mHandler.removeCallbacks(mTrackTimer);
        mPlayerDelegate.sendPausePlaybackState();
    }

    /** Doesn't change the playback state. */
    public void stopPlayback() {
        // TODO: see if the test can be removed without side effects.
        if (mIsPlaying) {
            mPlaybackItem = null;
            mCurrentPositionMs = 0;
            mHandler.removeCallbacks(mTrackTimer);
            mIsPlaying = false;
        }
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
