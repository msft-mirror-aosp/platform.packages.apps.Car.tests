package com.android.car.media.testmediaapp;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.notification.CarPendingIntent;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.android.car.media.testmediaapp.carapp.TmaCarAppService;
import com.android.car.media.testmediaapp.carapp.TmaSession;

import java.util.Collections;
import java.util.List;

/**
 * Empty browser to test having two browsers in the apk.
 * Also provides access to the car settings template service in the browse extras.
 */
public class TmaBrowser2 extends MediaBrowserServiceCompat {

    private BrowserRoot mRoot;
    private MediaSessionCompat mSession;
    private static final String MEDIA_SESSION_TAG = "TEST_MEDIA_SESSION_2";
    private static final String ROOT_ID = "_ROOT_ID_";
    // TODO: replace this key with a direct androidx reference once available
    private static final String CAR_APP_SETTINGS_KEY =
            "androidx.media.BrowserRoot.Extras"
                    + ".APPLICATION_PREFERENCES_USING_CAR_APP_LIBRARY_INTENT";

    @Override
    public void onCreate() {
        super.onCreate();

        ComponentName mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(this);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mbrComponent);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE);
        mSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG, mbrComponent, mbrIntent);
        setSessionToken(mSession.getSessionToken());

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
        result.sendResult(Collections.EMPTY_LIST);
    }

    private PendingIntent getCarSettingsIntent() {
        Intent settingsIntent = new Intent().setComponent(
                new ComponentName(getApplicationContext(), TmaCarAppService.class)).setAction(
                TmaSession.SETTINGS_INTENT_ACTION);

        return CarPendingIntent.getCarApp(getApplicationContext(), /* requestCode= */0,
                settingsIntent, /* flags= */ 0);
    }
}
