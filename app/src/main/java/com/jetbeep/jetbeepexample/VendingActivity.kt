package com.jetbeep.jetbeepexample

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
import kotlinx.android.synthetic.main.activity_vending.*
import kotlinx.android.synthetic.main.item_list_connectable_device.view.*

class VendingActivity : Activity() {

    private val vending = JetBeepSDK.locations.vendingDevices

    private var list = listOf<VendingDevices.ConnectableDevice>()

    lateinit var adapter: DevicesAdapter

    private val callback = object : VendingDevices.DeviceChangeListener {
        override fun onChangeDevices(devices: List<VendingDevices.ConnectableDevice>) {
            updateResult()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_vending)

        adapter = DevicesAdapter(this)

        val linearLayoutManager = LinearLayoutManager(this)
        rvDevicesList.layoutManager = linearLayoutManager
        rvDevicesList.adapter = adapter

        btnUpdate?.setOnClickListener {
            updateResult()
        }

        btnDisconnect?.setOnClickListener {
            vending.disconnect()
        }
    }


    private fun updateResult() {
        list = vending.getVisibleDevices()

        if (list.isEmpty()) {
            tvVendingStatus?.text = "Devices not found"
        } else {

            tvVendingStatus?.text = "Found ${list.size} device(s)"

            adapter.update(list as MutableList<VendingDevices.ConnectableDevice>, object : DevicesClickListener {
                override fun onConnectButtonClicked(position: Int) {

                    val item = list[position]

                    if (item.isConnectable()) {
                        vending.connect(item)
                        tvVendingStatus?.text = "ShopID: ${item.shopId}" // change to recycler item update
                    } else {
                        tvVendingStatus?.text = "Device isn't connectable" // change to recycler item update
                    }

                }
            })
        }
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

        private var devices = mutableListOf<VendingDevices.ConnectableDevice>()
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
                //tvDeviceName?.text = item.address
                tvShopId?.text = item.shopId.toString()
                this.btnConnectToDevice?.setOnClickListener {
                    listener?.onConnectButtonClicked(position)
                }
            }
        }

        fun update(devices: MutableList<VendingDevices.ConnectableDevice>, listener: DevicesClickListener) {

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