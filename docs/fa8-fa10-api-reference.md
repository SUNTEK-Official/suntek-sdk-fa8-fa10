# FA8 SDK API

Android SDK for SUNTEK FA8 / FA10 series devices. Provides system-level control over hardware peripherals, device configuration, display, network, and application lifecycle.

## Contents

- `FA8API-SUNTEK.jar` — Java SDK (`com.suntek.SUNTEK`)

## Getting Started

Copy `FA8API-SUNTEK.jar` into the Android project's `app/libs` folder:

```gradle
dependencies {
    implementation files('libs/FA8API-SUNTEK.jar')
}
```

```java
import com.suntek.SUNTEK;
import com.suntek.StorageInfo;
import com.suntek.entity.IpConfig;

SUNTEK mAPI = SUNTEK.getInstance();
```

On-device checks: **Settings > User Settings**. Open device settings test utility via the test app is `FA8CommonApi` (`com.suntek.fa8`).



## API Overview

### System Parameters

Most setters need `mAPI.reboot()` to take effect.

| Feature | Getter | Setter |
|---------|--------|--------|
| Board model | `getBoardModel()` | — |
| Serial number | `getSerialNo()` | `setSerialNo(String)` |
| Ethernet MAC address | `getEthernetMAC()` | `setEthernetMAC(String)` |
| 4G module IMEI | `getIMEI()` | `setIMEI(String)` |
| Device model | `getProductModel()` | `setProductModel(String)` |
| Storage info | `getStorageInfos()` | — |
| Language | — | `updateLanguage(String language, String country)` |

- **Boot logo** — `setBootLogo(String path)` (8-bit BMP)
- **Boot animation** — `setBootAnimation(String path)` (standard Android zip)
- **APK install policy** — `setInstallPackagePolicy(SUNTEK.InstallPackagePolicy)` (normal / deny-all / password)

### OTA Upgrade

```java
mAPI.otaUpdate("/sdcard/update.zip");
```

The package must be named `update.zip` and placed on sdcard.

### APK Management

- **Auto-launch on boot** — `setBootLaunchApk(String packageName, boolean launch)`
- **Silent install** — `installApkSilently(String apkPath, String packageName, String className)` — pass `null` for the last two parameters to skip auto-launch
- **Silent uninstall** — `uninstallApkSilently(String packageName)`
- **App keep-alive** — `setAppKeepLive(String packageName, int keepAliveTimeSec)`
- **Foreground keep-alive** — `setForegroundAppKeepLive(String packageName, int keepAliveTimeSec)`

Keep-alive does not persist across reboots.

### Display

| Feature | Method |
|---------|--------|
| Screen rotation | `getScreenRotation(SUNTEK.ScreenType)` / `setScreenRotation(SUNTEK.ScreenType, SUNTEK.RotationDegree)` |
| Screen density | `getLcdDensity()` / `setLcdDensity(SUNTEK.LcdDensity)` |
| Screen on/off | `isScreenOn()` / `setScreenOnOff(boolean)` |

`ScreenType.MAIN` is the main screen, `ScreenType.AUX` is the secondary screen.

### Watchdog

```java
mAPI.enableWatchdog(true);
mAPI.setWatchdogTimeout(15);
mAPI.feedWatchdog();
```

Feed about every 10 seconds. If not fed before timeout, the device resets.

### Power Management

- **Shutdown** — `mAPI.shutdown()`
- **Reboot** — `mAPI.reboot()`
- **Sleep** — `mAPI.sleep()`
- **Wake up** — `mAPI.wakeup()`

### Navigation Bar & Status Bar

- **Navigation bar** — `setNavigationBarVisibility(SUNTEK.NavigationBarVisibility)`: `VISIBLE`, `INVISIBLE` (swipeable), `ALWAYS_INVISIBLE`
- **Status bar** — `setExtendStatusBarVisibility(SUNTEK.ExtendStatusBarVisibility)` — reboot required

### Root Privileges

```java
if (!mAPI.isRoot()) {
    mAPI.enableRoot(true);
}
```

### Network (Ethernet)

- **IP configuration** — `getIpConfig()` returns `IpConfig` (ip, mask, gateway, dnsList)
- **Static IP** — `setStaticIp(IpConfig)`
- **DHCP** — `setDhcpIp()`
- **IP mode** — `getIpMode()` returns `SUNTEK.IpMode.STATIC` or `SUNTEK.IpMode.DHCP`
- **Ethernet switch** — `isEthernetOpen()` / `setEthernetState(boolean)`

