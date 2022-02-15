package com.jetbeep.jetbeepexample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jetbeep.JetBeepSDK
import com.jetbeep.beeper.events.BeeperEvent
import com.jetbeep.beeper.events.SessionClosed
import com.jetbeep.beeper.events.helpers.BeeperCallback
import com.jetbeep.connection.ConnectableDevice
import com.jetbeep.connection.ConnectableDeviceStateChangeListener
import com.jetbeep.connection.vending.VendingDevices
import kotlinx.android.synthetic.main.activity_main.console
import kotlinx.android.synthetic.main.activity_vending_new.*
import kotlinx.android.synthetic.main.item_list_connectable_device.view.*
import java.text.SimpleDateFormat
import java.util.*

class VendingActivity : Activity() {

    private val vending = JetBeepSDK.connections.vendingDevices

    private val format = SimpleDateFormat("HH:mm:ss: ", Locale.getDefault())
    private var list = listOf<ConnectableDevice>()

    lateinit var adapter: DevicesAdapter

    private val callback = object : ConnectableDeviceStateChangeListener {
        override fun onChangeDevices(devices: List<ConnectableDevice>) {
            update()
            printToConsole("Devices changed. Found ${devices.size} devices")
        }
    }

    private val beeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            printToConsole("Beeper -> ${beeperEvent.javaClass.simpleName}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_vending_new)

        adapter = DevicesAdapter(this)

        val linearLayoutManager = LinearLayoutManager(this)
        rvDevicesList.layoutManager = linearLayoutManager
        rvDevicesList.adapter = adapter

        update()

        btnUpdate?.setOnClickListener {
            update()
            printToConsole("Update pressed")
        }

        btnDisconnect?.setOnClickListener {
            vending.disconnect()
            printToConsole("Disconnected!")
        }

        printToConsole("Welcome to Vending test!")
    }

    @SuppressLint("SetTextI18n")
    private fun printToConsole(text: String) {
        if (!isFinishing) {
            val oldText = console.text.toString()
            console.text = format.format(Date()) + text + "\n" + oldText
        }
    }

    private fun update() {

        list = vending.getVisibleDevices()

        if (vending.getVisibleDevices().isNotEmpty()) {
            printToConsole("Found ${list.size} device(s)")
        } else {
            printToConsole("Devices not found")
        }

        adapter.update(list, object : DevicesClickListener {
            override fun onConnectButtonClicked(position: Int) {

                val item = list[position]

                if (item.isConnectable) {
                    vending.connect(item)
                    printToConsole("Connected! Shop name: ${item.shopName}")
                } else {
                    printToConsole("Device isn't connectable")
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        vending.subscribe(callback)
        JetBeepSDK.beeper.subscribe(beeperCallback)

        if (JetBeepSDK.beeper.lastEvent !is SessionClosed) {
            beeperCallback.onEvent(JetBeepSDK.beeper.lastEvent)
        }
    }

    override fun onPause() {
        super.onPause()

        vending.unsubscribe(callback)
        JetBeepSDK.beeper.unsubscribe(beeperCallback)
    }

    class DevicesAdapter(private var context: Context) : RecyclerView.Adapter<DeviceViewHolder>() {

        private var devices = listOf<ConnectableDevice>()
        private var listener: DevicesClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_list_connectable_device, parent, false)

            return DeviceViewHolder(view)
        }

        override fun getItemCount() = devices.size

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {

            val item = devices.get(position)

            holder.itemView.apply {
                tvDeviceName?.text = "Device #${position + 1}"
                tvShopId?.text = "ShopID: ${item.shopId}"

                this.btnConnectToDevice?.setOnClickListener {
                    listener?.onConnectButtonClicked(position)
                }
            }
        }

        fun update(
            devices: List<ConnectableDevice>,
            listener: DevicesClickListener
        ) {

            this.listener = listener
            this.devices = devices

            notifyDataSetChanged()
        }
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface DevicesClickListener {
        fun onConnectButtonClicked(position: Int)
    }
}
