#!/bin/bash

# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Download and start the Android Automotive Emulator.
#
# Simple script to launch AAOS emulator builds in ci.android.com
# This tool can be used to test Car apps or 3rd party apps on the latest
# AAOS builds easily.
#
# Pre-requisite:
#  1. Linux or Mac
#  2. Curl
#  3. Make this script as executable (chmod +x)
#
# Typical usage:
#  1. Visit https://ci.android.com/builds/branches/aosp-main-throttled/grid?legacy=1
#  2. Find the latest AAOS emulator build id in ci.android.com e.g. 1126293
#  3. Launch AAOS emulator build by using this script e.g. launch_emu.sh -i -a 1126293
#  4. To clean the current locally installed emulator builds, launch_emu.sh -c
#  5. Re-do from step 1 to install the AAOS emulator build
#
# e: Emulator Build ID
# a: Automotive Build ID
# w: working directory for installation of emulator images and binaries
# h: type of environment (linux or darwin)
# p: download and package emulator images and binaries
# i: download and/or install emulator images and binaries
# l: local system image zip file name
# I: only download and/or install once
# k: specify an APK, or a ZIP file containing APKs to install upon startup
# s: specify screen size (default: 1080x720)
# g: specify the cpu arch, like x86_64 (default: x86)
# v: emulator options, like, -no-snapshot and -allow-host-audio. More can
#    be found at https://developer.android.com/studio/run/emulator-comparison
#
# Download and run the latest builds from a working directory
# ... -w /tmp/linux
#
# Download and package the latest builds for linux
# ... -w /tmp/linux -h linux -p
#
# Download and package the latest builds for mac
# ... -w /tmp/darwin -h darwin -p
#
# Download and run a specific build
# ... -i -e 4826754 -a 10448730
#
# Install with supplementary APKs
# ... -i -k MyApp.apk
# ... -i -k MyApps.zip
#
# Start emulator with host audio input and no snapshot (this assumes the package is installed)
# ./launch_emu.sh -v "-no-snapshot -allow-host-audio"
#
# To make the emulator remountable  (this assumes the package is installed):
# ./launch_emu.sh -v “-writable-system”
#

# Get extracted data CRC-32 for all top-level files/directories
# from the zip file header.
function ziphash() {
  # process all crc-32 hashes into a single line
  echo "$(unzip -v "$1" | awk '(NR>3){print $7}' | tr -d '\n')"
  return 0;
}

colorClear='\033[0m'
colorWarning='\033[1;35m'
colorError='\033[1;31m'

ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL=5
CORES="4"
MEMORY="6144"

function echoWarning() {
    echo -e "${colorWarning}WARN: $1${colorClear}"
}

function echoError() {
    echo -e "${colorError}ERROR: $1${colorClear}"
}

### Help message
usage="
Version: Nov 8, 2023 updated

Usage:
    -m  branch name (default: aosp-master-throttled)
    -t  target name (default: sdk_car_x86_64-userdebug)
    -a  build id (this is mandatory option on gMac)
    -b  QEMU branch name
    -e  QEMU emulator build id
    -w  working directory
    -h  host type
    -k  apks to install
    -s  skin
    -p  package only
    -I  install once
    -i  install
    -l  local system image zip file name
    -q  package after
    -g  specify the cpu arch
    -v  emulator options
    -c  clear workspace"

