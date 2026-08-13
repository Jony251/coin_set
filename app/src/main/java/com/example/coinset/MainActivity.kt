package com.example.coinset

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.coinset.api.TokenManager
import com.example.coinset.ui.auth.LoginScreen
import com.example.coinset.ui.auth.RegisterScreen
import com.example.coinset.ui.catalog.*
import com.example.coinset.ui.collection.MyCollectionScreen
import com.example.coinset.ui.home.HomeScreen
import com.example.coinset.ui.settings.PremiumScreen
import com.example.coinset.ui.settings.SettingsScreen
import com.example.coinset.ui.theme.CoinSetTheme
import com.example.coinset.ui.theme.Dimens

/**
 * Main Activity of the application.
 * Initializes TokenManager and sets up the root navigation.
 */
/**
 * AppCompatActivity (not plain ComponentActivity) because
 * AppCompatDelegate.setApplicationLocales() silently no-ops without it - see
 * res/values/themes.xml for the matching AppCompat-compatible theme change.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        TokenManager.init(this)
        
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
    val accessToken = TokenManager.getAccessToken()

    NavHost(
        navController = navController,
        startDestination = if (accessToken != null) "main" else "login"
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
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Primary Tabs
            composable("home") { HomeScreen(bottomNavController) }
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
 * No-op Indication (draws content only, no ripple) used to fully silence the
 * reserved camera slot in AppBottomBar - LocalIndication is non-nullable, so
 * "no ripple" means providing a real Indication that does nothing rather
 * than null. Modifier.Node-based per the current (non-deprecated) API.
 */
private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node(), DrawModifierNode {
            override fun ContentDrawScope.draw() {
                drawContent()
            }
        }
    }

    override fun equals(other: Any?) = other === this
    override fun hashCode() = System.identityHashCode(this)
}

/**
 * Customized Bottom Navigation Bar with state tracking.
 */
@Composable
fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box {
        NavigationBar {
            // Catalog Tab Detection (includes sub-screens)
            val isCatalogActive = currentRoute == "catalog_root" ||
                currentRoute?.startsWith("rulers") == true ||
                currentRoute?.startsWith("categories") == true ||
                currentRoute?.startsWith("coins") == true ||
                currentRoute?.startsWith("coin_type") == true ||
                currentRoute?.startsWith("coin_detail") == true

            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text(stringResource(R.string.nav_home)) },
                selected = currentRoute == "home",
                onClick = { navigateToTab(navController, "home") }
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Search, null) },
                label = { Text(stringResource(R.string.nav_catalog)) },
                selected = isCatalogActive,
                onClick = { navigateToTab(navController, "catalog_root") }
            )

            // Reserved for a future "photograph a coin" action. Deliberately a
            // real, tappable NavigationBarItem (not bolted on) so Material3
            // still splits the bar into 5 even slots - but every color is
            // transparent and LocalIndication is suppressed just for this one
            // item, so there's no visible icon, label, or ripple, and the
            // click is a no-op. Matches the decorative raised circle below,
            // which has no clickable of its own - this item is the only real
            // tap target for this slot.
            CompositionLocalProvider(LocalIndication provides NoRippleIndication) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, null) },
                    label = null,
                    selected = false,
                    onClick = { /* no-op: reserved for future camera capture */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Transparent,
                        unselectedIconColor = Color.Transparent,
                        selectedTextColor = Color.Transparent,
                        unselectedTextColor = Color.Transparent,
                        indicatorColor = Color.Transparent
                    )
                )
            }

            NavigationBarItem(
                icon = { Icon(Icons.Default.Favorite, null) },
                label = { Text(stringResource(R.string.nav_collection)) },
                selected = currentRoute == "my_collection",
                onClick = { navigateToTab(navController, "my_collection") }
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = currentRoute == "settings" || currentRoute == "premium",
                onClick = { navigateToTab(navController, "settings") }
            )
        }

        // Purely decorative raised circle over the invisible camera slot,
        // matching the Vivino reference look. No clickable modifier - the
        // NavigationBarItem beneath it already handles (and no-ops) the tap.
        // A faint outline (no fill) marks it as a deliberate reserved slot
        // rather than a render glitch, while keeping the icon itself hidden.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = Dimens.bottomNavRaisedCircleOffset)
                .size(Dimens.bottomNavRaisedCircle)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {}
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
