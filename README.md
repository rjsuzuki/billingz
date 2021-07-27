# Billingz

A simple/convenience library for implementing Android's Billing Library.

Currently supports up to: 
   - `google billing: 3.0.2`
   - `amazon in-app: 2.0.76`
## Version History

`v1.0.0`

## Documentation

[Click here to view full documentation](https://rjsuzuki.github.io/billingz-dokka/)

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
- This is an opinionated design to be used with Android's LiveData and Lifecycle components.

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

## Linting & Style Guide

- [Spotless Linting](https://github.com/diffplug/spotless)

To run Spotless's gradle tasks, navigate to the root directory of the project first:
  (Note: the syntax may be slightly different depending on your environment)

1. To run/validate the project: `./gradlew spotlessCheck`
2. To automatically apply all non-error linting: `./gradlew spotlessApply`

This project defers to many of the default configurations of Spotless for linting and styling, but also adheres to Google's Android coding standards as well.
- [Google's Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

## KDoc Annotations 

kdoc annotations are very similar to javadoc annotations.

- `@param name` Documents a value parameter of a function or a type parameter of a class, property or function. Two acceptable syntaxes are: `@param name description` or `@param[name] description`
- `@return` provides a description of what a method will or can return
- `@constructor` primary constructor of a class
- `@receiver` the receiver of an extension function
- `@property name` Documents the property of a class which has the specified name. This tag can be used for documenting properties declared in the primary constructor, where putting a doc comment directly before the property definition would be awkward.
- `@throws class, @exception class` Documents an exception which can be thrown by a method. Since Kotlin does not have checked exceptions, there is also no expectation that all possible exceptions are documented, but you can still use this tag when it provides useful information for users of the class.
- `@sample identifier` Embeds the body of the function with the specified qualified name into the documentation for the current element, in order to show an example of how the element could be used.
- `@see identifier` will generate a link similar to the {@link} tag, but more in the context of a reference and not inline
- `@author` author of the relevant document
- `@since` specifies which version the class, field, or method was added to the project
- `@Deprecated` gives an explanation of why code was deprecated, when it may have been deprecated, and what the alternatives are
- `@suppress` Excludes the element from the generated documentation. Can be used for elements which are not part of the official API of a module but still have to be visible externally.

- [dokka](https://github.com/Kotlin/dokka/blob/master/README.md)
- [kdoc syntax](https://kotlinlang.org/docs/kotlin-doc.html#block-tags)

To manually generate a new version of the library's documentation, simply navigate to the root directory of the project and run: `./gradlew dokkaHtml`

## References

- [security](https://developer.android.com/google/play/billing/security)
A special case of sensitive data and logic that should be handled in the backend is purchase verification. After a user has made a purchase, you should do the following:

1. Send the corresponding purchaseToken to your backend. This means that you should maintain a record of all purchaseToken values for all purchases.
2. Verify that the purchaseToken value for the current purchase does not match any previous purchaseToken values. purchaseToken is globally unique, so you can safely use this value as a primary key in your database.
3. Use the Purchases.products:get or Purchases.subscriptions:get endpoints in the Google Play Developer API to verify with Google that the purchase is legitimate.
4. If the purchase is legitimate and has not been used in the past, you can then safely grant entitlement to the in-app item or subscription.
5. For subscriptions, when linkedPurchaseToken is set in Purchases.subscriptions:get, you should also remove the linkedPurchaseToken from your database and revoke the entitlement that is granted to the linkedPurchaseToken to ensure that multiple users are not entitled for the same purchase.
6. Note: Do not use orderId to check for duplicate purchases or as a primary key in your database, as not all purchases are guaranteed to generate an orderId. In particular, purchases made with promo codes do not generate an orderId.
Google Play tracks products and transactions using purchase tokens and Order IDs.
7. Ensure to support [voided purchases](https://developers.google.com/android-publisher/voided-purchases)

A purchase token is a string that represents a buyer's entitlement to a product on Google Play. It indicates that a Google user is entitled to a specific product that is represented by a SKU. You can use the purchase token with the Google Play Developer API.
An Order ID is a string that represents a financial transaction on Google Play. This string is included in a receipt that is emailed to the buyer. You can use the Order ID to manage refunds in the used in sales and payout reports.

Life of a purchase
Here's a typical purchase flow for a one-time purchase or a subscription.

Show the user what they can buy.
Launch the purchase flow for the user to accept the purchase.
Verify the purchase on your server.
Give content to the user, and acknowledge delivery of the content. Optionally, mark the item as consumed so that the user can buy the item again.
Subscriptions automatically renew until they are canceled. A subscription can go through the following states:

Active: User is in good standing and has access to the subscription.
Cancelled: User has cancelled but still has access until expiration.
In grace period: User experienced a payment issue, but still has access while Google is retrying the payment method.
On hold: User experienced a payment issue, and no longer has access while Google is retrying the payment method.
Paused: User paused their access, and does not have access until they resume.
Expired: User has cancelled and lost access to the subscription. The user is considered churned at expiration.

## Licensing

Apache License 2.0
The complete license can be found in the `LICENSE.md` file in the root directory of this project.

Copyright (c) 2021 rjsuzuki
 
