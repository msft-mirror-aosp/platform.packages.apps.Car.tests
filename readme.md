# Car test apps

This repository is only for car test applications.

## Downloading and building the code

* Follow these [instructions](https://source.android.com/docs/automotive/unbundled_apps/integration)

### TestMediaApp

TestMediaApp has two run configurations: test-media-app.automotive for AAOS devices and
test-media-app.mobile for phones. The green Run button should build and install the app.

To see TestMediaApp in Android Auto Projected:

1. Open Android Auto on phone
2. Click hamburger icon at top left -> Settings
3. Scroll to Version at bottom and tap ~10 times to unlock Developer Mode
4. Click kebab icon at top right -> Developer settings
5. Scroll to bottom and enable "Unknown sources"
6. Exit and re-open Android Auto
7. TestMediaApp should now be visible (click headphones icon in phone app to see app picker)

### RotaryPlayground

RotaryPlayground is a test and reference application for the AAOS Rotary framework to use with an
external rotary input device.

Beside building in Android Studio, you can also build and install RotaryPlayground into an AAOS
device:

```
$ make RotaryPlayground
$ adb install -r -g out/target/[path]/system/app/RotaryPlayground/RotaryPlayground.apk
```

* See tools/go_rotary.sh for an example build, install and run the test app in an Android tree. 


### RotaryIME

RotaryIME is a sample input method for rotary controllers.

To build and install RotaryIME onto an AAOS device:
```
$ make RotaryIME
$ adb install -r -g out/target/[path]/system/app/RotaryIME/RotaryIME.apk
```
