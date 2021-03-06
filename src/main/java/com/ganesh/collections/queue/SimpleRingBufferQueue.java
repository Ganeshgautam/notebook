package com.ganesh.collections.queue;

/**
 * 
 * @author ganeshgautam
 * @see <a href = "http://tutorials.jenkov.com/java-performance/ring-buffer.html">ring-buffer</a>
 *
 */
public class SimpleRingBufferQueue {
    public Object[] elements = null;

    private int capacity = 0;
    private int writePos = 0;
    private int available = 0;

    public SimpleRingBufferQueue(int capacity) {
        this.capacity = capacity;
        elements = new Object[capacity];
    }

    public void reset() {
        writePos = 0;
        available = 0;
    }

    public int capacity() {
        return capacity;
    }

    public int available() {
        return available;
    }

    public int remainingCapacity() {
        return capacity - available;
    }

    public boolean put(Object element) {

        if (available < capacity) {
            if (writePos >= capacity) {
                writePos = 0;
            }
            elements[writePos] = element;
            writePos++;
            available++;
            return true;
        }

        return false;
    }

    public Object take() {
        if (available == 0) {
            return null;
        }
        int nextSlot = writePos - available;
        if (nextSlot < 0) {
            nextSlot += capacity;
        }
        Object nextObj = elements[nextSlot];
        available--;
        return nextObj;
    }
}
