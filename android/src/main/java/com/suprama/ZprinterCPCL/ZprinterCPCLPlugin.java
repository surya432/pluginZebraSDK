package com.suprama.ZprinterCPCL;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.PrinterStatusMessages;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;


/**
 * ZprinterCPCLPlugin
 */
public class ZprinterCPCLPlugin extends Activity implements FlutterPlugin, MethodCallHandler {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 1; // Unique request code
    boolean successPrint = false;
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private BluetoothAdapter mBluetoothAdapter;
    private Boolean isEnableBluetooth = false;
    private Connection connection;
    private String printerStatusMessage = "";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ZprinterCPCL");
        channel.setMethodCallHandler(this);
    }

    public void isBluetoothEnabled() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            
        } else {
            isEnableBluetooth = true;
        }
    }

    private JSONArray getDevicesBluetooth() {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ActivityCompat.startActivityForResult(this, enableBtIntent, REQUEST_ENABLE_BT, null);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        JSONArray devices = new JSONArray();
        for (BluetoothDevice bt : pairedDevices) {
            try {
                Log.i("TAG", "getDevicesBluetooth: " + bt.toString());
                JSONObject data = new JSONObject();
                data.put("name", bt.getName());
                data.put("mac", bt.getAddress());
                data.put("uuid", bt.getUuids());
                devices.put(data);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        mBluetoothAdapter.cancelDiscovery();
//        Log.d("getDevicesBT:", "getDevicesBluetooth: " + devices.toString());
        return devices;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        io.flutter.Log.d("Result ", resultCode + "");
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was enabled
                isEnableBluetooth = true;
            } else if (resultCode == RESULT_CANCELED) {
                // Bluetooth was not enabled
                isEnableBluetooth = false;
            }
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        HashMap<String, Object> map = call.arguments();
        if (call.method.equals("checkBluetoothIsEnable")) {
            isBluetoothEnabled();
            Log.d("bluetoothEnabled: ", isEnableBluetooth.toString());
            result.success(isEnableBluetooth);
        } else if (call.method.equals("getDevicesBluetooth")) {
            JSONArray btDevices = getDevicesBluetooth();
            if (btDevices.length() > 0) {
                result.success(btDevices.toString());
            } else {
                result.error("UNAVAILABLE", "Printer tidak ditemukan.", null);
            }
        } else if (call.method.equals("printTextAndCheck")) {
            successPrint = false;
            String macAddress = "", textPrint = "";
            JSONObject params = new JSONObject(map);
            try {
                macAddress = params.getString("mac");
                textPrint = params.getString("data");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Connection connection = new BluetoothConnection(macAddress);
            try {
                connection.open();
                ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                PrinterStatus printerStatus = printer.getCurrentStatus();
                // printerStatusMessage = printerStatus.toString();
                if (printerStatus.isReadyToPrint) {
                    System.out.println("Ready To Print");
                    System.out.println(printerStatus.printMode);
                    connection.write(textPrint.getBytes());
                    successPrint = true;
                }
                //  else {
                //     successPrint = false;
                //     PrinterStatusMessages statusMessage = new PrinterStatusMessages(printerStatus);
                //         System.out.println("paperOut : "+printerStatus.isPaperOut);
                //     String[] statusMessages = statusMessage.getStatusMessage();
                //     String joinedStatusMessage = "";
                //     for (int i = 0; i < statusMessages.length; i++) {
                //         printerStatusMessage += statusMessages[i] + ";";
                //     }
                //     System.out.println("Cannot Print: " + printerStatusMessage);
                // }
               else if (printerStatus.isPaused) {
                   successPrint = false;
                   printerStatusMessage = "Cannot Print because the printer is paused.";
               } else if (printerStatus.isHeadOpen) {
                   successPrint = false;
                   printerStatusMessage = "Cannot Print because the printer head is open.";
               } else if (printerStatus.isPaperOut) {
                   successPrint = false;
                   printerStatusMessage = "Cannot Print because the paper is out.";
               } else {
                   successPrint = false;
                   printerStatusMessage = "Cannot Print.";
               }
                connection.close();
            } catch (ConnectionException e) {
                System.out.println(e.getMessage());
                printerStatusMessage = e.getMessage();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                printerStatusMessage = e.getMessage();
            } 
            if (successPrint) {
                result.success("Berhasil Di Print");
            } else {
                result.error("Gagal Print", printerStatusMessage, "");
            }

        } else {
            result.notImplemented();
        }
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    private void sendCpclOverBluetooth(final String mac, final String dataPrint) {

        try {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Connection connection = new BluetoothConnectionInsecure(mac);
                        // Initialize
                        Looper.prepare();

                        // Open the connection - physical connection is established here.
                        connection.open();
                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        PrinterStatus printerStatus = printer.getCurrentStatus();
                        if (printerStatus.isReadyToPrint) {
                            System.out.println("Ready To Print");
                            connection.write(dataPrint.getBytes());
                        } else if (printerStatus.isPaused) {
                            System.out.println("Cannot Print because the printer is paused.");
                        } else if (printerStatus.isHeadOpen) {
                            System.out.println("Cannot Print because the printer head is open.");
                        } else if (printerStatus.isPaperOut) {
                            System.out.println("Cannot Print because the paper is out.");
                        } else {
                            System.out.println("Cannot Print.");
                        }                        // connection.write(cpclData.getBytes());

                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);

                        // Close the connection to release resources.
                        connection.close();

//                     result.success("wrote " + data.getBytes().length + "bytes");

                        Looper.myLooper().quit();
                    } catch (ConnectionException e) {
                        System.out.println(e.getMessage());
                    } catch (ZebraPrinterLanguageUnknownException | InterruptedException e) {
                        // Handle communications error here.
                        System.out.println(e.getMessage());
                    }
                }
            }).start();
        } catch (Exception e) {
            Log.e("TAG", "sendCpclOverBluetooth: " + e.getMessage());
        }
    }
}
