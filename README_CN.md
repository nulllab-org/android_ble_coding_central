# Android BLE Coding Central

## 概述

这是一个Android Demo APK工程，功能包含通过蓝牙传输Micropython的python代码到ESP32从机上运行，从而实现无线编程，该应用还监听了ESP32的串口功能，可以实现远程无线串口信息传输。该应用需要搭配特定ESP32板子和固件才可以运行。

## 开始

### 编译

下载完该工程后需要更新子模块，请使用如下命令:

```shell
 git submodule update --init --recursive
```

更新完成之后就可以正常使用Android Studio编译

### 传输代码

- 在手机上安装完成APK后运行，请确保有开启手机蓝牙

- 上电启动ESP32板子，可以看到板子上面的蓝牙LED灯亮了之后马上又熄灭，代表无主机与它连接

- 在APP界面上点击`scan device`，该作用是会搜索出所有以`ble_coding_peripheral`的蓝牙设备，并显示出蓝牙MAC地址，如果当前附近只有一个ESP32设备，那么就可以选择当前的唯一的MAC地址

- 选择完成，拨动界面上的`connect 开关`进行连接，连接完成之后`state`会显示`connected`，同时ESP32的**蓝色信号灯**会常亮

- 连接完成，就可以进行代码的传输，当前有预置一些`demo code`，可以选中`demo code`中的`print`, 点击`send main.py`

- 发送完成应用会显示`file transmitted`, 同时ESP32会自动软重启并运行新的程序

- 成功运行`print`程序串口输出框会有持续打印信息

- APK退到后台或者ESP32硬重启会自动断开蓝牙连接，重新连接即可

### 事项说明

- 该应该主要是使用Android提供的ble标准API进行连接和传输数据

- 关于读取串口输入，请参考示例代码`stdio`，使用`sys.stdin.readline()`读取串口输入信息会进行阻塞直到读取到一行串口信息(以'\n'结尾的信息)，所以使用APK发送一行串口信息给ESP32时，需要在结尾加一个换行，也就是键盘上的回车符

- 由于蓝牙设备扫描是一个耗电的行为，所以各家手机厂商对蓝牙扫描功能有不同程度的限制，比如有些手机无法频繁扫描或者无法长时间扫描，所以如果我们已经
扫描到了想要的蓝牙设备即可停止扫描，然后可以在蓝牙地址选择框上进行设备选择。如果我们提前知道了EPS32的蓝牙地址就可以跳过扫描这个过程，直接使用MAC地址进行连接。

- 如果EPS32蓝牙已经连接上某个主机那么蓝色信号灯会常亮，此时其他主机设备无法扫描到它和连接它。