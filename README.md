# StoryHive 📚✨

## Overview
StoryHive is a social reading platform that allows book enthusiasts to share their reading experiences, discover new books, and connect with fellow readers. The app provides a comprehensive platform for users to post book reviews, explore new titles, and engage with a community of readers.

## Features 🌟

### User Authentication
- Secure sign-up and login using Firebase Authentication
- Profile creation with optional profile picture
- Email-based authentication

### Book Discovery
- Search books using the Google Books API
- Detailed book information view
- Browse and explore a wide range of books

### Social Interactions
- Create and share book reviews
- Like and comment on posts
- Edit and delete personal posts
- View other users' posts and profiles

### Offline Support
- Local caching of posts, books, and images
- Seamless offline experience with data synchronization

### Additional Functionalities
- Image caching and optimization
- Network connectivity monitoring
- User profile management

## Tech Stack 🛠️

### Android Development
- Kotlin
- Android Jetpack Components
- Navigation Component
- ViewModel & LiveData
- Room Database
- Coroutines

### Backend & Services
- Firebase Authentication
- Firebase Firestore
- Google Books API

### Libraries
- Retrofit
- Picasso (Image Loading)
- Glide
- Gson
- Room Persistence
- Firebase Storage

## Architecture 🏗️
The app follows the MVVM (Model-View-ViewModel) architecture with clean code principles:
- Separation of concerns
- Dependency injection
- Reactive programming with Kotlin Flow
- Repository pattern for data management

## Setup and Installation 🔧

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or later
- JDK 11
- Android SDK 31+

### Clone the Repository
```bash
git clone https://github.com/yourusername/StoryHive.git
```

### Firebase Configuration
1. Create a Firebase project
2. Add an Android app to your Firebase project
3. Download `google-services.json`
4. Place `google-services.json` in the `app/` directory

### Google Books API
1. Get an API key from the Google Cloud Console
2. Add the API key to your `local.properties` file:
```
GOOGLE_BOOKS_API_KEY=your_api_key_here
```

### Dependencies
All dependencies are managed through Gradle. Sync the project in Android Studio to download required libraries.

## Environment Variables 🔐
Create a `local.properties` file in the project root with:
```
GOOGLE_BOOKS_API_KEY=your_google_books_api_key
```

### Running Tests
```bash
./gradlew test         # Run unit tests
./gradlew connectedTest  # Run instrumented tests
```

## Contributing 🤝
1. Fork the repository
2. Create a new branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Permissions 📋
The app requires the following permissions:
- Internet Access
- Camera
- Storage Read/Write
- Network State

## Troubleshooting 🛠
- Ensure you have the latest version of Android Studio
- Check Firebase configuration
- Verify API keys
- Ensure proper internet connectivity

## License 📄
Distributed under the MIT License. See `LICENSE` for more information.

## Contact 📧
Project Link: https://github.com/ChenKraitenberg/StoryHive

---

**Happy Reading! 📖**
