package br.asthen.essentials.core;

import java.util.regex.Pattern;

public final class Names {
    /** Letters (accents included), digits, _ and -, up to 24 characters. */
    private static final Pattern PLACE = Pattern.compile("^[\\p{L}\\p{N}_-]{1,24}$");

    private Names() {
    }

    public static boolean validPlace(String s) {
        return s != null && PLACE.matcher(s).matches();
    }
}
