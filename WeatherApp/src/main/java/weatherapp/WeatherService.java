package weatherapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the OpenWeatherMap "Current Weather" API and turns the JSON
 * response into a {@link WeatherData} object.
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final int TIMEOUT_MS = 8000;

    private final String apiKey;

    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Fetches the current weather for a city.
     *
     * @param city city name, optionally "City,CountryCode" (e.g. "Manila,PH")
     * @throws WeatherException with a user-friendly message on any failure
     */
    public WeatherData fetchWeather(String city) throws WeatherException {
        validateCity(city);

        String url = BASE_URL
                + "?q=" + URLEncoder.encode(city.trim(), StandardCharsets.UTF_8)
                + "&units=metric"
                + "&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        String json = httpGet(url);
        return parse(json);
    }

    /** Rejects empty input or input with characters that cannot be part of a city name. */
    private void validateCity(String city) throws WeatherException {
        if (city == null || city.isBlank()) {
            throw new WeatherException("Please enter a city name.");
        }
        // Letters (any language), spaces, comma, period, hyphen, apostrophe
        if (!city.trim().matches("[\\p{L}\\s,.'-]+")) {
            throw new WeatherException("City names can only contain letters, spaces, commas, periods, hyphens and apostrophes.");
        }
    }

    /** Performs the HTTP GET and maps HTTP status codes to friendly errors. */
    private String httpGet(String urlString) throws WeatherException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) java.net.URI.create(urlString).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int status = conn.getResponseCode();
            switch (status) {
                case 200 -> {
                    try (InputStream in = conn.getInputStream()) {
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                case 401 -> throw new WeatherException(
                        "Invalid API key. New keys can take up to a few hours to activate.");
                case 404 -> throw new WeatherException("City not found. Check the spelling and try again.");
                case 429 -> throw new WeatherException("Too many requests. Please wait a moment and retry.");
                default -> throw new WeatherException("Weather service error (HTTP " + status + ").");
            }
        } catch (UnknownHostException e) {
            throw new WeatherException("No internet connection. Please check your network.", e);
        } catch (SocketTimeoutException e) {
            throw new WeatherException("The request timed out. Please try again.", e);
        } catch (IOException e) {
            throw new WeatherException("Network error: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Parses the OpenWeatherMap JSON into a WeatherData object using Gson. */
    WeatherData parse(String json) throws WeatherException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            JsonObject main = root.getAsJsonObject("main");
            JsonObject wind = root.getAsJsonObject("wind");
            JsonObject sys = root.getAsJsonObject("sys");
            JsonObject clouds = root.getAsJsonObject("clouds");
            JsonArray weatherArr = root.getAsJsonArray("weather");
            JsonObject weather = weatherArr.get(0).getAsJsonObject();

            return new WeatherData(
                    root.get("name").getAsString(),
                    sys != null && sys.has("country") ? sys.get("country").getAsString() : "",
                    weather.get("main").getAsString(),
                    weather.get("description").getAsString(),
                    weather.get("icon").getAsString(),
                    main.get("temp").getAsDouble(),
                    main.get("feels_like").getAsDouble(),
                    main.get("temp_min").getAsDouble(),
                    main.get("temp_max").getAsDouble(),
                    main.get("humidity").getAsInt(),
                    main.get("pressure").getAsInt(),
                    wind != null && wind.has("speed") ? wind.get("speed").getAsDouble() : 0,
                    root.has("visibility") ? root.get("visibility").getAsInt() : 0,
                    clouds != null && clouds.has("all") ? clouds.get("all").getAsInt() : 0
            );
        } catch (JsonParseException | IllegalStateException | NullPointerException
                 | IndexOutOfBoundsException | ClassCastException e) {
            throw new WeatherException("Received unexpected data from the weather service.", e);
        }
    }
}
