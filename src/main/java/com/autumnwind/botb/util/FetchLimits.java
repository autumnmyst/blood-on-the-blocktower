package com.autumnwind.botb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Limits for content the client fetches on a script's say-so (custom role images, almanac
 * pages). Every player's client fetches these automatically, so a script can't be allowed to
 * hang a client on a dead host or hand it a multi-gigabyte response. The values are set from
 * the player's config file at client start; the defaults are meant to be generous.
 */
public final class FetchLimits {

    private FetchLimits() {}

    /** Connect and read timeout for each request. */
    public static int timeoutMs = 10_000;

    /** Largest response body accepted, in bytes. */
    public static int maxBytes = 8 * 1024 * 1024;

    /** Largest image accepted, per side in pixels. Role icons are 108x108. */
    public static int maxImageDimension = 2048;

    /** Only web URLs are fetched; file:, jar: and the like are refused outright. */
    public static boolean isAllowedScheme(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /** Reads the whole stream, failing once it grows past {@code max} bytes. */
    public static byte[] readBounded(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            if (out.size() + n > max) {
                throw new IOException("Response exceeds the " + max + " byte limit");
            }
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }
}
