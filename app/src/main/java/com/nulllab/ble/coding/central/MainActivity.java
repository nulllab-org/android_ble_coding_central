package com.nulllab.ble.coding.central;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final List<String> mBleAddressList = new LinkedList<>();
    private final Set<String> mBleAddressSet = new HashSet<>();
    private final LinkedHashMap<String, BluetoothDevice> mDiscoveredBleDevices = new LinkedHashMap<>();
    ArrayAdapter<String> mDialogAdapter = null;
    ArrayAdapter<String> mBleAddressSpinnerAdapter = null;
    Spinner mBleAddressSpinner = null;
    SwitchCompat mConnectSwitch = null;
    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            Log.d(TAG, "onScanResult: " + result);
            if (result != null) {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    final String device_name = device.getName();
                    if (device_name != null && device_name.startsWith(("codiplay"))) {
                        if (!mDiscoveredBleDevices.containsKey(device.getAddress())) {
                            mDiscoveredBleDevices.put(device.getAddress(), device);
                            mBleAddressList.add("Device Name: " + device_name + "\nMac Address: " + device.getAddress());
                            mDialogAdapter.notifyDataSetChanged();
                            mBleAddressSpinnerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

//            if (!mBleAddressSet.contains(address)) {
//                    mBleAddressSet.add(address);
//                    mBleAddressList.add(address);
//                    mDialogAdapter.notifyDataSetChanged();
//                    mBleAddressSpinnerAdapter.notifyDataSetChanged();
//                }
//            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults: " + results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: " + errorCode);
        }
    };
    private LogView mLogView;
    private TextView mStateTextView;
    private final BleCodingPeripheral mBleCodingPeripheral = new BleCodingPeripheral(this, new BleCodingPeripheral.Listener() {
        @Override
        public void onStateChange(BleCodingPeripheral.State previous_state, BleCodingPeripheral.State state) {
            Log.d(TAG, "onStateChange: " + state);
            mStateTextView.setText(state.toString().toLowerCase().replace('_', ' '));

            switch (state) {
                case CONNECTED:
                    if (previous_state == BleCodingPeripheral.State.DISCONNECTED) {
                        Toast.makeText(MainActivity.this, "device is connected", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case DISCONNECTED:
                    Toast.makeText(MainActivity.this, "device is disconnected", Toast.LENGTH_SHORT).show();
                    mConnectSwitch.setChecked(false);
                    break;
            }
        }

        @Override
        public void onFileTransmitted() {
            Log.d(TAG, "onCodeTransmitted: ");
            Toast.makeText(MainActivity.this, "file transmitted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReceivedFromStdout(byte[] data) {
            mLogView.append(data);
        }

        @Override
        public void onTransmittedToStdin() {
            Log.d(TAG, "onTransmittedToStdin: ");
            Toast.makeText(MainActivity.this, "data transmitted to stdin", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    @SuppressLint("MissingPermission")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, mBleAddressList);
        mBleAddressSpinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mBleAddressList);
//        mBleAddressSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBleAddressSpinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mConnectSwitch = findViewById(R.id.connect_switch);
        EditText codeEditText = findViewById(R.id.code_edit_text);
        mLogView = findViewById(R.id.log_text_view);
        mStateTextView = findViewById(R.id.state_text_view);
        mBleAddressSpinner = findViewById(R.id.ble_address_spinner);
        mConnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged: " + isChecked);
            if (!enableBluetooth()) {
                mConnectSwitch.setChecked(false);
                return;
            }
            if (isChecked) {
                if (mBleAddressSpinner.getSelectedItem() == null) {
                    mConnectSwitch.setChecked(false);
                    Toast.makeText(MainActivity.this, "please select a ble address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mDiscoveredBleDevices.isEmpty()) {
                    return;
                }
//                mDiscoveredBleDevices.keySet().toArray()[mBleAddressSpinner.getSelectedItemPosition()].toString();
//                mDiscoveredBleDevices.entrySet().toArray()[mBleAddressSpinner.getSelectedItemPosition()].toString();
//                final String address = mBleAddressSpinner.getSelectedItem().toString();
                final String address = mDiscoveredBleDevices.keySet().toArray()[mBleAddressSpinner.getSelectedItemPosition()].toString();
                BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.connect(address);
                if (BleCodingPeripheral.ResultCode.OK != ret) {
                    Log.e(TAG, "failed to connect " + address + ": " + ret);
                    Toast.makeText(MainActivity.this, "failed to connect " + address + ": " + ret, Toast.LENGTH_SHORT).show();
                    mConnectSwitch.setChecked(false);
                }
            } else {
                mBleCodingPeripheral.disconnect();
            }
        });

        findViewById(R.id.send_file_button).setOnClickListener(view -> {
            final String code = codeEditText.getText().toString();
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.sendFile(code.getBytes(StandardCharsets.UTF_8));
            if (BleCodingPeripheral.ResultCode.OK != ret) {
                Log.e(TAG, "failed to send file, " + ret);
                Toast.makeText(this, "failed to send file: " + ret, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.send_to_stdin).setOnClickListener(view -> {
            final String data = ((EditText) findViewById(R.id.stdin_data_text_view)).getText().toString();
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.sendToStdin(data.getBytes(StandardCharsets.UTF_8));
            if (ret != BleCodingPeripheral.ResultCode.OK) {
                Toast.makeText(this, "failed to send data to stdin: " + ret, Toast.LENGTH_SHORT).show();
            }
        });

        mBleAddressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: " + position);
                mBleAddressSpinner.setSelection(position);
                mConnectSwitch.setChecked(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected: ");
                mConnectSwitch.setChecked(false);
            }
        });
        mBleAddressSpinner.setAdapter(mBleAddressSpinnerAdapter);

        findViewById(R.id.scan_button).setOnClickListener(v -> {
            if (!checkAndRequestScanPermissions()) {
                return;
            }
            if (!enableBluetooth()) {
                return;
            }
            mBleCodingPeripheral.disconnect();
            mBleAddressSpinnerAdapter.clear();
            mBleAddressSpinnerAdapter.notifyDataSetChanged();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setSingleChoiceItems(mDialogAdapter, 0, (dialog, which) -> ((Spinner) findViewById(R.id.ble_address_spinner)).setSelection(which))
                    .setOnCancelListener(dialog -> BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(mScanCallback))
                    .setTitle("scanning...")
                    .setPositiveButton("OK", (dialog, which) -> BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(mScanCallback));
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            startScan();
        });

        Spinner demoCodeSpinner = findViewById(R.id.demo_code);
        List<String> demoList = new ArrayList<>(DemoCode.Codes.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, demoList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        demoCodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                codeEditText.setText(DemoCode.Codes.values().toArray(new String[0])[position]);
                InputStream inputStream = null;
                try {
                    inputStream = getAssets().open(DemoCode.Codes.values().toArray(new String[0])[position]);
                    byte[] text = new byte[inputStream.available()];
                    inputStream.read(text);
                    codeEditText.setText(new String(text));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        demoCodeSpinner.setAdapter(adapter);
        demoCodeSpinner.setSelection(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        mConnectSwitch.setChecked(false);
        View rootView = getWindow().getDecorView().getRootView();
        rootView.clearFocus();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    private boolean checkAndRequestScanPermissions() {
        String[] permissions = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        List<String> request_permissions = new ArrayList<>();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "requesting " + permission, Toast.LENGTH_SHORT).show();
                request_permissions.add(permission);
            }
        }

        if (request_permissions.isEmpty()) {
            return true;
        }

        requestPermissions(request_permissions.toArray(new String[0]), 0);

        return false;
    }

    private boolean enableBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "checkBluetooth: device doesn't support Bluetooth");
            Toast.makeText(this, "device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
            Toast.makeText(this, "requesting android.permission.BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "checkBluetooth: bluetooth is disabled");
            Toast.makeText(this, "enable Bluetooth and try again.", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return false;
        }

        return true;
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "connect: device doesn't support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "connect: bluetooth is disabled");
            return;
        }
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanSettings scanSettings = new ScanSettings.Builder().setMatchMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

//        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00000001-8c26-476f-89a7-a108033a69c7")).build();
//        mBleAddressSet.clear();
        mDiscoveredBleDevices.clear();
        mBleAddressList.clear();
        mDialogAdapter.notifyDataSetChanged();
        bluetoothLeScanner.startScan(null, scanSettings, mScanCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            Toast.makeText(this, permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"), Toast.LENGTH_SHORT).show();
        }
    }
}