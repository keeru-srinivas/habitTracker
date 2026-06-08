# CozyTrack Android Frontend Guide

This guide explains the current Android frontend implementation for the Habit Tracker API from the beginning: Gradle setup, dependencies, clean architecture, API calls, token storage, navigation, ViewModels, habit check-ins, streaks, heatmap UI, and the cozy bear login/signup/home screens.

The app follows one important backend rule: Android never talks directly to Firebase. The app only calls the HTTP API:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- protected routes with `Authorization: Bearer <accessToken>`

## 1. What We Built

CozyTrack is a Kotlin + Jetpack Compose Android app that lets a user:

- Sign up with name, email, and password.
- Log in with email and password.
- Store the returned backend access token locally.
- Automatically attach that token to protected API requests.
- Create daily or weekly habits.
- Load only the habits owned by the logged-in user.
- Check in for the current server UTC day.
- Show today's completed state on the home page.
- Open a habit detail page to see that habit's 365-day read-only heatmap.
- Show current and best streaks per habit, not globally.
- Edit, archive, restore, and delete habits.
- Log out and clear the saved session.
- Return to login automatically when the backend says the token expired.

The final UI uses a cozy bear theme with warm cream/brown colors, rounded cards, teddy/paw emojis, and separate login/signup/home screens.

## 2. Backend Rules Used By The App

The app follows these rules from the Swagger API:

```text
POST /api/auth/signup
POST /api/auth/login
GET  /api/clock
GET  /api/me
GET  /api/thought
GET  /api/users/{userId}/habits
POST /api/habits
GET  /api/habits/{habitId}
PUT  /api/habits/{habitId}
DELETE /api/habits/{habitId}
POST /api/habits/check
GET  /api/users/{userId}/entries/{checkDate}
GET  /api/habits/{habitId}/entries
GET  /api/habits/{habitId}/progress
```

Important backend behavior:

- Normal check-ins use `POST /api/habits/check`.
- The app sends only `habitId` and `completed`.
- The backend assigns the UTC day.
- The app uses `GET /api/clock` for the current authoritative UTC date.
- Daily habits use daily streak rules.
- Weekly habits use ISO-week rules from the backend.
- User-specific routes require the stored `userId`.
- The backend verifies the Bearer token, so another user cannot load someone else's habits.

## 3. Project Structure

The implementation is organized like this:

```text
app/src/main/java/com/example/cozytrack/
  CozyTrackApplication.kt
  MainActivity.kt

  core/
    di/AppContainer.kt
    network/ApiResult.kt
    network/AuthInterceptor.kt
    session/SessionManager.kt

  data/
    remote/HabitTrackerApi.kt
    remote/dto/AuthDtos.kt
    remote/dto/ClockDtos.kt
    remote/dto/HabitDtos.kt
    repository/AuthRepositoryImpl.kt
    repository/HabitRepositoryImpl.kt
    repository/ThoughtRepositoryImpl.kt

  domain/
    model/AuthSession.kt
    model/Frequency.kt
    model/Habit.kt
    model/HabitEntry.kt
    model/HabitProgress.kt
    model/ServerClock.kt
    model/Thought.kt
    repository/AuthRepository.kt
    repository/HabitRepository.kt
    repository/ThoughtRepository.kt
    usecase/auth/GetCurrentUserNameUseCase.kt
    usecase/auth/LoginUseCase.kt
    usecase/auth/LogoutUseCase.kt
    usecase/auth/SignUpUseCase.kt
    usecase/habit/CheckHabitUseCase.kt
    usecase/habit/CreateHabitUseCase.kt
    usecase/habit/DeleteHabitUseCase.kt
    usecase/habit/GetEntriesForDayUseCase.kt
    usecase/habit/GetHabitEntriesUseCase.kt
    usecase/habit/GetHabitProgressUseCase.kt
    usecase/habit/GetHabitsUseCase.kt
    usecase/habit/GetServerClockUseCase.kt
    usecase/habit/UpdateHabitUseCase.kt
    usecase/thought/GetThoughtUseCase.kt

  presentation/
    auth/LoginScreen.kt
    auth/LoginViewModel.kt
    auth/SignUpScreen.kt
    auth/SignUpViewModel.kt
    habits/HabitListScreen.kt
    habits/HabitListViewModel.kt
    navigation/AppNavHost.kt
    navigation/Routes.kt
```

Why this structure exists:

- `core` contains app-wide infrastructure.
- `data` knows Retrofit, DTOs, API response formats, and remote repositories.
- `domain` contains pure app models, repository contracts, and use cases.
- `presentation` contains Compose UI and ViewModels.

This is clean architecture with MVVM:

```text
Compose Screen
  -> ViewModel
  -> UseCase
  -> Repository interface
  -> Repository implementation
  -> Retrofit API
  -> Backend
```

## 4. Version Catalog: `gradle/libs.versions.toml`

