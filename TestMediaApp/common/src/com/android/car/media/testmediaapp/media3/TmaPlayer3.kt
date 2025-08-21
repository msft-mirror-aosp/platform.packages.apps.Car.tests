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

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionError.ERROR_INVALID_STATE
import androidx.media3.session.SessionError.ERROR_NOT_SUPPORTED
import androidx.media3.session.SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED
import androidx.media3.session.SessionError.ERROR_SESSION_CONCURRENT_STREAM_LIMIT
import androidx.media3.session.SessionError.ERROR_SESSION_CONTENT_ALREADY_PLAYING
import androidx.media3.session.SessionError.ERROR_SESSION_END_OF_PLAYLIST
import androidx.media3.session.SessionError.ERROR_SESSION_NOT_AVAILABLE_IN_REGION
import androidx.media3.session.SessionError.ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED
import androidx.media3.session.SessionError.ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED
import androidx.media3.session.SessionError.ERROR_SESSION_SKIP_LIMIT_REACHED
import androidx.media3.session.SessionError.ERROR_UNKNOWN
import androidx.media3.session.SessionError.INFO_CANCELLED
import com.android.car.media.testmediaapp.R
import com.android.car.media.testmediaapp.TmaLibrary
import com.android.car.media.testmediaapp.TmaMediaEvent
import com.android.car.media.testmediaapp.TmaMediaEvent.EventState.PLAYING
import com.android.car.media.testmediaapp.TmaMediaEvent.ResolutionIntent
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.ACTION_ABORTED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.APP_ERROR
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.AUTHENTICATION_EXPIRED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.CONCURRENT_STREAM_LIMIT
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.CONTENT_ALREADY_PLAYING
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.END_OF_QUEUE
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.NOT_AVAILABLE_IN_REGION
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.NOT_SUPPORTED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.PARENTAL_CONTROL_RESTRICTED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.PREMIUM_ACCOUNT_REQUIRED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.SKIP_LIMIT_REACHED
import com.android.car.media.testmediaapp.TmaMediaEvent.StateErrorCode.UNKNOWN_ERROR
import com.android.car.media.testmediaapp.TmaMediaItem.TmaBrowsedMediaItem
import com.android.car.media.testmediaapp.TmaPlayer
import com.android.car.media.testmediaapp.TmaPlayer.PlayerDelegate
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.stream.Collectors
import kotlin.math.max