### Parse input parameters
while getopts "b:t:m:e:a:w:h:k:s:v:g:l:cpIiq" opt; do
  case $opt in
    m) BRANCH_AAE=$OPTARG;;
    t) TARGET=$OPTARG;;
    e) EMUBID=$OPTARG;;
    a) AAEBID=$OPTARG;;
    w) ROOTDIR=$OPTARG;;
    h) HOST_TYPE=$OPTARG;;
    k) APKS_TO_INSTALL=$OPTARG;;
    s) SKIN=$OPTARG;;
    p) PACKAGE_ONLY="true";;
    I) INSTALL_ONCE="true" && INSTALL="true";;
    i) INSTALL="true";;
    l) LOCALZIP=$OPTARG;;
    q) PACKAGE_AFTER="true";;
    v) EMU_OPTIONS=$OPTARG;;
    g) ARCH_OPTIONS=$OPTARG;;
    c)
      rm -rf sdkhome
      rm -rf sdkroot
      rm sdk-repo-linux-system-images-*
      exit
      ;;
    [?])
      SELF="$PWD/launch_emu.sh"
      echo "$usage"
      echo ""
      echo "Typical usages:"
      echo " $SELF -i -a <build id>  # Download and install specific build"
      echo " $SELF -i -l <system_img_file> # Install with local system image"
      echo " $SELF  # Run installed emulator"
      echo " $SELF -v \"-no-snapshot -wipe-data\"  # Run emulator with options"
      exit
      ;;
  esac
done

# If local zip exists, convert relative path to absolute one
if [[ ! -z "$LOCALZIP" ]]; then
  if [[ -f "$LOCALZIP" ]]; then
    LOCALZIP="$(cd "$(dirname "$LOCALZIP")"; pwd)/$(basename "$LOCALZIP")"
  else
    echoError "$LOCALZIP is not exist. Please check."
    exit
  fi
fi

### Change to working directory
if [[ -z "$ROOTDIR" ]]; then
  ROOTDIR="."
fi
if [[ ! -f "$ROOTDIR" ]]; then
  mkdir -p "$ROOTDIR"
fi
cd "$ROOTDIR"

# Copy local zip file to working directory
if [[ -f "$LOCALZIP" ]]; then
  echo "Copy $LOCALZIP to working directory."
  cp "$LOCALZIP" ./sdk-repo-linux-system-images-local.zip
fi

### Validate and initialize the parameters
if [[ -z "$HOST_TYPE" ]]; then
  if [[ "$(uname)" == "Darwin" ]]; then
    HOST_TYPE="darwin"
  elif [[ "$(uname)" == "Windows" ]]; then
    HOST_TYPE="windows"
  else
    HOST_TYPE="linux"
  fi
fi

if [[ -z "$ARCH_OPTIONS" ]]; then
  if [[ "$HOST_TYPE" == "darwin" && "$(uname -m)" == 'arm64' ]]; then
    ARCH_OPTIONS="arm64"
  else
    ARCH_OPTIONS="x86_64"
  fi
fi

# Default values for branch, target and build id
default_branch="aosp-master-throttled"
default_target="sdk_car_x86_64-trunk_staging-userdebug"
default_aae_bid="11049203"
default_qemu_branch="aosp-emu-master-dev"
default_qemu_bid="10558851"
default_sdk_branch="aosp-sdk-release"
default_sdk_bid="9570255"

# QEMU emulator target and host type
case $HOST_TYPE in
  "linux")
    EMU_TARGET="emulator-linux_x64_gfxstream"
    EMU_HOST_TYPE="linux";;
  "windows")
    EMU_TARGET="emulator-windows_x64_gfxstream"
    EMU_HOST_TYPE="windows";;
  "darwin")
    if [[ "$ARCH_OPTIONS" == 'arm64' ]]; then
      EMU_TARGET="emulator-mac_aarch64_gfxstream"
      EMU_HOST_TYPE="darwin_aarch64"
    else
      EMU_TARGET="emulator-mac_x64_gfxstream"
      EMU_HOST_TYPE="darwin"
    fi;;
esac

if [[ -z "$BRANCH_AAE" ]]; then
  BRANCH_AAE="$default_branch"
fi

if [[ -z "$BRANCH_EMU" ]]; then
  BRANCH_EMU="$default_qemu_branch"
fi

if [[ -z "$TARGET" ]]; then
  TARGET="$default_target"
fi

if [[ -z "$AAEBID" ]]; then
  AAEBID=$default_aae_bid
  AAEZIP=$(ls sdk-repo-linux-system-images-*.zip | tail -1)
