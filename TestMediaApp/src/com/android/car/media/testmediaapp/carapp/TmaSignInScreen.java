package com.android.car.media.testmediaapp.carapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.PinSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;

import com.android.car.media.testmediaapp.R;
import com.android.car.media.testmediaapp.prefs.TmaEnumPrefs.TmaAccountType;
import com.android.car.media.testmediaapp.prefs.TmaPrefs;

public class TmaSignInScreen extends Screen {

    private enum State {
        USERNAME,
        PASSWORD,
        PIN,
        CHOOSING,
    }
    private State mState = State.CHOOSING;
    private String mUsername = "";

    protected TmaSignInScreen(@NonNull CarContext carContext) {
        super(carContext);

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mState != State.CHOOSING) {
                    mState = State.CHOOSING;
                    invalidate();
                } else {
                    // If the user hits back at CHOOSING, exit sign-in
                    getCarContext().finishCarApp();
                }
            }
        };
        carContext.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        TmaPrefs.getInstance(carContext).mAccountType.registerChangeListener(
                (oldValue, newValue) -> {
                    // When TmaAccountType is FREE or PAID (i.e. not NONE), sign-in is done.
                    if (!TmaAccountType.NONE.equals(newValue)) {
                        getCarContext().finishCarApp();
                    }
                });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        switch (mState) {
            case USERNAME:
                return getUsernameSignInTemplate();
            case PASSWORD:
                return getPasswordSignInTemplate();
            case PIN:
                return getPinSignInTemplate();
            case CHOOSING:
                return getChoosingTemplate();
        }
        throw new IllegalStateException("Invalid state: " + mState);
    }

    private Template getChoosingTemplate() {
        Row passRow = new Row.Builder()
                .setTitle(getCarContext().getString(R.string.username_password_sign_in))
                .setOnClickListener(() -> {
                    mState = State.USERNAME;
                    invalidate();
                }).build();
        Row pinRow = new Row.Builder()
                .setTitle(getCarContext().getString(R.string.pin_sign_in))
                .setOnClickListener(() -> {
                    mState = State.PIN;
                    invalidate();
                }).build();
        ItemList itemList = new ItemList.Builder().addItem(passRow).addItem(pinRow).build();
        return new ListTemplate.Builder()
                .setSingleList(itemList)
                .setLoading(false)
                .setHeaderAction(Action.BACK)
                .setTitle(getCarContext().getString(R.string.choose_sign_in))
                .build();
    }

    private Template getPinSignInTemplate() {
        PinSignInMethod pinSignInMethod = new PinSignInMethod("4350");
        return new SignInTemplate.Builder(pinSignInMethod)
                .setTitle(getCarContext().getString(R.string.pin_sign_in))
                .setInstructions(getCarContext().getString(R.string.enter_pin))
                .setHeaderAction(Action.BACK)
                .build();
    }

    private Template getUsernameSignInTemplate() {
        InputSignInMethod userNameInput =
                new InputSignInMethod.Builder(userNameCallback)
                        .setHint(getCarContext().getString(R.string.enter_uname))
                        .setInputType(InputSignInMethod.INPUT_TYPE_DEFAULT)
                        .build();
        return new SignInTemplate.Builder(userNameInput)
                .setTitle(getCarContext().getString(R.string.username_password_sign_in))
                .setHeaderAction(Action.BACK)
                .setInstructions(getCarContext().getString(R.string.enter_uname))
                .build();
    }

    private Template getPasswordSignInTemplate() {
        InputSignInMethod passInput = new InputSignInMethod.Builder(passCallback)
                .setHint(getCarContext().getString(R.string.enter_pass))
                .setInputType(InputSignInMethod.INPUT_TYPE_PASSWORD)
                .build();
        return new SignInTemplate.Builder(passInput)
                .setTitle(getCarContext().getString(R.string.enter_pass))
                .setHeaderAction(Action.BACK)
                .setInstructions(mUsername)
                .build();
    }

    InputCallback userNameCallback =
            new InputCallback() {
                @Override
                public void onInputSubmitted(@NonNull String text) {
                    // If this wasn't a test app, validation would go here.
                    mUsername = text;
                    mState = State.PASSWORD;
                    invalidate();
                }
            };

    InputCallback passCallback = new InputCallback() {
        @Override
        public void onInputSubmitted(@NonNull String text) {
            // If this wasn't a test app, validation would go here.
            signIn();
            getCarContext().finishCarApp();
        }
    };

    private void signIn() {
        TmaPrefs.getInstance(
                getCarContext().getApplicationContext()).mAccountType.setValue(
                TmaAccountType.PAID);
    }
}
