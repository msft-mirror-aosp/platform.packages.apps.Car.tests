/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.car.media.testmediaapp.media3

import android.os.Bundle
import androidx.car.app.annotations2.ExperimentalCarApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession


@UnstableApi
@ExperimentalCarApi
class TmaBrowser3 : MediaLibraryService() {

    /** Extras key to allow Android Auto to identify the browse service from the media session.  */
    private val BROWSE_SERVICE_FOR_SESSION_KEY = "android.media.session.BROWSE_SERVICE"
    private lateinit var mediaLibrarySession: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        val delegate = TmaMedia3BrowserDelegate(this)
        val player = delegate.getPlayer()
        val extras = Bundle()
        extras.putString(BROWSE_SERVICE_FOR_SESSION_KEY, TmaBrowser3::class.qualifiedName)
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, delegate)
                .setId("TEST_MEDIA3_SESSION")
                .setExtras(extras)
                .build()
        delegate.initialize(mediaLibrarySession)

        mediaLibrarySession.sessionExtras = extras // TODO(media3) remove once setExtras works
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession.release()
        super.onDestroy()
    }


}