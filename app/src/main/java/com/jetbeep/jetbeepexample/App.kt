package com.jetbeep.jetbeepexample

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.jetbeep.*
import com.jetbeep.beeper.events.*
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.jetbeepexample.notification.SilentNotificationHolder
import com.jetbeep.locations.PushNotificationListener
import com.jetbeep.locations.PushNotificationManager
import com.jetbeep.model.MerchantType
import com.jetbeep.model.entities.Merchant
import com.jetbeep.model.entities.Shop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class App : Application() {

    private lateinit var beeperCallback: BeeperCallback

    private val silentNotificationHolder by lazy { SilentNotificationHolder(applicationContext) }

    override fun onCreate() {
        super.onCreate()

        JetBeepSDK.init(
            this,
            "179c",
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
        JetBeepSDK.pushNotificationManager.subscribe(object : PushNotificationListener {
            override fun onShowNotification(info: PushNotificationManager.NotificationInfo) {
                val merchant = info.merchant

                if (MerchantType.TRANSPORT.name == merchant.type ||
                    MerchantType.VENDING.name == merchant.type
                ) {
                    silentNotificationHolder.showNotification(info)
                } else {
                    val shop = info.shop
                    //OfferNotification(applicationContext, shop.id).show(info)
                    JetBeepSDK.notificationsManager.showNotification(
                        "Enter event",
                        "Welcome to ${shop.name}",
                        R.mipmap.ic_launcher,
                        null,
                        null
                    )
                }
            }

            override fun onRemoveNotification(id: Int) {
                silentNotificationHolder.hideNotification(id)
            }
        })
    }

    private fun showNotification(beeperEvent: BeeperEvent) {
        when (beeperEvent) {
            is SessionOpened -> {
                Log.d("JB_App", "SessionOpened")
            }
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