The project already had Android, Compose, Activity Compose, Core KTX, Lifecycle Runtime KTX, and testing dependencies. These are the extra versions and dependencies added for this app.

Current added versions:

```toml
navigationCompose = "2.9.8"
retrofit = "2.11.0"
okhttp = "4.12.0"
retrofitKotlinxSerializationConverter = "1.0.0"
kotlinxSerializationJson = "1.9.0"
datastore = "1.1.7"
coroutines = "1.10.2"
```

Current added libraries:

```toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
retrofit-kotlinx-serialization-converter = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofitKotlinxSerializationConverter" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
```

Current added plugin:

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Why these exist:

- `navigation-compose`: app navigation between login, signup, and habits.
- `retrofit`: creates strongly typed API calls.
- `okhttp-logging-interceptor`: logs requests/responses while debugging.
- `retrofit2-kotlinx-serialization-converter`: connects Retrofit to Kotlinx Serialization.
- `kotlinx-serialization-json`: serializes/deserializes JSON DTOs.
- `datastore-preferences`: stores access token and user ID.
- `kotlinx-coroutines-android`: coroutine support on Android.

## 5. Root `build.gradle.kts`

Current root plugin setup:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

Why:

- Plugin versions are declared once.
- The app module applies the plugins it needs.
- We intentionally do not add Hilt here because the project had plugin compatibility issues with the current AGP setup, so the final app uses manual dependency injection.

## 6. App `build.gradle.kts`

The app applies Android, Compose, and Kotlin serialization:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
```

The backend URL is exposed through `BuildConfig`:

```kotlin
buildConfigField(
    "String",
    "API_BASE_URL",
    "\"https://habit.thatinsaneguy.com/\""
)
```

Why:

- Retrofit needs a base URL.
- Keeping it in Gradle makes it easy to switch between production and local testing.
- For Android emulator local backend testing, use `http://10.0.2.2:8000/`.

Important build features:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
}
```

Why:

- `compose = true` enables Jetpack Compose.
- `buildConfig = true` allows `BuildConfig.API_BASE_URL`.

Current dependencies:

```kotlin
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.navigation.compose)
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.ui.graphics)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.compose.material3)
implementation(libs.retrofit)
implementation(libs.okhttp.logging.interceptor)
implementation(libs.retrofit.kotlinx.serialization.converter)
implementation(libs.kotlinx.serialization.json)
implementation(libs.androidx.datastore.preferences)
implementation(libs.kotlinx.coroutines.android)
```

Result:

- The app can render Compose screens.
- ViewModels can be collected in Compose.
- Retrofit can call the backend.
- DataStore can persist the token.
- Navigation can switch between screens.

## 7. Manifest

The app needs internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

The custom application class is registered:

```xml
<application
    android:name=".CozyTrackApplication"
    ...>
</application>
```

Why:

- Internet permission is required for backend API calls.
- `CozyTrackApplication` creates the manual dependency container when the app starts.

## 8. Application Class

`CozyTrackApplication.kt`:

```kotlin
class CozyTrackApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
```

Why:

- This is the root of manual dependency injection.
- It creates one `AppContainer` for the app.
- Screens and ViewModels receive dependencies from this container.

Result:

- No Hilt setup is required.
- Dependencies are still centralized and testable.

## 9. API Result Wrapper

`ApiResult.kt`:

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}
```

Why:

- Repositories return either success data or a readable error message.
- ViewModels do not need to catch Retrofit exceptions directly.

Result:

- UI can show error messages cleanly.
- API failures are handled consistently.

## 10. Session Storage With DataStore

`SessionManager.kt` stores:

- `access_token`
- `user_id`

Key behavior:

```kotlin
val accessToken: Flow<String?> = appContext.authDataStore.data.map { preferences ->
    preferences[accessTokenKey]
}

val userId: Flow<String?> = appContext.authDataStore.data.map { preferences ->
    preferences[userIdKey]
}

suspend fun saveSession(accessToken: String, userId: String) {
    appContext.authDataStore.edit { preferences ->
        preferences[accessTokenKey] = accessToken
        preferences[userIdKey] = userId
    }
}

suspend fun clearSession() {
    appContext.authDataStore.edit { preferences ->
        preferences.clear()
    }
}
```

Why:

- The backend returns `accessToken` and `userId` after login/signup.
- Protected API calls require the token.
- User-specific endpoints require `userId`.
- DataStore keeps the session after app restart.

Result:

- If a user is already logged in, the app can open the habit screen.
- Logging out clears the saved token and user ID.

## 11. Auth Interceptor

`AuthInterceptor.kt` attaches the Bearer token:

```kotlin
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path == "/api/auth/login" || path == "/api/auth/signup") {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking {
            sessionManager.accessToken.first()
        }

        val request = if (token.isNullOrBlank()) {
            originalRequest
        } else {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
```

Why:

