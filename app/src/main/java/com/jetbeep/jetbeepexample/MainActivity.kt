package com.jetbeep.jetbeepexample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.jetbeep.JetBeepSDK
import com.jetbeep.beeper.events.BeeperEvent
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.isBluetoothPermissionsGranted
import com.jetbeep.isLocationPermissionsGranted
import com.jetbeep.locations.LocationCallbacks
import com.jetbeep.logger.LogCallback
import com.jetbeep.logger.LogLine
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

    private val REQUEST_ENABLE_ON_START_APP_BT = 123

    private val beeperCallback: BeeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            printToConsole(beeperEvent.javaClass.simpleName)
        }
    }

    private val loggerCallback = object : LogCallback() {
        override fun onLogLine(logLine: LogLine) {
            printToConsole(logLine.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        console.movementMethod = ScrollingMovementMethod()
        printToConsole("Hello world")

        loadVending.isEnabled = false

        loadMerchant.setOnClickListener { loadAllMerchants() }
        loadShop.setOnClickListener { loadAllShops() }
        loadOffers.setOnClickListener { loadAllOffers() }
        loadNotifications.setOnClickListener { loadAllNotifications() }
        checkPermission.setOnClickListener {
            printToConsole("Permissions granted: ${checkPermissions()}")
        }
        requestPermissions.setOnClickListener { requestPermissions() }

        if (checkBluetooth()) {
            enableVending()
        }
    }

    private fun requestPermissions() {
        // This permissions needs to scanning beacons
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            if (isLocationPermissionsGranted(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val b = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            printToConsole("Request permissions, shouldShowRequestPermissionRationale = $b")
        } else {
            printToConsole("Request permissions")
        }

        requestPermissions(permissions, object : PermissionCallBack {
            override fun permissionGranted() {
                printToConsole("Permissions granted")
                if (!JetBeepSDK.backgroundActive) {
                    JetBeepSDK.enableBackground()
                    printToConsole("Scan of beacons was started...")
                }
            }

            override fun permissionDenied() {
                printToConsole("Permissions denied")
            }
        })
    }

    private fun enableVending() {
        loadVending.isEnabled = true
        loadVending.setOnClickListener { startVending() }
    }

    private fun checkBluetooth(): Boolean {
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bAdapter = bm.adapter

        if (bAdapter != null) {
            if (bAdapter.isEnabled) {
                return true
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_ON_START_APP_BT)
            }
        } else {
            printToConsole("Error! Bluetooth adapter not found!")
        }
        return false
    }

    private fun getLocationListener(): LocationCallbacks {
        if (locationListener == null) {
            locationListener = object : LocationCallbacks {
                override fun onMerchantEntered(merchant: Merchant, shop: Shop) {
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

        JetBeepSDK.logger.subscribe(loggerCallback)
        JetBeepSDK.beeper.subscribe(beeperCallback)
        JetBeepSDK.locations.subscribe(getLocationListener())

        //startAdvertising()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()

        JetBeepSDK.logger.unsubscribe(loggerCallback)
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
                .subscribe({ shops ->
                    if (shops.isNotEmpty()) {
                        val str = StringBuilder("Shops:")
                        shops.forEachIndexed { i, s ->
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
                .subscribe({ offers ->
                    if (offers.isNotEmpty()) {
                        val str = StringBuilder("Offers:")
                        offers.forEachIndexed { i, o ->
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

    private fun loadAllNotifications() {
        compositeDisposable.add(
            JetBeepSDK.repository.notifications.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ notifications ->
                    if (notifications.isNotEmpty()) {
                        val str = StringBuilder("Notifications:")
                        notifications.forEachIndexed { i, o ->
                            str.append("\n").append("${i + 1})").append(o.title)
                        }
                        printToConsole(str.toString())
                    } else {
                        printToConsole("Notifications is empty")
                    }
                }, {
                    it.printStackTrace()
                    showToast("Failed to load notifications")
                })
        )
    }

    /*private fun startAdvertising() {
        if (!JetBeepSDK.isBeeping) {
            JetBeepSDK.startBeep()
        }
    }*/

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isBluetoothPermissionsGranted(this)
        } else {
            isLocationPermissionsGranted(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_ON_START_APP_BT) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    enableVending()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Please turn on Bluetooth!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