else
  AAEZIP=sdk-repo-linux-system-images-${AAEBID}.zip
fi

if [[ -z "$EMUBID" ]]; then
  # Check if any emulator zip file is exist
  EMUZIP=$(ls sdk-repo-${EMU_HOST_TYPE}-emulator-*.zip 2> /dev/null | tail -1)
  if [[ (! -f ${EMUZIP}) ]]; then
    EMUBID="$default_qemu_bid"
  fi
else
  EMUZIP=sdk-repo-${EMU_HOST_TYPE}-emulator-${EMUBID}.zip
fi

if [[ -z "$PACKAGE_ONLY" ]]; then
  PACKAGE_ONLY="false"
fi

if [[ -z "$INSTALL" ]]; then
  INSTALL="false"
fi

if [[ -z "$SKIN" ]]; then
  DISP_WIDTH="1080"
  DISP_HEIGHT="600"
  DISP_DENSITY="120"
  SKIN="${DISP_WIDTH}x${DISP_HEIGHT}"
fi

if [[ -z "$PACKAGE_AFTER" ]]; then
  PACKAGE_AFTER="false"
else
  INSTALL="true"
fi

# SDK
BRANCH_SDK="$default_sdk_branch"
PLATBID="$default_sdk_bid"
if [[ $HOST_TYPE == "darwin" ]]; then
  PLAT_TARGET="sdk_mac"
else
  PLAT_TARGET="sdk"
fi
BUILDTOOL_VERSION="latest"
BUILDZIP="sdk-repo-${HOST_TYPE}-build-tools"
DESCRIPTION="Android Automotive"
NAME="aae"
PLATZIP="sdk-repo-${HOST_TYPE}-platform-tools"
VARIANT="android-car"

QEMU_URL="https://androidbuildinternal.googleapis.com/android/internal/build/v3/builds/$EMUBID/$EMU_TARGET/attempts/latest/artifacts/sdk-repo-${EMU_HOST_TYPE}-emulator-${EMUBID}.zip/url"
AAE_IMG_URL="https://androidbuildinternal.googleapis.com/android/internal/build/v3/builds/$AAEBID/$TARGET/attempts/latest/artifacts/sdk-repo-linux-system-images-${AAEBID}.zip/url"
BUILD_TOOL_URL="https://androidbuildinternal.googleapis.com/android/internal/build/v3/builds/$PLATBID/$PLAT_TARGET/attempts/latest/artifacts/$BUILDZIP-$PLATBID.zip/url"
PLAT_TOOL_URL="https://androidbuildinternal.googleapis.com/android/internal/build/v3/builds/$PLATBID/$PLAT_TARGET/attempts/latest/artifacts/$PLATZIP-$PLATBID.zip/url"

# Make sure Ctrl+C stops the entire script. Without this Ctrl+C will kill the
# currently running command.
trap "exit" INT

if [[ ( "$INSTALL" == "true" ) && ( "$INSTALL_ONCE" == "true" ) && (-f "$ROOTDIR/.installed") ]]; then
  # check if zip files changed since the last run

  # .installed file empty -> can't validate, assume changed
  if [[ (! -s "$ROOTDIR/.installed") ]]; then
    echo "Cannot check for zip file changes. Reinstalling"
  else
    # .installed file has data -> check
    INSTALLED_HASHES=$(cat "$ROOTDIR/.installed")
    _PLATZIP=$(ls ${PLATZIP}-*.zip 2> /dev/null | head -1)
    _BUILDZIP=$(ls ${BUILDZIP}-*.zip 2> /dev/null | head -1)
    ZIP_HASHES="SYSTEM:$(ziphash "$AAEZIP");PLATFORM:$(ziphash "$_PLATZIP");EMULATOR:$(ziphash "$EMUZIP");BUILDTOOLS:$(ziphash "$_BUILDZIP")"
    if [[ "$INSTALLED_HASHES" == "$ZIP_HASHES" ]]; then
      echo "Already installed, launching emulator."
      INSTALL="false"
    else
      echo "The zip files have changed since the last run."
      read -p "Run existing version(r), overwrite existing(o), clean-install(c) or abort(any other key)?" -n 1 -r
      echo ""
      case $REPLY in
        r)
          echo "Launching emulator without extracting the zip files."
          INSTALL="false"
        ;;
        o)
          echo "Overwriting existing install without cleanup."
          echo "Warning: this might cause unexpected behavior."
        ;;
        c)
          echo "Cleaning existing installation"
          echo "Removing: ${ROOTDIR}/sdkhome"
          rm -rf "${ROOTDIR}/sdkhome"
          echo "Removing ${ROOTDIR}/sdkroot"
          rm -rf "${ROOTDIR}/sdkroot"
          rm "${ROOTDIR}/.installed"
          echo "Cleanup completed. Launching emulator"
        ;;
        *)
          echo "Aborted."
          exit 1
      esac
    fi
  fi
