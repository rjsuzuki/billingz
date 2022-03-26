# Billingz

A simple/convenience library for implementing Android's Billing Library. [![build_and_publish](https://github.com/rjsuzuki/billingz/actions/workflows/release-package.yml/badge.svg)](https://github.com/rjsuzuki/billingz/actions/workflows/release-package.yml) [![](https://jitpack.io/v/rjsuzuki/billingz.svg)](https://jitpack.io/#rjsuzuki/billingz)


Currently supports up to: 
   - `google billing: 4.1.0`  
   - `amazon in-app: 2.0.76`  
## Version History

[Release History and Notes](https://github.com/rjsuzuki/billingz/releases)

## Documentation

[Click here to view the wiki](https://github.com/rjsuzuki/billingz/wiki)
[Click here to view full documentation](https://rjsuzuki.github.io/billingz/)

## How to add module to your project

1. Clone or download project
2. Open Android Studio > open project you want to install the library into.

Next, choose one of the available methods:

3. File > New > New Module
4. Import .JAR/.AAR Package > click Next
   
or,

3. File > New > Import Module
4. Enter the location of the library module directory then click Finish

continue.

5. Make sure the library is listed at the top of your settings.gradle file,
as shown here for a library named "my-library-module":
`include ':app', ':my-library-module'`
6. Open the app module's build.gradle file and add a new line to the dependencies:
```
dependencies {
    implementation project(":my-library-module")
}
```
7. Sync project with gradle files.
[Android Reference](https://developer.android.com/studio/projects/android-library)

8. Initialize the Manager class in Activity class's `onCreate()` method:
```
override fun onCreate(savedInstanceState: Bundle?) {
  val manager = Manager()
  manager.init(context)
  lifecycle.addObserver(manager)
}
```

## Requirements

- minSdk = 21
- This is an opinionated design to be used with Android's LiveData and Lifecycle components, and Kotlin coroutines.

## Testing your integration
1. Review the Android documentation for testing in-app billing [here](https://developer.android.com/google/play/billing/test#testing-purchases)
2. Sign into your [Google Play Developer Account](https://play.google.com/apps/publish/) and setup [application licensing](https://developer.android.com/google/play/licensing/overview.html)
3. In Play Console > navigate to Settings > Account details > "License Testing" > add your testers Gmail address > Save

## Permissions

Declared permissions in the AndroidManifest file.

- com.android.vending.BILLING
- android.permission.ACCESS_NETWORK_STATE

## Changelog

- [Keep a changelog](https://keepachangelog.com/en/1.0.0/)

## Bug Reporting

- Create an Issue through the repository's github Issues page.

## Special Acknowledgements
- [TextMe, Inc.](www.textmeinc.com)

## Licensing

Apache License 2.0
The complete license can be found in the `LICENSE.md` file in the root directory of this project.

Copyright (c) 2021 [rjsuzuki](https://github.com/rjsuzuki)
 
