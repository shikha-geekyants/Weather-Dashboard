# Weather Dashboard App

A modern Android weather dashboard application that displays current weather information with automatic refresh functionality.

## Overview

This application provides real-time weather information for multiple cities with automatic data refresh every 30 seconds. The app is built using modern Android development practices including MVVM architecture, Jetpack Compose, and Kotlin Coroutines.

## Architecture

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern with the following layers:

### Data Layer
- **API Service**: Retrofit-based service for communicating with OpenWeatherMap API
- **Repository**: Handles data fetching and error handling
- **Data Models**: API response models (WeatherResponse, Main, Weather, Wind)

### Domain Layer
- **Domain Models**: Business logic models (WeatherData, WeatherError)
- Clean separation between data and domain layers

### Presentation Layer
- **ViewModel**: Manages UI state and business logic using StateFlow
- **Compose UI**: Modern declarative UI built with Jetpack Compose
- **State Management**: Reactive state management using Kotlin Flows

### Dependency Injection
- **Hilt**: Used for dependency injection throughout the app
- **Modules**: NetworkModule and RepositoryModule for providing dependencies

## Key Features

### 1. Weather Display Screen
- Displays comprehensive weather information:
  - City name
  - Temperature (in Celsius)
  - Weather condition (e.g., Clear, Clouds, Rain)
  - Weather description
  - Humidity percentage
  - Wind speed (m/s)
  - Last updated timestamp

### 2. Auto-Refresh Functionality
- Automatically refreshes weather data every 30 seconds
- Visual indicator shows when data is being refreshed
- Displays last updated timestamp
- Pauses when app goes to background, resumes when app comes to foreground
- Each city maintains its own auto-refresh cycle

### 3. Manual Refresh
- Refresh button in the app bar
- Button is disabled during active refresh operations
- Shows loading indicator when refreshing

### 4. City Selection
- Three pre-defined cities: London, New York, Tokyo
- Tab-based navigation for switching between cities
- Each city maintains its own weather data and refresh cycle

### 5. Error Handling
- Graceful handling of network errors
- User-friendly error messages
- Retry functionality for failed requests
- Error state is clearly displayed to users

## Technical Specifications

### Technology Stack
- **MVVM Architecture**: Clean separation of concerns
- **Kotlin Flows**: Reactive state management
- **Coroutines**: Asynchronous operations
- **Hilt**: Dependency injection
- **Retrofit**: HTTP client for API calls
- **Jetpack Compose**: Modern UI framework
- **Material Design 3**: Modern UI components

### API Integration
- **OpenWeatherMap API**: Free tier (60 calls per minute)
- **Endpoint**: Current Weather API
- **Units**: Metric (Celsius, m/s)

### Android Lifecycle Handling
- **Screen Rotation**: State is preserved using ViewModel
- **Background/Foreground**: Auto-refresh pauses/resumes appropriately
- **Memory Management**: Proper cleanup of coroutines and resources
- **Configuration Changes**: Handled automatically by ViewModel

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24 or higher
- Kotlin 1.9.20 or later

### API Key Configuration
1. Sign up for a free API key at [OpenWeatherMap](https://openweathermap.org/api)
2. Open `app/src/main/java/com/weather/dashboard/di/NetworkModule.kt`
3. Replace `"YOUR_API_KEY_HERE"` with your actual API key in the `provideApiKey()` function

```kotlin
@Provides
@Named("api_key")
fun provideApiKey(): String {
    return "YOUR_ACTUAL_API_KEY"
}
```

### Building the App
1. Clone or extract the project
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on an emulator or physical device

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/weather/dashboard/
│   │   │   ├── data/
│   │   │   │   ├── api/          # API service interfaces
│   │   │   │   ├── model/        # Data models (API responses)
│   │   │   │   └── repository/   # Data repositories
│   │   │   ├── domain/
│   │   │   │   └── model/        # Domain models
│   │   │   ├── di/               # Dependency injection modules
│   │   │   ├── ui/
│   │   │   │   ├── components/   # Reusable UI components
│   │   │   │   ├── screen/       # Screen composables
│   │   │   │   ├── state/        # UI state models
│   │   │   │   ├── theme/        # App theme
│   │   │   │   └── viewmodel/    # ViewModels
│   │   │   ├── MainActivity.kt
│   │   │   └── WeatherApplication.kt
│   │   └── res/                  # Resources
```

## Assumptions

1. **API Key**: The API key is provided in the NetworkModule. For production, this should be stored securely (e.g., using BuildConfig or a secure storage solution).

2. **Network Availability**: The app assumes network connectivity is available. Error handling covers network failures gracefully.

3. **City Names**: The app uses English city names that match OpenWeatherMap's city name format. City names are case-sensitive and must match exactly.

4. **Auto-Refresh Behavior**: 
   - Auto-refresh pauses when the app goes to background to save battery
   - Auto-refresh resumes when the app comes to foreground
   - Each city switch restarts the auto-refresh cycle

5. **Screen Rotation**: The app handles configuration changes (including rotation) automatically through ViewModel lifecycle.

## Known Limitations & Trade-offs

1. **API Rate Limiting**: 
   - Free tier allows 60 calls per minute
   - With 3 cities refreshing every 30 seconds, this is within limits (6 calls per minute)
   - If more cities are added, rate limiting may become an issue

2. **Battery Consumption**:
   - Auto-refresh every 30 seconds may impact battery life
   - Trade-off: More frequent updates vs. battery efficiency
   - Mitigation: Auto-refresh pauses when app is in background

3. **Network Usage**:
   - Continuous API calls consume network bandwidth
   - No offline caching implemented (could be added for better UX)

4. **Error Recovery**:
   - Network errors require manual retry
   - No automatic retry with exponential backoff (could be added)

5. **City Selection**:
   - Limited to 3 pre-defined cities
   - No search functionality for custom cities
   - Could be extended to support user-defined cities

6. **Data Persistence**:
   - No local database for caching weather data
   - Data is lost when app is closed
   - Could be improved with Room database

7. **UI/UX**:
   - Basic Material Design 3 implementation
   - Could be enhanced with animations and transitions
   - Weather icons could be added for better visual representation

## Future Enhancements

- Add weather icons based on conditions
- Implement offline caching with Room database
- Add support for more cities with search functionality
- Add weather forecast (5-day forecast)
- Implement automatic retry with exponential backoff
- Add pull-to-refresh gesture
- Add weather condition animations
- Support for different temperature units (Fahrenheit)
- Add location-based weather detection

## Testing

The app includes basic test structure. For production, consider adding:
- Unit tests for ViewModel
- Repository tests with mock API
- UI tests for Compose screens
- Integration tests

## License

This project is created for demonstration purposes.

## Contact

For questions or issues, please refer to the project repository.

