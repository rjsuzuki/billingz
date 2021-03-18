package com.zuko.billingz.demo

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.zuko.billingz.lib.manager.Manager
import com.zuko.billingz.lib.products.Product

class DemoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        val bm = Manager()
        lifecycle.addObserver(bm)
        bm.getAgent().isBillingClientReady().observe(this, Observer { isReady ->
            if(isReady) {
                // load products
                val products = mutableListOf<String>("product_01", "product_02", "product_03")
                bm.getAgent().getAvailableProducts(products, Product.ProductType.ALL).observe(this, Observer { map ->

                })
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun createMockProducts() {

    }

    companion object {
        private const val TAG = "DemoActivity"
    }
}