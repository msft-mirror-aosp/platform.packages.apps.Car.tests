package com.android.car.media.testmediaapp.phone;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media.utils.MediaConstants;

import com.android.car.media.testmediaapp.R;
import com.android.car.media.testmediaapp.media1.TmaBrowser1;
import com.android.car.media.testmediaapp.prefs.TmaPrefsActivity;

/**
 * Runs on a phone, thus making the browse tree available to bluetooth.
 * TODO: the fake playback doesn't work over BT, might need to send some real bytes...
 */
public class TmaLauncherActivity extends AppCompatActivity {

    private static final String TAG = "TmaLauncherActivity";

    private MediaBrowserCompat mediaBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tma_launcher_activity);

        findViewById(R.id.prefs_button).setOnClickListener(v -> {
            Intent prefsIntent = new Intent();
            prefsIntent.setClass(TmaLauncherActivity.this, TmaPrefsActivity.class);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(prefsIntent);
        });

        Bundle rootHints = new Bundle();
        // TODO: 256 is just a placeholder. We'd better find a proper value.
        rootHints.putInt(MediaConstants.BROWSER_ROOT_HINTS_KEY_MEDIA_ART_SIZE_PIXELS, 256);
        // TODO(media3) add preference to choose between the media1 and media3 services.
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, TmaBrowser1.class),
                mConnectionCallbacks, rootHints);
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                    try {
                        // Create a MediaControllerCompat
                        MediaControllerCompat controller =
                                new MediaControllerCompat(TmaLauncherActivity.this, token);
                        // Save the controller
                        MediaControllerCompat.setMediaController(
                                TmaLauncherActivity.this, controller);
                    } catch (Exception ex) {
                        // ToDo: b/166328624 Workaround for an Android Studio Build error:
                        //          unreported exception RemoteException
                        //       Whereas as an Android Soong Build error:
                        //          RemoteException is never thrown
                        Log.e(TAG, "Failed to create MediaControllerCompat", ex);
                        return;
                    }
                }
            };

    @Override
    public void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mediaBrowser.disconnect();
    }
}