@UnstableApi
class TmaPlayer3(
    looper: Looper,
    private val browser: TmaMedia3BrowserDelegate,
    library: TmaLibrary,
    audioManager: AudioManager,
    handler: Handler,
) : SimpleBasePlayer(looper), PlayerDelegate {

    private val fakePlayer = TmaPlayer(this, browser, library, audioManager, handler)
    private var state = buildDefaultState()
    private var accountType = TmaEnumPrefs.TmaAccountType.NONE

    private fun buildDefaultState(): State {
        val commands =
            Commands.Builder()
                .add(COMMAND_GET_METADATA)
                .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(COMMAND_GET_TIMELINE)
                .add(COMMAND_SET_MEDIA_ITEM)
                .build()
        return State.Builder().setAvailableCommands(commands).build()
    }

    override fun getState(): State {
        return state
    }

    override fun handleSetMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        if (mediaItems.size > 0) {
            fakePlayer.playFromMediaId(mediaItems[0].mediaId)
        }
        return Futures.immediateFuture(null)
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            fakePlayer.startPlayBack(true)
        } else {
            fakePlayer.pausePlayback()
        }
        return Futures.immediateFuture(null)
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        if (mediaItemIndex == fakePlayer.activeItemIndex) {
            fakePlayer.seekTo(positionMs)
            state = state.buildUpon().setContentPositionMs(positionMs).build()
            invalidateState()
        } else {
            fakePlayer.playQueueItem(mediaItemIndex)
        }

        return Futures.immediateFuture(null)
    }

    fun onCustomAction(action: String, extras: Bundle) {
        fakePlayer.onCustomAction(action, extras)
    }

    fun setAccountType(newAccount: TmaEnumPrefs.TmaAccountType) {
        accountType = newAccount
        if (accountType == TmaEnumPrefs.TmaAccountType.NONE) {
            // Create a new state.
            state = buildDefaultState()
        }
        invalidateState()
    }

    override fun getImpl(): TmaPlayer = fakePlayer

    private fun toM3ErrorCode(tmaCode: StateErrorCode): @SessionError.Code Int {
        return when (tmaCode) {
            UNKNOWN_ERROR -> ERROR_UNKNOWN
            APP_ERROR -> ERROR_INVALID_STATE
            NOT_SUPPORTED -> ERROR_NOT_SUPPORTED
            AUTHENTICATION_EXPIRED -> ERROR_SESSION_AUTHENTICATION_EXPIRED
            PREMIUM_ACCOUNT_REQUIRED -> ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED
            CONCURRENT_STREAM_LIMIT -> ERROR_SESSION_CONCURRENT_STREAM_LIMIT
            PARENTAL_CONTROL_RESTRICTED -> ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED
            NOT_AVAILABLE_IN_REGION -> ERROR_SESSION_NOT_AVAILABLE_IN_REGION
            CONTENT_ALREADY_PLAYING -> ERROR_SESSION_CONTENT_ALREADY_PLAYING
            SKIP_LIMIT_REACHED -> ERROR_SESSION_SKIP_LIMIT_REACHED
            ACTION_ABORTED -> INFO_CANCELLED
            END_OF_QUEUE -> ERROR_SESSION_END_OF_PLAYLIST
        }
    }

    override fun setPlaybackState(event: TmaMediaEvent) {
        val commands =
            state.availableCommands
                .buildUpon()
                .add(COMMAND_SET_MEDIA_ITEM)
                .add(COMMAND_PLAY_PAUSE)
                .removeIf(COMMAND_PLAY_PAUSE, (event.mState == TmaMediaEvent.EventState.ERROR))
                .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)

        var playbackState: @Player.State Int = STATE_READY
        var playerError: PlaybackException? = null
        if (event.mErrorCode != UNKNOWN_ERROR) {
            val res = browser.context.resources
            val extras = Bundle()
            if (ResolutionIntent.PREFS.equals(event.mResolutionIntent)) {
                extras.putString(
                    EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT,
                    res.getString(R.string.select_account),
                )
                extras.putParcelable(
                    EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT,
                    TmaPrefsActivity.getPendingIntent(browser.context),
                )
            }

            val errorCode = toM3ErrorCode(event.mErrorCode)
            if (event.mState == TmaMediaEvent.EventState.ERROR) {
                val message =
                    if (event.mErrorMessage.isNullOrEmpty()) "TODO(media3) b/376134760"
                    else event.mErrorMessage
                playerError = PlaybackException(message, null, errorCode, extras)
                playbackState = STATE_IDLE
            } else {
                val error = SessionError(errorCode, event.mErrorMessage, extras)
                browser.getSession().sendError(error)
            }
        }

        val playWhenReady = (event.mState == PLAYING)
        state =
            state
                .buildUpon()
                .setAvailableCommands(commands.build())
                .setPlaybackState(playbackState)
                .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlayerError(playerError)
                .setContentPositionMs(fakePlayer.positionMs)
                .build()
        invalidateState()
    }

    private fun uidOf(it: TmaBrowsedMediaItem) =
        Triple(it.mItemIndex, it.mItem.mediaId, it.mParentId)

    override fun setQueue() {
        setQueue(0)
    }

    private fun setQueue(activeItemIndex: Int) {
        val converter: (TmaBrowsedMediaItem) -> MediaItemData = { it ->
            MediaItemData.Builder(uidOf(it))
                .setMediaItem(it.mItem.toMediaItem(fakePlayer.library, it.mParentId))
                .setDefaultPositionUs(0)
                .setDurationUs(max(it.mItem.duration * 1000, 0))
                .build()
        }
        val playlist = fakePlayer.queue.stream().map(converter).collect(Collectors.toList())
        val commands = state.availableCommands.buildUpon().add(COMMAND_SEEK_TO_MEDIA_ITEM)
        state =
            state
                .buildUpon()
                .setAvailableCommands(commands.build())
                .setCurrentMediaItemIndex(activeItemIndex)
                .setPlayerError(null)
                .setPlaylist(playlist)
                .build()
        invalidateState()
    }

    override fun updateActiveItemMetadata() {
        setQueue(fakePlayer.activeItemIndex)
    }

    override fun maybeActivateSession() {}

    override fun resetMetadata() {}

    override fun setErrorState(message: String?) {
        // TODO(media3) cleanup error handling.
        TODO("Not yet implemented")
    }

    override fun setCurrentPlayingItem(activeItem: TmaBrowsedMediaItem) {
        val uid = uidOf(activeItem)
        val index = state.playlist.indexOfFirst { it.uid == uid }
        if (0 <= index) {
            val commands =
                state.availableCommands
                    .buildUpon()
                    .add(COMMAND_SEEK_TO_NEXT)
                    .add(COMMAND_SEEK_TO_PREVIOUS)
            if (index == 0) commands.remove(COMMAND_SEEK_TO_PREVIOUS)
            if (index == state.playlist.lastIndex) commands.remove(COMMAND_SEEK_TO_NEXT)

            val playWhenReady = (activeItem.mItem.mMediaEvents.get(0).mState == PLAYING)

            state =
                state
                    .buildUpon()
                    .setAvailableCommands(commands.build())
                    .setCurrentMediaItemIndex(index)
                    .setPlaybackState(STATE_READY)
                    .setPlayerError(null)
                    .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .setContentPositionMs(fakePlayer.positionMs)
                    .build()

            val actions = ArrayList<CommandButton>()
            if (activeItem.mItem.mCustomActions.size > 0) {
                val res = browser.context.resources
                for (action in activeItem.mItem.mCustomActions) {
                    val bb =
                        CommandButton.Builder(action.mIconId)
                            .setSessionCommand(SessionCommand(action.mId, Bundle.EMPTY))
                            .setDisplayName(res.getString(action.mNameId))
                            .setEnabled(true)
                            .setIconResId(action.mIcon)
                    actions.add(bb.build())
                }
            }
            browser.getSession().setCustomLayout(actions)

            invalidateState()
        }
    }

    override fun sendPausePlaybackState() {
        state =
            state
                .buildUpon()
                .setPlaybackState(STATE_READY)
                .setPlayerError(null)
                .setPlayWhenReady(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .build()
        invalidateState()
    }

    override fun stopAndUpdateState() {
        state =
            state
                .buildUpon()
                .setPlaybackState(if (state.playlist.size > 0) STATE_READY else STATE_IDLE)
                .setPlayerError(null)
                .setPlayWhenReady(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .build()
        invalidateState()
    }
}
