# [EasyAndroidMqtt](https://github.com/coderJohnZhang/EasyAndroidMqtt)

## Introduction

This is a more user-friendly Android MQTT Client library based on Eclipse Paho Android Service rewritten.


## Features

- Rewrite using Kotlin language.

- Adapted API behavior changes for Android 8.0 version and above.

- Library integration is very simple.

- The library will continue to maintain, update and adapt.

- Welcome to issue.


## How to use
[![](https://jitpack.io/v/coderJohnZhang/EasyAndroidMqtt.svg)](https://jitpack.io/#coderJohnZhang/EasyAndroidMqtt)

Step 1. Add the JitPack repository to your root build.gradle at the end of repositories.

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency.
```gradle
dependencies {
    implementation 'com.github.coderJohnZhang:EasyAndroidMqtt:1.0.1'
}
```

Step 3. Add the following permissions to AndroidManifest.xml in the app.
```xml
<!-- Permissions the MQTT Requires -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Step 4. Registering Android MQTT service.
```xml
<!-- Mqtt Service -->
<service android:name="com.zhangjun.mqtt.android.service.MqttService" />
```


## About me

Email: coder.john.cheung@gmail.com<br><br>
CSDN blog: http://blog.csdn.net/johnwcheung<br><br>
Github: https://github.com/coderJohnZhang

## License

		Copyright 2024 Zhang Jun

		Licensed under the Apache License, Version 2.0 (the "License");
		you may not use this file except in compliance with the License.
		You may obtain a copy of the License at

			http://www.apache.org/licenses/LICENSE-2.0

		Unless required by applicable law or agreed to in writing, software
		distributed under the License is distributed on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		See the License for the specific language governing permissions and
		limitations under the License.
