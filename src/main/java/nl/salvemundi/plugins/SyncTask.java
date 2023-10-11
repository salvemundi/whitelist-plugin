package nl.salvemundi.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncTask extends BukkitRunnable {

    private FileConfiguration config;
    private Logger logger;
    public SyncTask(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    private boolean containsPlayer(String[] array, String playerName) {
        for (String item : array) {
            if (item.equals(playerName)) {
                return true;
            }
        }
        return false;
    }
    private String getAccessToken() {
        // Define your OAuth token endpoint URL and client credentials
        String tokenUrl = config.getString("salvemundi_url") + "/oauth/token";
        String clientId = config.getString("salvemundi_api_client_id");
        String clientSecret = config.getString("salvemundi_api_client_secret");

        try {
            // Create a URL object
            URL url = new URL(tokenUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Enable input/output streams
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Create a map of form parameters
            Map<String, String> parameters = new HashMap<>();
            parameters.put("grant_type", "client_credentials");
            parameters.put("client_id", clientId);
            parameters.put("client_secret", clientSecret);

            // Build the request body as a query string
            StringBuilder requestBody = new StringBuilder();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (requestBody.length() != 0) {
                    requestBody.append("&");
                }
                requestBody.append(entry.getKey()).append("=").append(entry.getValue());
            }

            // Set the content type to "application/x-www-form-urlencoded"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Write the request body to the output stream
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes(requestBody.toString());
                outputStream.flush();
            }

            // Get the HTTP response code
            int responseCode = connection.getResponseCode();

            // Read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    // Parse the JSON response
                    String jsonResponse = response.toString();
                    JsonParser jsonParser = new JsonParser();
                    JsonObject json = jsonParser.parse(jsonResponse).getAsJsonObject();
                    return json.get("access_token").getAsString();
                }
            } else {
                logger.log(Level.SEVERE,"HTTP POST request failed with response code: " + responseCode);
            }

            // Close the connection
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private String getData() throws IOException {
        URL url = null;
        try {
            // Assuming config.get("salvemundi_url") and config.get("salvemundi_endpoint") are valid URLs
            url = new URL(config.getString("salvemundi_url") + config.getString("salvemundi_endpoint"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + this.getAccessToken());

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                return response.toString();
            } else {
                logger.log(Level.SEVERE,"Something went wrong retrieving the data!");

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return null;
    }
    @Override
    public void run() {
        try {
            String data = this.getData();
            try {
                Gson gson = new Gson();

                // Convert the JSON string to a String array
                String[] stringArray = gson.fromJson(data, String[].class);
                if(stringArray == null) {
                    return;
                }
                OfflinePlayer[] currentWhiteList = Bukkit.getWhitelistedPlayers().toArray(new OfflinePlayer[0]);
                for (OfflinePlayer player : currentWhiteList) {
                    String playerName = player.getName();

                    // Check if the player's name is not in the stringArray
                    if (!containsPlayer(stringArray, playerName)) {
                        player.setWhitelisted(false); // Remove the player from the whitelist
                    }
                }
                for (String item : stringArray) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(item);

                    player.setWhitelisted(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Something went wrong retrieving the data!");
        }
    }
}
