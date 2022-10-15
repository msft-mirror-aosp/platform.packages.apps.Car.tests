/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_BROWSE_NODE;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_MESSAGE;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_OPEN_PLAYBACK;
import static com.android.car.media.testmediaapp.loader.TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_REFRESH_ITEM;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.StringRes;

import java.util.function.Consumer;

public class ActionResultSender {
    private final Context mContext;
    private final Bundle mResultBundle;
    private Consumer<Bundle> mSendFunction = bundle -> {};
    private Runnable mCompleteFunction = () -> {};
    private long mDelay = 0;
    private Object mToken = new Object();
    private final Handler mHandler;

    public ActionResultSender(Context context, Handler handler) {
        mContext = context;
        mResultBundle = new Bundle();
        mHandler = handler;
    }

    ActionResultSender setRefreshMediaId(String mediaId) {
        mResultBundle.putString(BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_REFRESH_ITEM, mediaId);
        return this;
    }

    public ActionResultSender setShowPlaybackView(boolean show) {
        if (show){
            mResultBundle.putString(BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_OPEN_PLAYBACK, null);
        } else {
            mResultBundle.remove(BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_OPEN_PLAYBACK);
        }
        return this;
    }

    public ActionResultSender setBrowseNode(String mediaId) {
        mResultBundle.putString(BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_BROWSE_NODE, mediaId);
        return this;
    }

    ActionResultSender setMessage(@StringRes int stringRes) {
        mResultBundle.putString(BROWSE_CUSTOM_ACTIONS_EXTRA_RESULT_MESSAGE,
                mContext.getString(stringRes));
        return this;
    }

    ActionResultSender sendTo(Consumer<Bundle> sendFunction) {
        mSendFunction = sendFunction;
        return this;
    }

    ActionResultSender sendTo(Object token, Consumer<Bundle> sendFunction) {
        mToken = token;
        mSendFunction = sendFunction;
        return this;
    }

    ActionResultSender sendToDelayed(Object token, long delay, Consumer<Bundle> sendFunction) {
        mToken = token;
        mDelay = delay;
        mSendFunction = sendFunction;
        return this;
    }

    ActionResultSender onComplete(Runnable runnable) {
        mCompleteFunction = runnable;
        return this;
    }

    void send() {
        mHandler.removeCallbacksAndMessages(mToken);
        mHandler.postDelayed(() -> mCompleteFunction.run(), mToken, mDelay);
        mHandler.postDelayed(() -> mSendFunction.accept(mResultBundle), mToken, mDelay);
    }
}
