package com.nulllab.ble;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

public class BlePacketUtils {
    private static final String TAG = "BlePacketUtils";
    private static final byte PACKET_TYPE_SEND_FILE = 0x01;
    private static final byte FLAG_NONE = 0;
    private static final byte FLAG_FILE_BEGIN = 1;
    private static final byte FLAG_FILE_END = 1 << 1;

    private BlePacketUtils() {
    }

    public static List<byte[]> rawDataToBlePackets(byte[] bytes, int packetMaxLength) {
        List<byte[]> packets = new LinkedList<>();
        int start_position = 0;
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        while (start_position < bytes.length) {
            packet.reset();
            packet.write(PACKET_TYPE_SEND_FILE); // packet type: 1 byte
            int length = Integer.min(bytes.length - start_position, packetMaxLength - 2);
            byte flag = FLAG_NONE;
            if (start_position == 0) {
                flag |= FLAG_FILE_BEGIN;
            }

            if (start_position + length == bytes.length) {
                flag |= FLAG_FILE_END;
            }

            packet.write(flag); // flag: 1 byte
            packet.write(bytes, start_position, length); // data: n bytes
            start_position += length;
            packets.add(packet.toByteArray());
        }

        return packets;
    }
}
