import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

public class ParseTest {

    public static void main(String[] args) throws MalformedURLException {
        String variant = "classic";
        String name = "example";
        String imageUrl = "https://mcskin.com.cn/skin/Baroness.png";
        String request = "{"
                + "\"variant\":\"" + variant + "\","
                + "\"name\":\"" + name + "\","
                + "\"visibility\":\"public\","
                + "\"url\":\"" + imageUrl + "\""
                + "}";
        int responseCode;
        URL url = new URL("https://api.mineskin.org/v2/queue");
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent", "Chrome/120.0.0.0");
            http.setRequestProperty("Authorization", "Bearer msk_cQXSLKi3_ASU7SQIAZjHCZy255sHEjKKPPD3H-4RPx1WO-03qlvJRb2Vzel8ze89QIXpONK71");
            http.setConnectTimeout(20000);
            http.setReadTimeout(20000);
            try (OutputStream os = http.getOutputStream()) {
                byte[] input = request.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            responseCode = http.getResponseCode();
            System.out.println(responseCode);
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(http.getErrorStream(), StandardCharsets.UTF_8));
            }
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();
            System.out.println("Response Body: " + response);
            http.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