fi

### Download artifacts from the build server
if [[ ( "$INSTALL" == "true" || "$PACKAGE_ONLY" == "true" ) ]]; then
  if [[ (! -z "$EMUBID") && (! -f ${EMUZIP}) ]]; then
    echo "===================="
    echo "Fetch emulator by using sdk-repo-${EMU_HOST_TYPE}-emulator-${EMUBID}.zip"
    # shellcheck disable=SC2086
    curl -L "$QEMU_URL" -H "Accept: application/json" -o "sdk-repo-${EMU_HOST_TYPE}-emulator-${EMUBID}.zip"
  fi

  if [[ (! -z "$AAEBID") && (! -z "$BRANCH_AAE") && (! -z "$TARGET") && (! -f "$AAEZIP") ]]; then
    echo "Fetch system image by using --bid $AAEBID --target $TARGET"
    curl -L "$AAE_IMG_URL" -H "Accept: application/json" -o "sdk-repo-linux-system-images-$AAEBID.zip"
  fi

  if [[ ! -f "$(ls ${BUILDZIP}-*.zip | head -1)" ]]; then
    echo "===================="
    echo "Fetch build tools by using $BUILDZIP-$PLATBID.zip"
    # shellcheck disable=SC2086
    curl -L "$BUILD_TOOL_URL" -H "Accept: application/json" -o "$BUILDZIP-$PLATBID.zip"
  fi

  if [[ ! -f "$(ls ${PLATZIP}-*.zip | head -1)" ]]; then
    echo "===================="
    echo "Fetch platform tools by using $PLATZIP-$PLATBID.zip"
    # shellcheck disable=SC2086
    curl -L "$PLAT_TOOL_URL" -H "Accept: application/json" -o "$PLATZIP-$PLATBID.zip"
  fi
fi

if [[ -z "$EMUZIP" ]]; then
  EMUZIP=$(ls sdk-repo-${EMU_HOST_TYPE}-emulator-*.zip | tail -1)
fi
if [[ -z "$AAEZIP" ]]; then
  AAEZIP=$(ls sdk-repo-linux-system-images-*.zip | tail -1)
fi
PLATZIP=$(ls ${PLATZIP}-*.zip | head -1)
BUILDZIP=$(ls ${BUILDZIP}-*.zip | head -1)
README_PACKAGE_AFTER="$ROOTDIR/README_PREINSTALLED"

