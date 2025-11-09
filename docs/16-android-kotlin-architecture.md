# Android Architecture (Kotlin + Jetpack Compose)

## Table of Contents
- [Project Structure](#project-structure)
- [Architecture Pattern](#architecture-pattern)
- [Dependency Injection](#dependency-injection)
- [Networking](#networking)
- [State Management](#state-management)
- [Navigation](#navigation)
- [Image Loading](#image-loading)
- [Local Storage](#local-storage)
- [Real-time Communication](#real-time-communication)
- [Performance Optimization](#performance-optimization)

---

## Project Structure

### Modern Android App Structure

```
dating-app-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/datingapp/
│   │   │   │   ├── di/                    # Dependency Injection
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   │
│   │   │   │   ├── data/                  # Data Layer
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   │   ├── AuthApi.kt
│   │   │   │   │   │   │   ├── UserApi.kt
│   │   │   │   │   │   │   └── MatchApi.kt
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── LoginRequest.kt
│   │   │   │   │   │   │   ├── UserDto.kt
│   │   │   │   │   │   │   └── MatchDto.kt
│   │   │   │   │   │   └── interceptor/
│   │   │   │   │   │       └── AuthInterceptor.kt
│   │   │   │   │   │
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   │   │   └── MatchDao.kt
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   ├── UserEntity.kt
│   │   │   │   │   │   │   └── MatchEntity.kt
│   │   │   │   │   │   └── AppDatabase.kt
│   │   │   │   │   │
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   │   └── MatchRepository.kt
│   │   │   │   │   │
│   │   │   │   │   └── websocket/
│   │   │   │   │       └── SocketManager.kt
│   │   │   │   │
│   │   │   │   ├── domain/                # Domain Layer
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   ├── Match.kt
│   │   │   │   │   │   └── Message.kt
│   │   │   │   │   │
│   │   │   │   │   ├── usecase/
│   │   │   │   │   │   ├── auth/
│   │   │   │   │   │   │   ├── LoginUseCase.kt
│   │   │   │   │   │   │   └── RegisterUseCase.kt
│   │   │   │   │   │   ├── match/
│   │   │   │   │   │   │   ├── GetMatchesUseCase.kt
│   │   │   │   │   │   │   └── SwipeUseCase.kt
│   │   │   │   │   │   └── messaging/
│   │   │   │   │   │       ├── SendMessageUseCase.kt
│   │   │   │   │   │       └── GetMessagesUseCase.kt
│   │   │   │   │   │
│   │   │   │   │   └── repository/       # Repository interfaces
│   │   │   │   │       ├── IAuthRepository.kt
│   │   │   │   │       └── IUserRepository.kt
│   │   │   │   │
│   │   │   │   ├── presentation/         # Presentation Layer
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   ├── RegisterScreen.kt
│   │   │   │   │   │   └── AuthViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── profile/
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   ├── EditProfileScreen.kt
│   │   │   │   │   │   └── ProfileViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── discover/
│   │   │   │   │   │   ├── DiscoverScreen.kt
│   │   │   │   │   │   ├── CardStack.kt
│   │   │   │   │   │   └── DiscoverViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── matches/
│   │   │   │   │   │   ├── MatchesScreen.kt
│   │   │   │   │   │   └── MatchesViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── messaging/
│   │   │   │   │   │   ├── ConversationScreen.kt
│   │   │   │   │   │   ├── MessageItem.kt
│   │   │   │   │   │   └── MessagingViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   │   └── Screen.kt
│   │   │   │   │   │
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   │
│   │   │   │   ├── util/                 # Utilities
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   ├── Extensions.kt
│   │   │   │   │   └── DateUtils.kt
│   │   │   │   │
│   │   │   │   └── DatingApp.kt         # Application class
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml/
│   │   │   │       └── network_security_config.xml
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/                         # Unit tests
│   │       └── java/com/datingapp/
│   │           ├── viewmodel/
│   │           │   └── AuthViewModelTest.kt
│   │           └── repository/
│   │               └── AuthRepositoryTest.kt
│   │
│   └── build.gradle.kts
│
├── build.gradle.kts
└── settings.gradle.kts
```

### Build Configuration

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.datingapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.datingapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://api.datingapp.com\"")
        buildConfigField("String", "WEBSOCKET_URL", "\"wss://ws.datingapp.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit for Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room for Local Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // DataStore for Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Socket.IO for WebSocket
    implementation("io.socket:socket.io-client:2.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Accompanist (Compose utilities)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-pager:0.34.0")

    // Timber for Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## Architecture Pattern

### MVVM (Model-View-ViewModel) with Clean Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                          │
│  ┌──────────────┐           ┌──────────────┐                   │
│  │   Composable │ observes  │  ViewModel   │                   │
│  │   Screens    │◄──────────│    (State)   │                   │
│  └──────────────┘           └──────────────┘                   │
└────────────────────────────────┬────────────────────────────────┘
                                 │ calls
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Domain Layer                               │
│  ┌──────────────┐           ┌──────────────┐                   │
│  │   Use Cases  │   uses    │    Models    │                   │
│  │              │──────────►│              │                   │
│  └──────────────┘           └──────────────┘                   │
└────────────────────────────────┬────────────────────────────────┘
                                 │ calls
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                               │
│  ┌──────────────┐           ┌──────────────┐                   │
│  │ Repositories │   uses    │  Data Sources│                   │
│  │              │──────────►│ (Remote/Local)│                   │
│  └──────────────┘           └──────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

### Domain Layer (Use Cases)

```kotlin
// domain/usecase/auth/LoginUseCase.kt
package com.datingapp.domain.usecase.auth

import com.datingapp.domain.model.User
import com.datingapp.domain.repository.IAuthRepository
import com.datingapp.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    operator fun invoke(email: String, password: String): Flow<Resource<User>> {
        return authRepository.login(email, password)
    }
}

// domain/usecase/match/SwipeUseCase.kt
package com.datingapp.domain.usecase.match

import com.datingapp.domain.model.SwipeResult
import com.datingapp.domain.repository.IMatchRepository
import com.datingapp.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SwipeUseCase @Inject constructor(
    private val matchRepository: IMatchRepository
) {
    operator fun invoke(
        targetUserId: String,
        action: SwipeAction
    ): Flow<Resource<SwipeResult>> {
        return matchRepository.swipe(targetUserId, action)
    }
}

enum class SwipeAction {
    LIKE, DISLIKE, SUPER_LIKE
}

// util/Resource.kt
package com.datingapp.util

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
```

### Presentation Layer (ViewModel)

```kotlin
// presentation/auth/AuthViewModel.kt
package com.datingapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datingapp.domain.usecase.auth.LoginUseCase
import com.datingapp.domain.usecase.auth.RegisterUseCase
import com.datingapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginUseCase(email, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                user = resource.data,
                                error = null
                            )
                        }
                        Timber.d("Login successful for user: ${resource.data?.email}")
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                        Timber.e("Login failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun register(
        email: String,
        password: String,
        firstName: String,
        dateOfBirth: String,
        gender: String
    ) {
        viewModelScope.launch {
            registerUseCase(email, password, firstName, dateOfBirth, gender).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                user = resource.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

### Presentation Layer (Composable Screen)

```kotlin
// presentation/auth/LoginScreen.kt
package com.datingapp.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Navigate on successful login
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.login(email, password) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Don't have an account? Sign up")
            }
        }
    }
}
```

---

## Dependency Injection

### Hilt Configuration

```kotlin
// DatingApp.kt
package com.datingapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class DatingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

// di/AppModule.kt
package com.datingapp.di

import android.content.Context
import androidx.room.Room
import com.datingapp.data.local.AppDatabase
import com.datingapp.data.remote.api.AuthApi
import com.datingapp.data.remote.api.UserApi
import com.datingapp.data.remote.api.MatchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dating_app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase) = database.userDao()

    @Provides
    @Singleton
    fun provideMatchDao(database: AppDatabase) = database.matchDao()
}

// di/NetworkModule.kt
package com.datingapp.di

import com.datingapp.BuildConfig
import com.datingapp.data.remote.api.AuthApi
import com.datingapp.data.remote.api.MatchApi
import com.datingapp.data.remote.api.UserApi
import com.datingapp.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMatchApi(retrofit: Retrofit): MatchApi {
        return retrofit.create(MatchApi::class.java)
    }
}

// di/RepositoryModule.kt
package com.datingapp.di

import com.datingapp.data.repository.AuthRepositoryImpl
import com.datingapp.data.repository.MatchRepositoryImpl
import com.datingapp.data.repository.UserRepositoryImpl
import com.datingapp.domain.repository.IAuthRepository
import com.datingapp.domain.repository.IMatchRepository
import com.datingapp.domain.repository.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        matchRepositoryImpl: MatchRepositoryImpl
    ): IMatchRepository
}
```

---

## Networking

### Retrofit API Interfaces

```kotlin
// data/remote/api/AuthApi.kt
package com.datingapp.data.remote.api

import com.datingapp.data.remote.dto.LoginRequest
import com.datingapp.data.remote.dto.LoginResponse
import com.datingapp.data.remote.dto.RegisterRequest
import com.datingapp.data.remote.dto.RefreshTokenRequest
import com.datingapp.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}

// data/remote/api/MatchApi.kt
package com.datingapp.data.remote.api

import com.datingapp.data.remote.dto.SwipeRequest
import com.datingapp.data.remote.dto.SwipeResponse
import com.datingapp.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.*

interface MatchApi {

    @GET("matches/candidates")
    suspend fun getMatchCandidates(
        @Query("limit") limit: Int = 50
    ): Response<List<UserDto>>

    @POST("swipes")
    suspend fun swipe(@Body request: SwipeRequest): Response<SwipeResponse>

    @GET("matches")
    suspend fun getMatches(): Response<List<MatchDto>>
}
```

### Auth Interceptor (Auto Token Refresh)

```kotlin
// data/remote/interceptor/AuthInterceptor.kt
package com.datingapp.data.remote.interceptor

import com.datingapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login/register endpoints
        if (originalRequest.url.encodedPath.contains("/auth/login") ||
            originalRequest.url.encodedPath.contains("/auth/register")
        ) {
            return chain.proceed(originalRequest)
        }

        // Add access token to request
        val accessToken = runBlocking { tokenManager.getAccessToken() }
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        var response = chain.proceed(authenticatedRequest)

        // If 401 Unauthorized, try to refresh token
        if (response.code == 401) {
            Timber.d("Received 401, attempting token refresh")
            response.close()

            synchronized(this) {
                val refreshToken = runBlocking { tokenManager.getRefreshToken() }

                if (refreshToken != null) {
                    val newAccessToken = runBlocking {
                        refreshAccessToken(refreshToken)
                    }

                    if (newAccessToken != null) {
                        // Retry original request with new token
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()

                        response = chain.proceed(newRequest)
                    } else {
                        // Refresh failed, clear tokens and redirect to login
                        runBlocking { tokenManager.clearTokens() }
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshAccessToken(refreshToken: String): String? {
        // Call refresh token endpoint
        // This would need a separate Retrofit instance without this interceptor
        // to avoid infinite loop
        return try {
            // Implementation depends on your auth API
            null
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            null
        }
    }
}
```

### Repository Implementation

```kotlin
// data/repository/AuthRepositoryImpl.kt
package com.datingapp.data.repository

import com.datingapp.data.local.TokenManager
import com.datingapp.data.remote.api.AuthApi
import com.datingapp.data.remote.dto.LoginRequest
import com.datingapp.data.remote.dto.RegisterRequest
import com.datingapp.domain.model.User
import com.datingapp.domain.repository.IAuthRepository
import com.datingapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : IAuthRepository {

    override fun login(email: String, password: String): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())

            val response = authApi.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                // Save tokens
                tokenManager.saveAccessToken(loginResponse.accessToken)
                tokenManager.saveRefreshToken(loginResponse.refreshToken)

                // Map DTO to domain model
                val user = loginResponse.user.toDomainModel()

                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login exception")
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    override fun register(
        email: String,
        password: String,
        firstName: String,
        dateOfBirth: String,
        gender: String
    ): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())

            val request = RegisterRequest(email, password, firstName, dateOfBirth, gender)
            val response = authApi.register(request)

            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!

                tokenManager.saveAccessToken(registerResponse.accessToken)
                tokenManager.saveRefreshToken(registerResponse.refreshToken)

                val user = registerResponse.user.toDomainModel()

                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Registration exception")
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (e: Exception) {
            Timber.e(e, "Logout API call failed")
        } finally {
            tokenManager.clearTokens()
        }
    }
}
```

---

## State Management

### Token Manager (DataStore)

```kotlin
// data/local/TokenManager.kt
package com.datingapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ACCESS_TOKEN] = token
        }
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REFRESH_TOKEN] = token
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.ACCESS_TOKEN]
        }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.REFRESH_TOKEN]
        }.first()
    }

    fun getAccessTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.ACCESS_TOKEN]
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.ACCESS_TOKEN)
            preferences.remove(Keys.REFRESH_TOKEN)
        }
    }
}
```

---

## Navigation

### Compose Navigation

```kotlin
// presentation/navigation/Screen.kt
package com.datingapp.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Discover : Screen("discover")
    object Matches : Screen("matches")
    object Profile : Screen("profile")
    object Conversation : Screen("conversation/{matchId}") {
        fun createRoute(matchId: String) = "conversation/$matchId"
    }
}

