package com.ganesh.collections.queue;

public class FlipMarkerRingBufferQueue {
    public Object[] elements = null;

    public int capacity = 0;
    public int writePos = 0;
    public int readPos = 0;
    public boolean flipped = false; // the flip marker

    public FlipMarkerRingBufferQueue(int capacity) {
        this.capacity = capacity;
        elements = new Object[capacity];
    }

    public void reset() {
        writePos = 0;
        readPos = 0;
        flipped = false;
    }

    public int available() {
        if (!flipped) {
            return writePos - readPos;
        }
        return capacity - readPos + writePos;
    }

    public int remainingCapacity() {
        if (!flipped) {
            return capacity - writePos;
        }
        return readPos - writePos;
    }

    public boolean put(Object element) {
        if (!flipped) {
            if (writePos == capacity) {
                writePos = 0;
                flipped = true;

                if (writePos < readPos) {
                    elements[writePos++] = element;
                    return true;
                } else {
                    return false;
                }
            } else {
                elements[writePos++] = element;
                return true;
            }
        } else {
            if (writePos < readPos) {
                elements[writePos++] = element;
                return true;
            } else {
                return false;
            }
        }
    }


    public Object take() {
        if (!flipped) {
            if (readPos < writePos) {
                return elements[readPos++];
            } else {
                return null;
            }
        } else {
            if (readPos == capacity) {
                readPos = 0;
                flipped = false;

                if (readPos < writePos) {
                    return elements[readPos++];
                } else {
                    return null;
                }
            } else {
                return elements[readPos++];
            }
        }
    }
}
