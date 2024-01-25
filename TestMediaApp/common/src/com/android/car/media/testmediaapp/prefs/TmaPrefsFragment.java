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

package com.android.car.media.testmediaapp.prefs;

import static android.text.format.DateFormat.format;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_ERROR_EVENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_MEDIA_CLICKED_EVENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_UNKNOWN_EVENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_VIEW_CHANGE_EVENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.EVENT_TYPE_VISIBLE_ITEMS_EVENT;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.ANALYTICS_ON;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.DISPLAY;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.preference.DropDownPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.car.media.testmediaapp.R;
import com.android.car.media.testmediaapp.analytics.TmaAnalyticsBroadcastReceiver;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaBrowseNodeType;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaLoginEventOrder;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaReplyDelay;
import com.android.car.media.testmediaapp.prefs.TmaPrefs.PrefEntry;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OptIn(markerClass = androidx.car.app.annotations2.ExperimentalCarApi.class)
public class TmaPrefsFragment extends PreferenceFragmentCompat {

    private TmaPrefs mPrefs;
    private Resources mRes;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        Context context = getPreferenceManager().getContext();
        mPrefs = TmaPrefs.getInstance(context);
        mRes = getResources();

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        screen.addPreference(createEnumPref(context, "Account Type", mPrefs.mAccountType,
                TmaAccountType.values()));
        screen.addPreference(createEnumPref(context, "Root node type", mPrefs.mRootNodeType,
                TmaBrowseNodeType.values()));
        screen.addPreference(createEnumPref(context, "Root reply delay", mPrefs.mRootReplyDelay,
                TmaReplyDelay.values()));
        screen.addPreference(createEnumPref(context, "Asset delay: random value in [v, 2v]",
                mPrefs.mAssetReplyDelay, TmaReplyDelay.values()));
        screen.addPreference(createEnumPref(context, "Login event order", mPrefs.mLoginEventOrder,
                TmaLoginEventOrder.values()));
        screen.addPreference(createClickPref(context, "Request location perm",
                this::requestPermissions));
        screen.addPreference(createEnumPrefFlag(context, "Analytics Opt-in",
                mPrefs.mAnalyticsState, TmaEnumPrefs.AnalyticsState.class));

        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (canPrint()) {
            printAnalytics((ViewGroup) view);
        }
        super.onViewCreated(view, savedInstanceState);
    }

    private void printAnalytics(ViewGroup view) {
        View analyticsView =
                LayoutInflater.from(getContext()).inflate(R.layout.analytics_view, view);
        ((TextView) analyticsView.findViewById(R.id.analytics_header))
                .setText(R.string.analytics_prefs_output_title);
        ListView listView = analyticsView.findViewById(R.id.analytics_list);
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);
        TmaAnalyticsBroadcastReceiver.analyticsEventLiveData.observe(this,
                analyticsEvent -> {
                        arrayAdapter.clear();
                        arrayAdapter.addAll(
                                analyticsEvent
                                        .stream()
                                        .map(event -> {
                                            if(event == null) return null;
                                            long ms = event.getTimestampMillis();
                                            return mRes.getString(R.string.analytics_prefs_output,
                                                    format("dd-MM-yyyy hh:mm:ss", ms).toString(),
                                                    getEventType(event),
                                                    event.getAnalyticsVersion(),
                                                    event.getSessionId(),
                                                    event.getComponent());
                                        })
                                        .collect(Collectors.toCollection(ArrayList::new)));
                });
    }

    private String getEventType(AnalyticsEvent event) {
        switch (event.getEventType()) {
            case EVENT_TYPE_VISIBLE_ITEMS_EVENT: return "VISIBLE_ITEMS";
            case EVENT_TYPE_MEDIA_CLICKED_EVENT: return "MEDIA_CLICKED";
            case EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT: return "BROWSE_NODE_CHANGED";
            case EVENT_TYPE_VIEW_CHANGE_EVENT: return "VIEW_CHANGE";
            case EVENT_TYPE_ERROR_EVENT: return "ERROR";
            case EVENT_TYPE_UNKNOWN_EVENT: return "UNKNOWN";
            default: return "UNEXPECTED!!";
        }
    }

    private boolean isOn() {
        return mPrefs.mAnalyticsState.getValue().getFlags().contains(ANALYTICS_ON.getId());
    }

    private boolean canPrint() {
        return isOn() && mPrefs.mAnalyticsState.getValue().getFlags().contains(DISPLAY.getId());
    }

    private <T extends TmaEnumPrefs.EnumPrefValue> Preference createEnumPref(
            Context context, String title, PrefEntry pref, T[] enumValues) {
        DropDownPreference prefWidget = new DropDownPreference(context);
        prefWidget.setKey(pref.mKey);
        prefWidget.setTitle(title);
        prefWidget.setSummary("%s");
        prefWidget.setPersistent(true);

        int count = enumValues.length;
        CharSequence[] entries = new CharSequence[count];
        CharSequence[] entryValues = new CharSequence[count];
        for (int i = 0; i < count; i++) {
            entries[i] = enumValues[i].getTitle();
            entryValues[i] = enumValues[i].getId();
        }
        prefWidget.setEntries(entries);
        prefWidget.setEntryValues(entryValues);
        return prefWidget;
    }

    private <T extends TmaEnumPrefs.EnumPrefFlag> Preference createEnumPrefFlag(
            Context context, String title, PrefEntry pref,
            Class<? extends TmaEnumPrefs.EnumPrefValue> enumClass) {
        MultiSelectListPreference prefWidget = new MultiSelectListPreference(context);
        prefWidget.setKey(pref.mKey);
        prefWidget.setTitle(title);
        prefWidget.setPersistent(true);

        int count = enumClass.getEnumConstants().length;
        TmaEnumPrefs.EnumPrefValue[] enumValues = enumClass.getEnumConstants();
        CharSequence[] entries = new CharSequence[count];
        CharSequence[] entryValues = new CharSequence[count];
        for (int i = 0; i < count; i++) {
            entries[i] = enumValues[i].getTitle();
            entryValues[i] = enumValues[i].getId();
        }
        prefWidget.setEntries(entries);
        prefWidget.setEntryValues(entryValues);
        return prefWidget;
    }

    private Preference createClickPref(Context context, String title, Consumer<Context> runnable) {
        Preference prefWidget = new Preference(context);
        prefWidget.setTitle(title);
        prefWidget.setOnPreferenceClickListener(pref -> {
            runnable.accept(context);
            return true;
        });
        return prefWidget;
    }

    private void requestPermissions(Context context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission already granted", Toast.LENGTH_SHORT)
                    .show();
        } else {
            ((Activity) context).requestPermissions(
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}
