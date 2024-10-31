/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.media.testmediaapp.loader;

import static com.android.car.media.testmediaapp.TmaMediaItem.MULTI_ID_SEPARATOR;
import static com.android.car.media.testmediaapp.TmaMediaItem.TREE_PATH_SEPARATOR;
import static com.android.car.media.testmediaapp.loader.TmaLoaderUtils.enumNamesToValues;

import android.util.Log;

import com.android.car.media.testmediaapp.TmaMediaItem.MetadataKey;
import com.android.car.media.testmediaapp.TmaMediaItem.TmaMetadata;
import com.android.car.media.testmediaapp.TmaPublicProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;


public class TmaMediaMetadataReader {

    private static final String TAG = "TmaMetadataReader";

    private static TmaMediaMetadataReader sInstance;

    static synchronized TmaMediaMetadataReader getInstance() {
        if (sInstance == null) {
            sInstance = new TmaMediaMetadataReader();
        }
        return sInstance;
    }

    private final Map<String, MetadataKey> mMetadataKeys;

    private TmaMediaMetadataReader() {
        mMetadataKeys = enumNamesToValues(MetadataKey.values());
    }


    TmaMetadata fromJson(JSONObject object) throws JSONException {
        TmaMetadata result = new TmaMetadata();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String jsonKey = keys.next();
            MetadataKey key = mMetadataKeys.get(jsonKey);
            if (key != null) {
                switch (key.mKeyType) {
                    case DOUBLE:
                        result.putDouble(key, object.getDouble(jsonKey));
                        break;
                    case INT:
                    case LONG:
                        result.putLong(key, object.getLong(jsonKey));
                        break;
                    case TEXT:
                        String value = object.getString(jsonKey);
                        if (key == MetadataKey.MEDIA_ID) {
                            validateMediaId(value);
                        }
                        result.putString(key, value);
                        break;
                    case URI:
                        String uri = TmaPublicProvider.buildUriString(object.getString(jsonKey));
                        result.putString(key, uri);
                        break;
                }
            } else {
                Log.e(TAG, "Ignoring unsupported key: " + jsonKey);
            }
        }
        return result;
    }

    private void validateMediaId(String value) {
        if (value.indexOf(TREE_PATH_SEPARATOR) > 0) {
            throw new IllegalStateException("Illegal " + TREE_PATH_SEPARATOR +
                    " in media id: " + value);
        }
        if (value.indexOf(MULTI_ID_SEPARATOR) > 0) {
            throw new IllegalStateException("Illegal " + MULTI_ID_SEPARATOR +
                    " in media id: " + value);
        }
    }
}
