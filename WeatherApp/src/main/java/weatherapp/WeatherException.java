package weatherapp;

/** Checked exception carrying a user-friendly message for any weather lookup failure. */
public class WeatherException extends Exception {
    public WeatherException(String message) {
        super(message);
    }

    public WeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
