/*
 * Copyright 2021 rjsuzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zuko.billingz.demo

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zuko.billingz.core.store.client.Clientz
import com.zuko.billingz.core.store.model.Productz
import com.zuko.billingz.demo.sim.PurchaseOutcome
import com.zuko.billingz.demo.sim.StoreType
import com.zuko.billingz.demo.ui.theme.BillingzTheme

/**
 * A single-screen control panel for exercising the billingz library against a fully simulated
 * store. No Google Play / Amazon connection and no account are required — see [DemoController].
 */
class MainActivity : ComponentActivity() {

    private val controller = DemoController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Registering the store ties its connection lifecycle to this activity.
        lifecycle.addObserver(controller.lifecycleObserver)
        setContent {
            BillingzTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    DemoScreen(controller, this)
                }
            }
        }
    }
}

@Composable
private fun DemoScreen(controller: DemoController, activity: Activity) {
    val config by controller.config.collectAsState()
    val connection by controller.connectionState.observeAsState(Clientz.ConnectionStatus.DISCONNECTED)
    val inventoryReady by controller.inventoryReady.collectAsState()
    val products by controller.products.collectAsState()
    val receipts by controller.receipts.collectAsState()
    val events by controller.events.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Billingz Simulator") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                storeType = config.storeType,
                connection = connection,
                inventoryReady = inventoryReady
            )

            SimulationControls(
                storeType = config.storeType,
                outcome = config.purchaseOutcome,
                validatorApproves = config.validatorApproves,
                onStoreType = { type -> controller.setStoreType { it.copy(storeType = type) } },
                onOutcome = { outcome -> controller.setStoreType { it.copy(purchaseOutcome = outcome) } },
                onValidator = { approves -> controller.setStoreType { it.copy(validatorApproves = approves) } },
                onRefreshInventory = { controller.refreshInventory() }
            )

            ProductsCard(
                products = products?.values?.toList().orEmpty(),
                onBuy = { sku -> controller.buy(activity, sku) }
            )

            ReceiptsCard(
                receipts = receipts?.receipts?.values?.toList().orEmpty(),
                onRefresh = { controller.refreshReceipts() }
            )

            EventLogCard(events = events, onClear = { controller.clearLog() })
        }
    }
}

@Composable
private fun StatusCard(
    storeType: StoreType,
    connection: Clientz.ConnectionStatus,
    inventoryReady: Boolean
) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("Status")
            LabeledValue("Store", storeType.name)
            LabeledValue("Connection", connection.name)
            LabeledValue("Inventory ready", if (inventoryReady) "yes" else "no")
        }
    }
}

@Composable
private fun SimulationControls(
    storeType: StoreType,
    outcome: PurchaseOutcome,
    validatorApproves: Boolean,
    onStoreType: (StoreType) -> Unit,
    onOutcome: (PurchaseOutcome) -> Unit,
    onValidator: (Boolean) -> Unit,
    onRefreshInventory: () -> Unit
) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Simulated store")
            ChipRow(
                options = StoreType.values().toList(),
                selected = storeType,
                label = { it.name },
                onSelect = onStoreType
            )

            Divider()

            SectionTitle("Next purchase result")
            ChipRow(
                options = PurchaseOutcome.values().toList(),
                selected = outcome,
                label = { it.label },
                onSelect = onOutcome
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = validatorApproves, onCheckedChange = onValidator)
                Text(
                    "Validator approves order",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.body2
                )
            }
            Text(
                "Only affects \"Success\": off = your backend validation rejects the order.",
                style = MaterialTheme.typography.caption
            )

            OutlinedButton(onClick = onRefreshInventory) { Text("Re-query inventory") }
        }
    }
}

@Composable
private fun ProductsCard(products: List<Productz>, onBuy: (String) -> Unit) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Test SKUs (${products.size})")
            if (products.isEmpty()) {
                Text("No products loaded. Connect and query inventory.", style = MaterialTheme.typography.body2)
            }
            products.forEach { product ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(product.getTitle().orEmpty(), fontWeight = FontWeight.SemiBold)
                        Text(product.getProductId().orEmpty(), style = MaterialTheme.typography.caption)
                        Text(product.getPrice().orEmpty(), style = MaterialTheme.typography.body2)
                    }
                    Button(onClick = { product.getProductId()?.let(onBuy) }) { Text("Buy") }
                }
                Divider()
            }
        }
    }
}

@Composable
private fun ReceiptsCard(receipts: List<com.zuko.billingz.core.store.model.Receiptz>, onRefresh: () -> Unit) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Owned / receipts (${receipts.size})")
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            if (receipts.isEmpty()) {
                Text("Nothing owned yet.", style = MaterialTheme.typography.body2)
            }
            receipts.forEach { receipt ->
                Column(Modifier.fillMaxWidth()) {
                    Text(receipt.skus?.joinToString().orEmpty(), fontWeight = FontWeight.SemiBold)
                    Text("order: ${receipt.orderId}", style = MaterialTheme.typography.caption)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun EventLogCard(events: List<String>, onClear: () -> Unit) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Event log (${events.size})")
                OutlinedButton(onClick = onClear) { Text("Clear") }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                items(events.reversed()) { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            if (isSelected) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(label(option)) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(label(option)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.body2)
        Text(value, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
    }
}
