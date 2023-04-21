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

import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.DISPLAY;
import static com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.AnalyticsState.ANALYTICS_ON;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
                                        .map(event ->
                                                mRes.getString(R.string.analytics_prefs_output,
                                                        DateFormat.format("dd-MM-yyyy hh:mm:ss",
                                                                event.getTime()).toString(),
                                                        event.getEventType().toString(),
                                                        event.getAnalyticsVersion(),
                                                        event.getSessionId(),
                                                        event.getComponent()))
                                        .collect(Collectors.toCollection(ArrayList::new)));
                });
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