- Login and signup do not need a token.
- All protected routes need `Authorization: Bearer <accessToken>`.
- Centralizing this avoids manually adding headers in every API function.

Result:

- Retrofit calls automatically become authenticated after login/signup.

## 12. Retrofit API Interface

`HabitTrackerApi.kt` declares the backend calls:

```kotlin
interface HabitTrackerApi {
    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequestDto): AuthTokenResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthCredentialsDto): AuthTokenResponseDto

    @GET("api/me")
    suspend fun getMe(): JsonElement

    @GET("api/clock")
    suspend fun getClock(): ClockResponseDto

    @POST("api/habits")
    suspend fun createHabit(@Body request: CreateHabitRequestDto): ResponseBody

    @GET("api/users/{userId}/habits")
    suspend fun getHabits(
        @Path("userId") userId: String,
        @Query("includeArchived") includeArchived: Boolean = false
    ): List<HabitDto>

    @GET("api/habits/{habitId}")
    suspend fun getHabit(@Path("habitId") habitId: String): HabitDto

    @PUT("api/habits/{habitId}")
    suspend fun updateHabit(
        @Path("habitId") habitId: String,
        @Body request: HabitUpdateRequestDto
    ): ResponseBody

    @DELETE("api/habits/{habitId}")
    suspend fun deleteHabit(@Path("habitId") habitId: String): Response<Unit>

    @POST("api/habits/check")
    suspend fun checkHabit(@Body request: HabitCheckRequestDto): ResponseBody

    @GET("api/users/{userId}/entries/{checkDate}")
    suspend fun getEntriesForDay(
        @Path("userId") userId: String,
        @Path("checkDate") checkDate: String
    ): List<HabitEntryDto>

    @GET("api/habits/{habitId}/entries")
    suspend fun getHabitEntries(
        @Path("habitId") habitId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): List<HabitEntryDto>

    @GET("api/habits/{habitId}/progress")
    suspend fun getHabitProgress(
        @Path("habitId") habitId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): HabitProgressDto

    @GET("api/thought")
    suspend fun getThought(): JsonElement
}
```

Why:

- Retrofit turns this interface into a concrete HTTP client.
- Each annotation maps to the Swagger endpoint.
- DTOs match backend request and response shapes.

Result:

- Repositories can call Kotlin functions instead of manually building HTTP requests.

## 13. DTOs

Auth DTOs:

```kotlin
@Serializable
data class AuthCredentialsDto(
    val email: String,
    val password: String
)

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class AuthTokenResponseDto(
    val accessToken: String,
    val expiresIn: String,
    val tokenType: String,
    val userId: String
)
```

Habit DTOs:

```kotlin
@Serializable
data class CreateHabitRequestDto(
    val title: String,
    val frequency: String,
    val startDate: String,
    val userId: String
)

@Serializable
data class HabitUpdateRequestDto(
    val title: String? = null,
    val frequency: String? = null,
    val startDate: String? = null,
    val isArchived: Boolean? = null
)

@Serializable
data class HabitCheckRequestDto(
    val habitId: String,
    val completed: Boolean
)
```

Why:

- DTOs represent API JSON exactly.
- `CreateHabitRequestDto` includes `userId` because the backend validation requires it, even though ownership comes from the Bearer token.
- `HabitUpdateRequestDto` uses nullable fields for partial updates.

Important JSON setting:

```kotlin
explicitNulls = false
```

Why:

- Partial update requests should omit null fields.
- Archiving sends only `{ "isArchived": true }`.
- Editing title/frequency sends only changed fields.

## 14. Domain Models

Domain models are the app's internal version of backend data.

Examples:

```kotlin
data class Habit(
    val id: String,
    val title: String,
    val frequency: Frequency,
    val startDate: String,
    val isArchived: Boolean
)
```

```kotlin
enum class Frequency(val apiValue: String) {
    Daily("daily"),
    Weekly("weekly");

    companion object {
        fun fromApi(value: String): Frequency {
            return when (value.lowercase()) {
                "weekly" -> Weekly
                else -> Daily
            }
        }
    }
}
```

Why:

- UI should not depend directly on network DTOs.
- Domain types are safer and easier to use in ViewModels.
- `Frequency` prevents random strings in UI/business logic.

## 15. Repository Contracts

`AuthRepository.kt` defines authentication/session features:

```kotlin
interface AuthRepository {
    val accessToken: Flow<String?>
    val userId: Flow<String?>

    suspend fun signUp(email: String, password: String, name: String): ApiResult<AuthSession>
    suspend fun login(email: String, password: String): ApiResult<AuthSession>
    suspend fun getCurrentUserName(): ApiResult<String>
    suspend fun logout()
}
```

Why:

- Login and signup return the session.
- `getCurrentUserName()` calls `GET /api/me` and lets the header say `Welcome back, <name>`.
- Logout clears DataStore.

`HabitRepository.kt` defines what habit features exist:

