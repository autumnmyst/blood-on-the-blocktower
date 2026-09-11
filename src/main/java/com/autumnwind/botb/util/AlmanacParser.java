package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Parses almanac HTML pages from bloodstar.xyz and similar sources.
 * Fetches asynchronously and caches results.
 */
public class AlmanacParser {

    private static final Map<String, AlmanacData> cache = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<AlmanacData>> pendingFetches = new ConcurrentHashMap<>();

    // Track the last error for user feedback
    private static volatile Component lastError = null;

    // Patterns for extracting content
    private static final Pattern PAGE_PATTERN = Pattern.compile(
        "<li class=\"page\" id=\"([^\"]+)\">.*?<div class=\"page-contents[^\"]*\">(.*?)</div></li>",
        Pattern.DOTALL
    );

    private static final Pattern FLAVOR_PATTERN = Pattern.compile(
        "<div class=\"flavor\">(.*?)</div>",
        Pattern.DOTALL
    );

    private static final Pattern OVERVIEW_PATTERN = Pattern.compile(
        "<div class=\"overview\">(.*?)</div>",
        Pattern.DOTALL
    );

    private static final Pattern EXAMPLE_PATTERN = Pattern.compile(
        "<h3>Examples</h3>.*?<div class=\"example\">(.*?)</div>",
        Pattern.DOTALL
    );

    private static final Pattern HOW_TO_RUN_PATTERN = Pattern.compile(
        "<h3>How to Run</h3>.*?<div class=\"how-to-run\">(.*?)</div>",
        Pattern.DOTALL
    );

    private static final Pattern TIP_PATTERN = Pattern.compile(
        "<div class=\"tip\">(.*?)</div>",
        Pattern.DOTALL
    );

    /**
     * Fetch and parse an almanac asynchronously. Returns cached data if available.
     * @param almanacUrl The URL to the almanac HTML
     * @return A future that completes with the parsed almanac data
     */
    public static CompletableFuture<AlmanacData> fetchAlmanac(String almanacUrl) {
        if (almanacUrl == null || almanacUrl.isEmpty()) {
            return CompletableFuture.completedFuture(AlmanacData.empty());
        }

        // Return cached data if available
        if (cache.containsKey(almanacUrl)) {
            return CompletableFuture.completedFuture(cache.get(almanacUrl));
        }

        // Return pending fetch if one is already in progress
        if (pendingFetches.containsKey(almanacUrl)) {
            return pendingFetches.get(almanacUrl);
        }

        // Start a new async fetch
        CompletableFuture<AlmanacData> future = CompletableFuture.supplyAsync(() -> {
            try {
                lastError = null;
                String html = fetchHtml(almanacUrl);
                if (html == null || html.isEmpty()) {
                    lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.empty");
                    BloodOnTheBlocktower.LOGGER.warn("Almanac returned empty content from {}", almanacUrl);
                    return AlmanacData.empty();
                }
                AlmanacData data = parseHtml(html);
                if (!data.hasScriptData() && (data.roleData() == null || data.roleData().isEmpty())) {
                    lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.unexpected_format");
                    BloodOnTheBlocktower.LOGGER.warn("Failed to parse any content from almanac at {}", almanacUrl);
                }
                cache.put(almanacUrl, data);
                return data;
            } catch (UnknownHostException e) {
                lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.unreachable");
                BloodOnTheBlocktower.LOGGER.warn("Failed to fetch almanac from {} - unknown host: {}", almanacUrl, e.getMessage());
                return AlmanacData.empty();
            } catch (SocketTimeoutException e) {
                lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.timeout");
                BloodOnTheBlocktower.LOGGER.warn("Timeout fetching almanac from {}: {}", almanacUrl, e.getMessage());
                return AlmanacData.empty();
            } catch (IOException e) {
                lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.network", e.getMessage());
                BloodOnTheBlocktower.LOGGER.warn("IO error fetching almanac from {}: {}", almanacUrl, e.getMessage());
                return AlmanacData.empty();
            } catch (Exception e) {
                lastError = Component.translatable("gui.blood-on-the-blocktower.almanac.error.generic", e.getMessage());
                BloodOnTheBlocktower.LOGGER.warn("Failed to fetch almanac from {}: {}", almanacUrl, e.getMessage());
                return AlmanacData.empty();
            } finally {
                pendingFetches.remove(almanacUrl);
            }
        });

        pendingFetches.put(almanacUrl, future);
        return future;
    }

