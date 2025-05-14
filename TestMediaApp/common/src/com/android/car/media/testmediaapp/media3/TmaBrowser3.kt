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

import android.net.Uri
import android.os.Bundle
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowseAction

@UnstableApi
@ExperimentalCarApi
class TmaBrowser3 : MediaLibraryService() {

    private lateinit var mediaLibrarySession: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        val delegate = TmaMedia3BrowserDelegate(this)
        val player = delegate.getPlayer()
        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, delegate)
                .setId("TEST_MEDIA3_SESSION")
                .setCommandButtonsForMediaItems(createCustomActionsList())
                .build()
        delegate.initialize(mediaLibrarySession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession.release()
        super.onDestroy()
    }

    private fun createCustomActionsList(): List<CommandButton> {
        val result = ArrayList<CommandButton>()
        for (browseAction in TmaBrowseAction.entries) {
            val builder =
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setDisplayName(getString(browseAction.mLabelResId))
                    .setIconUri(Uri.parse(browseAction.mIcon))
                    .setSessionCommand(SessionCommand(browseAction.mId, Bundle.EMPTY))
            result.add(builder.build())
        }
        return result
    }
}
