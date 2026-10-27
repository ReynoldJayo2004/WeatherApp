# Weather App (Java Swing)

Desktop app that shows current weather for any city using the OpenWeatherMap API.

- **GUI:** Swing
- **HTTP:** `HttpURLConnection`
- **JSON:** Gson
- **Build:** Maven (JDK 17+)

## Setup
1. Get a free API key at https://openweathermap.org/api (new keys can take up to a couple of hours to activate).
2. Provide the key in **one** of these ways:
   - Environment variable: `OPENWEATHER_API_KEY=yourkey`
   - Copy `config.properties.example` to `config.properties` and fill in `apiKey`
   - Or just run the app and paste it into the dialog.

## Run
```
mvn package
java -jar target/weather-app-1.0-jar-with-dependencies.jar
```
(In VS Code you can also open `WeatherApp.java` and click **Run**.)

## Features
- Search by city (`Manila`, `London`, `Paris,FR`), via button or Enter key
- Temperature, feels-like, min/max, humidity, wind, pressure, visibility, cloudiness, icon
- °C / °F toggle without a new request
- Error handling: empty/invalid input, city not found, bad API key, rate limit, timeout, no internet, malformed JSON
- Requests run in a `SwingWorker`, so the UI never freezes

## Structure
| File | Role |
|---|---|
| `WeatherApp.java` | Swing GUI and entry point |
| `WeatherService.java` | API request + JSON parsing |
| `WeatherData.java` | Weather data model |
| `WeatherException.java` | User-friendly error type |
