package com.nulllab.ble.code.central;

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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class BlePeripheral {
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_RECONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTING = 4;
    public static final int STATE_DISCONNECTED = 5;
    public static final int OK = 0;
    public static final int BLUETOOTH_UNSUPPORTED = 1;
    public static final int BLUETOOTH_DISABLED = 2;
    private static final String TAG = "BlePeripheral";
    private static final int MTU_MAX = 517;
    private static final int MTU_MIN = 23;
    private static final UUID SERVICE_UUID = UUID.fromString("00000001-8c26-476f-89a7-a108033a69c7");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00000002-8c26-476f-89a7-a108033a69c7");
    private final Context mContext;
    private final Listener mListener;
    private final List<byte[]> mRawDatas = new LinkedList<>();
    private final List<byte[]> mBlePackets = new LinkedList<>();
    private boolean mIsSendingPacket = false;
    private int mMtu = MTU_MIN;
    private int mState = STATE_DISCONNECTED;
    private BluetoothGatt mBluetoothGatt;

    public BlePeripheral(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized int connect(String address) {
        Log.i(TAG, "connect: " + address);
        switch (mState) {
            case STATE_CONNECTING:
            case STATE_RECONNECTING:
            case STATE_CONNECTED: {
                if (mBluetoothGatt.getDevice().getAddress().equals(address)) {
                    return OK;
                }
            }
            case STATE_DISCONNECTING: {
                if (mBluetoothGatt.getDevice().getAddress().equals(address)) {
                    changeState(STATE_RECONNECTING);
                    return OK;
                }
            }
            default: {
                break;
            }
        }


        // close the previous connection with old address
        if (mBluetoothGatt != null) {
            Log.i(TAG, "close the previous connection with old address: " + mBluetoothGatt.getDevice().getAddress());
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }

        mRawDatas.clear();
        mBlePackets.clear();
        mIsSendingPacket = false;
        mBluetoothGatt = null;
        mMtu = MTU_MIN;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "connect: device doesn't support Bluetooth");
            return BLUETOOTH_UNSUPPORTED;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "connect: bluetooth is disabled");
            return BLUETOOTH_DISABLED;
        }

        // start connecting with new address
        changeState(STATE_CONNECTING);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address.toUpperCase());
        mBluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        Log.i(TAG, "start connecting with address: " + address);
        return OK;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized void disconnect() {
        mRawDatas.clear();
        mBlePackets.clear();
        mIsSendingPacket = false;
        mMtu = MTU_MIN;

        switch (mState) {
            case STATE_CONNECTING:
            case STATE_RECONNECTING: {
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                changeState(STATE_DISCONNECTED);
                return;
            }
            case STATE_CONNECTED: {
                changeState(STATE_DISCONNECTING);
                Log.i(TAG, "disconnect: start disconnecting");
                mBluetoothGatt.disconnect();
                return;
            }
            case STATE_DISCONNECTING:
            case STATE_DISCONNECTED:
            default: {
                break;
            }
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized void sendData(byte[] data) {
        mRawDatas.add(data);
        sendPacket();
    }

    private synchronized void changeState(int state) {
        mState = state;
        if (mListener != null) {
            mListener.onStateChange(state);
        }
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "onConnectionStateChange: gatt: " + gatt + ", status: " + status + ", newState: " + newState);
            synchronized (BlePeripheral.this) {
                if (gatt != mBluetoothGatt) {
                    Log.i(TAG, "onConnectionStateChange: not current connection");
                    return;
                }

                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED: {
                        Log.i(TAG, "onGattConnectionStateChange: BluetoothProfile.STATE_CONNECTED");
                        if (mState == STATE_CONNECTING || mState == STATE_RECONNECTING) {
//                            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                            mBluetoothGatt.requestMtu(MTU_MAX);
//                            mBluetoothGatt.discoverServices();
                        }
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTED: {
                        Log.i(TAG, "onGattConnectionStateChange: BluetoothProfile.STATE_DISCONNECTED");
                        String address = mBluetoothGatt.getDevice().getAddress();
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                        mIsSendingPacket = false;
                        if (mState == STATE_DISCONNECTING) { // user disconnect
                            changeState(STATE_DISCONNECTED);
                        } else if (mState == STATE_CONNECTED || mState == STATE_CONNECTING || mState == STATE_RECONNECTING) { // auto reconnect
                            changeState(STATE_RECONNECTING);
//                            try {
//                                TimeUnit.MILLISECONDS.sleep(150);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }

                            Log.i(TAG, "onConnectionStateChange: start reconnecting");
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (!bluetoothAdapter.isEnabled()) {
                                Log.e(TAG, "onConnectionStateChange: bluetooth is disabled");
                                changeState(STATE_DISCONNECTED);
                                return;
                            }
                            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address.toUpperCase());
                            mBluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
//                            mBluetoothGatt.connect();
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "onServicesDiscovered: ");
            synchronized (BlePeripheral.this) {
                if (gatt != mBluetoothGatt) {
                    Log.w(TAG, "onServicesDiscovered: not current connection");
                    return;
                }

                if (mState != STATE_CONNECTING && mState != STATE_RECONNECTING) {
                    Log.w(TAG, "onServicesDiscovered: invalid state: " + mState);
                    return;
                }

                changeState(STATE_CONNECTED);
                sendPacket();
            }
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
            Log.d(TAG, "onServiceChanged: ");
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
            Log.d(TAG, "onDescriptorRead: ");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: ");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted: ");
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged: mtu:" + mtu);
            synchronized (BlePeripheral.this) {
                if (gatt != mBluetoothGatt) {
                    Log.i(TAG, "onMtuChanged: not current connection");
                    return;
                }

                if (mState == STATE_CONNECTING || mState == STATE_RECONNECTING) {
                    mMtu = mtu;
                    mBluetoothGatt.discoverServices();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            Log.d(TAG, "onCharacteristicChanged: ");
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.d(TAG, "onCharacteristicRead: ");
        }

        @Override
        @SuppressLint("InlinedApi")
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite: status: " + status);
            synchronized (BlePeripheral.this) {
                if (gatt != mBluetoothGatt) {
                    Log.i(TAG, "onCharacteristicWrite: not current connection");
                    return;
                }

                if (mState != STATE_CONNECTED) {
                    Log.i(TAG, "onCharacteristicWrite: invalid state: " + mState);
                    return;
                }

                if (status == 0) {
                    Log.i(TAG, "onCharacteristicWrite: characteristic writing success");
                    mBlePackets.remove(0);
                }

                mIsSendingPacket = false;

                if (mBlePackets.isEmpty() && mRawDatas.isEmpty()) {
                    if (mListener != null) {
                        mListener.onDataSendCompleted();
                    }
                } else {
                    sendPacket();
                }
            }
        }
    };

    @SuppressLint("InlinedApi")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private synchronized void sendPacket() {
        Log.i(TAG, "sendPacket:");
        if (mState != STATE_CONNECTED) {
            return;
        }

        if (!mRawDatas.isEmpty()) {
            for (byte[] data : mRawDatas) {
                mBlePackets.addAll(BlePacketUtils.rawDataToBlePackets(data, mMtu - 3));
            }
            mRawDatas.clear();
        }

        // waiting previous packet response
        if (mIsSendingPacket) {
            Log.i(TAG, "sendPacket: is sending packet");
            return;
        }

        if (mBlePackets.isEmpty()) {
            Log.i(TAG, "sendPacket: packet is empty");
            return;
        }

        mIsSendingPacket = true;

        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mBluetoothGatt.writeCharacteristic(characteristic, mBlePackets.get(0), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(mBlePackets.get(0));
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    public interface Listener {
        void onStateChange(int state);

        void onDataSendCompleted();
    }
}
