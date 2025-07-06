public class Main {
    public static void main(String[] args) throws Exception {
        String[] usernames = {"Anga205", "munish42"};

        // Fetch totalSolved for each user
        java.util.Map<String, Integer> solvedMap = new java.util.HashMap<>();
        java.util.Map<String, String> rawJsonMap = new java.util.HashMap<>();

        for (String username : usernames) {
            String urlStr = "https://leetcode-api-faisalshohag.vercel.app/" + username;
            try {
                String json = httpGet(urlStr);
                rawJsonMap.put(username, json);
                Integer totalSolved = extractTotalSolved(json);
                if (totalSolved != null) {
                    solvedMap.put(username.toLowerCase(), totalSolved);
                    System.out.println("--- Output for " + username + " ---");
                    System.out.println(json);
                } else {
                    System.out.println("Could not parse totalSolved for " + username);
                }
            } catch (Exception e) {
                System.out.println("Error fetching data for " + username + ": " + e.getMessage());
            }
        }

        // If we have at least one result, write/update readings.json
        if (!solvedMap.isEmpty()) {
            double ts = currentUtcSeconds();
            String tsStr = formatDecimal(ts);
            String readingsPath = "UpdateManager/readings.json";
            String existing = readFileIfExists(readingsPath);
            String updatedJson;
            if (existing == null || existing.trim().isEmpty()) {
                // Create fresh object with entries for available users
                StringBuilder sb = new StringBuilder();
                sb.append("{\n");
                boolean first = true;
                for (java.util.Map.Entry<String, Integer> e : solvedMap.entrySet()) {
                    if (!first) sb.append(",\n");
                    first = false;
                    sb.append("    \"").append(e.getKey()).append("\": [\n");
                    sb.append("        [").append(e.getValue()).append(", ").append(tsStr).append("]\n");
                    sb.append("    ]");
                }
                sb.append("\n}");
                updatedJson = sb.toString();
            } else {
                // Append to existing arrays, inserting missing keys if needed
                updatedJson = existing;
                for (java.util.Map.Entry<String, Integer> e : solvedMap.entrySet()) {
                    updatedJson = upsertArrayAppendPretty(updatedJson, e.getKey(), e.getValue(), tsStr);
                }
            }
            writeString(readingsPath, updatedJson);
            System.out.println("Updated readings.json at " + readingsPath);
        }
    }

    // Format double as plain decimal, never scientific notation
    private static String formatDecimal(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%.8f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // Append an element to the array under key, pretty-printing with 4-space indentation
    private static String upsertArrayAppendPretty(String json, String key, int value, String tsStr) {
        String kq = "\"" + key + "\"";
        int keyIdx = json.indexOf(kq);
        if (keyIdx >= 0) {
            int colonIdx = json.indexOf(':', keyIdx + kq.length());
            if (colonIdx < 0) return json;
            int arrStart = json.indexOf('[', colonIdx + 1);
            if (arrStart < 0) return json;
            int arrEnd = findMatchingBracket(json, arrStart);
            if (arrEnd < 0) return json;
            String inner = json.substring(arrStart + 1, arrEnd).trim();
            StringBuilder sb = new StringBuilder();
            sb.append(json, 0, arrEnd);
            if (inner.isEmpty()) {
                sb.append("\n        [").append(value).append(", ").append(tsStr).append("]\n    ");
            } else {
                // Remove trailing whitespace/newlines before ]
                int trimEnd = arrEnd;
                while (trimEnd > arrStart + 1 && Character.isWhitespace(json.charAt(trimEnd - 1))) trimEnd--;
                sb.setLength(trimEnd);
                sb.append(",\n        [").append(value).append(", ").append(tsStr).append("]\n    ");
            }
            sb.append(json.substring(arrEnd));
            return sb.toString();
        } else {
            int objEnd = json.lastIndexOf('}');
            if (objEnd < 0) {
                return "{\n    \"" + key + "\": [\n        [" + value + ", " + tsStr + "]\n    ]\n}";
            }
            int objStart = json.indexOf('{');
            String middle = (objStart >= 0 && objEnd > objStart) ? json.substring(objStart + 1, objEnd).trim() : "";
            String pair = "    \"" + key + "\": [\n        [" + value + ", " + tsStr + "]\n    ]";
            String insert = (middle.isEmpty() ? pair : (",\n" + pair));
            return json.substring(0, objEnd) + insert + json.substring(objEnd);
        }
    }

    // Simple HTTP GET returning body as String
    private static String httpGet(String urlStr) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(15000);
        con.setReadTimeout(20000);
        int code = con.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(is));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();
        con.disconnect();
        return content.toString();
    }

    // Extract totalSolved integer via simple pattern search
    private static Integer extractTotalSolved(String json) {
        if (json == null) return null;
        String needle = "\"totalSolved\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + needle.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        int start = i;
        while (i < json.length() && Character.isDigit(json.charAt(i))) i++;
        if (start == i) return null;
        try {
            return Integer.parseInt(json.substring(start, i));
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private static double currentUtcSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    // Read file content if exists; else return null
    private static String readFileIfExists(String path) {
        java.io.File f = new java.io.File(path);
        if (!f.exists()) return null;
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Write string to file (UTF-8), overwriting
    private static void writeString(String path, String content) throws Exception {
        java.io.File f = new java.io.File(path);
        java.io.File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(f, false));
        bw.write(content);
        bw.flush();
        bw.close();
    }

    // Find matching ']' for array starting at '[' startIdx; ignores nested [] by counting depth
    private static int findMatchingBracket(String s, int startIdx) {
        int depth = 0;
        for (int i = startIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}