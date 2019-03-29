package com.jetbeep.jetbeepexample

import android.app.Application
import android.widget.Toast
import com.jetbeep.*
import com.jetbeep.beeper.events.BeeperEvent
import com.jetbeep.beeper.events.LoyaltyTransferred
import com.jetbeep.beeper.events.NoLoyaltyCard
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.locations.LocationCallbacks
import com.jetbeep.model.entities.Merchant
import com.jetbeep.model.entities.Shop

class App : Application() {

    private lateinit var locationCallbacks: LocationCallbacks

    private val beeperCallback: BeeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            showNotification(beeperEvent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        JetBeepSDK.init(
            this,
            "0179c",
            "jetbeep-test",
            "35117dd1-a7bf-4167-b154-86626f3fac17",
            JetBeepRegistrationType.ANONYMOUS
        )

        JetBeepSDK.barcodeRequestHandler = object : JBBarcodeRequestProtocol {
            override var listener: JBBarcodeTransferProtocol? = object : JBBarcodeTransferProtocol {
                override fun failureBarcodeTransfer(shop: Shop) {
                    Toast.makeText(
                        applicationContext, "failureBarcodeTransfer: ${shop.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun succeedBarcodeTransfer(shop: Shop) {
                    Toast.makeText(
                        applicationContext, "succeedBarcodeTransfer: ${shop.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }

            override fun barcodeRequest(merchant: Merchant, shop: Shop): Array<Barcode>? {
                //Put your barcodes based on merchant and shop
                return arrayOf(Barcode("123456789"), Barcode("789102335"), Barcode("111111111111"))
            }
        }

        //You need go through registration process to get valid auth-token: "769b70f4-043d-4b51-a748-0f32423b6cc8"
        JetBeepSDK.authToken = "769b70f4-043d-4b51-a748-0f32423b6cc8"

        JetBeepSDK.repository.trySync()

        locationCallbacks = object : LocationCallbacks {
            override fun onObtainActualShops(shops: List<Shop>) {
            }

            override fun onShopExit(shop: Shop, merchant: Merchant) {
            }

            override fun onShopEntered(shop: Shop, merchant: Merchant) {
                JetBeepSDK.notificationsManager.showNotification(
                    "Enter event",
                    "Welcome to ${shop.name}",
                    R.mipmap.ic_launcher,
                    null,
                    null
                )
            }
        }

        JetBeepSDK.locations.subscribe(locationCallbacks)
        JetBeepSDK.beeper.subscribe(beeperCallback)
    }

    private fun showNotification(beeperEvent: BeeperEvent) {
        when (beeperEvent) {
            is LoyaltyTransferred -> {
                JetBeepSDK.notificationsManager.showNotification(
                    "Loyalty card transferred",
                    "Loyalty card",
                    R.mipmap.ic_launcher,
                    null,
                    null
                )
            }
            is NoLoyaltyCard -> {
                JetBeepSDK.notificationsManager.showNotification(
                    "Loyalty card not found",
                    "Loyalty card",
                    R.mipmap.ic_launcher,
                    null,
                    null
                )
            }
        }
    }
}