### System Time

- **Set time** — `setSystemTime(long timeInMills)`
- **Scheduled power on/off** — `setPowerOnOffAlarmCycle(int type, int[] weekdays, int[] poweroff, int[] poweron)`
  - `type=1`: one-time
  - `type=3`: weekly
- **Cancel schedule** — `cancelPowerOnOffAlarm()`
- **Network time sync** — `isEnableNetworkProvidedTime()` / `setEnableNetworkProvidedTime(boolean)`

### Hardware — GPIO

General IO, relays, fill lights (red/green/blue/white/infrared), USB power, cash box, doorbell, onboard LEDs. Actual pins depend on the board.

| Operation | Method |
|-----------|--------|
| Get GPIO state | `getGpioState(int index)` |
| Set GPIO state | `setGpioState(int index, SUNTEK.GpioState)` |
| Get GPIO mode | `getGpioMode(int index)` |
| Set GPIO mode | `setGpioMode(int index, SUNTEK.GpioMode)` |
| Listen | `listenGpio(int index, SUNTEK.GpioListenerCallback)` |
| Unlisten | `unlistenGpio(int index, SUNTEK.GpioListenerCallback)` |

Listener polls about once per second — not for sub-second button presses.

### Hardware — LED Brightness

```java
mAPI.setLightBrightness(SUNTEK.LIGHT_RED, 204); // 80%, range 0-255
```

### Hardware — Wiegand

- **Mode** — `readWiegandMode()` / `writeWiegandMode(SUNTEK.WiegandMode)`
- **Synchronous read** — `readWiegand()` (blocking, use a background thread)
- **Asynchronous read** — `readWiegandAsyn(SUNTEK.WiegandCallback)` — call again in `onSuccess` / `onFailure` for continuous reading
- **Write** — `writeWiegand(SUNTEK.WiegandFormat, long code)`

## Reference Code

Cash box — pull low for 500ms, then high:

```java
CompletableFuture.runAsync(() -> {
    mAPI.setGpioState(SUNTEK.CASHBOX_0, SUNTEK.GpioState.LOW);
    try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException e) { }
}).thenAccept(u -> {
    mAPI.setGpioState(SUNTEK.CASHBOX_0, SUNTEK.GpioState.HIGH);
});
```

Root shell:

```java
Process p = Runtime.getRuntime().exec("su");
PrintWriter pw = new PrintWriter(p.getOutputStream(), true);
pw.println("your_command_here");
pw.println("exit");
boolean success = p.waitFor() == 0;
```


---

# FA8 / FA10 System API Detailed Reference

FA8 / FA10 SDK API User Guide

FA8API-SUNTEK.jar · com.suntek.SUNTEK

The API described in this document corresponds to the FA8/FA10 series products. You need to check the hardware specifications to confirm the actual peripherals supported by the board.

#### 0. Integrate the new JAR (changes relative to the old document)

Application projects should only depend on the new package and should not include YNHAPI-20251224.jar at the same time.

| project | New JAR |
| :--- | :--- |
| JAR file | FA8API-SUNTEK.jar |
| Java package name | com.suntek |
| Entry Class | SUNTEK |
| Take an example | SUNTEK mAPI = SUNTEK.getInstance(); |
| Internal type | SUNTEK.GpioState, etc. |
| IP Entity | com.suntek.entity.IpConfig |
| Stored information | com.suntek.StorageInfo |

#### 0.1 is placed in the project.

Copy FA8API-SUNTEK.jar to the app/libs/ directory of your Android project, and add the following to app/build.gradle:

```groovy
dependencies {
    implementation files('libs/FA8API-SUNTEK.jar')
}
```

### 0.2 Retrieve Interface Instance

All subsequent examples will use this mAPI.

```java
import com.suntek.SUNTEK;
import com.suntek.StorageInfo;
import com.suntek.entity.IpConfig;
SUNTEK mAPI = SUNTEK.getInstance();
```

### 0.3 Real Device Self-Test Entry

The relevant interfaces can be verified in the device's "Settings → User Settings". The current firmware menu text may still display YNHCommonAPI, which is the name of the pre-installed application, not the JAR class name.

The new test APK project is FA8CommonApi, and the source code package name is com.suntek.fa8.

#### Edit History

