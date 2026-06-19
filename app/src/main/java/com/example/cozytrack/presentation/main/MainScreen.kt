package com.example.cozytrack.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cozytrack.presentation.components.CozyBottomBar
import com.example.cozytrack.presentation.components.ScreenBackground
import com.example.cozytrack.presentation.habits.HabitDetailScreen
import com.example.cozytrack.presentation.habits.HabitListScreen
import com.example.cozytrack.presentation.habits.HabitListViewModel
import com.example.cozytrack.presentation.habits.HabitsTabScreen
import com.example.cozytrack.presentation.navigation.BottomNavDestination
import com.example.cozytrack.presentation.navigation.Routes
import com.example.cozytrack.presentation.profile.ProfileScreen
import com.example.cozytrack.presentation.stats.StatsScreen

@Composable
fun MainScreen(
    viewModel: HabitListViewModel,
    onLoggedOut: () -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val onDetailScreen = navBackStackEntry?.destination?.route
        ?.startsWith(Routes.HABIT_DETAIL) == true

    var selectedTab by rememberSaveable { mutableStateOf(BottomNavDestination.HOME) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLoggedOut()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadHome()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            if (!onDetailScreen) {
                CozyBottomBar(
                    selected = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Routes.TABS,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.TABS) {
                when (selectedTab) {
                    BottomNavDestination.HOME -> {
                        HabitListScreen(
                            viewModel = viewModel,
                            onHabitClick = { habitId ->
                                innerNavController.navigate(Routes.habitDetail(habitId))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomNavDestination.HABITS -> {
                        HabitsTabScreen(
                            viewModel = viewModel,
                            onHabitClick = { habitId ->
                                innerNavController.navigate(Routes.habitDetail(habitId))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomNavDestination.STATS -> {
                        StatsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomNavDestination.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onLoggedOut = onLoggedOut,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            composable("${Routes.HABIT_DETAIL}/{habitId}") { backStackEntry ->
                HabitDetailScreen(
                    viewModel = viewModel,
                    habitId = backStackEntry.arguments?.getString("habitId").orEmpty(),
                    onBackClick = {
                        if (!innerNavController.popBackStack()) {
                            innerNavController.navigate(Routes.TABS) {
                                popUpTo(Routes.TABS) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onLoggedOut = onLoggedOut,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
