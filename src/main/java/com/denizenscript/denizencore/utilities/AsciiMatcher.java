package com.denizenscript.denizencore.utilities;

public class AsciiMatcher {

    public static boolean[] accepted = new boolean[256];

    public AsciiMatcher(String allowThese) {
        for (int i = 0; i < allowThese.length(); i++) {
            accepted[allowThese.charAt(i)] = true;
        }
    }

    public boolean isOnlyMatches(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 255 || !accepted[c]) {
                return false;
            }
        }
        return true;
    }
}