```kotlin
interface HabitRepository {
    suspend fun getServerClock(): ApiResult<ServerClock>
    suspend fun getHabits(includeArchived: Boolean = false): ApiResult<List<Habit>>
    suspend fun getHabit(habitId: String): ApiResult<Habit>
    suspend fun createHabit(title: String, frequency: Frequency, startDate: String): ApiResult<String>
    suspend fun updateHabit(
        habitId: String,
        title: String? = null,
        frequency: Frequency? = null,
        startDate: String? = null,
        isArchived: Boolean? = null
    ): ApiResult<Unit>
    suspend fun deleteHabit(habitId: String): ApiResult<Unit>
    suspend fun checkHabit(habitId: String, completed: Boolean): ApiResult<Unit>
    suspend fun getEntriesForDay(checkDate: String): ApiResult<List<HabitEntry>>
    suspend fun getHabitEntries(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<List<HabitEntry>>
    suspend fun getHabitProgress(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<HabitProgress>
}
```

Why:

- ViewModels and use cases depend on abstractions.
- The data layer can change without rewriting UI code.

Result:

- Clean separation between app logic and Retrofit details.

## 16. Repository Implementations

`AuthRepositoryImpl`:

- Calls signup/login.
- Calls `GET /api/me` to read the current user's display name when available.
- Saves the returned token and user ID.
- Clears session on logout.
- Converts API errors into readable messages.

Key behavior:

```kotlin
private suspend fun safeAuthCall(
    call: suspend () -> AuthTokenResponseDto
): ApiResult<AuthSession> {
    return try {
        val session = call().toDomain()
        sessionManager.saveSession(
            accessToken = session.accessToken,
            userId = session.userId
        )
        ApiResult.Success(session)
    } catch (error: Exception) {
        ApiResult.Error(error.toAuthMessage())
    }
}
```

Why:

- After login/signup, all future protected requests need the token.
- Storing the session immediately makes navigation to the habit screen work.

`HabitRepositoryImpl`:

- Reads `userId` from `SessionManager`.
- Calls user-specific habit endpoints.
- Sends `userId` in create request because backend validation requires it.
- Converts DTOs into domain models.
- Returns `ApiResult`.

Create habit flow:

```kotlin
val userId = sessionManager.userId.first()
    ?: error("You must be logged in to create habits")

api.createHabit(
    CreateHabitRequestDto(
        title = title,
        frequency = frequency.apiValue,
        startDate = startDate,
        userId = userId
    )
).string()
```

Check-in flow:

```kotlin
api.checkHabit(
    HabitCheckRequestDto(
        habitId = habitId,
        completed = completed
    )
).close()
```

Why:

- The app does not send the date for check-ins.
- The backend assigns the UTC day.

Result:

- Check-ins match server time, not device time.

## 17. Manual Dependency Injection

`AppContainer.kt` creates all dependencies:

```kotlin
class AppContainer(context: Context) {
    val sessionManager = SessionManager(context)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                redactHeader("Authorization")
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
```

Why:

- `ignoreUnknownKeys` prevents crashes if backend adds extra fields.
- `explicitNulls = false` supports partial update requests.
- `AuthInterceptor` attaches token automatically.
- `redactHeader("Authorization")` hides tokens in logs.

Result:

- One consistent Retrofit client is used everywhere.
- ViewModels are created with factories from this container.

## 18. Use Cases

Use cases are small classes that describe app actions:

```text
LoginUseCase
SignUpUseCase
LogoutUseCase
GetCurrentUserNameUseCase
GetServerClockUseCase
GetHabitsUseCase
CreateHabitUseCase
UpdateHabitUseCase
DeleteHabitUseCase
CheckHabitUseCase
GetEntriesForDayUseCase
GetHabitEntriesUseCase
GetHabitProgressUseCase
GetThoughtUseCase
```

Example:

```kotlin
class CheckHabitUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String, completed: Boolean) =
        repository.checkHabit(habitId, completed)
}
```

Why:

- ViewModels call use cases instead of repositories directly.
- This keeps UI logic separate from app actions.
- It makes each user action easy to find.

Result:

- The code reads like the product: login, create habit, check habit, update habit.

## 19. Navigation

Routes:

```kotlin
object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val HABITS = "habits"
    const val HABIT_DETAIL = "habit_detail"

    fun habitDetail(habitId: String) = "$HABIT_DETAIL/$habitId"
}
```

`AppNavHost.kt` creates screens:

```kotlin
NavHost(
    navController = navController,
    startDestination = startDestination
) {
    composable(Routes.LOGIN) { ... }
    composable(Routes.SIGN_UP) { ... }
    composable(Routes.HABITS) { ... }
    composable("${Routes.HABIT_DETAIL}/{habitId}") { ... }
}
```

Why:

- Login, signup, habits, and habit detail are separate screens.
- Successful login/signup navigates to the habit screen.
- Tapping a habit navigates to `habit_detail/{habitId}`.
- The detail route receives the selected habit ID and shows only that habit's streak/grid data.
- Logout navigates back to login.

