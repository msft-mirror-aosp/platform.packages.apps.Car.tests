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

   public static final String SETTINGS_INTENT_ACTION =
           "com.android.car.media.testmediaapp.carapp.SETTINGS";

   @NonNull
   @Override
   public Screen onCreateScreen(@NonNull Intent intent) {
      return new TmaSettingsScreen(getCarContext());
   }
}
