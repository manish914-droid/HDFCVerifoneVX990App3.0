package com.example.verifonevx990app;

import org.junit.Test;

public class TestJavaCode {
    @Test
    public void myJavaMain() {
        W w1 = new W(1, 2);
        W w = new W(1, 2);
        String s1 = "io";
        String s2 = "io";
        System.out.println(s1 == s2);
        System.out.println(s1.equals(s2));
        System.out.println(w1 == w);
        System.out.println(w1.equals(w));
    }
}

class W {
    W(int a, int b) {

    }

}