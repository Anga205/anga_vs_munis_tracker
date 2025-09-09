import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpTextFetcher {
    public static String fetchText(String urlText) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        int status = conn.getResponseCode();
        if (status != 200) {
            // Try to read error stream for diagnostics, but do not require it
            InputStream es = conn.getErrorStream();
            String err = null;
            if (es != null) {
                try (BufferedReader er = new BufferedReader(new InputStreamReader(es))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = er.readLine()) != null) sb.append(line);
                    err = sb.toString();
                }
            }
            conn.disconnect();
            if (err != null && !err.isEmpty()) {
                throw new Exception("HTTP " + status + ": " + err);
            }
            throw new Exception("HTTP " + status);
        }
        InputStream stream = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line);
        }
        reader.close();
        conn.disconnect();
        return text.toString();
    }
}
