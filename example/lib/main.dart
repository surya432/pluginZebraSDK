import 'dart:convert';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:ZprinterCPCL/ZprinterCPCL.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _checkBluetoothIsEnable = false;
  bool isShowDevices = false;

  get dataDevices async {
    try {
      return jsonDecode(await ZprinterCPCL.getDevicesBluetooth);
    } on PlatformException catch (e) {
      print("dataDevices" + e.message);
    }
  }

  @override
  void initState() {
    initPlatformState();
    super.initState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    checkBluetoothIsEnable();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Bluetooth Is Enable: $_checkBluetoothIsEnable\n'),
              FlatButton(
                color: Colors.red,
                onPressed: () => checkBluetoothIsEnable(),
                child: Text("Check Bluetooth"),
              ),
              _checkBluetoothIsEnable == true
                  ? FutureBuilder<dynamic>(
                      future: dataDevices,
                      builder: (context, snapshot) {
                        if (snapshot.hasError) {
                          return Text("GAGAL Mendapatkan Device");
                        } else if (snapshot.hasData &&
                            snapshot.connectionState !=
                                ConnectionState.active &&
                            snapshot.connectionState == ConnectionState.done) {
                          return buildItemList(snapshot.data);
                        } else {
                          return Text("");
                        }
                      },
                    )
                  : Container(
                      child: Text("BlueTooth Tidak Aktif"),
                    ),
            ],
          ),
        ),
      ),
    );
  }

  Widget buildItemList(List<dynamic> snapshot) {
    String cpclData =
        '! 0 200 200 20 1\r\nTEXT 5 0 0 0 12345678901234567890123456789\r\nPRINT\r\n';
    return Container(
      constraints: new BoxConstraints(
        minHeight: 90.0,
        maxHeight: double.infinity,
      ),
      child: ListView.builder(
        itemCount: snapshot.length,
        shrinkWrap: true,
        // physics: NeverScrollableScrollPhysics(),
        scrollDirection: Axis.vertical,
        itemBuilder: (context, index) {
          Map<String, dynamic> dataVendor = snapshot[index];
          String name = dataVendor['name'].toString();
          String mac = dataVendor['mac'].toString();
          return SizedBox(
            child: FlatButton(
              onPressed: () async {
                // setState(() => isShowDevices = false);
                await _prinCpclCode(context, mac, cpclData);
                // await _getPrinterTest(mac);
              },
              child: Column(
                children: [
                  ListTile(
                    title: Text('$name ( $mac )'),
                  ),
                  SizedBox(
                    height: 8,
                  ),
                  new Divider(
                    height: 2,
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  checkBluetoothIsEnable() async {
    if (await Permission.location.isGranted ||
        await Permission.locationAlways.isGranted ||
        await Permission.locationWhenInUse.isGranted) {
      try {
        bool isEnableBluetooth = await ZprinterCPCL.checkBluetoothIsEnable;
        setState(() {
          _checkBluetoothIsEnable = isEnableBluetooth;
        });
      } on PlatformException catch (e) {
        print("checkBluetoothIsEnable" + e.message);
        setState(() {
          _checkBluetoothIsEnable = false;
        });
      }
    } else {
      await [
        Permission.location,
        Permission.locationAlways,
        Permission.locationWhenInUse,
        Permission.storage,
      ].request();
      if (await Permission.location.isPermanentlyDenied ||
          await Permission.locationWhenInUse.isPermanentlyDenied ||
          await Permission.locationAlways.isPermanentlyDenied ||
          await Permission.storage.isPermanentlyDenied) {
        openAppSettings();
        setState(() {
          _checkBluetoothIsEnable = false;
        });
      } else {
        setState(() {
          _checkBluetoothIsEnable = true;
        });
      }
    }
    // openAppSettings();
  }

  showAlertDialog(BuildContext context, String title, String body) {
    // set up the button
    Widget okButton = FlatButton(
      child: Text("OK"),
      onPressed: () {
        Navigator.of(context).pop();
        print("ok");
      },
    );

    // set up the AlertDialog
    AlertDialog alert = AlertDialog(
      title: Text(title),
      content: Text(body),
      actions: [
        okButton,
      ],
    );

    // show the dialog
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return alert;
      },
    );
  }

  _prinCpclCode(BuildContext context, String mac, String textPrint) async {
    try {
      String result = await ZprinterCPCL.printText(mac, textPrint);
      // showAlertDialog(context, "Berhasil", result);
      Scaffold.of(context).showSnackBar(SnackBar(content: Text(result)));
    } on PlatformException catch (e) {
      Scaffold.of(context).showSnackBar(SnackBar(
        content: Text(e.message),
      ));
    }
  }
}