### Check that the zip files are found if drop is not pre-installed.
if [[ ! -f "$README_PACKAGE_AFTER" ]]; then
  echo "===================="
  if [[ -f "$EMUZIP" ]]; then
    echo "Emulator: ${EMUZIP}"
  else
    echoError "Emulator ${EMUZIP} not found. Try installing first: launch_emu.sh -i" >&2
    ERROR_NOT_FOUND="true"
  fi

  if [[ -f "$AAEZIP" ]]; then
    echo "Android images: ${AAEZIP}"
  else
    echoError "Android images ${AAEZIP} not found. Try installing first: launch_emu.sh -i" >&2
    ERROR_NOT_FOUND="true"
  fi

  if [[ -f "$PLATZIP" ]]; then
    echo "Platform tools: ${PLATZIP}"
  else
    echoError "Platform tools ${PLATZIP} not found. Try installing first: launch_emu.sh -i" >&2
    ERROR_NOT_FOUND="true"
  fi

  if [[ -f "$BUILDZIP" ]]; then
    echo "Build tools: ${BUILDZIP}"
  else
    echoError "Build tools ${BUILDZIP} not found. Try installing first: launch_emu.sh -i" >&2
    ERROR_NOT_FOUND="true"
  fi
fi

if [[ "$ERROR_NOT_FOUND" == "true" ]]; then
  echoError "Cannot install emulator package. Exiting." >&2
  exit
fi

# Extract the image build id
[[ "${AAEZIP}" =~ sdk-repo-linux-system-images-(.*).zip ]]
PACKAGE_BUILD_ID=${BASH_REMATCH[1]}

if [[ "$PACKAGE_ONLY" == "true" ]]; then
  # Create a copy of this script
  LAUNCH_EMU="launch_emu.sh"
  echo "$(cat "$0")" > "${LAUNCH_EMU}"
  cat <<EOT >> "emulator"
#!/bin/bash
ARGS="\$@"
./launch_emu.sh -I -v "\$ARGS"
EOT
  chmod +x ${LAUNCH_EMU}
  chmod +x emulator

  # Create the package zip file
  PACKAGE=aae-emu-"${HOST_TYPE}"-"${PACKAGE_BUILD_ID}".zip
  zip "${PACKAGE}" "${EMUZIP}" "${AAEZIP}" "${PLATZIP}" "${BUILDZIP}" "launch_emu.sh" emulator
  echo "All Files Downloaded. Package created: ${ROOTDIR}/${PACKAGE}. Exiting."
  exit
fi


### Set environment variables
export ANDROID_SDK_HOME="${ROOTDIR}/sdkhome"
export ANDROID_SDK_ROOT="${ROOTDIR}/sdkroot"
echo "SDK home dir: ${ANDROID_SDK_HOME}"
echo "SDK tool dir: ${ANDROID_SDK_ROOT}"

if [[ -z "${API_LEVEL}" ]]; then
  ## Check api level inside the zip
  BUILD_PROPS_FILE_IN_ZIP=$(unzip -q -l "${AAEZIP}" | grep -m1 build.prop | awk '{print $4}')
  if [[ -n "$BUILD_PROPS_FILE_IN_ZIP" ]]; then
    API_LEVEL_PROP=$(unzip -p "${AAEZIP}" "${BUILD_PROPS_FILE_IN_ZIP}" | grep -m1 ro.system.build.version.sdk)
    if [[ ! -z "${API_LEVEL_PROP}" ]]; then
      API_LEVEL="${API_LEVEL_PROP#*=}"
      echo "Found API=${API_LEVEL}. Found in ${BUILD_PROPS_FILE_IN_ZIP}"
    else
      echoError "Can't find API level. Exiting"
      exit 1
    fi
  else
    echoError "Can't locate build.prop file in ${AAEZIP}"
  fi
fi

### Setup working directory
if [[ "$INSTALL" == "true" ]]; then
  rm -rf "${ROOTDIR}/sdkhome"
  rm -rf "${ROOTDIR}/sdkroot"
  mkdir -p "${ROOTDIR}/sdkhome"
  mkdir -p "${ROOTDIR}/sdkhome/.android/avd"
  mkdir -p "${ROOTDIR}/sdkroot"
  mkdir -p "${ROOTDIR}/sdkroot/build-tools"
  mkdir -p "${ROOTDIR}/sdkroot/platforms"
  mkdir -p "${ROOTDIR}/sdkroot/system-images/android-${API_LEVEL}/${VARIANT}/"
