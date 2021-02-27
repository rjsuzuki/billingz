# Billingz

a simple/convenience library for implementing Android's Billing Library.
### Version History
`v1.0.0`
### Architecture


Facade Pattern

Android Billing Lib --> Manager --> Client -->




### How to add module to your project

1. Clone or download project
2. Open Android Studio > open project you want to install the library into.
3a. File > New > New Module
4a. Import .JAR/.AAR Package > click Next
Or.
3b. File > New > Import Module
4b. Enter the location of the library module directory then click Finish
5. Make sure the library is listed at the top of your settings.gradle file,
as shown here for a library named "my-library-module":
`include ':app', ':my-library-module'
6. Open the app module's build.gradle file and add a new line to the dependencies:
```
dependencies {
    implementation project(":my-library-module")
}
```
7. Sync project with gradle files.
[Android Reference](https://developer.android.com/studio/projects/android-library)

### Requirements

- minSdk = 21

### Permissions

- android.permission.ACCESS_NETWORK_STATE

### Licensing

- todo

### Changelog

[Keep a changelog](https://keepachangelog.com/en/1.0.0/)

### Bug Reporting

- Create an Issue through the repository's github Issues page.

### References
Google Play tracks products and transactions using purchase tokens and Order IDs.

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