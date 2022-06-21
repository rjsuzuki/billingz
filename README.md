# Billingz

This is an opinionated, but convenient library for implementing Android's Google Play Billing Library and/or Amazon Appstore's In-App Purchasing API.
Through a combination of Adapter and Facade design patterns, this library allows a project to integrate both billing libraries without explicit references to either, reduces the amount of needed code for integration, and speeds up development environments.
- Supports Android's LiveData and Lifecycle components
- Supports Kotlin Coroutines and Flow
[![build_and_publish](https://github.com/rjsuzuki/billingz/actions/workflows/release-package.yml/badge.svg)](https://github.com/rjsuzuki/billingz/actions/workflows/release-package.yml) [![](https://jitpack.io/v/rjsuzuki/billingz.svg)](https://jitpack.io/#rjsuzuki/billingz)


Currently supports up to: 
   - `google billing: 4.1.0`  
   - `amazon in-app: 2.0.76`  (v2.0.6)
   - `amazon appstore sdk: 3.0.2` (v2.1.0)
## Version History

- new Appstore SDK from Amazon starting from: `v2.1.0+`

[Release History and Notes](https://github.com/rjsuzuki/billingz/releases)

## Documentation

- [Click here to view the wiki](https://github.com/rjsuzuki/billingz/wiki)
- [Click here to view the documentation](https://rjsuzuki.github.io/billingz/)

## Requirements

- minSdk     = 21
- compileSdk = 31
- targetSdk  = 31


## Testing your integration
1. Review the Android documentation for testing in-app billing [here](https://developer.android.com/google/play/billing/test#testing-purchases)
2. Sign into your [Google Play Developer Account](https://play.google.com/apps/publish/) and setup [application licensing](https://developer.android.com/google/play/licensing/overview.html)
3. In Play Console > navigate to Settings > Account details > "License Testing" > add your testers Gmail address > Save

## Permissions

Declared permissions in the AndroidManifest file.

- `com.android.vending.BILLING` (only for Google Play)
- `android.permission.ACCESS_NETWORK_STATE`

## Changelog

- [Keep a changelog](https://keepachangelog.com/en/1.0.0/)

## Bug Reporting

- Create an Issue through the repository's github Issues page.

## Special Acknowledgements

- [TextMe, Inc.](https://www.textmeinc.com)

## Licensing

Apache License 2.0
The complete license can be found in the `LICENSE.md` file in the root directory of this project.

Copyright (c) 2021 [rjsuzuki](https://github.com/rjsuzuki)
 