## 20. App Start Logic

`MainActivity.kt` checks saved token:

```kotlin
val startDestination by produceState<String?>(initialValue = null) {
    value = if (appContainer.sessionManager.accessToken.first().isNullOrBlank()) {
        Routes.LOGIN
    } else {
        Routes.HABITS
    }
}
```

Why:

- If there is no saved token, show login.
- If there is a token, open habits.

Result:

- User stays logged in after app restart.
- If the token expired, the habit ViewModel detects `HTTP 401`, clears the session, and navigates back to login.

## 21. Login Screen

The login UI is in `presentation/auth/LoginScreen.kt`.

It includes:

- Cozy bear header.
- Email/username field.
- Password field.
- Password visibility toggle.
- Remember me checkbox.
- Forgot password text.
- Brown paw `Log in` button.
- Google-style visual button.
- Signup navigation link.
- Black input text on cream fields so email and password stay readable.

The shared `CozyAuthTextField` sets explicit text colors:

```kotlin
colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuthAccent,
    unfocusedBorderColor = AuthBorder,
    focusedLabelColor = AuthBrown,
    unfocusedLabelColor = AuthMuted,
    focusedContainerColor = Color(0xFFFFFCF7),
    unfocusedContainerColor = Color(0xFFFFFCF7),
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    cursorColor = Color.Black
)
```

Why:

- Material3 defaults can inherit light text from the app theme.
- Explicit black text keeps typed input visible on the warm cream background.

Important note:

- Google login is currently visual only.
- The backend Swagger only provides email/password login and signup.

Login ViewModel behavior:

```kotlin
fun login() {
    val state = _uiState.value
    if (state.email.isBlank() || state.password.isBlank()) {
        _uiState.update { it.copy(errorMessage = "Email and password are required") }
        return
    }

    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        when (val result = loginUseCase(state.email, state.password)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true)
                }
            }

            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
```

Result:

- Empty fields show validation errors.
- Successful login stores session through repository.
- UI navigates to habits.

## 22. Signup Screen

The signup UI is in `presentation/auth/SignUpScreen.kt`.

It includes:

- Cozy bear header.
- Full name field.
- Email field.
- Password field.
- Confirm password field.
- Password visibility toggles.
- Terms checkbox.
- Brown paw `Sign up` button.
- Google-style visual button.
- Login navigation link.
- Same black-on-cream text field styling as login through the shared `CozyAuthTextField`.

Signup ViewModel validates:

- name is present
- email is present
- password is present
- confirm password is present
- password and confirm password match

Key validation:

```kotlin
if (state.password != state.confirmPassword) {
    _uiState.update { it.copy(errorMessage = "Passwords do not match") }
    return
}
```

Why:

- The backend only needs `name`, `email`, and `password`.
- Confirm password is a frontend-only safety check.

Result:

- Signup sends only the fields the backend expects.
- User is logged in after signup because the backend returns `accessToken`.

## 23. Habit Home Screen

The home screen is `presentation/habits/HabitListScreen.kt`.

It shows:

- CozyTrack header, or `Welcome back, <name>` when `GET /api/me` returns a name.
- Current server date.
- Thought of the day.
- Create habit card.
- Daily/weekly chips.
- Show archived toggle.
- Habit cards for today's entries/status.
- Today's complete/not-complete button for each habit.
- A hint telling the user to tap a habit to view streaks and the 365-day grid.
- Bottom navigation-style visual bar.

The home page intentionally does not show the full 365-day grid anymore. It stays focused on "what do I need to do today?"

When the user taps a habit card:

```text
HabitListScreen
  -> onHabitClick(habit.id)
  -> navController.navigate(Routes.habitDetail(habit.id))
  -> HabitDetailScreen
```

Why:

- The home page stays simple.
- The detail page can focus on one habit's history.
- Each habit has independent entries, current streak, and best streak.

Daily/weekly chips:

```kotlin
FilterChip(
    selected = frequency == Frequency.Daily,
    onClick = { onFrequencyChange(Frequency.Daily) },
    label = { Text("☀️  Daily") }
)

FilterChip(
    selected = frequency == Frequency.Weekly,
    onClick = { onFrequencyChange(Frequency.Weekly) },
    label = { Text("🗓️  Weekly") }
)
```

Why:

- The backend accepts `daily` or `weekly`.
- The UI maps chip selection to the `Frequency` enum.

## 24. Habit ViewModel

`HabitListViewModel.kt` owns the habit screen state:

