public class Main {
    private static class ReadingPair {
        int solved;
        double ts;
        ReadingPair(int s, double t) { this.solved = s; this.ts = t; }
    }
    public static void main(String[] args) throws Exception {
        String[] usernames = {"Anga205", "munish42"};
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
        if (!solvedMap.isEmpty()) {
            double ts = currentUtcSeconds();
            String readingsPath = "UpdateManager/readings.json";
            String existing = readFileIfExists(readingsPath);
            java.util.Map<String, java.util.List<ReadingPair>> readings = parseReadings(existing);
            for (java.util.Map.Entry<String, Integer> e : solvedMap.entrySet()) {
                String key = e.getKey();
                readings.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                        .add(new ReadingPair(e.getValue(), ts));
            }
            String updatedJson = buildPrettyJson(readings);
            writeString(readingsPath, updatedJson);
            System.out.println("Updated readings.json at " + readingsPath);
        }
    }
    private static String formatDecimal(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%.8f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private static java.util.Map<String, java.util.List<ReadingPair>> parseReadings(String json) {
        java.util.Map<String, java.util.List<ReadingPair>> map = new java.util.LinkedHashMap<>();
        if (json == null) return map;
        int i = 0;
        int objStart = json.indexOf('{');
        int objEnd = json.lastIndexOf('}');
        if (objStart < 0 || objEnd < 0 || objEnd <= objStart) return map;
        i = objStart + 1;
        while (i < objEnd) {
            while (i < objEnd && Character.isWhitespace(json.charAt(i))) i++;
            if (i < objEnd && json.charAt(i) == ',') { i++; continue; }
            while (i < objEnd && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= objEnd) break;
            if (json.charAt(i) != '"') { i++; continue; }
            i++;
            StringBuilder keySb = new StringBuilder();
            while (i < objEnd && json.charAt(i) != '"') {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < objEnd) {
                    keySb.append(json.charAt(i + 1));
                    i += 2;
                    continue;
                }
                keySb.append(c);
                i++;
            }
            if (i >= objEnd) break;
            String key = keySb.toString();
            i++;
            int colon = json.indexOf(':', i);
            if (colon < 0 || colon > objEnd) break;
            int arrStart = json.indexOf('[', colon + 1);
            if (arrStart < 0 || arrStart > objEnd) break;
            int arrEnd = findMatchingBracket(json, arrStart);
            if (arrEnd < 0) break;
            String inner = json.substring(arrStart + 1, arrEnd);
            java.util.List<ReadingPair> list = new java.util.ArrayList<>();
            int j = 0; int n = inner.length();
            while (j < n) {
                while (j < n && Character.isWhitespace(inner.charAt(j))) j++;
                if (j < n && inner.charAt(j) == ',') { j++; continue; }
                while (j < n && Character.isWhitespace(inner.charAt(j))) j++;
                if (j >= n) break;
                if (inner.charAt(j) != '[') { j++; continue; }
                int pairEnd = findMatchingBracket(inner, j);
                if (pairEnd < 0) break;
                String pairStr = inner.substring(j + 1, pairEnd).trim();
                int comma = pairStr.indexOf(',');
                if (comma > 0) {
                    try {
                        String aStr = pairStr.substring(0, comma).trim();
                        String bStr = pairStr.substring(comma + 1).trim();
                        int solved = Integer.parseInt(aStr.replaceAll("[^0-9]", ""));
                        double ts = Double.parseDouble(bStr);
                        list.add(new ReadingPair(solved, ts));
                    } catch (Exception ignore) { }
                }
                j = pairEnd + 1;
            }
            map.put(key, list);
            i = arrEnd + 1;
        }
        return map;
    }
    private static String buildPrettyJson(java.util.Map<String, java.util.List<ReadingPair>> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean firstKey = true;
        for (java.util.Map.Entry<String, java.util.List<ReadingPair>> e : map.entrySet()) {
            if (!firstKey) sb.append(",\n");
            firstKey = false;
            sb.append("    \"").append(e.getKey()).append("\": [\n");
            java.util.List<ReadingPair> list = e.getValue();
            for (int idx = 0; idx < list.size(); idx++) {
                ReadingPair p = list.get(idx);
                sb.append("        [").append(p.solved).append(", ").append(formatDecimal(p.ts)).append("]");
                if (idx < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]");
        }
        sb.append("\n}\n");
        return sb.toString();
    }
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
    private static void writeString(String path, String content) throws Exception {
        java.io.File f = new java.io.File(path);
        java.io.File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(f, false));
        bw.write(content);
        bw.flush();
        bw.close();
    }
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
