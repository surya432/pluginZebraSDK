import 'dart:async';

import 'package:flutter/services.dart';

class ZprinterCPCL {
  static const MethodChannel _channel = const MethodChannel('ZprinterCPCL');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> get checkBluetoothIsEnable async {
    final bool isEnable = await _channel.invokeMethod('checkBluetoothIsEnable');
    return isEnable;
  }

  static Future<String> get checkPermissionBluetooth async {
    final String version =
        await _channel.invokeMethod('checkPermissionBluetooth');
    return version;
  }

  static Future<String> get getDevicesBluetooth async {
    String version = await _channel.invokeMethod('getDevicesBluetooth');
    return version;
  }

  static Future<String> printText(String mac, String data) async {
    String version = await _channel
        .invokeMethod('printTextAndCheck', {"mac": mac, "data": data});
    return version;
  }
}