// presentation/navigation/AppNavigation.kt
package com.datingapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.datingapp.presentation.auth.LoginScreen
import com.datingapp.presentation.auth.RegisterScreen
import com.datingapp.presentation.discover.DiscoverScreen
import com.datingapp.presentation.matches.MatchesScreen
import com.datingapp.presentation.messaging.ConversationScreen
import com.datingapp.presentation.profile.ProfileScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Discover.route) {
            DiscoverScreen(
                onNavigateToMatches = {
                    navController.navigate(Screen.Matches.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Matches.route) {
            MatchesScreen(
                onNavigateToConversation = { matchId ->
                    navController.navigate(Screen.Conversation.createRoute(matchId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Conversation.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId")!!
            ConversationScreen(
                matchId = matchId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

---

## Image Loading

### Coil Configuration

```kotlin
// presentation/discover/ProfileCard.kt
package com.datingapp.presentation.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.datingapp.domain.model.User

@Composable
fun ProfileCard(
    user: User,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photos.firstOrNull()?.url)
                    .crossfade(true)
                    .scale(Scale.FILL)
                    .transformations(RoundedCornersTransformation(16f))
                    .build(),
                contentDescription = "Profile photo of ${user.firstName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay and user info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "${user.firstName}, ${user.age}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (user.bio != null) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
```

---

## Local Storage

### Room Database

```kotlin
// data/local/AppDatabase.kt
package com.datingapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.datingapp.data.local.dao.MatchDao
import com.datingapp.data.local.dao.UserDao
import com.datingapp.data.local.entity.MatchEntity
import com.datingapp.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, MatchEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchDao(): MatchDao
}

// data/local/entity/UserEntity.kt
package com.datingapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val firstName: String,
    val lastName: String?,
    val age: Int,
    val bio: String?,
    val photoUrl: String?,
    val distance: Double?
)

// data/local/dao/UserDao.kt
package com.datingapp.data.local.dao

import androidx.room.*
import com.datingapp.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
```

---

## Real-time Communication

### Socket.IO Manager

```kotlin
// data/websocket/SocketManager.kt
package com.datingapp.data.websocket

import com.datingapp.BuildConfig
import com.datingapp.data.local.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    private var socket: Socket? = null

    private val _messageFlow = MutableSharedFlow<Message>()
    val messageFlow: SharedFlow<Message> = _messageFlow.asSharedFlow()

    private val _typingFlow = MutableSharedFlow<TypingEvent>()
    val typingFlow: SharedFlow<TypingEvent> = _typingFlow.asSharedFlow()

    fun connect() {
        if (socket?.connected() == true) {
            Timber.d("Socket already connected")
            return
        }

        val token = runBlocking { tokenManager.getAccessToken() }

        val options = IO.Options().apply {
            auth = mapOf("token" to token)
            reconnection = true
            reconnectionDelay = 1000
            reconnectionDelayMax = 5000
        }

        socket = IO.socket(BuildConfig.WEBSOCKET_URL, options)

        socket?.on(Socket.EVENT_CONNECT) {
            Timber.d("Socket connected")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Timber.d("Socket disconnected")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Timber.e("Socket connection error: ${args.firstOrNull()}")
        }

        socket?.on("new_message") { args ->
            val data = args.firstOrNull() as? JSONObject
            data?.let {
                val message = parseMessage(it)
                runBlocking { _messageFlow.emit(message) }
            }
        }

        socket?.on("user_typing") { args ->
            val data = args.firstOrNull() as? JSONObject
            data?.let {
                val event = parseTypingEvent(it)
                runBlocking { _typingFlow.emit(event) }
            }
        }

        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun sendMessage(matchId: String, content: String) {
        val data = JSONObject().apply {
            put("matchId", matchId)
            put("content", content)
        }
        socket?.emit("send_message", data)
    }

    fun sendTyping(matchId: String) {
        val data = JSONObject().apply {
            put("matchId", matchId)
        }
        socket?.emit("typing", data)
    }

    private fun parseMessage(json: JSONObject): Message {
        return Message(
            id = json.getString("id"),
            matchId = json.getString("matchId"),
            senderId = json.getString("senderId"),
            content = json.getString("content"),
            timestamp = json.getLong("timestamp")
        )
    }

    private fun parseTypingEvent(json: JSONObject): TypingEvent {
        return TypingEvent(
            userId = json.getString("userId"),
            matchId = json.getString("matchId")
        )
    }
}

data class Message(
    val id: String,
    val matchId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long
)

data class TypingEvent(
    val userId: String,
    val matchId: String
)
```

---

## Performance Optimization

### LazyColumn with Key

```kotlin
@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true, // Latest message at bottom
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id } // Crucial for performance
        ) { message ->
            MessageItem(message = message)
        }
    }
}
```

### Remember and Derivations

```kotlin
@Composable
fun ExpensiveComposable(users: List<User>) {
    // Expensive calculation - only recalculate when users change
    val sortedUsers = remember(users) {
        users.sortedByDescending { it.compatibilityScore }
    }

    LazyColumn {
        items(sortedUsers, key = { it.id }) { user ->
            UserItem(user)
        }
    }
}
```

### Image Caching

```kotlin
// di/AppModule.kt
@Provides
@Singleton
fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25) // Use 25% of app's memory
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                .build()
        }
        .build()
}
```

---

## Summary

This Android architecture provides:

✅ **Clean Architecture** with clear separation of concerns
✅ **MVVM pattern** with Jetpack Compose
✅ **Hilt** for dependency injection
✅ **Retrofit** for networking with auto token refresh
✅ **Room** for local database
✅ **DataStore** for preferences
✅ **Coil** for efficient image loading
✅ **Socket.IO** for real-time messaging
✅ **Coroutines & Flow** for async operations
✅ **Navigation Component** for Compose
✅ **Material 3** design system

**Next:** Combine with Java backend (`14-java-backend-architecture.md`) and hybrid setup (`15-hybrid-architecture-java-nodejs.md`) for complete solution.
