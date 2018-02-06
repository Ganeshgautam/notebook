package com.ganesh.reverse.string;

import com.ganesh.reverse.Reverser;

public class LoopBasedStringReverser implements Reverser<String> {


    @Override
    public String reverse(String t) {
        if (t == null || t.isEmpty()) {
            return t;
        }
        return reverseIt(t);
    }

    private String reverseIt(String t) {
        char[] charArray = t.toCharArray();
        char[] newCharArray = new char[t.length()];

        for (int i = 0; i < charArray.length; i++) {
            newCharArray[charArray.length - i - 1] = charArray[i];
        }
        return new String(newCharArray);
    }


}
