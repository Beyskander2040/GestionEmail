package com.example.mail.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class UTF7Decoder {

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]*={0,2}$");

    public static String decodeUTF7(String utf7Content) {
        if (utf7Content == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        boolean inBase64 = false;
        StringBuilder base64Buffer = new StringBuilder();

        for (char c : utf7Content.toCharArray()) {
            if (c == '&' && !inBase64) {
                inBase64 = true;
            } else if (c == '-' && inBase64) {
                if (base64Buffer.length() > 0) {
                    result.append(decodeBase64(base64Buffer.toString()));
                    base64Buffer.setLength(0);
                } else {
                    result.append('&');
                }
                inBase64 = false;
            } else if (inBase64) {
                if (c == ',') {
                    base64Buffer.append('/');
                } else {
                    base64Buffer.append(c);
                }
            } else {
                result.append(c);
            }
        }

        if (inBase64 && base64Buffer.length() > 0) {
            result.append(decodeBase64(base64Buffer.toString()));
        }

        return result.toString();
    }

    private static String decodeBase64(String base64Content) {
        base64Content = base64Content.trim().replaceAll("[^A-Za-z0-9+/=]", "");

        // Validate Base64 content
        if (!BASE64_PATTERN.matcher(base64Content).matches()) {
            throw new IllegalArgumentException("Invalid Base64 content");
        }

        // Pad Base64 string if necessary
        int padding = base64Content.length() % 4;
        if (padding > 0) {
            base64Content += "=".repeat(4 - padding);
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64Content);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error decoding Base64 content: " + base64Content, e);
        }
    }
}
