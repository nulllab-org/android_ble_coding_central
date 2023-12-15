package com.nulllab.ble.coding.central;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.nulllab.ble.coding.central.util.MainThreadUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class BleCodingPeripheral {
    private static final String TAG = "BleCodingPeripheral";
    private static final int MTU_MAX = 517;
    private static final int MTU_MIN = 23;
    private static final UUID SERVICE_UUID = UUID.fromString("00000001-8c26-476f-89a7-a108033a69c7");
    private static final UUID FILE_UUID = UUID.fromString("00000002-8c26-476f-89a7-a108033a69c7");
    private static final UUID STDIO_UUID = UUID.fromString("00000003-8c26-476f-89a7-a108033a69c7");
    private final Context mContext;
    private final BleFile mBleFile = new BleFile();
    private final Listener mListener;
    private ByteArrayInputStream mStdinStream = null;
    private BluetoothGatt mBluetoothGatt;
    private int mMtu = MTU_MIN;
    private State mState = State.DISCONNECTED;
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange, newState: " + newState);
            synchronized (BleCodingPeripheral.this) {
                if (mState == State.CONNECTING && newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "onConnectionStateChange: connected and request mtu");
                    gatt.requestMtu(MTU_MAX);
                    changeState(State.CONFIGURING_MTU);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED && mState != State.DISCONNECTED) {
                    Log.d(TAG, "onConnectionStateChange: disconnected");
                    reset();
                    changeState(State.DISCONNECTED);
                }
            }
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered: ");
            synchronized (BleCodingPeripheral.this) {
                if (mState == State.DISCOVERING_SERVICES) {
                    final BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(SERVICE_UUID).getCharacteristic(STDIO_UUID);
                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                    }
                    changeState(State.WRITING_DESCRIPTOR);
                }
            }
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + status);
            synchronized (BleCodingPeripheral.this) {
                if (State.SENDING_FILE == mState) {
                    byte[] packet = mBleFile.readBlePacket(mMtu - 3);
                    if (packet != null && packet.length > 0) {
                        sendPacket(FILE_UUID, packet);
                    } else {
                        changeState(State.CONNECTED);
                        MainThreadUtils.run(() -> {
                            if (mListener != null) {
                                mListener.onFileTransmitted();
                            }
                        });
                    }
                } else if (State.SENDING_TO_STDIN == mState) {
                    if (mStdinStream.available() == 0) {
                        changeState(State.CONNECTED);
                        MainThreadUtils.run(() -> {
                            if (mListener != null) {
                                mListener.onTransmittedToStdin();
                            }
                        });
                        return;
                    }

                    byte[] packet = new byte[Integer.min(mStdinStream.available(), mMtu - 3)];
                    try {
                        mStdinStream.read(packet);
                    } catch (IOException e) {
                        Log.e(TAG, "onCharacteristicWrite: ", e);
                        throw new RuntimeException(e);
                    }
                    sendPacket(STDIO_UUID, packet);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return;
            }
            if (mListener != null) {
                mListener.onReceivedFromStdout(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if (mListener != null) {
                mListener.onReceivedFromStdout(value);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: ");
            synchronized (BleCodingPeripheral.this) {
                if (mState == State.WRITING_DESCRIPTOR) {
                    changeState(State.CONNECTED);
                }
            }
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged: " + mtu);
            synchronized (BleCodingPeripheral.this) {
                if (mState == State.CONFIGURING_MTU) {
                    mMtu = mtu;
                    mBluetoothGatt.discoverServices();
                    changeState(State.DISCOVERING_SERVICES);
                }
            }
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
            Log.d(TAG, "onServiceChanged: ");
        }
    };

    public BleCodingPeripheral(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized ResultCode connect(final String bleAddress) {
        if (mState != State.DISCONNECTED) {
            Log.w(TAG, "connect: current state is not disconnected");
            return ResultCode.INVALID_STATE;
        }

        reset();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "connect: device doesn't support Bluetooth");
            return ResultCode.BLUETOOTH_UNSUPPORTED;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "connect: bluetooth is disabled");
            return ResultCode.BLUETOOTH_DISABLED;
        }

        changeState(State.CONNECTING);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bleAddress.toUpperCase());
        mBluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        return ResultCode.OK;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized ResultCode disconnect() {
        if (mState == State.DISCONNECTED) {
            return ResultCode.OK;
        }

        reset();
        changeState(State.DISCONNECTED);
        return ResultCode.OK;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized ResultCode sendFile(byte[] bytes) {
        Log.i(TAG, "sendCode: ");
        if (mState != State.CONNECTED) {
            Log.e(TAG, "sendCode: invalid state: " + mState);
            return ResultCode.INVALID_STATE;
        }

        changeState(State.SENDING_FILE);
        mBleFile.setData(bytes);
        byte[] packet = mBleFile.readBlePacket(mMtu - 3);
        if (packet != null) {
            sendPacket(FILE_UUID, packet);
        }
        return ResultCode.OK;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized ResultCode sendToStdin(byte[] bytes) {
        Log.i(TAG, "sendToStdin: ");
        if (mState != State.CONNECTED) {
            Log.e(TAG, "sendToStdin: invalid state: " + mState);
            return ResultCode.INVALID_STATE;
        }

        mStdinStream = new ByteArrayInputStream(bytes);
        byte[] packet = new byte[Integer.min(mStdinStream.available(), mMtu - 3)];
        try {
            mStdinStream.read(packet);
        } catch (IOException e) {
            return ResultCode.INVALID_ARGUMENTS;
        }

        if (packet.length > 0) {
            changeState(State.SENDING_TO_STDIN);
            sendPacket(STDIO_UUID, packet);
            return ResultCode.OK;
        } else {
            return ResultCode.INVALID_ARGUMENTS;
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private void sendPacket(UUID uuid, byte[] packet) {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(SERVICE_UUID).getCharacteristic(uuid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mBluetoothGatt.writeCharacteristic(characteristic, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(packet);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private synchronized void reset() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
        mStdinStream = null;
        mMtu = MTU_MIN;
        mBleFile.setData(null);
        mState = State.DISCONNECTED;
    }

    private synchronized void changeState(State state) {
        State previous_state = mState;
        mState = state;
        MainThreadUtils.run(() -> {
            if (mListener != null) {
                mListener.onStateChange(previous_state, state);
            }
        });
    }

    public enum State {
        DISCONNECTED, CONNECTING, CONFIGURING_MTU, DISCOVERING_SERVICES, WRITING_DESCRIPTOR, CONNECTED, SENDING_FILE, SENDING_TO_STDIN,
    }

    public enum ResultCode {
        OK, INVALID_STATE, INVALID_ARGUMENTS, BLUETOOTH_UNSUPPORTED, BLUETOOTH_DISABLED,
    }

    public interface Listener {
        void onStateChange(State previous_state, State new_state);

        void onFileTransmitted();

        void onReceivedFromStdout(byte[] data);

        void onTransmittedToStdin();
    }
}