| date | Modification points |
| :--- | :--- |
| 20220531 | The single parameter `installApkSilently` has been deprecated; now, it automatically launches after installation by package name/class name. |
| 20220625 | Add serial number, device model, Ethernet MAC address, and IMEI. |
| 20220630 | Add Ethernet switch and status |
| 20220701 | Add App Protection |
| 20220713 | Add boot image/animation and drop-down status bar |
| 20220830 | Get screen on/off status |
| 20220922 | Multiple modes added to timed on/off |
| 20221107 | Add foreground protection to the app |
| 20230413 | Individual control of screen display switch |
| 20240321 | Modify Wiegand asynchronous read documentation |
| 20240521 | Add setInstallPackagePolicy to the whitelist for installation. |
| 20250310 | Add language settings |
| 20251224 | Increase watchdog timeout time |
| 20260813 | The document has been changed to Word; all examples have been switched to FA8API-SUNTEK.jar / com.suntek.SUNTEK |

#### 2. System Parameters

Most setters require a subsequent call to mAPI.reboot() to take effect.

### 2.1 Obtain the motherboard model

```java
String model = mAPI.getBoardModel();
```

### 2.2 Serial Number

#### 2.2.1 Obtaining

```java
String sn = mAPI.getSerialNo(); // Consistent with Build.SERIAL
```

#### 2.2.2 Settings

```java
String serialNo = "12345678";
mAPI.setSerialNo(serialNo);
mAPI.reboot();
```

### 2.3 Ethernet MAC

```java
String mac = mAPI.getEthernetMAC();
mAPI.setEthernetMAC("00:E0:EC:37:2A:42");
mAPI.reboot();
```

### 2.4 4G Module IMEI

```java
String imei = mAPI.getIMEI();
mAPI.setIMEI("864793030776509");
mAPI.reboot();
```

### 2.5 Equipment Model

```java
String model = mAPI.getProductModel(); // Consistent with Build.MODEL
mAPI.setProductModel("123456");
mAPI.reboot();
```

### 2.6 Device Storage Information

Returns a list of com.suntek.StorageInfo locations, including internal storage, TF/SD, and USB storage. Unit: KB.

```java
List<StorageInfo> list = mAPI.getStorageInfos();
for (StorageInfo info : list) {
    int type = info.getType(); // TYPE_MEMORY / TYPE_LOCAL_STORAGE / TYPE_TFCARD / TYPE_USB_STORAGE
    long total = info.getTotalSize();
    long free = info.getFreeSize();
    String path = info.getPath();
}
```

### 2.7 Boot Image

```java
mAPI.setBootLogo("/sdcard/boot.bmp"); // Must be an 8-bit BMP
```

### 2.8 Boot Animation

```java
mAPI.setBootAnimation("/sdcard/bootanimation.zip"); // Standard Android bootanimation
```

### 2.9 APK Installation Strategy (Whitelist)

```java
SUNTEK.InstallPackagePolicy policy = mAPI.new InstallPackagePolicy();
List<String> allow = new ArrayList<>();
allow.add("com.example.app");
policy.setAllowInstallPackageList(allow);
// ALLOW_INSTALL_TYPE_NORMAL = -1 Cancel policy
// ALLOW_INSTALL_TYPE_NOT_ALLOW = 0 Whitelist only, otherwise reject
// ALLOW_INSTALL_TYPE_PASSWORD = 1 Password required for non-whitelisted users
policy.setAllowInstallType(SUNTEK.InstallPackagePolicy.ALLOW_INSTALL_TYPE_PASSWORD);
policy.setAllowInstallPassword("1234abcd");
mAPI.setInstallPackagePolicy(policy);
```

### 2.10 Setting the Language

```java
mAPI.updateLanguage("zh", "CN"); // Corresponds to values-zh-rCN
```

Commonly used: zh/CN, zh/TW, zh/HK, en/US, en/GB, ja/JP, ko/KR, etc., consistent with Android values-xx-rYY.

#### 3. OTA upgrade

The upgrade package must be placed in the sdcard directory, and the file name should be update.zip.

```java
mAPI.otaUpdate("/sdcard/update.zip");
```

#### 4. APK Management

### 4.1 Automatic startup upon system boot

```java
mAPI.setBootLaunchApk("com.example.app", true);
```

### 4.2 Silent Installation

