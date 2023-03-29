# JetBeep Android sdk

## Hardware setup
https://drive.google.com/drive/u/1/folders/1exPvE0fJYBYEf-XRj5r4i4IQqMalLuma

## Gradle dependencies

Add this dependency to your project's build file:

```groovy
implementation 'com.jetbeep:jetbeepsdk:1.11.8'
```

## Permissions

#### Target Android 12 or higher

If your app targets Android 12 (API level 31) or higher, you must explicitly request user approval
in your app before you can use our sdk:

```xml 
    android.permission.BLUETOOTH_SCAN
    android.permission.BLUETOOTH_CONNECT
    android.permission.BLUETOOTH_ADVERTISE
```

These permissions are runtime permissions.

#### Target Android 11 or lower

If your app targets Android 11 (API level 30) or lower, declare the following permissions in your
app's manifest file:
```android.permission.ACCESS_FINE_LOCATION``` is necessary because, on Android 11 and lower, a
Bluetooth scan could potentially be used to gather information about the location of the user.

In order for Android 11 and Android 10 to receive scan results in the background, you must also
declare the ```android.permission.ACCESS_BACKGROUND_LOCATION``` permission in your app's manifest
file.

On Android 11 (API level 30) and higher, however, the system dialog doesn't include
the ```Allow all the time``` option. Instead, users must enable background location on a settings
page. You can help users navigate to this settings page by following best practices when requesting
the background location permission. First ask the user
for ```android.permission.ACCESS_FINE_LOCATION, android.permission.ACCESS_COARSE_LOCATION```
permissions. After the user grants these permissions, request
the ```android.permission.ACCESS_BACKGROUND_LOCATION``` permission.

