package weatherapp;

/**
 * Immutable model holding the weather details we extract from the API's JSON.
 * Temperatures are stored in Celsius; the GUI converts to Fahrenheit if needed.
 */
public record WeatherData(
        String city,
        String country,
        String condition,      // e.g. "Clouds"
        String description,    // e.g. "scattered clouds"
        String iconCode,       // e.g. "03d" (used to download the icon)
        double tempC,
        double feelsLikeC,
        double tempMinC,
        double tempMaxC,
        int humidity,          // percent
        int pressure,          // hPa
        double windSpeed,      // m/s
        int visibility,        // metres
        int cloudiness         // percent
) {
    /** Converts Celsius to Fahrenheit. */
    public static double toFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }
}