`mAPI.installApkSilently("/sdcard/API.apk", "com.example", "com.example.MainActivity"); ` // The last two parameters being null indicates that
`mAPI.installApkSilently("/sdcard/API.apk", null, null);` indicates that the application will not launch automatically after installation .

### 4.3 Silent Uninstallation

```java
mAPI.uninstallApkSilently("com.example.app");
```

### 4.4 Application Guardian

Restarting does not retain the changes; the application needs to access them again after booting up.

```java
// Non-foreground guardian, checks every 10 seconds.
mAPI.setAppKeepLive("com.example.app", 10);
mAPI.setAppKeepLive("", 1); // Cancel
// Foreground guardian
mAPI.setForegroundAppKeepLive("com.example.app", 10);
mAPI.setForegroundAppKeepLive("", 1);
```

#### 5. Display

#### 5.1 / 5.2 Screen Rotation

```java
SUNTEK.RotationDegree deg = mAPI.getScreenRotation(SUNTEK.ScreenType.MAIN);
if (deg == SUNTEK.RotationDegree.DEGREE_90) { }
mAPI.setScreenRotation(SUNTEK.ScreenType.AUX, SUNTEK.RotationDegree.DEGREE_180);
```

#### 5.3 / 5.4 screen density

```java
SUNTEK.LcdDensity density = mAPI.getLcdDensity();
mAPI.setLcdDensity(SUNTEK.LcdDensity.DENSITY_240);
```

#### 5.5 / 5.6 Screen On/Off

isScreenOn() cannot be used to determine this function after sleep/wakeup; it is only applicable when the app itself calls setScreenOnOff.

```java
boolean on = mAPI.isScreenOn();
mAPI.setScreenOnOff(false);
```

#### 6. Watchdog

```java
mAPI.enableWatchdog(true);
mAPI.setWatchdogTimeout(15); // seconds, may vary by 1-2 seconds depending on the platform.
mAPI.feedWatchdog(); // It is recommended to feed the watchdog approximately every 10 seconds.
mAPI.enableWatchdog(false);
```

#### 7. Shutting down and restarting

```java
mAPI.shutdown();
mAPI.reboot();
```

#### 8. Hibernation and Wake-up

```java
PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
boolean isInteractive = pm.isInteractive();
mAPI.sleep();
mAPI.wakeup();
```

#### 9. Navigation bar and status bar

Navigation bar: VISIBLE (visible); INVISIBLE (invisible but can be swiped up); ALWAYS_INVISIBLE (invisible and cannot be swiped out).

```java
mAPI.setNavigationBarVisibility(SUNTEK.NavigationBarVisibility.ALWAYS_INVISIBLE);
SUNTEK.NavigationBarVisibility v = mAPI.getNavigationBarVisibility();
if (v == SUNTEK.NavigationBarVisibility.ALWAYS_INVISIBLE) {
    mAPI.setNavigationBarVisibility(SUNTEK.NavigationBarVisibility.VISIBLE);
}
```

A restart is required for the settings in the drop-down status bar to take effect.

```java
mAPI.setExtendStatusBarVisibility(SUNTEK.ExtendStatusBarVisibility.VISIBLE);
SUNTEK.ExtendStatusBarVisibility sv = mAPI.getExtendStatusBarVisibility();
```

#### 10. Root privileges

```java
if (!mAPI.isRoot()) {
    if (mAPI.enableRoot(true)) {
        Log.d(TAG, "Successful root");
    }
}
```

#### 11. Ethernet

