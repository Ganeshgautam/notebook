package com.ganesh.array.problems;

public class SubArrayFinder {

    public boolean find(int[] data, int[] subArr) {

        if (!(data == null || subArr == null || data.length == 0 || subArr.length == 0)) {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < subArr.length; j++) {
                    if (data[i + j] != subArr[j]) {
                        break;
                    } else if (j == subArr.length - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
