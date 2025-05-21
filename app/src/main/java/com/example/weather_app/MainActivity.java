package com.example.weather_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WeatherApp"; // Tag for logging

    // Define UI components
    private EditText cityNameInput;
    private Button searchButton;
    private MaterialCardView weatherCardView;
    private MaterialCardView forecastCardView;
    private TextView cityNameText, temperatureText, weatherConditionText;
    private TextView humidityText, windSpeedText, pressureText;
    private ProgressBar loadingIndicator;
    private RecyclerView forecastRecyclerView;
    private ForecastAdapter forecastAdapter;

    // Using BuildConfig to store API key securely (should be defined in gradle)
    private String apiKey = "d32853d8fe257364ab3b7f8c209c324b";

    // Thread pool for network operations
    private ExecutorService executorService;

    // Flag to track if activity is active
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Get API key from BuildConfig (should be defined in your app's build.gradle)
            // For testing we'll use the key directly, but in production this should come from BuildConfig
            apiKey = "d32853d8fe257364ab3b7f8c209c324b"; // Should be BuildConfig.WEATHER_API_KEY

            // Initialize executor service for background tasks
            executorService = Executors.newFixedThreadPool(2);

            // Initialize all UI components
            initializeUI();

            // Set up click listeners
            setupEventListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeUI() {
        cityNameInput = findViewById(R.id.cityNameInput);
        searchButton = findViewById(R.id.searchButton);
        weatherCardView = findViewById(R.id.weatherCardView);
        forecastCardView = findViewById(R.id.forecastCardView);

        cityNameText = findViewById(R.id.cityNameText);
        temperatureText = findViewById(R.id.temperatureText);
        weatherConditionText = findViewById(R.id.weatherConditionText);

        humidityText = findViewById(R.id.humidityText);
        windSpeedText = findViewById(R.id.windSpeedText);
        pressureText = findViewById(R.id.pressureText);

        loadingIndicator = findViewById(R.id.loadingIndicator);

        // Initialize RecyclerView for forecast
        forecastRecyclerView = findViewById(R.id.forecastRecyclerView);
        forecastAdapter = new ForecastAdapter(new ArrayList<>());
        forecastRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        forecastRecyclerView.setAdapter(forecastAdapter);
    }

    private void setupEventListeners() {
        cityNameInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Hide keyboard
                    hideKeyboard();

                    // Trigger search
                    searchWeather();
                    return true;
                }
                return false;
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchWeather();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(cityNameInput.getWindowToken(), 0);
        }
    }

    private void searchWeather() {
        String city = cityNameInput.getText().toString().trim();

        if (city.isEmpty()) {
            Toast.makeText(MainActivity.this, "Enter City Name", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Show loading indicator and hide weather card before fetch begins
            loadingIndicator.setVisibility(View.VISIBLE);
            weatherCardView.setVisibility(View.GONE);
            forecastCardView.setVisibility(View.GONE);

            // URL encode the city name to handle spaces and special characters
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());

            // Get current weather
            String currentWeatherUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + apiKey;
            Log.d(TAG, "Current Weather API URL: " + currentWeatherUrl);
            fetchCurrentWeather(currentWeatherUrl);

            // Get 5-day forecast
            String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedCity + "&appid=" + apiKey;
            Log.d(TAG, "Forecast API URL: " + forecastUrl);
            fetchForecast(forecastUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding city name", e);
            showError("Error processing city name: " + e.getMessage());
        }
    }

    private void fetchCurrentWeather(final String apiUrl) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String result = fetchDataFromApi(apiUrl);

                if (!isActive.get()) return;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processCurrentWeatherResult(result);
                    }
                });
            }
        });
    }

    private void fetchForecast(final String apiUrl) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String result = fetchDataFromApi(apiUrl);

                if (!isActive.get()) return;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processForecastResult(result);
                    }
                });
            }
        });
    }

    private String fetchDataFromApi(String apiUrl) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            // Set timeout to avoid hanging
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "HTTP Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            } else {
                Log.e(TAG, "Error response code: " + responseCode);
                // Read the error stream
                InputStream errorStream = urlConnection.getErrorStream();
                if (errorStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    String line;
                    StringBuilder errorResult = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorResult.append(line);
                    }
                    Log.e(TAG, "Error response: " + errorResult.toString());
                    return "ERROR:" + responseCode + ":" + errorResult.toString();
                }
                return "ERROR:" + responseCode;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception during API call", e);
            return "ERROR:" + e.getMessage();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void processCurrentWeatherResult(String result) {
        // Hide loading indicator
        loadingIndicator.setVisibility(View.GONE);

        if (result == null) {
            showError("Unable to retrieve weather data. Check your internet connection.");
            return;
        }

        if (result.startsWith("ERROR:")) {
            String errorDetails = result.substring(6);
            Log.e(TAG, "Current Weather API Error Details: " + errorDetails);

            // Check if it's an API key issue
            if (errorDetails.contains("401") || errorDetails.contains("invalid API key")) {
                showError("Invalid API key. Please check your API key and try again.");
            }
            // Check if it's a city not found issue
            else if (errorDetails.contains("404") || errorDetails.contains("city not found")) {
                showError("City not found. Please check the city name and try again.");
            }
            // General error
            else {
                showError("Error retrieving weather data: " + errorDetails);
            }
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(result);

            // Check if the response contains an error message
            if (jsonObject.has("cod") && !jsonObject.getString("cod").equals("200")) {
                String errorMessage = jsonObject.optString("message", "Unknown error");
                showError("API Error: " + errorMessage);
                return;
            }

            // Get city name
            String city = jsonObject.getString("name");
            Log.d(TAG, "City: " + city);

            // Get weather condition
            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                String condition = weatherObj.getString("main");
                Log.d(TAG, "Weather condition: " + condition);

                // Get main weather data
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp") - 273.15; // Convert from Kelvin to Celsius
                int pressure = main.getInt("pressure");
                int humidity = main.getInt("humidity");
                Log.d(TAG, "Temp: " + temp + ", Pressure: " + pressure + ", Humidity: " + humidity);

                // Get wind speed - check if wind object exists
                double windSpeed = 0;
                if (jsonObject.has("wind")) {
                    JSONObject wind = jsonObject.getJSONObject("wind");
                    if (wind.has("speed")) {
                        windSpeed = wind.getDouble("speed");
                    }
                }
                Log.d(TAG, "Wind speed: " + windSpeed);

                // Update the UI with the fetched data
                updateWeatherUI(city, temp, condition, humidity, windSpeed, pressure);
            } else {
                showError("Weather data not available for this location");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Current Weather JSON Parse Exception", e);
            showError("Error parsing weather data. Details: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Current Weather General Exception", e);
            showError("Unexpected error occurred. Please try again.");
        }
    }

    private void processForecastResult(String result) {
        if (result == null || result.startsWith("ERROR:")) {
            // Error handling for forecast is less intrusive - we just don't show the forecast
            Log.e(TAG, "Failed to retrieve forecast data");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(result);

            // Verify forecast response
            if (jsonObject.has("cod") && !jsonObject.getString("cod").equals("200")) {
                Log.e(TAG, "Forecast API Error: " + jsonObject.optString("message", "Unknown error"));
                return;
            }

            // Parse the 5-day forecast data
            List<ForecastItem> forecastItems = parseForecastData(jsonObject);

            // Update the UI with forecast data
            updateForecastUI(forecastItems);

        } catch (JSONException e) {
            Log.e(TAG, "Forecast JSON Parse Exception", e);
        } catch (Exception e) {
            Log.e(TAG, "Forecast General Exception", e);
        }
    }

    private List<ForecastItem> parseForecastData(JSONObject forecastJson) throws JSONException {
        List<ForecastItem> forecastItems = new ArrayList<>();
        JSONArray listArray = forecastJson.getJSONArray("list");

        // The API returns forecasts in 3-hour intervals
        // We'll pick one forecast per day (around noon) to display
        String currentDate = "";

        for (int i = 0; i < listArray.length(); i++) {
            JSONObject forecastObject = listArray.getJSONObject(i);

            // Get date from timestamp
            long timestamp = forecastObject.getLong("dt") * 1000;
            Date date = new Date(timestamp);
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String forecastDate = dayFormat.format(date);

            // Get time to check if it's around noon (12:00)
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String forecastTime = timeFormat.format(date);

            // Skip if we already added a forecast for this date unless it's closer to noon
            if (currentDate.equals(forecastDate) && !forecastTime.startsWith("12")) {
                continue;
            }

            // Get main weather data
            JSONObject main = forecastObject.getJSONObject("main");
            double temp = main.getDouble("temp") - 273.15; // Convert from Kelvin to Celsius

            // Get weather condition
            JSONArray weatherArray = forecastObject.getJSONArray("weather");
            String condition = "";
            String icon = "";
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                condition = weatherObj.getString("main");
                icon = weatherObj.getString("icon");
            }

            // Format the date to display
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            String displayDate = displayFormat.format(date);

            // Only add if this is a new date or a better time for the existing date
            if (!currentDate.equals(forecastDate) || forecastTime.startsWith("12")) {
                // Create forecast item
                ForecastItem item = new ForecastItem(displayDate, temp, condition, icon);
                forecastItems.add(item);
                currentDate = forecastDate;
            }

            // Limit to 5 days
            if (forecastItems.size() >= 5) {
                break;
            }
        }

        return forecastItems;
    }

    private void updateWeatherUI(String city, double temperature, String condition,
                                 int humidity, double windSpeed, int pressure) {
        try {
            // Update all TextViews with weather data
            cityNameText.setText(city);
            temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", temperature));
            weatherConditionText.setText(condition);

            humidityText.setText(String.format(Locale.getDefault(), "%d%%", humidity));
            windSpeedText.setText(String.format(Locale.getDefault(), "%.1f km/h", windSpeed));
            pressureText.setText(String.format(Locale.getDefault(), "%d hPa", pressure));

            // Show the weather card with animation
            if (weatherCardView.getVisibility() != View.VISIBLE) {
                weatherCardView.setAlpha(0f);
                weatherCardView.setVisibility(View.VISIBLE);
                weatherCardView.animate()
                        .alpha(1f)
                        .translationYBy(50)
                        .translationY(0)
                        .setDuration(500)
                        .setListener(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            showError("Error displaying weather data");
        }
    }

    private void updateForecastUI(List<ForecastItem> forecastItems) {
        if (forecastItems.isEmpty()) {
            forecastCardView.setVisibility(View.GONE);
            return;
        }

        // Update the RecyclerView adapter with forecast data
        forecastAdapter.updateForecastItems(forecastItems);

        // Show the forecast card with animation
        if (forecastCardView.getVisibility() != View.VISIBLE) {
            forecastCardView.setAlpha(0f);
            forecastCardView.setVisibility(View.VISIBLE);
            forecastCardView.animate()
                    .alpha(1f)
                    .translationYBy(50)
                    .translationY(0)
                    .setDuration(500)
                    .setStartDelay(100)  // Start slightly after weather card animation
                    .setListener(null);
        }
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Hide the loading indicator in case it's still visible
        loadingIndicator.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mark the activity as inactive
        isActive.set(false);
        // Shutdown executor service to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Model class for forecast items
    public static class ForecastItem {
        private final String day;
        private final double temperature;
        private final String condition;
        private final String iconCode;

        public ForecastItem(String day, double temperature, String condition, String iconCode) {
            this.day = day;
            this.temperature = temperature;
            this.condition = condition;
            this.iconCode = iconCode;
        }

        public String getDay() {
            return day;
        }

        public double getTemperature() {
            return temperature;
        }

        public String getCondition() {
            return condition;
        }

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        }
    }

    // RecyclerView adapter for forecast items
    public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {
        private List<ForecastItem> forecastItems;

        public ForecastAdapter(List<ForecastItem> forecastItems) {
            this.forecastItems = forecastItems;
        }

        public void updateForecastItems(List<ForecastItem> newItems) {
            this.forecastItems = newItems;
            notifyDataSetChanged();
        }

        @Override
        public ForecastViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_forecast, parent, false);
            return new ForecastViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ForecastViewHolder holder, int position) {
            ForecastItem item = forecastItems.get(position);
            holder.dayText.setText(item.getDay());
            holder.temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", item.getTemperature()));
            holder.conditionText.setText(item.getCondition());

            // Load weather icon using Picasso
            Picasso.get()
                    .load(item.getIconUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.iconImageView);
        }

        @Override
        public int getItemCount() {
            return forecastItems.size();
        }

        class ForecastViewHolder extends RecyclerView.ViewHolder {
            TextView dayText;
            TextView temperatureText;
            TextView conditionText;
            ImageView iconImageView;

            public ForecastViewHolder(View itemView) {
                super(itemView);
                dayText = itemView.findViewById(R.id.forecastDay);
                temperatureText = itemView.findViewById(R.id.forecastTemperature);
                conditionText = itemView.findViewById(R.id.forecastCondition);
                iconImageView = itemView.findViewById(R.id.forecastIcon);
            }
        }
    }
}