```kotlin
data class HabitListUiState(
    val habits: List<Habit> = emptyList(),
    val progressByHabitId: Map<String, HabitProgress> = emptyMap(),
    val heatmapByHabitId: Map<String, List<HabitHeatmapDay>> = emptyMap(),
    val checkedHabitIds: Set<String> = emptySet(),
    val userName: String = "",
    val thought: Thought? = null,
    val utcCalendarDate: String = "",
    val newHabitTitle: String = "",
    val newHabitFrequency: Frequency = Frequency.Daily,
    val includeArchived: Boolean = false,
    val editingHabitId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)
```

Why:

- `habits`: list of habit cards.
- `progressByHabitId`: current/best streak data.
- `heatmapByHabitId`: 365-day completed state.
- `checkedHabitIds`: whether today's check-in is completed.
- `userName`: display name loaded from `GET /api/me` when available.
- `utcCalendarDate`: authoritative server date.
- `includeArchived`: controls archived habits.
- `editingHabitId`: tracks edit mode.

## 25. Loading Home Data

When the habit screen opens, the ViewModel calls:

```kotlin
loadClock()
loadCurrentUserName()
loadThought()
loadHabitsProgressAndEntries()
```

Why:

- `loadClock()` gets server UTC date.
- `loadCurrentUserName()` calls `GET /api/me` for the welcome header.
- `loadThought()` gets quote/inspiration.
- `loadHabitsProgressAndEntries()` loads habits, progress, heatmap entries, and today's check-ins.

The flow:

```text
HabitListScreen opens
  -> HabitListViewModel.init
  -> loadHome()
  -> GET /api/clock
  -> GET /api/me
  -> GET /api/thought
  -> GET /api/users/{userId}/habits
  -> for each habit:
       GET /api/habits/{habitId}/progress
       GET /api/habits/{habitId}/entries?startDate=...&endDate=...
  -> GET /api/users/{userId}/entries/{utcCalendarDate}
  -> update UI state
```

Result:

- The screen displays only the logged-in user's habits.
- It knows which habits are completed today.
- It has the data needed to open a detail page with the selected habit's heatmap and streaks.

## 26. Creating A Habit

User enters title, chooses daily/weekly, and taps `Add habit`.

The create and edit habit `OutlinedTextField` components also use black text:

```kotlin
colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = Color(0xFFE4DEF7),
    focusedLabelColor = PurplePrimary,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    cursorColor = Color.Black
)
```

Why:

- Habit titles must stay readable while typing on light cards.

ViewModel:

```kotlin
val startDate = state.utcCalendarDate.ifBlank {
    when (val clock = getServerClockUseCase()) {
        is ApiResult.Success -> clock.data.utcCalendarDate
        is ApiResult.Error -> return@launch
    }
}
```

Then:

```kotlin
createHabitUseCase(
    title = state.newHabitTitle,
    frequency = state.newHabitFrequency,
    startDate = startDate
)
```

Repository sends:

```json
{
  "title": "Walk",
  "frequency": "daily",
  "startDate": "2026-05-24",
  "userId": "current-user-id"
}
```

Why:

- Backend requires `startDate` as UTC calendar date.
- Backend validation requires `userId`.
- Ownership still comes from the Bearer token.

Result:

- Habit is created for the current logged-in user.
- Different users see only their own habits.

## 27. Checking In

User taps `Complete today` or `Not complete`.

The button only controls the current backend UTC day. It does not edit yesterday or tomorrow.

Before sending the request, the ViewModel refreshes the server clock:

```kotlin
when (val clock = getServerClockUseCase()) {
    is ApiResult.Success -> {
        if (clock.data.utcCalendarDate != previousState.utcCalendarDate) {
            _uiState.update {
                it.copy(utcCalendarDate = clock.data.utcCalendarDate)
            }
            loadHabitsProgressAndEntries()
            return@launch
        }
    }
    is ApiResult.Error -> return@launch
}
```

Why:

- If the app stayed open overnight, the visible screen might still show yesterday.
- Refreshing `GET /api/clock` prevents accidentally changing the wrong day.
- The backend still decides the actual check-in day.

Then the ViewModel optimistically updates UI:

```kotlin
checkedHabitIds = checkedHabitIds + habitId
```

For undoing today's completion:

```kotlin
checkedHabitIds = checkedHabitIds - habitId
```

Then it calls:

```kotlin
checkHabitUseCase(habitId = habitId, completed = checked)
```

Repository sends:

```json
{
  "habitId": "string",
  "completed": true
}
```

Endpoint:

```text
POST /api/habits/check
```

Important:

- The app does not send date.
- Backend assigns UTC day.
- `completed: true` means complete today.
- `completed: false` means mark today as not complete.
- After success, the app reloads habit progress and entries.

Result:

- Today's check-in card shows completed.
- Today's heatmap square turns completed.
- Current streak updates from completed heatmap entries.
- If today's status is changed to not complete, current streak recalculates from the remaining completed history.

## 28. Streak Calculation

The backend returns streaks through:

```text
GET /api/habits/{habitId}/progress
```

The app also calculates daily streak display from the 365-day heatmap entries so the UI immediately matches completed squares.

Current daily streak logic:

