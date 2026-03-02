package com.example.coinset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.coinset.ui.auth.LoginScreen
import com.example.coinset.ui.auth.RegisterScreen
import com.example.coinset.ui.catalog.*
import com.example.coinset.ui.collection.MyCollectionScreen
import com.example.coinset.ui.settings.PremiumScreen
import com.example.coinset.ui.settings.SettingsScreen
import com.example.coinset.ui.theme.CoinSetTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Main Activity of the application.
 * Initializes Firebase and sets up the root navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase SDK
        try { 
            FirebaseApp.initializeApp(this) 
        } catch (e: Exception) {
            // App already initialized or failed
        }
        
        enableEdgeToEdge()
        
        setContent {
            CoinSetTheme {
                RootNavigation()
            }
        }
    }
}

/**
 * The Root Navigation Graph.
 * Decides whether to show Auth screens or the Main App content.
 */
@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val currentUser = Firebase.auth.currentUser

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "main" else "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("main") { MainContent(navController) }
    }
}

/**
 * The main container screen after login.
 * Includes Bottom Navigation and manages the internal app state.
 */
@Composable
fun MainContent(parentNavController: NavController) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            AppBottomBar(bottomNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController, 
            startDestination = "my_collection", 
            modifier = Modifier.padding(innerPadding)
        ) {
            // Primary Tabs
            composable("catalog_root") { CountryListScreen(bottomNavController) }
            composable("my_collection") { MyCollectionScreen(bottomNavController) }
            composable("settings") { SettingsScreen(bottomNavController, parentNavController) }
            
            // Nested Catalog Navigation
            composable("rulers/{countryId}/{countryName}") { backStackEntry ->
                RulerListScreen(
                    bottomNavController, 
                    backStackEntry.arguments?.getString("countryId") ?: "", 
                    backStackEntry.arguments?.getString("countryName") ?: ""
                )
            }
            composable("categories/{rulerId}/{rulerName}") { backStackEntry ->
                CategoryListScreen(
                    bottomNavController, 
                    backStackEntry.arguments?.getString("rulerId") ?: "", 
                    backStackEntry.arguments?.getString("rulerName") ?: ""
                )
            }
            composable("coins/{rulerId}/{category}") { backStackEntry ->
                CoinListScreen(
                    bottomNavController, 
                    backStackEntry.arguments?.getString("rulerId") ?: "", 
                    backStackEntry.arguments?.getString("category") ?: ""
                )
            }
            composable("coin_type/{rulerId}/{category}/{denomination}") { backStackEntry ->
                CoinTypeScreen(
                    bottomNavController, 
                    backStackEntry.arguments?.getString("rulerId") ?: "", 
                    backStackEntry.arguments?.getString("category") ?: "",
                    backStackEntry.arguments?.getString("denomination") ?: ""
                )
            }
            composable("coin_detail/{coinId}") { backStackEntry ->
                CoinDetailScreen(bottomNavController, backStackEntry.arguments?.getString("coinId") ?: "")
            }
            
            // Premium Feature
            composable("premium") { PremiumScreen(bottomNavController) }
        }
    }
}

/**
 * Customized Bottom Navigation Bar with state tracking.
 */
@Composable
fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        // Catalog Tab Detection (includes sub-screens)
        val isCatalogActive = currentRoute == "catalog_root" || 
            currentRoute?.startsWith("rulers") == true || 
            currentRoute?.startsWith("categories") == true || 
            currentRoute?.startsWith("coins") == true || 
            currentRoute?.startsWith("coin_type") == true || 
            currentRoute?.startsWith("coin_detail") == true

        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, null) }, 
            label = { Text("Catalog") }, 
            selected = isCatalogActive, 
            onClick = { navigateToTab(navController, "catalog_root") }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, null) }, 
            label = { Text("Collection") }, 
            selected = currentRoute == "my_collection", 
            onClick = { navigateToTab(navController, "my_collection") }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, null) }, 
            label = { Text("Settings") }, 
            selected = currentRoute == "settings" || currentRoute == "premium", 
            onClick = { navigateToTab(navController, "settings") }
        )
    }
}

/**
 * Standardized navigation logic for tab switching.
 */
private fun navigateToTab(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { 
            saveState = true 
        }
        launchSingleTop = true
        restoreState = true
    }
}
