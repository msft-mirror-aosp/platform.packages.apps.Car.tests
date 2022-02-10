package com.android.car.media.testmediaapp.carapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import com.android.car.media.testmediaapp.R;

/**
 * Demo {@link Screen} for showing off car-screen settings in a media app.
 * Consists of a single-level hierarchy with one toggleable element.
 */
public class TmaSettingsScreen extends Screen {

    /* Static allows this to persist across multiple screen opens during a session. */
    private static boolean toggleSettingValue = false;

    protected TmaSettingsScreen(@NonNull CarContext carContext) {
        super(carContext);

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getCarContext().finishCarApp();
            }
        };
        carContext.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Toggle.OnCheckedChangeListener listener = isChecked -> {
            toggleSettingValue = isChecked;
        };
        Toggle toggle = new Toggle.Builder(listener).setChecked(toggleSettingValue).build();
        Row row = new Row.Builder().setToggle(toggle)
            .setTitle(getCarContext().getResources().getString(R.string.car_settings_toggle_item))
            .build();
        ItemList itemList = new ItemList.Builder().addItem(row).build();
        return new ListTemplate.Builder()
            .setSingleList(itemList)
            .setLoading(false)
            .setHeaderAction(Action.BACK)
            .setTitle(getCarContext().getResources().getString(R.string.car_settings_header))
            .build();
    }
}
