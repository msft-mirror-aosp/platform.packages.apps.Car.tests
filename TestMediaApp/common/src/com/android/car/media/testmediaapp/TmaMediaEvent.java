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

import android.util.Log;

/**
 * Contains the info needed to generate a new playback state.
 */
public class TmaMediaEvent {

    private static final String TAG = "TmaMediaEvent";

    public static final TmaMediaEvent INSTANT_PLAYBACK =
            new TmaMediaEvent(EventState.PLAYING, StateErrorCode.UNKNOWN_ERROR, null, null,
                    ResolutionIntent.NONE, Action.NONE, 0, null, null);

    /** The name of each entry is the value used in the json file. */
    public enum EventState {
        NONE,
        STOPPED,
        PAUSED,
        PLAYING,
        FAST_FORWARDING,
        REWINDING,
        BUFFERING,
        ERROR,
        CONNECTING,
        SKIPPING_TO_PREVIOUS,
        SKIPPING_TO_NEXT,
        SKIPPING_TO_QUEUE_ITEM,
    }

    /** The name of each entry is the value used in the json file. */
    public enum StateErrorCode {
        UNKNOWN_ERROR,
        APP_ERROR,
        NOT_SUPPORTED,
        AUTHENTICATION_EXPIRED,
        PREMIUM_ACCOUNT_REQUIRED,
        CONCURRENT_STREAM_LIMIT,
        PARENTAL_CONTROL_RESTRICTED,
        NOT_AVAILABLE_IN_REGION,
        CONTENT_ALREADY_PLAYING,
        SKIP_LIMIT_REACHED,
        ACTION_ABORTED,
        END_OF_QUEUE,
    }

    /** The name of each entry is the value used in the json file. */
    public enum ResolutionIntent {
        NONE,
        PREFS
    }

    /** The name of each entry is the value used in the json file. */
    public enum Action {
        NONE,
        RESET_METADATA
    }

    public final EventState mState;
    public final StateErrorCode mErrorCode;
    public final String mErrorMessage;
    public final String mActionLabel;
    public final ResolutionIntent mResolutionIntent;
    final Action mAction;
    /** How long to wait before sending the event to the app. */
    final int mPostDelayMs;
    private final String mExceptionClass;
    final String mMediaItemIdToToggle;

    public TmaMediaEvent(EventState state, StateErrorCode errorCode, String errorMessage,
            String actionLabel, ResolutionIntent resolutionIntent, Action action, int postDelayMs,
            String exceptionClass, String mediaItemIdToToggle) {
        mState = state;
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
        mActionLabel = actionLabel;
        mResolutionIntent = resolutionIntent;
        mAction = action;
        mPostDelayMs = postDelayMs;
        mExceptionClass = exceptionClass;
        mMediaItemIdToToggle = mediaItemIdToToggle;
    }

    boolean premiumAccountRequired() {
        return mState == EventState.ERROR && mErrorCode == StateErrorCode.PREMIUM_ACCOUNT_REQUIRED;
    }

    void maybeThrow() {
        if (mExceptionClass != null) {
            RuntimeException exception = null;
            try {
                Class aClass = Class.forName(mExceptionClass);
                exception = (RuntimeException) aClass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Log.e(TAG, "Class error for " + mExceptionClass + " : " + e);
            }

            if (exception != null) throw exception;
        }
    }

    @Override
    public String toString() {
        return "TmaMediaEvent{" +
                "mState=" + mState +
                ", mErrorCode=" + mErrorCode +
                ", mErrorMessage='" + mErrorMessage + '\'' +
                ", mActionLabel='" + mActionLabel + '\'' +
                ", mResolutionIntent=" + mResolutionIntent +
                ", mAction=" + mAction +
                ", mPostDelayMs=" + mPostDelayMs +
                ", mExceptionClass=" + mExceptionClass +
                '}';
    }
}
