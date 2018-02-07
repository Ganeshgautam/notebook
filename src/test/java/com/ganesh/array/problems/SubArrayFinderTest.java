package com.ganesh.array.problems;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubArrayFinderTest {

    @Test
    public void successCase_SingleElement_Exists() throws Exception {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        int dataArr[] = new int[] { 1, 2, 3 };
        int subArr[] = new int[] { 2 };
        boolean found = subArrayFinder.find(dataArr, subArr);
        assertTrue(found);
    }

    @Test
    public void successCase_MultiElement_Exists() throws Exception {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        int dataArr[] = new int[] { 1, 2, 3 };
        int subArr[] = new int[] { 2, 3 };
        boolean found = subArrayFinder.find(dataArr, subArr);
        assertTrue(found);
    }

    @Test
    public void successCase_SingleElementAsFirstOne_Exists() throws Exception {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        int dataArr[] = new int[] { 1, 2, 3 };
        int subArr[] = new int[] { 1 };
        boolean found = subArrayFinder.find(dataArr, subArr);
        assertTrue(found);
    }

    @Test
    public void failureCase_SingleElement_NotExists() throws Exception {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        int dataArr[] = new int[] { 1, 2, 3, 4, 5 };
        int subArr[] = new int[] { 7 };
        boolean found = subArrayFinder.find(dataArr, subArr);
        assertFalse(found);
    }

    @Test
    public void null_data_array_should_return_false() {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        boolean found = subArrayFinder.find(null, new int[] {});
        assertFalse(found);
    }

    @Test
    public void null_sub_array_should_return_false() {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        boolean found = subArrayFinder.find(new int[] {}, null);
        assertFalse(found);
    }

    @Test
    public void empty_data_array_should_return_false() {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        boolean found = subArrayFinder.find(new int[] {}, new int[] { 1 });
        assertFalse(found);
    }

    @Test
    public void empty_sub_array_should_return_false() {
        SubArrayFinder subArrayFinder = new SubArrayFinder();
        boolean found = subArrayFinder.find(new int[] { 1 }, new int[] {});
        assertFalse(found);
    }
}
