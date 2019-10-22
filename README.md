# JetBeep Android sdk

## Hardware setup
https://drive.google.com/drive/u/1/folders/1exPvE0fJYBYEf-XRj5r4i4IQqMalLuma

## Gradle dependencies

Add this dependency to your project's build file:

```groovy
implementation 'com.jetbeep:jetbeepsdk:1.2.0'
```

To build debug version of app add snapshot repository to your build.gradle file:

```groovy
allprojects {
    repositories {
        //...
        maven {url "https://oss.sonatype.org/content/repositories/snapshots"}
    }
}
```

Then add release and debug dependencies to your project's build file:

```groovy
    releaseImplementation 'com.jetbeep:jetbeepsdk:1.2.0'
    debugImplementation 'com.jetbeep:jetbeepsdk:1.2.0-SNAPSHOT'
```

### Now you are ready to go!

Example of initialization of JetBeepSdK:

```kotlin
    JetBeepSDK.init(
        application, // instance of Application
        serviceUUID, // serviceUUID
        appId, // your app name that you can request from our side,
        appToken, // your app token key that you can request from our side
        registrationType, // Jetbeep registration type
        paymentProcessor // optional, implement PaymentProcessor interface if you want to pay on third-party servers
    )
```

#### Jetbeep registration types

`JetBeepRegistrationType.REGISTERED` - gives ability to get personalized offers, notifications and access to loyalty cards. 
Additionally need to send authToken for identifying the user.
```kotlin
    JetBeepSDK.authToken = "123a45b6-123a-4b56-789c-0e22345b6cd7" // example of authToken
```
`JetBeepRegistrationType.ANONYMOUS` - you can access to common offers and notifications only, but no access to personalized items. 
Also, you need to handle a callback for loyalty cards by yourself.

Instance of barcode handler protocol, it will be used when you will provide barcodes

```kotlin
    JetBeepSDK.barcodeRequestHandler = object : JBBarcodeRequestProtocol {
        override var listener: JBBarcodeTransferProtocol? = object : JBBarcodeTransferProtocol {
            override fun failureBarcodeTransfer(shop: Shop) {
                //TODO failureBarcodeTransfer
            }

            override fun succeedBarcodeTransfer(shop: Shop) {
                //TODO succeedBarcodeTransfer
            }
        }

        override fun barcodeRequest(merchant: Merchant, shop: Shop): Array<Barcode>? {
            //TODO Put your barcodes based on merchant and shop
        }
    }
```

Handle result on your place to track is barcodes transfering moved succeed or not
```kotlin
    JBBarcodeRequestProtocol.listener
```

To receive events of entry and exit into the zone of beacon, add a listener and subscribe to it:

```kotlin
    val locationCallbacks = object : LocationCallbacks {
        override fun onMerchantEntered(merchant: Merchant) {
        // TODO onMerchantEntered
        }

        override fun onMerchantExit(merchant: Merchant) {
        // TODO onMerchantExit
        }

        override fun onShopEntered(shop: Shop) {
        // TODO onShopEntered
        }

        override fun onShopExit(shop: Shop) {
        // TODO onShopExit
        }
    }
    
    JetBeepSDK.locations.subscribe(locationCallbacks)
```

To receive push notifications, subscribe to PushNotificationListener. Inside you can add a logic for different types of notifications.
As show in example below, we show silent notifications for merchant types TRANSPORT and VENDING. 
Full realization of *SilentNotification* see in the test application.

```kotlin
    JetBeepSDK.pushNotificationManager.subscribe(object : PushNotificationListener {
                override fun onShowNotification(info: PushNotificationManager.NotificationInfo) {
                    val merchant = info.merchant
    
                    if (MerchantType.TRANSPORT.name == merchant.type ||
                        MerchantType.VENDING.name == merchant.type
                    ) {
                        silentNotificationHolder.showNotification(info)
                    } else {
                        val shop = info.shop
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
```

To receive events such as loyalty card transfers, add this listener and subscribe to it:

```kotlin
    val beeperCallback: BeeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            showNotification(beeperEvent)
        }
    }
    
    JetBeepSDK.beeper.subscribe(beeperCallback)
```

Implement the interface if you want to pay on third-party servers

```kotlin
interface PaymentProcessor {

    fun pay(paymentRequest: PaymentRequest,
            pinCode: String,
            paymentInstrument: PaymentInstrument,
            protocolVersion: Byte): Single<PaymentResult>

    fun confirm(orderId: String, confirmation: String): Single<Any>
}
```

See the test application for more details.

## Work with Vending

You can get instance of VendingDevices here `JetBeepSDK.locations.vendingDevices`
It contains `ConnectableDevice`, entity for devices and `DeviceChangeListener`, that notifies any updates of devices. 

```kotlin
    private val vending = JetBeepSDK.locations.vendingDevices
```

### Get a list of connectable devices

````kotlin
    devices = vending.getVisibleDevices()
````

### Create callback & subscribe to DeviceChangeListener

```kotlin
    private val callback = object : VendingDevices.DeviceChangeListener {
            override fun onChangeDevices(devices: List<VendingDevices.ConnectableDevice>) {
                update(devices)
            }
        }
```
```kotlin
    vending.subscribe(callback)
```

### Connect & disconnect

Once you've got a list of visible devices, you can connect them

```kotlin
    item = devices.get(position)
    vending.connect(item)
```

And disconnect from connected device

```kotlin
    vending.disconnect()
```
Note: During active connection all other devices become non-connectable.

### Receiving events from devices

You need to receive events for interact with device after connection.
In order to open a session and initiate payment, we need to receive events from the device.
To do this, we need to create an instance for *BeeperCallback()* and subscribe to events:

```kotlin
private val beeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            when (beeperEvent) {
                is Advertising -> { }
                is SessionOpened -> { }
                is SessionClosed -> { }
                is LoyaltyTransferred -> { }
                is NoLoyaltyCard -> { }
                is PaymentInitiated -> { }
                is PaymentInProgress -> { }
                is PaymentError -> { }
                is PaymentSuccessful -> { }
                is BluetoothFeatureNotSupported -> { }
            }
        }
    }
```
    
In example above all possible events are listed, but you can use only those that are necessary for vending.

Then you need to subscribe to get events and start the beeper: 
```kotlin
JetBeepSDK.beeper.subscribe(beeperCallback)
```
When closing, don't forget to unsubscribe and stop the beeper: 
```kotlin
JetBeepSDK.beeper.unsubscribe(beeperCallback)
```

### Testing

IMPORTANT NOTE: Before testing ensure that your devices is configured for VENDING-type merchants. Please double-check them.
  
### Classes & methods in VendingDevices
  
`connect(device: ConnectableDevice): Boolean` - to connect to specified device. Returns *true* on successful connection.

`disconnect(): Boolean` - to disconnect from device. Returns *true* on successful disconnection. 

`subscribe(customerCallback: DeviceChangeListener)` - subscribe to a listener to get updates of devices and their statuses. 

`unsubscribe(customerCallback: DeviceChangeListener)` - unsubscribe from a listener.

`getVisibleDevices(): List<ConnectableDevice>` - get a list of all visible connectable devices.

**`ConnectableDevice`** - entity of connectable device. Contains some public things:
 - `shopId: Int` - shop id of the device
 - `shopName: String` - shop name
 - `isConnectable(): Boolean` - returns true if device is connectable at the moment.
