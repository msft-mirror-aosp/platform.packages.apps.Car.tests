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
import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_APP_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;

import static com.android.car.media.common.MediaConstants.ERROR_RESOLUTION_ACTION_INTENT;
import static com.android.car.media.common.MediaConstants.ERROR_RESOLUTION_ACTION_LABEL;

import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.android.car.media.testmediaapp.TmaMediaEvent.EventState;
import com.android.car.media.testmediaapp.TmaMediaEvent.ResolutionIntent;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity;


/**
 * This class simulates all media interactions (no sound is actually played).
 */
public class TmaPlayer extends MediaSessionCompat.Callback {

    private static final String TAG = "TmaPlayer";

    // TODO: refactor actions (make some per item, per state ??)...
    private static final long PLAYING_ACTIONS = PlaybackStateCompat.ACTION_PAUSE
            | ACTION_PLAY_FROM_MEDIA_ID | ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS
            | ACTION_SKIP_TO_QUEUE_ITEM | ACTION_SEEK_TO;

    private static final long PAUSED_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | ACTION_PLAY_FROM_MEDIA_ID | ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS
            | ACTION_SEEK_TO;

    private static final long STOPPED_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | ACTION_PLAY_FROM_MEDIA_ID | ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS
            | ACTION_SEEK_TO;


    private final Context mContext;
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
    @Nullable
    private TmaMediaItem mActiveItem;
    private int mNextEventIndex = -1;


    TmaPlayer(Context context, TmaLibrary library, AudioManager audioManager, Handler handler,
            MediaSessionCompat session) {
        mContext = context;
        mPrefs = TmaPrefs.getInstance(mContext);
        mLibrary = library;
        mAudioManager = audioManager;
        mHandler = handler;
        mSession = session;

        // TODO add focus listener ?
        mAudioFocusRequest = new AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).build();
    }

    /** Updates the state in the media session based on the given {@link TmaMediaEvent}. */
    void setPlaybackState(TmaMediaEvent event) {
        Log.i(TAG, "setPlaybackState " + event);

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(event.mState.mValue, mCurrentPositionMs, mPlaybackSpeed)
                .setErrorMessage(event.mErrorCode.mValue, event.mErrorMessage)
                .setActions(PLAYING_ACTIONS);
        if (ResolutionIntent.PREFS.equals(event.mResolutionIntent)) {
            Intent prefsIntent = new Intent();
            prefsIntent.setClass(mContext, TmaPrefsActivity.class);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, prefsIntent, 0);

            Bundle extras = new Bundle();
            extras.putString(ERROR_RESOLUTION_ACTION_LABEL, event.mActionLabel);
            extras.putParcelable(ERROR_RESOLUTION_ACTION_INTENT, pendingIntent);
            state.setExtras(extras);
        }
        mSession.setPlaybackState(state.build());
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        mActiveItem = mLibrary.getMediaItemById(mediaId);
        if (requestAudioFocus()) {
            startPlayBack();
        }
    }

    @Override
    public void onPlay() {
        super.onPlay();
        if (requestAudioFocus()) {
            startPlayBack();
        }
    }

    @Override
    public void onSeekTo(long pos) {
        super.onSeekTo(pos);
        boolean wasPlaying = mIsPlaying;
        if (wasPlaying) {
            mHandler.removeCallbacks(mTrackTimer);
        }
        mCurrentPositionMs = pos;
        if (wasPlaying) {
            startPlayBack();
        }
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

    private boolean requestAudioFocus() {
        return mAudioManager.requestAudioFocus(mAudioFocusRequest) == AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void onProcessMediaEvent() {
        TmaMediaEvent event = mActiveItem.mMediaEvents.get(mNextEventIndex);

        if (event.premiumAccountRequired() &&
                TmaAccountType.PAID.equals(mPrefs.mAccountType.getValue())) {
            Log.i(TAG, "Ignoring even for paid account");
            return;
        } else {
            setPlaybackState(event);
        }

        if (event.mState == EventState.PLAYING) {
            if (!mSession.isActive()) {
                mSession.setActive(true);
            }

            long trackDurationMs = mActiveItem.getDuration();
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
        if (mNextEventIndex < mActiveItem.mMediaEvents.size()) {
            mHandler.postDelayed(mEventTrigger,
                    mActiveItem.mMediaEvents.get(mNextEventIndex).mPostDelayMs);
        }
    }

    private void startPlayBack() {
        if (mActiveItem == null || mActiveItem.mMediaEvents.size() <= 0) {
            PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                    .setState(STATE_ERROR, mCurrentPositionMs, mPlaybackSpeed)
                    .setErrorMessage(ERROR_CODE_APP_ERROR, "null mActiveItem or empty events")
                    .build();
            mSession.setPlaybackState(state);
            return;
        }

        mActiveItem.updateSessionMetadata(mSession);

        mHandler.removeCallbacks(mEventTrigger);
        mNextEventIndex = 0;
        mHandler.postDelayed(mEventTrigger, mActiveItem.mMediaEvents.get(0).mPostDelayMs);
    }

    private void pausePlayback() {
        mCurrentPositionMs += (System.currentTimeMillis() - mPlaybackStartTimeMs) / mPlaybackSpeed;
        mHandler.removeCallbacks(mTrackTimer);
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, mCurrentPositionMs, mPlaybackSpeed)
                .setActions(PAUSED_ACTIONS)
                .build();
        mSession.setPlaybackState(state);
        mIsPlaying = false;
    }

    /** Doesn't change the playback state. */
    private void stopPlayback() {
        mCurrentPositionMs = 0;
        mHandler.removeCallbacks(mTrackTimer);
        mIsPlaying = false;
    }

    private void sendStopPlaybackState() {
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, mCurrentPositionMs, mPlaybackSpeed)
                .setActions(STOPPED_ACTIONS)
                .build();
        mSession.setPlaybackState(state);
    }
}
