package com.example.cozytrack.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cozytrack.BuildConfig
import com.example.cozytrack.core.network.AuthInterceptor
import com.example.cozytrack.core.session.SessionManager
import com.example.cozytrack.data.remote.HabitTrackerApi
import com.example.cozytrack.data.repository.AuthRepositoryImpl
import com.example.cozytrack.data.repository.HabitRepositoryImpl
import com.example.cozytrack.data.repository.ThoughtRepositoryImpl
import com.example.cozytrack.domain.repository.AuthRepository
import com.example.cozytrack.domain.repository.HabitRepository
import com.example.cozytrack.domain.repository.ThoughtRepository
import com.example.cozytrack.domain.usecase.auth.GetCurrentUserNameUseCase
import com.example.cozytrack.domain.usecase.auth.LoginUseCase
import com.example.cozytrack.domain.usecase.auth.LogoutUseCase
import com.example.cozytrack.domain.usecase.auth.SignUpUseCase
import com.example.cozytrack.domain.usecase.habit.CheckHabitUseCase
import com.example.cozytrack.domain.usecase.habit.CreateHabitUseCase
import com.example.cozytrack.domain.usecase.habit.DeleteHabitUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitProgressUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitsUseCase
import com.example.cozytrack.domain.usecase.habit.GetEntriesForDayUseCase
import com.example.cozytrack.domain.usecase.habit.GetHabitEntriesUseCase
import com.example.cozytrack.domain.usecase.habit.GetServerClockUseCase
import com.example.cozytrack.domain.usecase.habit.UpdateHabitUseCase
import com.example.cozytrack.domain.usecase.thought.GetThoughtUseCase
import com.example.cozytrack.presentation.auth.LoginViewModel
import com.example.cozytrack.presentation.auth.SignUpViewModel
import com.example.cozytrack.presentation.habits.HabitListViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

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

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(HabitTrackerApi::class.java)

    private val authRepository: AuthRepository = AuthRepositoryImpl(api, sessionManager)
    private val habitRepository: HabitRepository = HabitRepositoryImpl(api, sessionManager)
    private val thoughtRepository: ThoughtRepository = ThoughtRepositoryImpl(api)

    private val loginUseCase = LoginUseCase(authRepository)
    private val signUpUseCase = SignUpUseCase(authRepository)
    private val logoutUseCase = LogoutUseCase(authRepository)
    private val getCurrentUserNameUseCase = GetCurrentUserNameUseCase(authRepository)
    private val getServerClockUseCase = GetServerClockUseCase(habitRepository)
    private val getHabitsUseCase = GetHabitsUseCase(habitRepository)
    private val getEntriesForDayUseCase = GetEntriesForDayUseCase(habitRepository)
    private val getHabitEntriesUseCase = GetHabitEntriesUseCase(habitRepository)
    private val createHabitUseCase = CreateHabitUseCase(habitRepository)
    private val updateHabitUseCase = UpdateHabitUseCase(habitRepository)
    private val deleteHabitUseCase = DeleteHabitUseCase(habitRepository)
    private val checkHabitUseCase = CheckHabitUseCase(habitRepository)
    private val getHabitProgressUseCase = GetHabitProgressUseCase(habitRepository)
    private val getThoughtUseCase = GetThoughtUseCase(thoughtRepository)

    val loginViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            LoginViewModel(loginUseCase)
        }
    }

    val signUpViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SignUpViewModel(signUpUseCase)
        }
    }

    val habitListViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HabitListViewModel(
                getServerClockUseCase = getServerClockUseCase,
                getHabitsUseCase = getHabitsUseCase,
                getEntriesForDayUseCase = getEntriesForDayUseCase,
                getHabitEntriesUseCase = getHabitEntriesUseCase,
                createHabitUseCase = createHabitUseCase,
                updateHabitUseCase = updateHabitUseCase,
                deleteHabitUseCase = deleteHabitUseCase,
                checkHabitUseCase = checkHabitUseCase,
                getHabitProgressUseCase = getHabitProgressUseCase,
                getThoughtUseCase = getThoughtUseCase,
                getCurrentUserNameUseCase = getCurrentUserNameUseCase,
                logoutUseCase = logoutUseCase
            )
        }
    }
}
