package com.nulllab.ble.code.central.util;

public class RingBuffer {
    private final byte[] mBuffer;
    private int write_ptr_ = 0;
    private int read_ptr_ = 0;

    public RingBuffer(int length) {
        mBuffer = new byte[length];
    }

    public int write(byte[] data) {
        int offset = 0;

        while (true) {
            final int copy_length =
                    Integer.min(Integer.min(free(), mBuffer.length - write_ptr_), data.length - offset);
            if (copy_length > 0) {
                System.arraycopy(data, offset, mBuffer, write_ptr_, copy_length);
                offset += copy_length;
                write_ptr_ = (write_ptr_ + copy_length) % mBuffer.length;
            } else {
                return offset;
            }
        }
    }

    public byte[] read(int length) {
        length = Integer.min(Integer.min(avail(), mBuffer.length - read_ptr_), length);
        byte[] out = new byte[length];
        System.arraycopy(mBuffer, read_ptr_, out, 0, length);
        read_ptr_ = (read_ptr_ + length) % mBuffer.length;
        return out;
    }

    public int free() {
        return (mBuffer.length + read_ptr_ - write_ptr_ - 1) % mBuffer.length;
    }

    public int avail() {
        return (mBuffer.length + write_ptr_ - read_ptr_) % mBuffer.length;
    }
}
