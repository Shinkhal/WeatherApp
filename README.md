# 🌦️ WeatherApp

An Android application that provides real-time weather updates for your current location or any city worldwide. Built with Kotlin, this app fetches data from the OpenWeatherMap API to display current weather conditions, temperature, humidity, and more.

## 📱 Features

- 🌍 Detects your current location automatically
- 🔍 Search for weather information by city name
- 🌡️ Displays temperature, humidity, and weather conditions
- 💨 Shows wind speed and atmospheric pressure
- 🕒 Provides local time of the searched location
- 🎨 Clean and intuitive user interface

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Framework:** Android SDK
- **Build System:** Gradle
- **API:** OpenWeatherMap API
- **Architecture:** MVVM (Model-View-ViewModel)

## 🚀 Getting Started

### Prerequisites

- Android Studio Bumblebee or later
- Android SDK 31 or higher
- OpenWeatherMap API Key (Sign up at [OpenWeatherMap](https://openweathermap.org/api))

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Shinkhal/WeatherApp.git
   ````

2. **Open the project in Android Studio.**
3. **Add your OpenWeatherMap API key:**

   * Navigate to `app/src/main/java/com/yourpackage/utils/Constants.kt`
   * Replace `YOUR_API_KEY` with your actual API key:

     ```kotlin
     const val API_KEY = "YOUR_API_KEY"
     ```
4. **Build and run the app on an emulator or physical device.**


## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any enhancements or bug fixes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ Acknowledgements

* [OpenWeatherMap](https://openweathermap.org/) for providing the weather data API.