See the official Android documentation for more details.
[Request background location](https://developer.android.com/training/location/permissions#request-background-location)

#### Target Android 9 or lower

If your app targets Android 9 (API level 28) or lower, you can declare
the ```android.permission.ACCESS_COARSE_LOCATION``` permission instead of
the ```android.permission.ACCESS_FINE_LOCATION``` permission.

Because location permissions are runtime permissions, you must request these permissions at runtime
along with declaring them in your manifest.

The following code snippet shows how to declare location permissions:

```xml
    <manifest>
        <uses-permission
        android:name="android.permission.ACCESS_COARSE_LOCATION"
        android:maxSdkVersion="30" />
        <uses-permission
        android:name="android.permission.ACCESS_FINE_LOCATION"
        android:maxSdkVersion="30" />
        <uses-permission
        android:name="android.permission.ACCESS_BACKGROUND_LOCATION"
        tools:targetApi="R"
        android:maxSdkVersion="30" />
    </manifest>
```

### Now you are ready to go!

Example of initialization of JetBeepSdK (do this in the onCreate method in the Application class):

```kotlin
    JetBeepSDK.init(
        application, // instance of Application
        serviceUUID, // Bluetooth service UUID, please request this value from Jetbeep
        appId, // application identifier, please request this value from Jetbeep
        appToken, // application access token, please request this value from Jetbeep
        registrationType, // Jetbeep registration type (for example JetBeepRegistrationType.ANONYMOUS)
        isDebuggable, // To use the debug version of sdk (our dev server)
    )
```

After receiving all permissions, you can start working with our devices:
```kotlin
    if (!JetBeepSDK.backgroundActive) {
        JetBeepSDK.enableBackground()
    }
```

// trySync() will cache information about devices in SDK
// as the device configuration might change, you have to always run this code from here
```kotlin
    JetBeepSDK.repository.trySync()
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

## Personalized Offers and Notifications

The Jetbeep SDK now supports personalized offers and notifications based on users' phone numbers or loyalty card numbers.

### Assigning User Numbers

To enable personalized offers and notifications, assign an array of strings containing phone numbers or loyalty card numbers to the `JetBeepSDK.repository.userNumbers` property:

```kotlin
    JetBeepSDK.repository.userNumbers = listOf("380007890000", ..)
```

### Usage

Once you have assigned user numbers to `JetBeepSDK.repository.userNumbers`, the SDK will automatically start providing personalized offers and notifications to the specified users at the next fetching request of `trySync()` function.

Please ensure that your application has obtained the necessary permissions from users to use their phone numbers or loyalty card numbers for this purpose.

### Example

Here's an example of how to set up personalized offers and notifications in your application:

```kotlin
    // Obtain user's phone number or loyalty card number
    val userPhoneNumber = "380007890000"

    // Assign the number to Jetbeep.shared.userNumbers
    JetBeepSDK.repository.userNumbers = listOf(userPhoneNumber)

    // Update offers and notifications
    JetBeepSDK.repository.trySync()
```

The SDK will now provide personalized offers and notifications for the specified user

## Work with Vending

You can get instance of VendingDevices here `JetBeepSDK.locations.vendingDevices`
It contains `ConnectableDevice`, entity for devices and `DeviceChangeListener`, that notifies any updates of devices. 

```kotlin
    private val vending = JetBeepSDK.connections.vendingDevices
```

### Get a list of connectable devices

````kotlin
    devices = vending.getVisibleDevices()
````

### Create callback & subscribe to DeviceChangeListener

```kotlin
    private val callback = object : ConnectableDeviceStateChangeListener {
            override fun onChangeDevices(devices: List<ConnectableDevice>) {
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

### Working with tokens

Step 1: Obtain token information from backend

    In order to work with Locker devices you have to obtain tokens, which are generated by Jetbeep Locker utility. Typically these information will be passed to your mobile application from your backend.

Step 2: Create `Token` objects from the hex representation of the token, received from the backend:

```kotlin
    val token = Token.createToken(hexToken)
```

`hexToken` - String representation of the token in hex format

Step 3: Subscribe to locker events:

```kotlin
JetBeepSDK.connections.lockers.subscribe(
    object : DeviceStatusCallback {
        override fun onLockerDeviceDetected(lockerDevice: LockerDevice) {
            // new Locker device detected
        }

        override fun onLockerDeviceLost(lockerDevice: LockerDevice) {
            // Locker device was lost (out of range)
        }

        override fun onLockerDeviceChanged(lockerDevice: List<LockerDevice>) {
            // Locker device status has changed
        }
    }
)
```

Step 4: Search for the nearby Locker devices

All communications between mobile application and Jetbeep locker device implemented
via [Lockers](#lockers). First of all you have to start searching for the devices nearby, by
previously generated tokens:

```kotlin
    JetBeepSDK.connections.lockers.startSearch(tokens)
```

`tokens` - list of [`Token`](#token)

Step 5: Open (or confirm) the lock

1. Select one of the available Locker devices and corresponding token information from the array:

```kotlin
    val lockerDevice = JetBeepSDK.connections.lockers.getVisibleDevices().get(index)
```

or get them from the events, explained in "Step 3".

2. Connect & apply token to Locker device:

```kotlin
if (lockerDevice.device.isConnectable) {
    val tokenResult = JetBeepSDK.connections.lockers.apply(token)
}
```

Send token result to the backend:

In case "Step 5" was successful, you can send [`TokenResult`](#tokenresult), information about
current Locker device status (e.g. battery level and information about the locks) to your backend,
which could be further decoded by the locker utility or display this information in your mobile app.

Step 7: Finishing the flow:

Once you are done, simply stop the search and unsubscribe from events in your `Fragment`
or `Activity`:

```kotlin
    JetBeepSDK.connections.lockers.stopSearch()
    JetBeepSDK.connections.lockers.unsubscribe()
```

### Testing

IMPORTANT NOTE: Before testing ensure that your devices is configured for VENDING-type merchants. Please double-check them.

### Classes

#### VendingDevices methods
  
`connect(device: ConnectableDevice): Boolean` - to connect to specified device. Returns *true* on successful connection.

`disconnect(): Boolean` - to disconnect from device. Returns *true* on successful disconnection. 

`subscribe(customerCallback: ConnectableDeviceStateChangeListener)` - subscribe to a listener to get updates of devices and their statuses. 

`unsubscribe(customerCallback: ConnectableDeviceStateChangeListener)` - unsubscribe from a listener.

`getVisibleDevices(): List<ConnectableDevice>` - get a list of all visible connectable devices.

**`ConnectableDevice`** - entity of connectable device. Contains some public things:
 - `shopId: Int` - shop id of the device
 - `shopName: String` - shop name
 - `isConnectable(): Boolean` - returns true if device is connectable at the moment.

#### Token

```kotlin
class Token(val hex: String) {
    val tokenVersion: Byte
    val tokenCounter: Long
    val tokenType: TokenType
    val tokenParams: Byte
    val deviceId: Int
    val lockIndex: Byte
    val signature: ByteArray
    val token: ByteArray
}
```

#### LockerDevice

```kotlin
data class LockerDevice(
    val device: ConnectableDevice,
    val tokens: List<Token>
)
```

#### DeviceStatusCallback

```kotlin
interface DeviceStatusCallback {
    fun onLockerDeviceDetected(lockerDevice: LockerDevice)
    fun onLockerDeviceLost(lockerDevice: LockerDevice)
    fun onLockerDeviceStatusChanged(lockerDevice: List<LockerDevice>)
}
```

#### Lockers

```kotlin
interface Lockers {
    fun startSearch(tokens: List<Token>)
    fun stopSearch()
    suspend fun apply(token: Token): TokenResult?
    fun getVisibleDevices(): List<LockerDevice>
    fun subscribe(listener: DeviceStatusCallback)
    fun unsubscribe(listener: DeviceStatusCallback)
}
```

#### TokenResult

```kotlin
class TokenResult(val result: ByteArray)
```