    /**
     * Fetch and parse multiple almanacs, merging role data (main almanac takes precedence for script data).
     * @param mainAlmanacUrl The main almanac URL (provides script data + role data)
     * @param extraAlmanacUrls Additional almanac URLs (only provide role data)
     * @return A future that completes with merged almanac data
     */
    public static CompletableFuture<AlmanacData> fetchAlmanacs(String mainAlmanacUrl, List<String> extraAlmanacUrls) {
        if ((mainAlmanacUrl == null || mainAlmanacUrl.isEmpty()) &&
            (extraAlmanacUrls == null || extraAlmanacUrls.isEmpty())) {
            return CompletableFuture.completedFuture(AlmanacData.empty());
        }

        // Fetch main almanac
        CompletableFuture<AlmanacData> mainFuture = fetchAlmanac(mainAlmanacUrl);

        // If no extra almanacs, just return main
        if (extraAlmanacUrls == null || extraAlmanacUrls.isEmpty()) {
            return mainFuture;
        }

        // Fetch all extra almanacs in parallel
        List<CompletableFuture<AlmanacData>> extraFutures = extraAlmanacUrls.stream()
            .map(AlmanacParser::fetchAlmanac)
            .collect(Collectors.toList());

        // Combine all futures
        CompletableFuture<Void> allExtras = CompletableFuture.allOf(
            extraFutures.toArray(new CompletableFuture[0])
        );

        return mainFuture.thenCombine(allExtras, (mainData, ignored) -> {
            // Merge role data from extras (main takes precedence)
            Map<String, AlmanacData.RoleAlmanacData> mergedRoleData = new HashMap<>();

            // First add all extra role data
            for (CompletableFuture<AlmanacData> extraFuture : extraFutures) {
                try {
                    AlmanacData extraData = extraFuture.join();
                    if (extraData.roleData() != null) {
                        mergedRoleData.putAll(extraData.roleData());
                    }
                } catch (Exception e) {
                    // Ignore failed extra fetches
                }
            }

            // Then overlay main role data (takes precedence)
            if (mainData.roleData() != null) {
                mergedRoleData.putAll(mainData.roleData());
            }

            return new AlmanacData(mainData.scriptData(), mergedRoleData);
        });
    }

    /**
     * Get cached almanac data if available.
     */
    public static AlmanacData getCached(String almanacUrl) {
        return cache.get(almanacUrl);
    }

    /**
     * Get the last error message, if any.
     */
    public static Component getLastError() {
        return lastError;
    }

