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

// TODO(media3) uncomment once the prebuilt has been updated
// import androidx.media3.session.SessionError
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.mediaextensions.analytics.client.RootHintsPopulator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.LibraryResult.ofError
import androidx.media3.session.LibraryResult.ofItem
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
import androidx.media3.session.MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.android.car.media.testmediaapp.R
import com.android.car.media.testmediaapp.TmaBrowserDelegate
import com.android.car.media.testmediaapp.TmaCustomAction
import com.android.car.media.testmediaapp.TmaLibrary
import com.android.car.media.testmediaapp.TmaLibrary.ROOT_MEDIA_ID
import com.android.car.media.testmediaapp.TmaMediaItem
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaReplyDelay
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaSearchMode
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.stream.Collectors

@UnstableApi
@ExperimentalCarApi
class TmaMedia3BrowserDelegate(context: Context) :
    TmaBrowserDelegate(context), MediaLibraryService.MediaLibrarySession.Callback {

    companion object {
        private const val TAG = "TMA3-BrowserDelegate"
    }

    private val audioManager = context.getSystemService<AudioManager>(AudioManager::class.java)
    private lateinit var session: MediaLibraryService.MediaLibrarySession
    private val player: TmaPlayer3 =
        TmaPlayer3(Looper.getMainLooper(), this, mLibrary, audioManager, mHandler)
    private val browserRootExtras = Bundle()
    private val controllers = HashSet<MediaSession.ControllerInfo>()

    fun initialize(session: MediaLibraryService.MediaLibrarySession) {
        this.session = session
        addPrefsListeners()
        updateRootExtras()
    }

    fun getPlayer(): Player {
        return player
    }

    fun getSession(): MediaLibraryService.MediaLibrarySession {
        return session
    }

    override fun updateRootExtras() {
        browserRootExtras.clear()

        // TODO(media3) custom browse actions
        // browserRootExtras.putParcelableArrayList(
        // TmaMetaDataKeys.BROWSE_CUSTOM_ACTIONS_ROOT_LIST, ArrayList(createCustomActionsList()))
        // browserRootExtras.putParcelable(TmaMedia1BrowserDelegate.FAVORITES_MEDIA_ITEM,
        // getFavoritesMediaItem())

        val flags = mPrefs.mAnalyticsState.value.flags
        RootHintsPopulator(browserRootExtras)
            .setAnalyticsOptIn(flags.contains(AnalyticsState.ANALYTICS_ON.id))
            .setSharePlatform(flags.contains(AnalyticsState.SHARE_GOOGLE.id))
            .setShareOem(flags.contains(AnalyticsState.SHARE_OEM.id))

        setAccountType(mPrefs.mAccountType.value)
    }

    override fun setAccountType(accountType: TmaAccountType) {
        player.setAccountType(accountType)
    }

    override fun invalidateRoot() {
        // TODO(media3) how can we update the root extras now ??
        notifyChildrenChanged(TmaLibrary.ROOT_PATH)
    }

    override fun addItemToQueue(mediaId: String?) { // TODO(media3) custom browse actions
    }

    override fun removeItemFromQueue(mediaId: String?) { // TODO(media3) custom browse actions
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        controllers.add(controller)
        return AcceptedResultBuilder(session)
            .setAvailableSessionCommands(getSessionCommands())
            .build()
    }

    override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
        controllers.remove(controller)
        super.onDisconnected(session, controller)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        val updatedMediaItems: MutableList<MediaItem> =
            mediaItems
                .map { mediaItem ->
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .build()
                }
                .toMutableList()
        return Futures.immediateFuture(updatedMediaItems)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        player.onCustomAction(customCommand.customAction, args)
        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    override fun notifyChildrenChanged(parentId: String) {
        Log.i(TAG, "notifyChildrenChanged for $parentId")
        session.notifyChildrenChanged(parentId, Integer.MAX_VALUE, null)
    }

    override fun onSearchModeChanged(oldValue: TmaSearchMode, newValue: TmaSearchMode) {
        for (controller in controllers) {
            session.setAvailableCommands(controller, getSessionCommands(), getPlayerCommands())
        }
    }

    private fun getSessionCommands(): SessionCommands {
        val builder = DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        if (!canSearch()) {
            builder.remove(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
        }
        val commands = ArrayList<SessionCommand>(TmaCustomAction.values().size)
        for (action in TmaCustomAction.values()) {
            commands.add(SessionCommand(action.mId, Bundle.EMPTY))
        }
        builder.addSessionCommands(commands)
        return builder.build()
    }

    private fun getPlayerCommands(): Player.Commands {
        return DEFAULT_PLAYER_COMMANDS
    }

    @OptIn(UnstableApi::class)
    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        // No delay here as getRoot is required to be quick.
        Log.i(TAG, "onGetLibraryRoot ${browser.packageName} Hints: ${stringify(params?.extras)}")

        val rootParams = LibraryParams.Builder().setExtras(browserRootExtras).build()
        return Futures.immediateFuture(getMedia3ItemById(ROOT_MEDIA_ID, rootParams))
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return doLater { getMedia3ItemById(mediaId, null) }
    }

    private fun getMedia3ItemById(mediaId: String, lp: LibraryParams?): LibraryResult<MediaItem> {
        return when (val node = mLibrary.getMediaItemById(mediaId)) {
            null -> ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            else -> ofItem(node.toMediaItem(mLibrary, mLibrary.getParentPath(mediaId)), lp)
        }
    }

    private fun getMedia3Items(
        parentId: String,
        filter: String?,
        params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
        val items = getMediaItems(parentId, filter)
        if (items == null) {
            if (TmaAccountType.NONE == mPrefs.mAccountType.value) {
                Log.i(TAG, "onGetChildren log in required for $parentId")
                val message = context.resources.getString(R.string.no_account)
                val errorExtras = Bundle()
                errorExtras.putString(
                    EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT,
                    context.resources.getString(R.string.select_account),
                )
                errorExtras.putParcelable(
                    EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT,
                    TmaPrefsActivity.getPendingIntent(context),
                )

                // TODO(media3) uncomment once the prebuilt has been updated
                // return ofError(SessionError(RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED,
                //     message, errorExtras))
            } else {
                Log.i(TAG, "onLoadChildren has null result for: $parentId")
                // TODO(media3) uncomment once the prebuilt has been updated
                // return ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params)
            }
        } else {
            val converter: (TmaMediaItem.TmaBrowsedMediaItem) -> MediaItem = { it ->
                it.mItem.toMediaItem(mLibrary, it.mParentId)
            }
            val m3Items = items.stream().map(converter).collect(Collectors.toList())
            Log.i(TAG, "onLoadChildren has ${m3Items.size} children for: $parentId")
            return LibraryResult.ofItemList(m3Items, params)
        }
        return LibraryResult.ofItemList(ImmutableList.of(), params) // TODO(media3) remove
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return doLater { getMedia3Items(parentId, null, params) }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        session.notifySearchResultChanged(browser, query, Integer.MAX_VALUE, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    override fun onSubscribe(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        onSubscriptionDelta(parentId, +1)
        return super.onSubscribe(session, browser, parentId, params)
    }

    override fun onUnsubscribe(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
    ): ListenableFuture<LibraryResult<Void>> {
        onSubscriptionDelta(parentId, -1)
        return super.onUnsubscribe(session, browser, parentId)
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return doLater { getMedia3Items(TmaLibrary.ROOT_PATH, query, params) }
    }

    private fun <T> doLater(expr: () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> {
        when (val delay: TmaReplyDelay = mPrefs.mRootReplyDelay.value) {
            TmaReplyDelay.NONE -> return Futures.immediateFuture(expr())
            else -> {
                val result: SettableFuture<LibraryResult<T>> = SettableFuture.create()
                mHandler.postDelayed({ result.set(expr()) }, delay.mReplyDelayMs.toLong())
                return result
            }
        }
    }
}
