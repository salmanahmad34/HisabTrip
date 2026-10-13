package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MainCurvedBottomNavBar
import com.example.ui.screens.CreateTripDialog
import com.example.ui.screens.CreateTripScreen
import com.example.ui.screens.ExpenseAnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinTripDialog
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TripDashboardScreen
import com.example.ui.screens.TripsListScreen
import com.example.ui.theme.HisabTripTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HisabTripViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      HisabTripTheme {
        HisabTripApp()
      }
    }
  }
}

@Composable
fun HisabTripApp(viewModel: HisabTripViewModel = viewModel()) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()
  val allProfiles by viewModel.allProfiles.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  var showCreateTripDialog by remember { mutableStateOf(false) }
  var showJoinTripDialog by remember { mutableStateOf(false) }

  LaunchedEffect(snackbarMessage) {
    if (snackbarMessage != null) {
      snackbarHostState.showSnackbar(snackbarMessage!!)
      viewModel.clearSnackbar()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        // Show curved bottom nav bar on Home, Trips, Profile when not in splash/sign-in/create-trip
        if (currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.SIGN_IN && currentScreen != AppScreen.CREATE_TRIP) {
          MainCurvedBottomNavBar(
            currentScreen = currentScreen,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onCreateTripClick = { viewModel.navigateTo(AppScreen.CREATE_TRIP) }
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentScreen) {
          AppScreen.SPLASH -> {
            SplashScreen(
              onContinue = { viewModel.navigateTo(AppScreen.SIGN_IN) }
            )
          }

          AppScreen.SIGN_IN -> {
            SignInScreen(
              onSignInSuccess = { viewModel.navigateTo(AppScreen.HOME) }
            )
          }

          AppScreen.HOME -> {
            HomeScreen(
              viewModel = viewModel,
              onCreateTripClick = { viewModel.navigateTo(AppScreen.CREATE_TRIP) },
              onJoinTripClick = { showJoinTripDialog = true },
              onNotificationsClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }
            )
          }

          AppScreen.TRIPS_LIST -> {
            TripsListScreen(
              viewModel = viewModel,
              onCreateTripClick = { viewModel.navigateTo(AppScreen.CREATE_TRIP) }
            )
          }

          AppScreen.CREATE_TRIP -> {
            CreateTripScreen(
              viewModel = viewModel,
              onBack = { viewModel.navigateTo(AppScreen.HOME) },
              onTripCreated = { tripId ->
                viewModel.openTrip(tripId)
                viewModel.navigateTo(AppScreen.TRIP_DASHBOARD)
              }
            )
          }

          AppScreen.TRIP_DASHBOARD -> {
            TripDashboardScreen(
              viewModel = viewModel,
              onBackToHome = { viewModel.navigateTo(AppScreen.HOME) }
            )
          }

          AppScreen.NOTIFICATIONS -> {
            NotificationsScreen(
              viewModel = viewModel,
              onBack = { viewModel.navigateTo(AppScreen.HOME) }
            )
          }

          AppScreen.PROFILE -> {
            ProfileScreen(
              viewModel = viewModel,
              onNavigateToHome = { viewModel.navigateTo(AppScreen.HOME) }
            )
          }

          AppScreen.EXPENSE_ANALYTICS -> {
            ExpenseAnalyticsScreen(
              viewModel = viewModel,
              onBack = { viewModel.navigateTo(AppScreen.TRIP_DASHBOARD) }
            )
          }
        }
      }
    }

    // Global Modal Dialogs
    if (showCreateTripDialog) {
      CreateTripDialog(
        availableCompanions = allProfiles,
        onDismiss = { showCreateTripDialog = false },
        onCreate = { name, desc, start, end, budgetPaise, memberIds ->
          viewModel.createTrip(
            name = name,
            description = desc,
            startDate = start,
            endDate = end,
            budgetPaise = budgetPaise,
            currencyCode = "INR",
            initialMemberIds = memberIds
          ) {
            showCreateTripDialog = false
          }
        }
      )
    }

    if (showJoinTripDialog) {
      JoinTripDialog(
        onDismiss = { showJoinTripDialog = false },
        onJoin = { code ->
          viewModel.joinTrip(code) {
            showJoinTripDialog = false
          }
        }
      )
    }
  }
}
