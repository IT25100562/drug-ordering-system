package com.medisys.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Tiny JSON helper for pages that update without reloading (JavaScript fetch).
 *
 * The same servlet serves both kinds of request:
 *   - a normal form post           -> redirect to a page (works without JavaScript)
 *   - a fetch() from our JavaScript -> small JSON answer
 * Our scripts send the header "Accept: application/json" to ask for JSON.
 *
 * Only flat objects with text, numbers and true/false are needed, so no JSON
 * library is used.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** True when the request came from our JavaScript and wants JSON back. */
    public static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /** Writes the map as a JSON object with the given HTTP status. */
    public static void write(HttpServletResponse response, int status, Map<String, ?> values)
            throws IOException {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append(quote(value.toString()));
            }
        }
        json.append('}');

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    /** Puts text in quotes and escapes the characters JSON does not allow. */
    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                case '<':  out.append("\\u003c"); break;   // never lets "</script>" through
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }
}
