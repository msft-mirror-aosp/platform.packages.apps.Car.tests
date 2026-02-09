/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.car.media.testmediaapp.carapp

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSession
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.android.car.media.testmediaapp.media3.TmaMedia3BrowserDelegate.Companion.GET_PLATFORM_TOKEN
import com.android.car.media.testmediaapp.media3.TmaMedia3BrowserDelegate.Companion.PLATFORM_TOKEN_KEY
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/** Creates a MediaSession and exposes Media Items of a Media Service */
class MediaSessionController(
    context: Context,
    mediaSessionServiceClass: Class<*>,
    onControllerReady: (MediaSession.Token) -> Unit,
) {

    private var controllerFuture: ListenableFuture<MediaController>
    private var mediaController: MediaController? = null
    private val TAG = "MediaSessionController"

    init {
        val sessionToken = SessionToken(context, ComponentName(context, mediaSessionServiceClass))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    mediaController = controllerFuture.get()
                    val command = SessionCommand(GET_PLATFORM_TOKEN, Bundle.EMPTY)
                    val commandFuture = mediaController?.sendCustomCommand(command, Bundle.EMPTY)

                    commandFuture?.addListener(
                        {
                            try {
                                val result: SessionResult = commandFuture.get()
                                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                                    val platformToken =
                                        result.extras.getParcelable(
                                            PLATFORM_TOKEN_KEY,
                                            MediaSession.Token::class.java,
                                        )
                                    if (platformToken != null) {
                                        onControllerReady(platformToken)
                                    }
                                } else {
                                    Log.w(TAG, "Custom command failed: ${result.resultCode}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting platform token: ${e.message}", e)
                            }
                        },
                        MoreExecutors.directExecutor(),
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting MediaController: ${e.message}", e)
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    /** Play the selected [MediaItem] */
    fun play(mediaItem: MediaItem) {
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }

    /** Pause the currently playing [MediaItem] */
    fun pause() {
        mediaController?.pause()
    }
}
