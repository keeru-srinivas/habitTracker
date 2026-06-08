package com.example.cozytrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cozytrack.core.di.AppContainer
import com.example.cozytrack.presentation.auth.LoginScreen
import com.example.cozytrack.presentation.auth.LoginViewModel
import com.example.cozytrack.presentation.auth.SignUpScreen
import com.example.cozytrack.presentation.auth.SignUpViewModel
import com.example.cozytrack.presentation.habits.HabitDetailScreen
import com.example.cozytrack.presentation.habits.HabitListScreen
import com.example.cozytrack.presentation.habits.HabitListViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val viewModel = viewModel<LoginViewModel>(
                factory = appContainer.loginViewModelFactory
            )
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HABITS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Routes.SIGN_UP)
                }
            )
        }

        composable(Routes.SIGN_UP) {
            val viewModel = viewModel<SignUpViewModel>(
                factory = appContainer.signUpViewModelFactory
            )
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = {
                    navController.navigate(Routes.HABITS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HABITS) {
            val viewModel = viewModel<HabitListViewModel>(
                factory = appContainer.habitListViewModelFactory
            )
            HabitListScreen(
                viewModel = viewModel,
                onHabitClick = { habitId ->
                    navController.navigate(Routes.habitDetail(habitId))
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HABITS) { inclusive = true }
                    }
                }
            )
        }

        composable("${Routes.HABIT_DETAIL}/{habitId}") { backStackEntry ->
            val viewModel = viewModel<HabitListViewModel>(
                factory = appContainer.habitListViewModelFactory
            )
            HabitDetailScreen(
                viewModel = viewModel,
                habitId = backStackEntry.arguments?.getString("habitId").orEmpty(),
                onBackClick = {
                    navController.popBackStack()
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HABITS) { inclusive = true }
                    }
                }
            )
        }
    }
}
