# ATWDP
Android Things Wireless Device Provisioning

This is an android library to help connect your android things device to a WiFi network via Bluetooth Low Energy.  There is a [companion app](https://github.com/chmoder/ATWDPC) to help make finding and connecting to your device easy.

#### Usage:
```java
@Override
protected void onResume() {
  super.onResume();
  chmoderBLEManager = ChmoderBLEManager.getInstance(this);
}
```