fi

### Copy adb keys
cp ~/.android/adb* "${ANDROID_SDK_HOME}/.android/"


### Create the AVD and config.ini
AVDDIR="${ROOTDIR}/sdkhome/.android/avd/${NAME}.avd"
mkdir -p "$AVDDIR"
AVD_INI="${ROOTDIR}/sdkhome/.android/avd/${NAME}.ini"
if [[ ! -f "$AVD_INI" ]]; then
cat <<EOT >> "${AVD_INI}"
avd.ini.encoding=UTF-8
path=$AVDDIR
path.rel=avd/${NAME}.avd
target=android-${API_LEVEL}
EOT
fi
CONFIG_INI="$AVDDIR/config.ini"
if [[ (-f "$ROOTDIR/config.ini") && (! -f "$CONFIG_INI") ]]; then
  cp "$ROOTDIR/config.ini" "$CONFIG_INI"
  sed -i --regexp-extended "s/image.sysdir.1\s?=.*/image.sysdir.1=system-images\/android-${API_LEVEL}\/${VARIANT}\/${ARCH_OPTIONS}\//g" "$CONFIG_INI"
fi
if [[ ! -f "$CONFIG_INI" ]]; then
cat <<EOT >> "$CONFIG_INI"
abi.type=${ARCH_OPTIONS}
avd.ini.encoding=UTF-8
hw.accelerometer=yes
hw.gyroscope=yes
hw.sensors.light=no
hw.sensors.pressure=no
hw.sensors.humidity=no
hw.sensors.proximity=no
hw.sensors.magnetic_field=no
hw.sensors.orientation=no
hw.sensors.temperature=no
hw.sensor.hinge=no
hw.audioInput=yes
hw.battery=no
hw.camera.back=none
hw.camera.front=none
hw.cpu.arch=${ARCH_OPTIONS}
hw.dPad=yes
hw.device.hash2=MD5:2fa0e16c8cceb7d385c832a4107c0c88
hw.device.manufacturer=AOSP
hw.device.name=${DESCRIPTION}
hw.gps=yes
hw.gsmModem=yes
hw.keyboard=yes
hw.lcd.width=${DISP_WIDTH}
hw.lcd.height${DISP_HEIGHT}
hw.lcd.depth=16
hw.lcd.circular=false
hw.lcd.density=${DISP_DENSITY}
hw.lcd.backlight=true
hw.lcd.vsync=60
hw.mainKeys=no
hw.cpu.ncore=${CORES}
hw.ramSize=${MEMORY}
hw.sdCard=no
hw.trackBall=no
image.sysdir.1=system-images/android-${API_LEVEL}/${VARIANT}/${ARCH_OPTIONS}/
sdcard.size=2G
disk.dataPartition.size=8G
skin.dynamic=no
skin.name=${SKIN}
skin.path=${SKIN}
tag.display=${DESCRIPTION}
tag.id=${VARIANT}
vm.heapSize=64
EOT
fi


if [[ "$HOST_TYPE" == "windows" ]]; then
LAUNCH_EMU_BAT="$ROOTDIR/launch_emu.bat"
if [[ ! -f "$LAUNCH_EMU_BAT" ]]; then
cat <<EOT >> "$LAUNCH_EMU_BAT"
SET ROOTDIR=%CD%
SET ANDROID_SDK_HOME=%ROOTDIR%/sdkhome
SET ANDROID_SDK_ROOT=%ROOTDIR%/sdkroot
ECHO "SDK home dir: " + %ANDROID_SDK_HOME%
ECHO "SDK tool dir: " + %ANDROID_SDK_ROOT%
cd %ANDROID_SDK_ROOT%/emulator
emulator @aae -partition-size 2047 -verbose -no-snapshot -wipe-data -allow-host-audio &
PAUSE
EOT
fi
fi

