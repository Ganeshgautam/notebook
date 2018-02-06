package com.ganesh.collections.queue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleRingBufferQueueTest {

    @Before
    public void setup() {
    }

    @Test
    public void put_and_take_normal_single_element() {
        SimpleRingBufferQueue queue = new SimpleRingBufferQueue(1);
        Object element = new Object();
        queue.put(element);
        Object takenELement = queue.take();
        Assert.assertEquals(element, takenELement);
    }

}
