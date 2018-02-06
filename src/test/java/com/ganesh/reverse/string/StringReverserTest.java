package com.ganesh.reverse.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ganesh.reverse.Reverser;

@RunWith(Parameterized.class)
public class StringReverserTest {

    private Reverser<String> reverser;

    @Parameters
    public static Object[] allStringReversers() {
        return new Object[] { new LoopBasedStringReverser(), new RecursionBasedStringReverser() };
    }

    public StringReverserTest(Reverser<String> reverser) {
        this.reverser = reverser;
    }

    @Test
    public void null_test() {
        String input = null;
        String output = reverser.reverse(input);
        assertNull(output);
    }

    @Test
    public void empty_test() {
        String input = "";
        String reverse = "";
        String output = reverser.reverse(input);
        assertEquals(reverse, output);
    }

    @Test
    public void basic_test() {
        String input = "Ganesh";
        String reverse = "hsenaG";
        String output = reverser.reverse(input);
        assertEquals(reverse, output);
    }

}