package com.google.refine.geojson.util;

import java.util.concurrent.ThreadLocalRandom;

public final class Util {
    public static int sum(int a, int b) {
        return a + b;
    }

    public static int diff(int a, int b) {
        return a - b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    public static double divide(int a, int b) {
        return (double) a / b;
    }

    public static int randomNumber(int a, int b) {
        return ThreadLocalRandom.current().nextInt(((Number) a).intValue(), ((Number) b).intValue() + 1);
    }
}