```kotlin
private fun List<HabitHeatmapDay>.currentCompletedStreak(): Int {
    val todayIndex = indexOfLast { it.isToday }
    if (todayIndex == -1 || !this[todayIndex].completed) return 0

    var streak = 0
    for (index in todayIndex downTo 0) {
        if (!this[index].completed) break
        streak += 1
    }
    return streak
}
```

Best daily streak logic:

```kotlin
private fun List<HabitHeatmapDay>.bestCompletedStreak(): Int {
    var best = 0
    var current = 0

    forEach { day ->
        if (day.completed) {
            current += 1
            best = maxOf(best, current)
        } else {
            current = 0
        }
    }

    return best
}
```

Why:

- The check-in card and heatmap already know today's completed state.
- Showing streaks from the same data avoids UI mismatch.
- Weekly streaks still use backend progress because weekly logic is ISO-week based.

Result:

- A new habit starts at `0 days`.
- After checking in today, current streak shows `1 day`.
- Consecutive completed days increase the streak.

## 29. Heatmap

The heatmap is shown on `HabitDetailScreen`, not directly on the home list.

The heatmap uses:

```text
GET /api/habits/{habitId}/entries?startDate=<today-minus-364>&endDate=<today>
```

The app builds up to 365 `HabitHeatmapDay` items:

```kotlin
data class HabitHeatmapDay(
    val calendarDate: String,
    val completed: Boolean,
    val isToday: Boolean
)
```

Why:

- Backend returns actual completed entries.
- The UI fills missing dates as not completed.
- Past/future days are read-only.
- Only today's check-in button can create/update today's completion.
- The grid is a rolling 365-day history, not a permanently growing grid.
- Tomorrow, a new day appears and the oldest day drops off.
- Completed days are remembered because they come from backend entries, not hardcoded local UI state.
- Each habit has its own grid because entries are loaded by `habitId`.

Result:

- Completed days are brown.
- Today completed is highlighted red/brown.
- Uncompleted days are pale cream.
- The heatmap scrolls to the newest days.

Important:

- The grid is not "how many days of the calendar year have passed."
- It means "the last 365 days ending on the backend's current UTC date."
- If the user completed a habit yesterday, that square remains highlighted when they log in tomorrow, as long as the backend returns that entry.
- If one habit was completed and another was missed, only the completed habit continues its streak.

## 30. Habit Detail Screen

The detail screen is also implemented in `presentation/habits/HabitListScreen.kt` as `HabitDetailScreen`.

It receives a `habitId` from navigation:

```text
habit_detail/{habitId}
```

It finds that habit from the loaded state:

```kotlin
val habit = state.habits.firstOrNull { it.id == habitId }
```

Then it displays:

- Back button.
- Selected habit title and management actions.
- Today's complete/not-complete status for this habit.
- Current streak for this habit.
- Best streak for this habit.
- 365-day rolling grid for this habit.

Why:

- The home page should not become crowded with every habit's full year history.
- The detail page makes it clear that streaks and grids belong to one selected habit.
- This matches the backend model: entries are fetched by `habitId`.

Result:

- Tapping `Read a Book` shows only `Read a Book` streaks and grid.
- Tapping `Walk 10K Steps` shows only `Walk 10K Steps` streaks and grid.
- Completing one habit does not complete or protect the streak for another habit.

## 31. Editing, Archiving, Restoring, Deleting

The app supports habit management from Swagger.

Edit:

```text
PUT /api/habits/{habitId}
```

Archive:

```json
{
  "isArchived": true
}
```

Restore:

```json
{
  "isArchived": false
}
```

Delete:

```text
DELETE /api/habits/{habitId}
```

Why:

- Archive hides a habit without permanently deleting it.
- Restore brings archived habits back.
- Delete removes the habit and associated entries as implemented by the backend.

Result:

- `Show archived habits` calls `GET /api/users/{userId}/habits?includeArchived=true`.
- Archived habits can be restored.

## 32. Expired Token Handling

If the saved token expires, the backend returns:

```text
HTTP 401: Invalid or expired access token
```

The ViewModel detects it:

```kotlin
private fun String.isExpiredTokenError(): Boolean {
    val lowerMessage = lowercase(Locale.US)
    return "http 401" in lowerMessage ||
        "invalid or expired access token" in lowerMessage ||
        "unauthorized" in lowerMessage
}
```

Then:

```kotlin
logoutUseCase()
isLoggedOut = true
```

Why:

- DataStore may contain an old token.
- The app should not leave the user stuck on a broken habit screen.

Result:

- Token is cleared.
- App navigates back to login.
- User logs in again and receives a fresh token.

## 33. Thought Of The Day

The app calls:

```text
GET /api/thought
```

The response can vary, so `ThoughtRepositoryImpl` accepts flexible JSON:

- plain string
- `{ "quote": "...", "author": "..." }`
- `{ "text": "..." }`
- `{ "q": "...", "a": "..." }`

