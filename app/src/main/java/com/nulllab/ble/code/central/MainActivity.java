package com.nulllab.ble.code.central;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.nulllab.ble.test.R;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private LogView mLogView;
    private TextView mStateTextView;
    private final BleCodingPeripheral mBleCodingPeripheral = new BleCodingPeripheral(this, new BleCodingPeripheral.Listener() {
        @Override
        public void onStateChange(BleCodingPeripheral.State state) {
            Log.d(TAG, "onStateChange: " + state);
            mStateTextView.setText(state.toString().toLowerCase().replace('_', ' '));
        }

        @Override
        public void onFileTransmitted() {
            Log.d(TAG, "onCodeTransmitted: ");
            Toast.makeText(MainActivity.this, "file transmitted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSerialDataReceived(byte[] data) {
            mLogView.append(data);
        }

        @Override
        public void onSerialDataTransmitted() {
            Log.d(TAG, "onSerialDataTransmitted: ");
            Toast.makeText(MainActivity.this, "serial data transmitted", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText codeEditText = findViewById(R.id.code_edit_text);
        mLogView = findViewById(R.id.log_text_view);
        mStateTextView = findViewById(R.id.state_text_view);

//        String default_code = "import machine\n" +
//                "import time\n" +
//                "pin = machine.Pin(12, machine.Pin.OUT)\n" +
//                "sleep_time = 1\n" +
//                "while True:\n" +
//                "    pin.value(0)\n" +
//                "    time.sleep(sleep_time)\n" +
//                "    pin.value(1)\n" +
//                "    time.sleep(sleep_time)\n";
        String default_code = "import time\n\n" +
                "count = 0\n" +
                "while True:\n" +
                "    print('hello world', count)\n" +
                "    count += 1\n" +
                "    time.sleep_ms(1000)\n";
//        String default_code = "import sys\n" +
//                "\n" +
//                "while True:\n" +
//                "  print(\"read :\", sys.stdin.readline())";
        codeEditText.setText(default_code);

        findViewById(R.id.connect_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            final String address = ((EditText) findViewById(R.id.bluetooth_address)).getText().toString();
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.connect(address);
            if (BleCodingPeripheral.ResultCode.OK != ret) {
                Log.e(TAG, "failed to connect " + address + ": " + ret);
                Toast.makeText(this, "failed to connect " + address + ": " + ret, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.disconnect_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.disconnect();
            if (BleCodingPeripheral.ResultCode.OK != ret) {
                Log.e(TAG, "failed to disconnect, " + ret);
                Toast.makeText(this, "failed to disconnect: " + ret, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.send_file_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            final String address = ((EditText) findViewById(R.id.bluetooth_address)).getText().toString();
            final String code = codeEditText.getText().toString();
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.sendFile(code.getBytes(StandardCharsets.UTF_8));
            if (BleCodingPeripheral.ResultCode.OK != ret) {
                Log.e(TAG, "failed to send file, " + ret);
                Toast.makeText(this, "failed to send file: " + ret, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.send_serial_data_button).setOnClickListener(view -> {
            final String serial_data = ((EditText) findViewById(R.id.serial_data_to_send_text_view)).getText().toString();
            BleCodingPeripheral.ResultCode ret = mBleCodingPeripheral.sendSerialData(serial_data.getBytes(StandardCharsets.UTF_8));
            if (ret != BleCodingPeripheral.ResultCode.OK) {
                Toast.makeText(this, "failed to serial data: " + ret, Toast.LENGTH_SHORT).show();
            }
        });
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

    private boolean bluetoothIsDisabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "checkBluetooth: device doesn't support Bluetooth");
            Toast.makeText(this, "device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
            Toast.makeText(this, "android.permission.BLUETOOTH_CONNECT is not granted", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "checkBluetooth: bluetooth is disabled");
            Toast.makeText(this, "bluetooth is disabled", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return true;
        }
        return false;
    }
}