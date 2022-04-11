package com.android.car.media.testmediaapp.carapp;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

/**
 * Entry point for Test Media App car screens.
 *
 * {@link CarAppService} is the main interface between the app and the car host.
 */
public final class TmaCarAppService extends CarAppService {

  @NonNull
  @Override
  public HostValidator createHostValidator() {
    return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
  }

  @NonNull
  @Override
  public Session onCreateSession() {
    return new TmaSession();
  }
}
