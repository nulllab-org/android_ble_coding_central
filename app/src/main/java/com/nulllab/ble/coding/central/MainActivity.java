package com.nulllab.ble.coding.central;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final LinkedHashSet<String> mDeviceNames = new LinkedHashSet<>();
    ArrayAdapter<String> mDeviceNameAdapter = null;
    AutoCompleteTextView mDeviceNamesAutoCompleteTextView = null;
    SwitchCompat mConnectSwitch = null;
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
                    mDeviceNamesAutoCompleteTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
                    mDeviceNamesAutoCompleteTextView.setTextColor(Color.GREEN);
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

    void readDeviceNamesFromCache() {
        File file = new File(getCacheDir(), "device_names.json");
        if (file.length() == 0) {
            return;
        }
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            JSONObject jsonObject = new JSONObject(new String(bytes));
            JSONArray json_device_names = jsonObject.getJSONArray("device_names");
            for (int i = 0; i < json_device_names.length(); i++) {
                mDeviceNames.add(json_device_names.getString(i));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (file.exists()) {
                file.delete();
            }
        }
    }

    void addDeviceNameToCache(String deviceName) {
        Log.d(TAG, "addDeviceNameToCache: " + deviceName);
        File file = new File(getCacheDir(), "device_names.json");
        mDeviceNames.remove(deviceName);
        mDeviceNames.add(deviceName);
        try {
            JSONArray jsonArray = new JSONArray(mDeviceNames.toArray());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("device_names", jsonArray);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(jsonObject.toString(2));
            bufferedWriter.close();
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
        UpdateDeviceNameToTextView();
    }

    void UpdateDeviceNameToTextView() {
        List<String> deviceNameList = Arrays.asList(mDeviceNames.toArray(new String[0]));
        Collections.reverse(deviceNameList);

        mDeviceNameAdapter.clear();
        mDeviceNameAdapter.addAll(deviceNameList);
        mDeviceNameAdapter.notifyDataSetChanged();
        if (deviceNameList.size() > 0) {
            mDeviceNamesAutoCompleteTextView.setText(deviceNameList.get(0));
            mDeviceNamesAutoCompleteTextView.setTextColor(Color.GREEN);
            mDeviceNamesAutoCompleteTextView.dismissDropDown();
        }
    }

    @Override
    @SuppressLint({"MissingPermission", "ClickableViewAccessibility"})
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readDeviceNamesFromCache();
        mDeviceNamesAutoCompleteTextView = findViewById(R.id.device_name);
        mDeviceNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        mDeviceNamesAutoCompleteTextView.setAdapter(mDeviceNameAdapter);
        UpdateDeviceNameToTextView();
        mDeviceNamesAutoCompleteTextView.setOnTouchListener((v, event) -> {
            if (mDeviceNamesAutoCompleteTextView.getInputType() == InputType.TYPE_NULL) {
                Toast.makeText(MainActivity.this, "please disconnected before modify device name", Toast.LENGTH_SHORT).show();
            }
            return false;
        });
        mDeviceNamesAutoCompleteTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
        mDeviceNamesAutoCompleteTextView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        mDeviceNamesAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mDeviceNamesAutoCompleteTextView.length() == 6) {
                    mDeviceNamesAutoCompleteTextView.dismissDropDown();
                    mDeviceNamesAutoCompleteTextView.setTextColor(Color.GREEN);
                } else {
                    mDeviceNamesAutoCompleteTextView.setTextColor(Color.RED);
                }
            }
        });

        mConnectSwitch = findViewById(R.id.connect_switch);
        EditText codeEditText = findViewById(R.id.code_edit_text);
        mLogView = findViewById(R.id.log_text_view);
        mStateTextView = findViewById(R.id.state_text_view);
        mConnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged: " + isChecked);
            if (!checkAndRequestScanPermissions()) {
                enableBluetooth();
                mConnectSwitch.setChecked(false);
                return;
            }
            if (!enableBluetooth()) {
                mConnectSwitch.setChecked(false);
                return;
            }
            if (isChecked) {
                if (mDeviceNamesAutoCompleteTextView.getText() == null) {
                    return;
                }

                String deviceIdStr = mDeviceNamesAutoCompleteTextView.getText().toString();
                if (deviceIdStr.length() != 6) {
                    Toast.makeText(MainActivity.this, "Please enter the 6-digit device ID correctly.", Toast.LENGTH_SHORT).show();
                    mConnectSwitch.setChecked(false);
                    return;
                }
                long deviceId = Long.parseLong(deviceIdStr);
                deviceId += 0x100000000000L;
                Log.d(TAG, "connect: " + Long.toString(deviceId, 16));
                ByteBuffer macAddressBytes = ByteBuffer.allocate(Long.SIZE >> 3).putLong(deviceId);
                String address = String.format("%02x:%02x:%02x:%02x:%02x:%02x",
                        macAddressBytes.get(2), macAddressBytes.get(3), macAddressBytes.get(4), macAddressBytes.get(5), macAddressBytes.get(6), macAddressBytes.get(7));
                Log.d(TAG, "address: " + address);

                BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.connect(address);
                addDeviceNameToCache(deviceIdStr);
                if (BleCodingPeripheral.ResultCode.OK != ret) {
                    Log.e(TAG, "failed to connect " + address + ": " + ret);
                    Toast.makeText(MainActivity.this, "failed to connect " + address + ": " + ret, Toast.LENGTH_SHORT).show();
                    mConnectSwitch.setChecked(false);
                    return;
                } else {
                    mDeviceNamesAutoCompleteTextView.setInputType(InputType.TYPE_NULL);
                    mDeviceNamesAutoCompleteTextView.setTextColor(Color.GRAY);
                }
            } else {
                mBleCodingPeripheral.disconnect();
                mDeviceNamesAutoCompleteTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
                mDeviceNamesAutoCompleteTextView.setTextColor(Color.GREEN);
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
            permissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT};
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            Toast.makeText(this, permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"), Toast.LENGTH_SHORT).show();
        }
    }
}