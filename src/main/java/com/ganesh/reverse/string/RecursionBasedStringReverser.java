package com.ganesh.reverse.string;

import com.ganesh.reverse.Reverser;

public class RecursionBasedStringReverser implements Reverser<String> {

    @Override
    public String reverse(String t) {
        if (t == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        return reverse(builder, t);
    }

    private String reverse(StringBuilder builder, String input) {
        if (builder.length() == input.length()) {
            return builder.toString();
        }

        builder.append(input.charAt(input.length() - builder.length() - 1));

        return reverse(builder, input);
    }
}