    /**
     * Fetch HTML content from a URL.
     */
    private static String fetchHtml(String urlString) throws Exception {
        if (!FetchLimits.isAllowedScheme(urlString)) {
            throw new IOException("Only http(s) almanac URLs are fetched: " + urlString);
        }
        URL url = new URI(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(FetchLimits.timeoutMs);
        conn.setReadTimeout(FetchLimits.timeoutMs);
        conn.setRequestProperty("User-Agent", "BloodOnTheBlocktower-Mod/1.0");

        try (InputStream in = conn.getInputStream()) {
            return new String(FetchLimits.readBounded(in, FetchLimits.maxBytes), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse almanac HTML into AlmanacData.
     */
    private static AlmanacData parseHtml(String html) {
        AlmanacData.ScriptAlmanacData scriptData = parseScriptData(html);
        Map<String, AlmanacData.RoleAlmanacData> roleData = parseRoleData(html);
        return new AlmanacData(scriptData, roleData);
    }

    /**
     * Parse script-level data (synopsis, overview, changelog).
     */
    private static AlmanacData.ScriptAlmanacData parseScriptData(String html) {
        String synopsis = null;
        String overview = null;
        String changelog = null;

        Matcher pageMatcher = PAGE_PATTERN.matcher(html);
        while (pageMatcher.find()) {
            String pageId = pageMatcher.group(1);
            String content = pageMatcher.group(2);

            switch (pageId) {
                case "synopsis" -> synopsis = stripHtml(content);
                case "overview" -> overview = stripHtml(content);
                case "changelog" -> {
                    // Changelog has its content inside a nested div
                    Matcher overviewMatcher = OVERVIEW_PATTERN.matcher(content);
                    if (overviewMatcher.find()) {
                        changelog = stripHtml(overviewMatcher.group(1));
                    } else {
                        changelog = stripHtml(content);
                    }
                }
            }
        }

        return new AlmanacData.ScriptAlmanacData(synopsis, overview, changelog);
    }

    /**
     * Parse per-role data from almanac HTML.
     */
    private static Map<String, AlmanacData.RoleAlmanacData> parseRoleData(String html) {
        Map<String, AlmanacData.RoleAlmanacData> roleData = new HashMap<>();

        // Find all page elements
        Matcher pageMatcher = PAGE_PATTERN.matcher(html);
        while (pageMatcher.find()) {
            String pageId = pageMatcher.group(1);
            String content = pageMatcher.group(2);

            // Skip non-role pages
            if (pageId.equals("synopsis") || pageId.equals("overview") ||
                pageId.equals("changelog") || pageId.equals("nightOrder")) {
                continue;
            }

            // Parse role-specific sections
            String flavor = extractSection(content, FLAVOR_PATTERN);
            String roleOverview = extractSection(content, OVERVIEW_PATTERN);
            String examples = extractSection(content, EXAMPLE_PATTERN);
            String howToRun = extractSection(content, HOW_TO_RUN_PATTERN);
            String tip = extractSection(content, TIP_PATTERN);

            // Only add if we have any data
            if (flavor != null || roleOverview != null || examples != null ||
                howToRun != null || tip != null) {
                String normalizedId = AlmanacData.normalizeRoleId(pageId);
                roleData.put(normalizedId, new AlmanacData.RoleAlmanacData(
                    flavor, roleOverview, examples, howToRun, tip
                ));
            }
        }

        return roleData;
    }

    /**
     * Extract a section using a regex pattern.
     */
    private static String extractSection(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return stripHtml(matcher.group(1));
        }
        return null;
    }

    /**
     * Strip HTML tags and decode entities, preserving line breaks.
     */
    private static String stripHtml(String html) {
        if (html == null) return null;

        // Convert block-level elements to line breaks before stripping
        String text = html;

        // Convert </p> and <br> variants to newline markers
        text = text.replaceAll("</p>\\s*<p[^>]*>", "\n\n"); // Paragraph breaks
        text = text.replaceAll("</p>", "\n");
        text = text.replaceAll("<p[^>]*>", "");
        text = text.replaceAll("<br\\s*/?>", "\n");
        text = text.replaceAll("</div>\\s*<div[^>]*>", "\n"); // Div breaks
        text = text.replaceAll("<hr\\s*/?>", "\n---\n"); // Horizontal rules

        // Remove remaining HTML tags
        text = text.replaceAll("<[^>]+>", " ");

        // Decode common HTML entities
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&apos;", "'")
                   .replace("&nbsp;", " ");

        // Normalize whitespace within lines, but preserve line breaks
        // First, normalize multiple spaces to single space
        text = text.replaceAll("[ \\t]+", " ");
        // Normalize multiple newlines to at most two (paragraph break)
        text = text.replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n");
        // Trim each line
        String[] lines = text.split("\\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() || (i > 0 && i < lines.length - 1)) {
                if (result.length() > 0 && !line.isEmpty()) {
                    result.append("\n");
                }
                result.append(line);
            }
        }

        text = result.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
