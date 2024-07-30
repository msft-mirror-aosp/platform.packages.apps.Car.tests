/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.media.testmediaapp.automotive;

import static androidx.car.app.mediaextensions.MediaIntentExtras.ACTION_MEDIA_TEMPLATE_V2;
import static androidx.car.app.mediaextensions.MediaIntentExtras.EXTRA_KEY_MEDIA_COMPONENT;
import static androidx.car.app.mediaextensions.MediaIntentExtras.EXTRA_KEY_MEDIA_ID;
import static androidx.car.app.mediaextensions.MediaIntentExtras.EXTRA_KEY_SEARCH_ACTION;
import static androidx.car.app.mediaextensions.MediaIntentExtras.EXTRA_KEY_SEARCH_QUERY;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.car.media.testmediaapp.TmaBrowser;

/**
 * An activity that handles the intents sent to open TestMediaApp and then generates a new intent to
 * open the Media Center.
 *
 * <p>The following adb command will trigger this activity.
 * adb shell am start -W -d [URI]
 * The Uri needs to have the following format:
 * "app://com.android.car.media.testmediaapp?[specific data]
 * The following keys will be handled:
 * mediaId=[value], search=[value], searchAction=[value], and they should be joined together.
 * Examples for the Uri are
 * "app://com.android.car.media.testmediaapp?search=normal\&searchAction=1"
 * "app://com.android.car.media.testmediaapp?mediaId=_ROOT_%23advanced%23art%20nodes%23"
 */
public class TmaTrampolineActivity extends AppCompatActivity {

    private static final String TAG = "TmaTrampolineActivity";
    private static final String KEY_MEDIA_ID = "mediaId";
    private static final String KEY_SEARCH = "search";
    private static final String KEY_SEARCH_ACTION = "searchAction";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "Null uri!");
            return;
        }

        Intent mcIntent = new Intent();
        mcIntent.setAction(ACTION_MEDIA_TEMPLATE_V2);
        mcIntent.putExtra(EXTRA_KEY_MEDIA_COMPONENT,
                new ComponentName(getApplicationContext(), TmaBrowser.class).flattenToString());
        String mediaId = uri.getQueryParameter(KEY_MEDIA_ID);
        String searchQuery = uri.getQueryParameter(KEY_SEARCH);
        if (!TextUtils.isEmpty(mediaId)) {
            mcIntent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId);
        } else if (!TextUtils.isEmpty(searchQuery)) {
            mcIntent.putExtra(EXTRA_KEY_SEARCH_QUERY, searchQuery);
            String action = uri.getQueryParameter(KEY_SEARCH_ACTION);
            if (!TextUtils.isEmpty(action) && TextUtils.isDigitsOnly(action)) {
                mcIntent.putExtra(EXTRA_KEY_SEARCH_ACTION, Integer.parseInt(action));
            }
        }
        startActivity(mcIntent);
        finish();
    }
}
