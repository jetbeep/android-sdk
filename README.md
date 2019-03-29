# JetBeep Android sdk

## Hardware setup
https://drive.google.com/drive/u/1/folders/1exPvE0fJYBYEf-XRj5r4i4IQqMalLuma

## Gradle dependencies

Add this dependency to your project's build file:

```groovy
implementation 'com.jetbeep:jetbeepsdk:0.8.3'
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
    releaseImplementation 'com.jetbeep:jetbeepsdk:0.8.3'
    debugImplementation 'com.jetbeep:jetbeepsdk:0.8.4-SNAPSHOT'
```

### Now you are ready to go!

Example of initialization of JetBeepSdK:

```kotlin
    JetBeepSDK.init(
        application, // instance of Application
        serviceUUID, // serviceUUID
        appId, // your app name that you can request from our side,
        appToken, // your app token key that you can request from our side
        registrationType // Jetbeep registration type
    )
```

Instance of barcode handler protocol, it will be used when you will provide barcodes

```kotlin
    JetBeepSDK.barcodeRequestHandler = object : JBBarcodeRequestProtocol {
        override var listener: JBBarcodeTransferProtocol? = object : JBBarcodeTransferProtocol {
            override fun failureBarcodeTransfer(shop: Shop) {
                //
            }

            override fun succeedBarcodeTransfer(shop: Shop) {
                //
            }

        }

        override fun barcodeRequest(merchant: Merchant, shop: Shop): Array<Barcode>? {
            //Put your barcodes based on merchant and shop
        }
    }
```

Handle result on your place to track is barcodes transfering moved succeed or not
```kotlin
    JBBarcodeRequestProtocol.listener
```

To receive events of entry and exit into the zone of beacon, install a listener:

```kotlin
    val locationCallbacks = object : LocationCallbacks {
        override fun onObtainActualShops(shops: List<Shop>) {
            //
        }

        override fun onShopExit(shop: Shop, merchant: Merchant) {
            //
        }

        override fun onShopEntered(shop: Shop, merchant: Merchant) {
            //
        }
    }
    
    JetBeepSDK.locations.subscribe(locationCallbacks)
```

To receive events such as loyalty card transfers, install a listener:

```kotlin
    val beeperCallback: BeeperCallback = object : BeeperCallback() {
        override fun onEvent(beeperEvent: BeeperEvent) {
            showNotification(beeperEvent)
        }
    }
    
    JetBeepSDK.beeper.subscribe(beeperCallback)
```

See the test application for more details.
