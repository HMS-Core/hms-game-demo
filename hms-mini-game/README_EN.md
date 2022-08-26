English | [Chinese]()
## Contents

* [Introduction](#Introduction)
* [Installation](#Installation)
* [Environment Requirements](#Environment)
* [Getting Started](#Getting)
* [Configuration](#Configuration)
* [Technical Support](#Technical)
* [License](#License)

## Introduction
The sample code integrates the features including sign-in, floating window, In-App Purchases (IAP), product query and redelivery, and player information query of Game Service, to provide a sample program for your reference and use.

You can use HMS Toolkit to quickly run the sample code. HMS Toolkit supports one-stop kit integration, and provides functions such as free app debugging on remote real devices. For details about HMS Toolkit, please refer to the [HMS Toolkit document](https://developer.huawei.com/consumer/en/doc/development/Tools-Guides/getting-started-0000001077381096).

## Installation
Before using the sample code, check whether Android Studio has been set up. Open the sample code in Android Studio and run the same game on a mobile phone or simulator where the latest HMS Core (APK) is installed.

## Environment Requirements
Android SDK (API level 19 or higher) is recommended

## Getting Started
1. Check whether Android Studio is ready for app development. Prepare a device running the latest Huawei Mobile Services (HMS).
2. Register a [HUAWEI ID](https://developer.huawei.com/consumer/en/).
3. Create an app and configure app information in AppGallery Connect.
For details, please refer to [Game Service Development Preparations](https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/config-agc-0000001050166285).
4. [Enable required services](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/appgallerykit-preparation-game-0000001055356911#section382112213818) in AppGallery Connect.
5. Import the demo to Android Studio 3.0 or a later version.
6. Configure the sample code.
(1) Download the **agconnect-services.json** file of your app from AppGallery Connect, and add the file to the root directory (**\app**) of the sample project.
(2) Open the app-level **build.gradle** file of the same project and set **applicationId** to your app package name.
(3) Configure the signature in the sample project and configure the signature certificate fingerprint in AppGallery Connect.
7. Run the sample code on the Android device.

## Configuration
Before using the functions provided in the sample code, you need to [configure your app information](https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/config-agc-0000001050166285) in AppGallery Connect.
 
## Technical Support
You can visit our [Reddit community](https://www.reddit.com/r/HuaweiDevelopers/) to obtain the latest information about HMS Core and communicate with other developers.

If you have any questions about the sample code, try the following:
- Visit [Stack Overflow](https://stackoverflow.com/questions/tagged/huawei-mobile-services) and submit your development problem under the `huawei-mobile-services` tag. Huawei experts will help you solve the problem in one-to-one mode.
- Visit the HMS Core section in the [Huawei Developer Forum](https://forums.developer.huawei.com/forumPortal/en/forum/hms-core) and communicate with other developers.

If you encounter any issues when using the sample code, submit your [issues](https://github.com/HMS-Core/hms-game-demo/issues) or submit a [pull request](https://github.com/HMS-Core/hms-game-demo/pulls).

## License
The sample code is licensed under [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

For more SDK development guides, please refer to:
[Development Guide](https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/game-introduction-v4)
[API References](https://developer.huawei.com/consumer/en/doc/development/HMS-References/jos-games-v4)

