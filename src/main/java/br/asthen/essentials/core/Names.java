package br.asthen.essentials.core;

import java.util.regex.Pattern;

public final class Names {
    private static final Pattern PLACE = Pattern.compile("^[A-Za-z0-9_-]{1,24}$");

    private Names() {
    }

    public static boolean validPlace(String s) {
        return s != null && PLACE.matcher(s).matches();
    }
}
