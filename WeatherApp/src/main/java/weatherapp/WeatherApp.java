package weatherapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Weather App - Swing GUI.
 * Type a city, press Search (or Enter) and the current weather is displayed.
 *
 * API key lookup order:
 *   1. Environment variable OPENWEATHER_API_KEY
 *   2. config.properties file (apiKey=...) in the working directory
 *   3. A dialog asking the user to paste the key
 */
public class WeatherApp extends JFrame {

    private final WeatherService service;

    // --- input widgets ---
    private final JTextField cityField = new JTextField(18);
    private final JButton searchButton = new JButton("Search");
    private final JComboBox<String> unitBox = new JComboBox<>(new String[]{"°C", "°F"});

    // --- output widgets ---
    private final JLabel locationLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel iconLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel tempLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel descLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel feelsLabel = valueLabel();
    private final JLabel minMaxLabel = valueLabel();
    private final JLabel humidityLabel = valueLabel();
    private final JLabel windLabel = valueLabel();
    private final JLabel pressureLabel = valueLabel();
    private final JLabel visibilityLabel = valueLabel();
    private final JLabel cloudLabel = valueLabel();
    private final JLabel statusLabel = new JLabel("Enter a city to get started.", SwingConstants.CENTER);

    /** Last successful result, kept so changing °C/°F doesn't need a new request. */
    private WeatherData current;

    public WeatherApp(WeatherService service) {
        super("Weather App");
        this.service = service;
        buildUi();
        wireEvents();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ UI

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(0xEAF4FB));
        setContentPane(root);

        // Top: search bar
        JPanel search = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        search.setOpaque(false);
        search.add(new JLabel("City:"));
        search.add(cityField);
        search.add(searchButton);
        search.add(unitBox);
        cityField.setToolTipText("e.g. Manila, London, or Paris,FR");
        root.add(search, BorderLayout.NORTH);

        // Center: main reading
        locationLabel.setFont(locationLabel.getFont().deriveFont(Font.BOLD, 22f));
        tempLabel.setFont(tempLabel.getFont().deriveFont(Font.BOLD, 44f));
        descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC, 15f));
        iconLabel.setPreferredSize(new Dimension(100, 100));

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setOpaque(false);
        for (JComponent c : new JComponent[]{locationLabel, iconLabel, tempLabel, descLabel}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            summary.add(c);
        }

        // Details grid
        JPanel details = new JPanel(new GridLayout(0, 2, 10, 6));
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB5D3E7)),
                new EmptyBorder(10, 14, 10, 14)));
        addRow(details, "Feels like", feelsLabel);
        addRow(details, "Min / Max", minMaxLabel);
        addRow(details, "Humidity", humidityLabel);
        addRow(details, "Wind", windLabel);
        addRow(details, "Pressure", pressureLabel);
        addRow(details, "Visibility", visibilityLabel);
        addRow(details, "Cloudiness", cloudLabel);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(summary, BorderLayout.NORTH);
        center.add(details, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // Bottom: status / error messages
        statusLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        root.add(statusLabel, BorderLayout.SOUTH);

        // Reserve a sensible window size
        root.setPreferredSize(new Dimension(440, 560));
    }

    private static JLabel valueLabel() {
        JLabel l = new JLabel("-");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static void addRow(JPanel grid, String name, JLabel value) {
        grid.add(new JLabel(name + ":"));
        grid.add(value);
    }

    // -------------------------------------------------------------- events

    private void wireEvents() {
        searchButton.addActionListener(e -> search());
        cityField.addActionListener(e -> search());          // Enter key
        unitBox.addActionListener(e -> { if (current != null) display(current); });
    }

    /** Runs the network request off the UI thread so the window never freezes. */
    private void search() {
        final String city = cityField.getText();
        setBusy(true);
        setStatus("Fetching weather...", false);

        new SwingWorker<WeatherData, Void>() {
            private ImageIcon icon;

            @Override
            protected WeatherData doInBackground() throws Exception {
                WeatherData data = service.fetchWeather(city);
                icon = loadIcon(data.iconCode());   // optional; null on failure
                return data;
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    display(current);
                    iconLabel.setIcon(icon);
                    setStatus("Updated for " + current.city() + ".", false);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String msg = (cause instanceof WeatherException)
                            ? cause.getMessage()
                            : "Unexpected error: " + cause;
                    setStatus(msg, true);
                    JOptionPane.showMessageDialog(WeatherApp.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------- display

    /** Fills the labels, converting units according to the selected combo item. */
    private void display(WeatherData d) {
        boolean f = unitBox.getSelectedIndex() == 1;
        String unit = f ? "°F" : "°C";

        locationLabel.setText(d.city() + (d.country().isEmpty() ? "" : ", " + d.country()));
        tempLabel.setText(fmtTemp(d.tempC(), f) + unit);
        descLabel.setText(capitalize(d.description()));
        feelsLabel.setText(fmtTemp(d.feelsLikeC(), f) + unit);
        minMaxLabel.setText(fmtTemp(d.tempMinC(), f) + unit + " / " + fmtTemp(d.tempMaxC(), f) + unit);
        humidityLabel.setText(d.humidity() + " %");
        windLabel.setText(String.format("%.1f m/s (%.1f km/h)", d.windSpeed(), d.windSpeed() * 3.6));
        pressureLabel.setText(d.pressure() + " hPa");
        visibilityLabel.setText(String.format("%.1f km", d.visibility() / 1000.0));
        cloudLabel.setText(d.cloudiness() + " %");
    }

    private static String fmtTemp(double celsius, boolean fahrenheit) {
        return String.format("%.1f", fahrenheit ? WeatherData.toFahrenheit(celsius) : celsius);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void setBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        cityField.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        if (!busy) cityField.requestFocusInWindow();
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setForeground(error ? new Color(0xB00020) : new Color(0x333333));
        statusLabel.setText(text);
    }

    /** Downloads the weather icon; returns null if it can't be loaded (non-critical). */
    private static ImageIcon loadIcon(String code) {
        try {
            ImageIcon icon = new ImageIcon(java.net.URI.create("https://openweathermap.org/img/wn/" + code + "@2x.png").toURL());
            return icon.getImageLoadStatus() == MediaTracker.COMPLETE ? icon : null;
        } catch (IOException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- main

    /** Finds the API key from env var, config.properties, or by asking the user. */
    private static String resolveApiKey() {
        String key = System.getenv("OPENWEATHER_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            key = props.getProperty("apiKey");
            if (key != null && !key.isBlank() && !key.startsWith("YOUR_")) return key.trim();
        } catch (IOException ignored) {
            // no config file - fall through to the dialog
        }

        key = JOptionPane.showInputDialog(null,
                "Enter your OpenWeatherMap API key\n(free at https://openweathermap.org/api):",
                "API Key Required", JOptionPane.QUESTION_MESSAGE);
        return key == null ? "" : key.trim();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }

            String apiKey = resolveApiKey();
            if (apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(null, "An API key is required. Exiting.",
                        "No API Key", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new WeatherApp(new WeatherService(apiKey)).setVisible(true);
        });
    }
}
