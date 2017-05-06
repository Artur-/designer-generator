package org.vaadin.artur.designer.generator;

import java.util.Locale;

public class Util {
    private Util() {

    }

    public static String dashSeparatedToCamelCase(String dashSeparated) {
        if (dashSeparated == null) {
            return null;
        }
        String[] parts = dashSeparated.split("-");
        for (int i = 1; i < parts.length; i++) {
            parts[i] = capitalize(parts[i]);
        }

        return join(parts, "");
    }

    public static String capitalize(String string) {
        if (string == null) {
            return null;
        }

        if (string.length() <= 1) {
            return string.toUpperCase();
        }

        return string.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + string.substring(1);
    }

    public static String join(String[] parts, String separator) {
        if (parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            sb.append(separator);
        }
        return sb.substring(0, sb.length() - separator.length());
    }
}
