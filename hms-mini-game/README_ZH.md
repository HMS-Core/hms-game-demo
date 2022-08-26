中文 | [English]() 
## 目录
 * [概述](#概述)
 * [安装](#安装)
 * [环境要求](#环境要求)
 * [快速入门](#快速入门)
 * [配置](#配置)
 * [技术支持](#技术支持)
 * [开源协议](#开源协议)


## 概述
游友家园游戏示例代码集成了华为游戏服务的登录、浮标、支付、商品查询、掉单补单、个人信息查询等接口能力，提供了示例代码程序供您参考和使用。


该示例也可以通过HMS Toolkit快速启动运行，且支持各Kit一站式集成，并提供远程真机免费调测等功能。了解更多信息，请参考HMS Toolkit官方链接：https://developer.huawei.com/consumer/cn/doc/development/Tools-Guides/getting-started-0000001077381096

## 安装
在使用示例代码之前，检查Android Studio开发环境是否准备就绪。在Android Studio中打开示例代码，在安装有最新版本的HMS（华为移动服务）的手机或者模拟器上运行。

## 环境要求
建议使用19或更高的安卓SDK版本。

##快速入门
   1、检查Android Studio开发环境是否已准备好。在安装了最新华为移动服务（HMS）的设备上运行应用程序。
   2、注册【华为帐号】（https://developer.huawei.com/consumer/cn/）。
   3、创建应用，并在AppGallery Connect中配置应用信息。
   详细内容请参见：[HUAWEI Game Service Development Preparation](https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/config-agc-0000001050166285)
   4.开发服务权益，在AppGallery Connect控制台打开相关服务开关。
   详细内容参见：https://developer.huawei.com/consumer/cn/doc/development/AppGallery-connect-Guides/appgallerykit-preparation-game-0000001055356911#section382112213818
   5.要构建此demo，请首先在Android Studio (3.x+)中导入该demo。
   6、配置示例代码：
   (1)在AGC上下载应用的文件“agconnect-services.json”，并将该文件添加到示例工程的应用根目录（\app）中。
   (2)将示例工程的应用级build.gradle文件中的applicationid修改为您的应用的包名。
   (3)在示例工程里配置签名并在AGC后台配置签名证书指纹。
   7.在Android设备上运行示例。

## 配置
使用示例代码提供的功能之前，你需要在AppGallery Connect上配置您的应用信息。
详情请见: [HUAWEI Game Service Development Preparation](https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/game-preparation)


## 技术支持
如果您对HMS Core还处于评估阶段，可在[Reddit社区](https://www.reddit.com/r/HuaweiDevelopers/)获取关于HMS Core的最新讯息，并与其他开发者交流见解。

如果您对使用HMS示例代码有疑问，请尝试：
- 开发过程遇到问题上[Stack Overflow](https://stackoverflow.com/questions/tagged/huawei-mobile-services)，在`huawei-mobile-services`标签下提问，有华为研发专家在线一对一解决您的问题。
- 到[华为开发者论坛](https://developer.huawei.com/consumer/cn/forum/blockdisplay?fid=18) HMS Core板块与其他开发者进行交流。

如果您在尝试示例代码中遇到问题，请向仓库提交[issue](https://github.com/HMS-Core/hms-game-demo/issues)，也欢迎您提交[Pull Request](https://github.com/HMS-Core/hms-game-demo/pulls)。

##  开源协议
  Demo示例代码遵循以下开源协议: [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

  更多SDK开发指南，请点击以下链接：
  [Devlopment Guide](https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/game-introduction-v4)
  [API](https://developer.huawei.com/consumer/en/doc/development/HMS-References/jos-games-v4)