Why:

- The backend proxies external quote APIs and may return different shapes.
- The UI should not crash if one quote provider responds differently.

Result:

- The quote card is optional.
- Habit loading is not blocked if the quote fails.

## 34. Account Isolation

The app keeps habit data per user because:

1. Login/signup stores the returned `userId`.
2. Habit list uses:

```text
GET /api/users/{userId}/habits
```

3. AuthInterceptor sends:

```text
Authorization: Bearer <accessToken>
```

4. The backend verifies the token and enforces that the user ID matches.

Result:

- User A sees User A's habits.
- User B sees User B's habits.
- If User B logs in on the same device, DataStore is updated with User B's token and user ID.

## 35. Why There Is No Room Database

This app does not use Room/local caching.

Stored locally:

- access token
- user ID

Not stored locally:

- habits
- entries
- progress
- streaks

Why:

- The backend is the source of truth.
- Streaks and check-in dates depend on server UTC time.
- Avoiding a local database keeps the first version simpler.

Result:

- The app reloads habits/progress from the backend.
- Offline habit editing is not supported in this version.

## 36. UI Theme Result

Final visual direction:

- Login/signup use cozy bear style.
- Input text is black on cream fields across login, signup, and habit create/edit screens.
- Home uses cream/brown cards.
- Emojis are used as lightweight icons:
  - `🧸` bear/profile
  - `🐾` paw/create
  - `☀️` daily
  - `🗓️` weekly
  - `🪴` plant accent
  - `👟` habit example icon
  - `✏️` edit
  - `🗃️` archive
  - `🗑️` delete
  - `✅` completed
  - `🔥` current streak
  - `⭐` best streak

Why:

- It matches the provided cozy mockups without adding image assets.
- It keeps the app lightweight.
- Exact custom bear artwork would require PNG/vector assets in `res/drawable`.

## 37. Build And Run Notes

To run:

```bash
./gradlew :app:assembleDebug
```

Or use Android Studio:

1. Sync Gradle.
2. Select the `app` configuration.
3. Run on emulator/device.

Important:

- This project requires a Java runtime/JDK.
- The terminal used during implementation could not run Gradle because Java was not visible there:

```text
Unable to locate a Java Runtime.
```

If Android Studio builds successfully, the project is fine; the issue is only the shell/JDK environment.

## 38. Final API Calling Summary

Signup:

```text
SignUpScreen
  -> SignUpViewModel
  -> SignUpUseCase
  -> AuthRepositoryImpl
  -> POST /api/auth/signup
  -> save accessToken + userId
  -> navigate to habits
```

Login:

```text
LoginScreen
  -> LoginViewModel
  -> LoginUseCase
  -> AuthRepositoryImpl
  -> POST /api/auth/login
  -> save accessToken + userId
  -> navigate to habits
```

Load home:

```text
HabitListScreen
  -> HabitListViewModel.loadHome()
  -> GET /api/clock
  -> GET /api/me
  -> GET /api/thought
  -> GET /api/users/{userId}/habits
  -> GET /api/habits/{habitId}/progress
  -> GET /api/habits/{habitId}/entries
  -> GET /api/users/{userId}/entries/{checkDate}
```

Open habit detail:

```text
Tap a habit card
  -> navController.navigate("habit_detail/{habitId}")
  -> HabitDetailScreen
  -> reads selected habit from HabitListViewModel state
  -> shows only that habit's today's status, streaks, and 365-day grid
```

Create habit:

```text
Add habit button
  -> CreateHabitUseCase
  -> POST /api/habits
  -> reload habits/progress/entries
```

Check in:

```text
Check in button
  -> CheckHabitUseCase
  -> POST /api/habits/check
  -> backend assigns UTC day
  -> reload progress and entries
  -> streak and heatmap update
```

Edit/archive/restore:

```text
Edit/archive/restore action
  -> UpdateHabitUseCase
  -> PUT /api/habits/{habitId}
  -> reload habits
```

Delete:

```text
Delete action
  -> DeleteHabitUseCase
  -> DELETE /api/habits/{habitId}
  -> reload habits
```

Logout:

```text
Log out
  -> LogoutUseCase
  -> SessionManager.clearSession()
  -> navigate to login
```

## 39. Final Result

The final CozyTrack app is a backend-driven habit tracker:

- Authentication is handled through the provided API.
- Tokens are persisted with DataStore.
- Protected API calls automatically include the Bearer token.
- Habits are scoped to the logged-in user.
- Check-ins use backend UTC date rules.
- Home page shows today's habit statuses.
- Habit detail page shows the selected habit's streaks and 365-day rolling grid.
- Heatmap and streak UI reflect completed entries returned by the backend.
- Each habit has its own independent current streak and best streak.
- Habit management supports create, update, archive, restore, and delete.
- Login, signup, and home screens use a consistent cozy bear theme.
