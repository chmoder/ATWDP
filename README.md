# ATWDP
Android Things Wireless Device Provisioning

This is an android library to help connect your android things device to a WiFi network via Bluetooth Low Energy.  There is a [companion app](https://github.com/chmoder/ATWDPC) to help make finding and connecting to your device easy.

#### Usage:
Module build.gradle
```groovy
dependencies {
  ...
  compile 'org.chmoder.atwdp:ATWDP:0.2.0'
}
```
AndroidManifest.xml
```xml
<application
  ...
  android:name="org.chmoder.atwdp.ChmoderApplication">
```

SomeActivity.java
```java
@Override
protected void onCreate() {    
  ...
  ChmoderBLEManager chmoderBLEManager = ChmoderBLEManager.getInstance(this);
  ChmoderWIFIManager chmoderWiFiManager = ChmoderWIFIManager.getInstance(this);

  if(!chmoderWiFiManager.isConnected()) {
      chmoderBLEManager.toggleBluetooth();
  }
}
```
