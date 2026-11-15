package br.asthen.essentials.core;

/** Money is stored as whole cents (long) so there is never any floating-point drift. */
public final class Money {
    public static final long MAX = 999_999_999_99L; // 999.999.999,99

    private Money() {
    }

    /** Parses "10", "10,5", "10.50". Returns -1 when invalid. Zero is allowed (callers decide). */
    public static long parse(String s) {
        if (s == null) return -1;
        s = s.trim().replace(',', '.');
        if (!s.matches("\\d{1,9}(\\.\\d{1,2})?")) return -1;
        int dot = s.indexOf('.');
        long whole = Long.parseLong(dot < 0 ? s : s.substring(0, dot));
        long frac = 0;
        if (dot >= 0) {
            String f = s.substring(dot + 1);
            frac = Long.parseLong(f.length() == 1 ? f + "0" : f);
        }
        long v = whole * 100 + frac;
        return v > MAX ? -1 : v;
    }

    public static String format(long cents, String symbol) {
        long abs = Math.abs(cents);
        String whole = String.valueOf(abs / 100);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) sb.append('.');
            sb.append(whole.charAt(i));
        }
        return (cents < 0 ? "-" : "") + symbol + sb + "," + String.format("%02d", abs % 100);
    }
}
