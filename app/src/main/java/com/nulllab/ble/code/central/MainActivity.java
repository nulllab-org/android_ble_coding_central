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
import com.nulllab.ble.code.central.util.MainThreadUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private LogView mLogView;
    private TextView mStateTextView;
    private final BlePeripheral mBlePeripheral = new BlePeripheral(this, new BlePeripheral.Listener() {
        @Override
        public void onStateChange(int state) {
            Log.i(TAG, "OnStateChange: " + state);
            switch (state) {
                case BlePeripheral.STATE_CONNECTING: {
                    mLogView.print("connecting");
                    MainThreadUtils.run(() -> mStateTextView.setText("connecting"));
                    break;
                }
                case BlePeripheral.STATE_RECONNECTING: {
                    mLogView.print("reconnecting");
                    MainThreadUtils.run(() -> mStateTextView.setText("reconnecting"));
                    break;
                }
                case BlePeripheral.STATE_CONNECTED: {
                    mLogView.print("connected");
                    MainThreadUtils.run(() -> mStateTextView.setText("connected"));
                    break;
                }
                case BlePeripheral.STATE_DISCONNECTING: {
                    mLogView.print("disconnecting");
                    MainThreadUtils.run(() -> mStateTextView.setText("disconnecting"));
                    break;
                }
                case BlePeripheral.STATE_DISCONNECTED: {
                    mLogView.print("disconnected");
                    MainThreadUtils.run(() -> mStateTextView.setText("disconnected"));
                    break;
                }
            }
        }

        @Override
        public void onDataSendCompleted() {
            Log.i(TAG, "onDataSendCompleted: ");
            mLogView.print("code send completed");
            MainThreadUtils.run(() ->
                    Toast.makeText(MainActivity.this, "code send completed", Toast.LENGTH_SHORT).show()
            );
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

        String default_code = "import machine\n" +
                "import time\n" +
                "pin = machine.Pin(12, machine.Pin.OUT)\n" +
                "sleep_time = 1\n" +
                "while True:\n" +
                "    pin.value(0)\n" +
                "    time.sleep(sleep_time)\n" +
                "    pin.value(1)\n" +
                "    time.sleep(sleep_time)\n";

        codeEditText.setText(default_code);
        mLogView.print("app create");

        findViewById(R.id.connect_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            final String address = ((EditText) findViewById(R.id.bluetooth_address)).getText().toString();
            mBlePeripheral.connect(address);
        });

        findViewById(R.id.disconnect_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            mBlePeripheral.disconnect();
        });

        findViewById(R.id.send_code_button).setOnClickListener(view -> {
            if (bluetoothIsDisabled()) {
                return;
            }
            final String address = ((EditText) findViewById(R.id.bluetooth_address)).getText().toString();
            final String code = codeEditText.getText().toString();
            mBlePeripheral.connect(address.trim().toUpperCase());
            mBlePeripheral.sendData(code.getBytes());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
        mLogView.print("app start");
//        requestPermissions();
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