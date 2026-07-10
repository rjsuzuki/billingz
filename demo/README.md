# Billingz Demo — Simulated Store Harness

A single-screen Android app for exercising the billingz library's public API **without** connecting
to Google Play or the Amazon Appstore and **without any account or authentication**.

## Why a simulation?

The library starts a purchase only for a product that `queryInventory` has loaded
(`GoogleStore.startOrder` looks the SKU up in the inventory cache first). Real inventory comes from
`queryProductDetailsAsync`, which only returns products configured in the Play Console, and a real
purchase dialog requires a license-tester account signed into the device. Google's old reserved
static SKUs (`android.test.purchased`, …) are no longer returned by the v7 Billing Library.

To get a true end-to-end flow with zero setup, this demo ships its own implementation of the
library's contracts instead of talking to a store.

## How it works

Everything is expressed through the library's public facade, `com.zuko.billingz.core.store.agent.Agentz`:

- **`sim/SimulatedStore.kt`** — a fake `Storez`/`Agentz` that imitates store responses in memory and
  drives the real purchase lifecycle (`PROCESSING → VALIDATING → COMPLETE`, plus canceled/failed/
  pending/already-owned). It honors a live `SimulationConfig` and exposes a `Storez.Builder` that
  mirrors `BillingzStore.Builder`.
- **`sim/SimulationConfig.kt`** — the knobs: which store to imitate (`StoreType`), the next
  `PurchaseOutcome`, whether the validator approves, and artificial latency.
- **`sim/TestCatalog.kt`** — four test SKUs spanning consumable / non-consumable / subscription.
- **`DemoController.kt`** — the "integrating app" half. It implements the two listeners every
  consumer must provide (`Salez.OrderUpdaterListener` and `Salez.OrderValidatorListener`) and exposes
  observable state to the UI.
- **`MainActivity.kt`** — a Compose control panel: connection/inventory status, store + outcome +
  validator controls, the SKU list with **Buy** buttons, an owned-receipts list, and a live event log.

## Running it

```bash
./gradlew :demo:assembleDebug          # build
./gradlew :demo:installDebug           # install on a connected device/emulator
```

No Play Console, signing, or license tester needed. Pick a store type and an outcome, tap **Buy**,
and watch the event log trace the library's purchase flow and listener callbacks.

## Switching to a real store

Because the demo only depends on `Agentz`, swapping in a real backend is a one-line change in
`DemoController`:

1. Add the platform module to `demo/build.gradle`: `implementation project(':lib:google')`
   (or `:lib:amazon`).
2. Replace `SimulatedStore.Builder()` with `com.zuko.billingz.google.BillingzStore.Builder()` and
   drop the simulation-only setters (`setConfigProvider` / `setCatalogProvider` / `setEventLogger`).

Real purchases then require the usual Play Console products + a license-tester account on the device.
