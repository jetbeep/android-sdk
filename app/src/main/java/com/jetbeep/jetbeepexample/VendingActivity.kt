package com.jetbeep.jetbeepexample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jetbeep.JetBeepSDK
import com.jetbeep.locations.VendingDevices
import com.jetbeep.model.entities.Shop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.console
import kotlinx.android.synthetic.main.activity_vending_new.*
import kotlinx.android.synthetic.main.item_list_connectable_device.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class VendingActivity : Activity() {

    private val vending = JetBeepSDK.locations.vendingDevices

    private val format = SimpleDateFormat("HH:mm:ss: ", Locale.getDefault())
    private var list = listOf<VendingDevices.ConnectableDevice>()

    lateinit var adapter: DevicesAdapter

    private val callback = object : VendingDevices.DeviceChangeListener {
        override fun onChangeDevices(devices: List<VendingDevices.ConnectableDevice>) {
            update()
            printToConsole("Devices changed. Found ${devices.size} devices")
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

        if(vending.getVisibleDevices().isNotEmpty()) {
            printToConsole("Found ${list.size} device(s)")
        } else {
            printToConsole("Devices not found")
        }

        adapter.update(list, object : DevicesClickListener {
            override fun onConnectButtonClicked(position: Int) {

                val item = list[position]

                if (item.isConnectable()) {
                    vending.connect(item)
                    printToConsole("Connected! ShopId: ${item.shopId}")
                } else {
                    printToConsole("Device isn't connectable")
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        vending.subscribe(callback)
    }

    override fun onPause() {
        super.onPause()

        vending.unsubscribe(callback)
    }

    class DevicesAdapter(private var context: Context) : RecyclerView.Adapter<DeviceViewHolder>() {

        private var devices = listOf<VendingDevices.ConnectableDevice>()
        private var listener: DevicesClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_list_connectable_device, parent, false)

            return DeviceViewHolder(view).listen { position, type ->
                val item = devices.get(position)
                // do other stuff here on click listener
            }
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

        fun update(devices: List<VendingDevices.ConnectableDevice>, listener: DevicesClickListener) {

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

fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition, itemViewType)
    }
    return this
}