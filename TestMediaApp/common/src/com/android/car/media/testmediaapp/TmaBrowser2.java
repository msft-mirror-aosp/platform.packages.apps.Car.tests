package com.android.car.media.testmediaapp;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.notification.CarPendingIntent;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.media.utils.MediaConstants;

import com.android.car.media.testmediaapp.carapp.TmaCarAppService;
import com.android.car.media.testmediaapp.carapp.TmaSession;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

import java.util.Collections;
import java.util.List;

/**
 * Empty browser to test having two browsers in the apk.
 * Also provides access to the car settings template service in the browse extras.
 */
public class TmaBrowser2 extends MediaBrowserServiceCompat {

    private TmaPrefs mPrefs;
    private BrowserRoot mRoot;
    private MediaSessionCompat mSession;
    private static final String MEDIA_SESSION_TAG = "TEST_MEDIA_SESSION_2";
    private static final String ROOT_ID = "_ROOT_ID_";
    // TODO: replace these keys with a direct androidx reference once available
    private static final String CAR_APP_SETTINGS_KEY =
            "androidx.media.BrowserRoot.Extras"
                    + ".APPLICATION_PREFERENCES_USING_CAR_APP_LIBRARY_INTENT";
    private static final String CAR_APP_ERROR_RESOLUTION_KEY =
            "androidx.media.PlaybackStateCompat.Extras"
                    + ".ERROR_RESOLUTION_USING_CAR_APP_LIBRARY_INTENT";

    private final TmaPrefs.PrefValueChangedListener<TmaEnumPrefs.TmaAccountType> mOnAccountChanged =
            (oldValue, newValue) -> {
                if (mSession != null) {
                    if (newValue != TmaEnumPrefs.TmaAccountType.NONE) {
                        // User is FREE or PAID, provide access to playback
                        mSession.setPlaybackState(makeNonErrorPlayBackState());
                    } else {
                        // User is not logged in, send error playback state
                        mSession.setPlaybackState(makeTemplatedErrorHandlingPlaybackState());
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = TmaPrefs.getInstance(this);
        mPrefs.mAccountType.registerChangeListener(mOnAccountChanged);

        ComponentName mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(this);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mbrComponent);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE);
        mSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG, mbrComponent, mbrIntent);
        setSessionToken(mSession.getSessionToken());

        if (mPrefs.mAccountType.getValue() == TmaEnumPrefs.TmaAccountType.NONE) {
            mSession.setPlaybackState(makeTemplatedErrorHandlingPlaybackState());
        } else {
            mSession.setPlaybackState(makeNonErrorPlayBackState());
        }

        Bundle browserRootExtras = new Bundle();
        browserRootExtras.putParcelable(CAR_APP_SETTINGS_KEY, getCarSettingsIntent());
        mRoot = new BrowserRoot(ROOT_ID, browserRootExtras);
    }

    @Override
    public void onDestroy() {
        mSession.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
            @Nullable Bundle rootHints) {
        return mRoot;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(Collections.emptyList());
    }

    private PendingIntent getCarSettingsIntent() {
        Intent settingsIntent = new Intent().setComponent(
                new ComponentName(getApplicationContext(), TmaCarAppService.class)).setAction(
                TmaSession.SETTINGS_INTENT_ACTION);

        return CarPendingIntent.getCarApp(getApplicationContext(), /* requestCode= */
                settingsIntent.hashCode(),
                settingsIntent, /* flags= */ 0);
    }

    private PendingIntent getSignInIntent() {
        Intent signInIntent = new Intent().setComponent(
                new ComponentName(getApplicationContext(), TmaCarAppService.class))
                .setAction(TmaSession.SIGN_IN_INTENT_ACTION);
        return CarPendingIntent.getCarApp(getApplicationContext(), /* requestCode= */
                signInIntent.hashCode(), signInIntent, /* flags= */0);
    }

    private PlaybackStateCompat makeTemplatedErrorHandlingPlaybackState() {
        Bundle errorExtras = new Bundle();
        errorExtras
                .putParcelable(
                        CAR_APP_ERROR_RESOLUTION_KEY,
                        getSignInIntent());

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 1.0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED,
                        getString(R.string.no_account_short))
                .setExtras(errorExtras);
        return state.build();
    }

    private PlaybackStateCompat makeNonErrorPlayBackState() {
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f);
        return state.build();
    }
}