```java
IpConfig ipConfig = mAPI.getIpConfig();
String ip = ipConfig.ip;
String mask = ipConfig.mask;
String gateway = ipConfig.gateway;
ArrayList<String> dnsList = ipConfig.dnsList;
```
mAPI.setStaticIp(new IpConfig("192.0.2.10", "255.255.255.0", "192.0.2.10",
```java
Arrays.asList("192.0.2.10")));
mAPI.setDhcpIp();
SUNTEK.IpMode mode = mAPI.getIpMode(); // STATIC / DHCP
boolean open = mAPI.isEthernetOpen();
mAPI.setEthernetState(true);
```

#### 12. System Time

```java
Calendar calendar = Calendar.getInstance(Locale.getDefault());
calendar.set(2021, 10, 28, 6, 0, 0); // Months start from 0
mAPI.setSystemTime(calendar.getTimeInMillis());
```

Scheduled power on/off: type=1 only takes effect once; type=3 cycles weekly, requires resetting after factory reset or flashing the firmware.

```java
int type = 3;
int[] weekdays = {1, 3, 4, 5}; // Monday, Wednesday, Thursday, Friday
int[] poweroff = {20, 30};
int[] poweron = {8, 35};
mAPI.setPowerOnOffAlarmCycle(type, weekdays, poweroff, poweron);
mAPI.cancelPowerOnOffAlarm();
boolean netTime = mAPI.isEnableNetworkProvidedTime();
mAPI.setEnableNetworkProvidedTime(true);
```

#### 13. Hardware Module

### 13.1 GPIO

GPIO is used for I/O, relays, fill lights, USB power supply, cash drawer, doorbell, etc. The actual ports supported by the board are specified in the hardware datasheet. Constants are defined in SUNTEK. The listener function polls approximately once per second, making it unsuitable for detecting key presses less than one second in duration.

| constant | value | meaning |
| :--- | :--- | :--- |
| SUNTEK.GPIO_1 … GPIO_15 | 0～14 | General I/O |
| SUNTEK.LIGHT_RED / GREEN / BLUE / WHITE | 15-18 | Fill light |
| SUNTEK.LIGHT_INFRARED | 19 | USB camera infrared fill light |
| SUNTEK.RELAY | 20 | relay |
| SUNTEK.BELL | twenty one | doorbell |
| SUNTEK.CASHBOX_0 / CASHBOX_1 | 22 / 23 | Cash Box |
| SUNTEK.LIGHT_BOARD_BLUE / RED / YELLOW | 24 / 28 / 30 | Onboard indicator lights |
| SUNTEK.USB_1 / USB_2 / USB_3 | 25 / 26 / 27 | USB powered |
| SUNTEK.LIGHT_CAMERA_EXTERNAL_INFRARED | 29 | MIPI camera infrared fill light |

```java
if (mAPI.getGpioState(SUNTEK.GPIO_1) == SUNTEK.GpioState.HIGH) { }
mAPI.setGpioState(SUNTEK.GPIO_1, SUNTEK.GpioState.HIGH);
SUNTEK.GpioMode mode = mAPI.getGpioMode(SUNTEK.GPIO_1);
mAPI.setGpioMode(SUNTEK.GPIO_1, SUNTEK.GpioMode.INPUT);
```
SUNTEK.GpioListenerCallback cb = new SUNTEK.GpioListenerCallback() {
```java
public void onChanged(int index, int oldValue, int newValue) { }
};
mAPI.listenGpio(SUNTEK.GPIO_1, cb);
mAPI.unlistenGpio(SUNTEK.GPIO_1, cb);
```

### 13.2 LED brightness

```java
mAPI.setLightBrightness(SUNTEK.LIGHT_RED, 204); // 0～255, 204 ≈ 80%
```

### 13.3 Wiegand

```java
SUNTEK.WiegandMode wm = mAPI.readWiegandMode();
mAPI.writeWiegandMode(SUNTEK.WiegandMode.INPUT);
// Synchronous blocking read, please put it in a background thread
long code = mAPI.readWiegand(); // Asynchronous read: For continuous reads, you must register
```
SUNTEK.WiegandCallback
again in onSuccess / onFailure wcb = new SUNTEK.WiegandCallback() {
```java
public void onSuccess(long code) { mAPI.readWiegandAsyn(this); }
public void onFailure() { mAPI.readWiegandAsyn(this); }
};
mAPI.readWiegandAsyn(wcb); // Callback in worker thread
mAPI.writeWiegand(SUNTEK.WiegandFormat.FORMAT_26, 12345678);
```

#### 14. Reference Code

### 14.1 Cash Box

First lower it by 500ms, then raise it.

CompletableFuture.runAsync(() -> {
```java
mAPI.setGpioState(SUNTEK.CASHBOX_0, SUNTEK.GpioState.LOW);
try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException e) { }
```
}).thenAccept(u -> {
```java
mAPI.setGpioState(SUNTEK.CASHBOX_0, SUNTEK.GpioState.HIGH);
});
```

### 14.2 Executing the root command

```java
boolean executeCmd(String content) throws IOException, InterruptedException {
    Process p = Runtime.getRuntime().exec("su");
    PrintWriter pw = new PrintWriter(p.getOutputStream(), true);
    pw.println(content);
    pw.println("exit");
    return p.waitFor() == 0;
}
```

#### Appendix. Explanation
