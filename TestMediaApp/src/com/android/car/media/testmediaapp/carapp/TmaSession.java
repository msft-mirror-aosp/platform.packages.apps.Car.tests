package com.android.car.media.testmediaapp.carapp;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;

/**
 * {@link Session} for Test Media App car screens.
 */
public final class TmaSession extends Session {

    public static final String SIGN_IN_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SIGN_IN";
    public static final String SETTINGS_INTENT_ACTION =
            "com.android.car.media.testmediaapp.carapp.SETTINGS";

    @NonNull
    @Override
    public Screen onCreateScreen(@NonNull Intent intent) {
        if (SIGN_IN_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSignInScreen(getCarContext());
        } else if (SETTINGS_INTENT_ACTION.equals(intent.getAction())) {
            return new TmaSettingsScreen(getCarContext());
        }
        throw new IllegalArgumentException("No valid action supplied!");
    }
}
