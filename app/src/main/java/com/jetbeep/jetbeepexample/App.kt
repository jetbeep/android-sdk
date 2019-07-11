package com.jetbeep.jetbeepexample

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.jetbeep.*
import com.jetbeep.beeper.events.*
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.locations.LocationCallbacks
import com.jetbeep.model.entities.Merchant
import com.jetbeep.model.entities.Shop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class App : Application() {

    private lateinit var locationCallbacks: LocationCallbacks

    private lateinit var beeperCallback: BeeperCallback

    override fun onCreate() {
        super.onCreate()

        JetBeepSDK.init(
            this,
            "0179c",
            "jetbeep-test",
            "35117dd1-a7bf-4167-b154-86626f3fac17",
            JetBeepRegistrationType.REGISTERED
        )

        JetBeepSDK.barcodeRequestHandler = object : JBBarcodeRequestProtocol {
            val handler = Handler(Looper.getMainLooper())
            override var listener: JBBarcodeTransferProtocol? = object : JBBarcodeTransferProtocol {
                override fun failureBarcodeTransfer(shop: Shop) {
                    handler.post {
                        Toast.makeText(
                            applicationContext, "failureBarcodeTransfer: ${shop.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun succeedBarcodeTransfer(shop: Shop) {
                    handler.post {
                        Toast.makeText(
                            applicationContext, "succeedBarcodeTransfer: ${shop.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

        beeperCallback = object : BeeperCallback() {
            override fun onEvent(beeperEvent: BeeperEvent) {
                if (!JetBeepSDK.isInBackgroundNow)
                    return

                showNotification(beeperEvent)
            }
        }

        JetBeepSDK.beeper.subscribe(beeperCallback)

        locationCallbacks = object : LocationCallbacks {
            override fun onMerchantEntered(merchant: Merchant) {
                JetBeepSDK.notificationsManager.showNotification(
                    "Enter event",
                    "Welcome to ${merchant.name}",
                    R.mipmap.ic_launcher,
                    null,
                    null
                )
            }

            override fun onMerchantExit(merchant: Merchant) { }

            override fun onShopEntered(shop: Shop) {
                JetBeepSDK.notificationsManager.showNotification(
                    "Enter event",
                    "Welcome to ${shop.name}",
                    R.mipmap.ic_launcher,
                    null,
                    null
                )
            }

            override fun onShopExit(shop: Shop) { }
        }

        JetBeepSDK.locations.subscribe(locationCallbacks)

    }

    private fun showNotification(beeperEvent: BeeperEvent) {
        when (beeperEvent) {
            is SessionOpened -> { Log.d("JB_App", "SessionOpened")}
            is LoyaltyTransferred -> {
                JetBeepSDK.repository.merchants.getByIdFromCache(beeperEvent.shop.merchantId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ merchant ->
                        if (merchant != null) {
                            JetBeepSDK.notificationsManager.showNotification(
                                "Loyalty card from $merchant transferred",
                                "Loyalty card",
                                R.mipmap.ic_launcher,
                                null,
                                null
                            )
                        }
                    }, {
                        Log.e("JB_customers", "is error :${it.message}")
                    })
            }
            is NoLoyaltyCard -> {
                JetBeepSDK.repository.merchants.getByIdFromCache(beeperEvent.shop.merchantId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ merchant ->
                        if (merchant != null) {
                            JetBeepSDK.notificationsManager.showNotification(
                                "Loyalty card for $merchant not found",
                                "Loyalty card not found",
                                R.mipmap.ic_launcher,
                                null,
                                null
                            )
                        }
                    }, {
                        Log.e("JB_customers", "is error :${it.message}")
                    })
            }
            is PaymentInitiated -> {
                JetBeepSDK.repository.merchants.getByIdFromCache(beeperEvent.paymentRequest.shop.merchantId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ merchant ->
                        if (merchant != null) {
                            JetBeepSDK.notificationsManager.showNotification(
                                "Payment initiated for $merchant",
                                "Payment initiated",
                                R.mipmap.ic_launcher,
                                null,
                                null
                            )
                        } else
                            Log.e("JB_customers", "is null")
                    }, {
                        Log.e("JB_customers", "is error :${it.message}")
                        //TODO: implement error handling
                    })
            }
        }
    }
}