package com.jetbeep.jetbeepexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import com.jetbeep.JetBeepSDK
import com.jetbeep.beeper.events.BeeperEvent
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.locations.LocationCallbacks
import com.jetbeep.model.entities.Merchant
import com.jetbeep.model.entities.Shop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.vrinda.kotlinpermissions.PermissionCallBack
import io.vrinda.kotlinpermissions.PermissionsActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : PermissionsActivity() {

    private val compositeDisposable = CompositeDisposable()
    private val format = SimpleDateFormat("HH:mm:ss: ", Locale.getDefault())

    private var locationListener: LocationCallbacks? = null

    private val beeperCallback: BeeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            printToConsole(beeperEvent.javaClass.simpleName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        console.movementMethod = ScrollingMovementMethod()
        printToConsole("Hello world")

        loadMerchant.setOnClickListener { loadAllMerchants() }
        loadShop.setOnClickListener { loadAllShops() }
        loadOffers.setOnClickListener { loadAllOffers() }
        startAdvertising.setOnClickListener { startAdvertising() }
        loadVending.setOnClickListener { startVending() }

        // This permissions needs to scanning beacons
        if (!checkPermissions()) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), object : PermissionCallBack {
                override fun permissionGranted() {
                    if (!JetBeepSDK.backgroundActive) {
                        JetBeepSDK.enableBackground()
                        printToConsole("Scan of beacons was started...")
                    }
                }
            })
        }
    }

    private fun getLocationListener(): LocationCallbacks {
        if (locationListener == null) {
            locationListener = object : LocationCallbacks {
                override fun onMerchantEntered(merchant: Merchant) {
                    printToConsole("Entered merchant: ${merchant.name}")
                }

                override fun onMerchantExit(merchant: Merchant) {
                    printToConsole("Exit merchant: ${merchant.name}")
                }

                override fun onShopEntered(shop: Shop) {
                    printToConsole("Entered shop: ${shop.name}")
                }

                override fun onShopExit(shop: Shop) {
                    printToConsole("Exit from shop: ${shop.name}")
                }
            }
        }

        return locationListener!!
    }

    override fun onResume() {
        super.onResume()

        JetBeepSDK.beeper.subscribe(beeperCallback)
        JetBeepSDK.locations.subscribe(getLocationListener())

        startAdvertising()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()

        JetBeepSDK.beeper.unsubscribe(beeperCallback)
        JetBeepSDK.locations.unsubscribe(getLocationListener())
    }

    private fun loadAllMerchants() {
        printToConsole("Load merchants...")

        compositeDisposable.add(
            JetBeepSDK.repository.merchants.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ merchants ->
                    if (merchants.isNotEmpty()) {
                        val str = StringBuilder("Merchants:")
                        merchants.forEachIndexed { i, m ->
                            str.append("\n").append("${i + 1})").append(m.name)
                        }
                        printToConsole(str.toString())
                    } else {
                        printToConsole("Merchants is empty")
                    }
                }, {
                    it.printStackTrace()
                    showToast("Failed to load merchants")
                })
        )
    }

    private fun loadAllShops() {
        printToConsole("Load shops...")

        compositeDisposable.addAll(
            JetBeepSDK.repository.shops.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ merchants ->
                    if (merchants.isNotEmpty()) {
                        val str = StringBuilder("Shops:")
                        merchants.forEachIndexed { i, s ->
                            str.append("\n").append("${i + 1})").append(s.name)
                        }
                        printToConsole(str.toString())
                    } else {
                        printToConsole("Shops is empty")
                    }
                }, {
                    it.printStackTrace()
                    showToast("Failed to load shops")
                })
        )
    }

    private fun loadAllOffers() {
        printToConsole("Load offers...")

        compositeDisposable.add(
            JetBeepSDK.repository.offers.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ merchants ->
                    if (merchants.isNotEmpty()) {
                        val str = StringBuilder("Offers:")
                        merchants.forEachIndexed { i, o ->
                            str.append("\n").append("${i + 1})").append(o.title)
                        }
                        printToConsole(str.toString())
                    } else {
                        printToConsole("Offers is empty")
                    }
                }, {
                    it.printStackTrace()
                    showToast("Failed to load offers")
                })
        )
    }

    private fun startAdvertising() {
        if (!JetBeepSDK.isBeeping) {
            JetBeepSDK.startBeep()
        }
    }

    private fun startVending() {
        val intent = Intent(this, VendingActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun printToConsole(text: String) {
        if (!isFinishing) {
            val oldText = console.text.toString()
            console.text = format.format(Date()) + text + "\n" + oldText
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        val permissionStateCoarse = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val permissionStateFine = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionStateCoarse == PackageManager.PERMISSION_GRANTED ||
                permissionStateFine == PackageManager.PERMISSION_GRANTED
    }
}
