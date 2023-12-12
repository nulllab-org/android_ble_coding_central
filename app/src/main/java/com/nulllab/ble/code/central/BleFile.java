package com.nulllab.ble.code.central;

import java.io.ByteArrayOutputStream;

public class BleFile {
    private static final String TAG = "BleFile";
    private static final byte PACKET_TYPE_SEND_FILE = 0x01;
    private static final byte FLAG_NONE = 0;
    private static final byte FLAG_FILE_BEGIN = 1;
    private static final byte FLAG_FILE_END = 1 << 1;
    private byte[] mData = null;
    private int mPosition = 0;

    public void setData(byte[] data) {
        mData = data;
        mPosition = 0;
    }

    public byte[] readBlePacket(int packetMaxLength) {
        if (mData.length == mPosition) {
            return null;
        }

        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(PACKET_TYPE_SEND_FILE); // packet type: 1 byte
        byte flag = FLAG_NONE;
        if (mPosition == 0) {
            flag |= FLAG_FILE_BEGIN;
        }

        int length = Integer.min(mData.length - mPosition, packetMaxLength - 2);

        if (mPosition + length == mData.length) {
            flag |= FLAG_FILE_END;
        }

        packet.write(flag); // flag: 1 byte
        packet.write(mData, mPosition, length); // data: n bytes
        mPosition += length;
        return packet.toByteArray();
    }
}
