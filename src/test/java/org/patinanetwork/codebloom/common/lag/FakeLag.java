package org.patinanetwork.codebloom.common.lag;

public class FakeLag {

    public static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
