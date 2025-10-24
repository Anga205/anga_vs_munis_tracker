import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReadingsFile {
    private final String filePath;

    public ReadingsFile(String filePath) {
        this.filePath = filePath;
    }

    public Map<String, List<Reading>> load() {
        String text = readText(filePath);
        return parse(text);
    }

    public void save(Map<String, List<Reading>> data) throws Exception {
        String json = toPrettyJson(data);
        writeText(filePath, json);
    }

    // Ensures that all specified keys exist in the data map
    public Map<String, List<Reading>> ensureKeys(Map<String, List<Reading>> data, String... keys) {
        if (data == null) data = new LinkedHashMap<>();
        for (String k : keys) {
            data.computeIfAbsent(k.toLowerCase(), x -> new ArrayList<>());
        }
        return data;
    }

    // Reads text content from the given file path
    private String readText(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Writes text content to the given file path
    private void writeText(String path, String content) throws Exception {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
        bw.write(content);
        bw.flush();
        bw.close();
    }

    // Parses the JSON content into a map of readings
    private Map<String, List<Reading>> parse(String json) {
        Map<String, List<Reading>> map = new LinkedHashMap<>();
        if (json == null) return map;
        int objStart = json.indexOf('{');
        int objEnd = json.lastIndexOf('}');
        if (objStart < 0 || objEnd < 0 || objEnd <= objStart) return map;
        int i = objStart + 1;
        while (i < objEnd) {
            while (i < objEnd && Character.isWhitespace(json.charAt(i))) i++;
            if (i < objEnd && json.charAt(i) == ',') { i++; continue; }
            while (i < objEnd && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= objEnd) break;
            if (json.charAt(i) != '"') { i++; continue; }
            i++;
            StringBuilder keyBuilder = new StringBuilder();
            while (i < objEnd && json.charAt(i) != '"') {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < objEnd) {
                    keyBuilder.append(json.charAt(i + 1));
                    i += 2;
                    continue;
                }
                keyBuilder.append(c);
                i++;
            }
            if (i >= objEnd) break;
            String key = keyBuilder.toString();
            i++;
            int colon = json.indexOf(':', i);
            if (colon < 0 || colon > objEnd) break;
            int arrStart = json.indexOf('[', colon + 1);
            if (arrStart < 0 || arrStart > objEnd) break;
            int arrEnd = findMatchingBracket(json, arrStart);
            if (arrEnd < 0) break;
            String inner = json.substring(arrStart + 1, arrEnd);
            List<Reading> list = new ArrayList<>();
            int j = 0; int n = inner.length();
            // Read pairs of [solved, timestamp]
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
                        String first = pairStr.substring(0, comma).trim();
                        String second = pairStr.substring(comma + 1).trim();
                        int solved = Integer.parseInt(first.replaceAll("[^0-9]", ""));
                        double time = Double.parseDouble(second);
                        list.add(new Reading(solved, time));
                    } catch (Exception ignore) { }
                }
                j = pairEnd + 1;
            }
            map.put(key, list);
            i = arrEnd + 1;
        }
        return map;
    }

    // Finds the matching closing bracket for the opening bracket at startIdx
    private int findMatchingBracket(String s, int startIdx) {
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

    // Converts the map of readings into pretty JSON format
    private String toPrettyJson(Map<String, List<Reading>> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean firstKey = true;
        for (Map.Entry<String, List<Reading>> entry : map.entrySet()) {
            if (!firstKey) sb.append(",\n");
            firstKey = false;
            sb.append("    \"").append(entry.getKey()).append("\": [\n");
            List<Reading> rows = entry.getValue();
            for (int i = 0; i < rows.size(); i++) {
                Reading r = rows.get(i);
                sb.append("        [").append(r.solvedCount).append(", ")
                  .append(TimeUtils.decimal(r.timestampSeconds)).append("]");
                if (i < rows.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]");
        }
        sb.append("\n}\n");
        return sb.toString();
    }
}
