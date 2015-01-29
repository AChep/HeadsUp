HeadsUp
==========
[![Build Status](https://travis-ci.org/AChep/HeadsUp.svg?branch=master)](https://travis-ci.org/AChep/HeadsUp) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/headsup/localized.png)](https://crowdin.com/project/headsup)

<img alt="Main screen: handling HeadsUp" align="right" height="300"
   src="https://github.com/AChep/HeadsUp/raw/master/screenshots/screenshot2.png" />
<img alt="Main screen" align="right" height="300"
   src="https://github.com/AChep/HeadsUp/raw/master/screenshots/screenshot1.png" />

*HeadsUp will inform you about new notifications by showing a little popup window in top of the screen.*

HeadsUp is a fork of [AcDisplay][AcDisplay] mainly focused on displaying notifications while your device is on. It will inform you about new notifications, while you're browsing/gaming/watching movies, by showing a minimal, beautiful popup, allowing you to open them directly or perform an action.

**[Join app's community on Google+](https://plus.google.com/u/0/communities/109603115264950891558)**
**[Join app's dev channel on freenode](http://webchat.freenode.net?channels=acdisplay)**

<a href="https://play.google.com/store/apps/details?id=com.achep.headsup">
  <img alt="Get HeadsUp on Google Play" vspace="20"
       src="http://developer.android.com/images/brand/en_generic_rgb_wo_60.png" />
</a>

Download & Build
----------------
Clone the project and come in:

``` bash
$ git clone git://github.com/AChep/HeadsUp.git
$ cd HeadsUp/project/
```

To build debug version: (only English and Russian locales included)

``` bash
$ ./gradlew assembleDebug
# Builds all the things. Grab compiled application from ./app/build/outputs/apk/
```

To build release version: (public test key)

``` bash
$ ./gradlew assembleRelease
# You will need to answer 'yes' later.
# Grab compiled application from ./app/build/outputs/apk/
```

To build release version:

``` bash
# First you need to set the path to your keystore and the alias.
# You may want to put those to your ~/.bashrc file to save them
# for future bash sessions.
$ export HEADSUP_SIGN_STORE_FILE=path_to_your_keystore
$ export HEADSUP_SIGN_KEY_ALIAS=key_alias

$ ./gradlew assembleRelease
# You will be asked for passwords in proccess.
# Grab compiled application from ./app/build/outputs/apk/
```

You may also use the Android Studio graphic interface to build.

Import to Android Studio
----------------
- Make sure JDK-7 or later is installed.
- Make sure latest Android Studio is installed.
- Launch Android Studio.
- Select: File -> Import project; and choose ./HeadsUp/project directory.
- Wait until it done.


[AcDisplay]:http://acdisplay.org