if [[ "$INSTALL" == "true" ]]; then
  ### Unzip the emulator images
  echo "Extracting $AAEZIP"
  SYS_IMG_PATH="${ROOTDIR}/sdkroot/system-images/android-${API_LEVEL}/${VARIANT}"
  unzip -q -o "$AAEZIP" -d "${SYS_IMG_PATH}/"
  ARCH_FOLDER=$(ls "${SYS_IMG_PATH}/")
  if [[ "${ARCH_FOLDER}" != "${ARCH_OPTIONS}" ]]; then
    echo "Rename ${ARCH_FOLDER} as ${ARCH_OPTIONS} under ${SYS_IMG_PATH}/"
    mv "${SYS_IMG_PATH}/${ARCH_FOLDER}" "${SYS_IMG_PATH}/${ARCH_OPTIONS}"
  fi
  echo "Extracting $PLATZIP"
  unzip -q -o "$PLATZIP" -d "${ROOTDIR}/sdkroot/"
  echo "Extracting $EMUZIP"
  unzip -q -o "$EMUZIP" -d "${ROOTDIR}/sdkroot/"
  echo "Extracting $BUILDZIP"
  unzip -q -o "$BUILDZIP" -d "${ROOTDIR}/sdkroot/"

  # find build tools in android-* (alphabetical last directory with this name)
  BUILD_TOOLS_DIR=$(ls -d ${ROOTDIR}/sdkroot/android-*| tail -1)
  mv "${BUILD_TOOLS_DIR}" "${ROOTDIR}/sdkroot/build-tools/${BUILDTOOL_VERSION}"
  echo "SYSTEM:$(ziphash "$AAEZIP");PLATFORM:$(ziphash "$PLATZIP");EMULATOR:$(ziphash "$EMUZIP");BUILDTOOLS:$(ziphash "$BUILDZIP")" > "$ROOTDIR/.installed"
fi

### Setup the PATH
export PATH="${ROOTDIR}/sdkroot/emulator:${ROOTDIR}/sdkroot/tools:${ROOTDIR}/sdkroot/platform-tools:\
${ROOTDIR}/sdkroot/build-tools/${BUILDTOOL_VERSION}:${PATH}"

if [[ "$PACKAGE_AFTER" == "true" ]]; then

cat <<EOT >> "$README_PACKAGE_AFTER"
This drop is pre-installed, no need to run launch_emu.sh with -i in the first run.
EOT

  if [[ "$HOST_TYPE" == "windows" ]]; then
    zip -r aae-emu-"${HOST_TYPE}"-"${PACKAGE_BUILD_ID}" "sdkhome/" "sdkroot/" "launch_emu.bat" "README_PREINSTALLED"
  else
    zip -r aae-emu-"${HOST_TYPE}"-"${PACKAGE_BUILD_ID}" "sdkhome/" "sdkroot/" "launch_emu.sh" "README_PREINSTALLED"
  fi

  echo "Package created, not need to install"
  exit
fi

unset ANDROID_HOME

# Check executables
EMU_VERSION=$(emulator -version)
EMU_RESULT=$?
if [[ "$EMU_RESULT" != "0" ]]; then
  echo "Failed to run emulator. Error: $EMU_RESULT"
  if [[ "$HOST_TYPE" == "darwin" ]]; then
    echo "Executing files downloaded from external sources was prevented by the OS."
    echo "WARNING: Only allow this for files downloaded from trusted sources."
    read -p "Do you want to attempt to fix this error? [y/n] " -n 1 -r
    echo # new line
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit
    else
      echo "Attempting fix."
      xattr -cr "$ANDROID_SDK_ROOT"
      xattr -cr "$ANDROID_SDK_HOME"
    fi
  else
    exit
  fi
fi

### Start the emulator
# Passing EMU_OPTIONS to emulator as-is. Disabling lint warning.
# shellcheck disable=SC2086
emulator @"${NAME}" -partition-size 2048 ${EMU_OPTIONS} &

cd -
echo "Launching the emulator completed."
echo "===================="
wait # for the emulator process to finish.