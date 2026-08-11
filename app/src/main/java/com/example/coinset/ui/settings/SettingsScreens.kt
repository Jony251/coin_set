package com.example.coinset.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.api.AuthRepository
import com.example.coinset.api.TokenManager
import com.example.coinset.api.UserResponse
import com.example.coinset.ui.components.BulletItem
import kotlinx.coroutines.launch

/**
 * Screen for user settings and account status.
 */
@Composable
fun SettingsScreen(navController: NavController, rootNavController: NavController) {
    val authRepository = remember { AuthRepository() }
    var user by remember { mutableStateOf<UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authRepository.getCurrentUser().onSuccess { result ->
            user = result
            isLoading = false
        }.onFailure { 
            isLoading = false 
        }
    }

    val isPro = user?.isAdmin ?: false
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = stringResource(R.string.settings_profile_picture_description),
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(R.string.settings_your_profile), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.settings_email_label, user?.email ?: stringResource(R.string.common_loading)),
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(24.dp))

        // Account Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = stringResource(R.string.settings_account_status), fontWeight = FontWeight.Bold)
                        Text(text = if (isPro) stringResource(R.string.settings_pro_active) else stringResource(R.string.settings_free_version))
                    }
                    if (!isPro && !isLoading) {
                        Button(onClick = { navController.navigate("premium") }) {
                            Text(stringResource(R.string.settings_upgrade_to_pro))
                        }
                    } else if (isPro) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Feature List
                Text(text = stringResource(R.string.settings_pro_features_label), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                BulletItem(stringResource(R.string.settings_feature_notes), isActive = isPro)
                BulletItem(stringResource(R.string.settings_feature_photos), isActive = isPro)
                BulletItem(stringResource(R.string.settings_feature_value_estimation), isActive = isPro)
                BulletItem(stringResource(R.string.settings_feature_priority_support), isActive = isPro)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Language Selector Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { showLanguageDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_language), fontWeight = FontWeight.Bold)
                TextButton(onClick = { showLanguageDialog = true }) {
                    Text(currentLanguageLabel())
                }
            }
        }

        if (showLanguageDialog) {
            LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
        }

        Spacer(Modifier.weight(1f))

        // Sign Out Button
        Button(
            onClick = {
                scope.launch {
                    TokenManager.clearTokens()
                    rootNavController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(stringResource(R.string.settings_sign_out))
        }
    }
}

/**
 * Returns a display label for the currently selected per-app language,
 * falling back to the "System Default" label when none is explicitly set.
 */
@Composable
private fun currentLanguageLabel(): String {
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return when {
        currentTag.startsWith("ru") -> stringResource(R.string.lang_russian)
        currentTag.startsWith("en") -> stringResource(R.string.lang_english)
        currentTag.startsWith("he") || currentTag.startsWith("iw") -> stringResource(R.string.lang_hebrew)
        else -> stringResource(R.string.settings_language_system_default)
    }
}

/**
 * Dialog letting the user pick the app's display language (Russian / English / Hebrew),
 * or reset to the device's system default. Uses the AndroidX per-app language API, which
 * persists the choice across app restarts automatically.
 */
@Composable
private fun LanguagePickerDialog(onDismiss: () -> Unit) {
    val options = listOf(
        stringResource(R.string.settings_language_system_default) to "",
        stringResource(R.string.lang_russian) to "ru",
        stringResource(R.string.lang_english) to "en",
        stringResource(R.string.lang_hebrew) to "he"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
        text = {
            Column {
                options.forEach { (label, tag) ->
                    TextButton(
                        onClick = {
                            val locales = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                                          else LocaleListCompat.forLanguageTags(tag)
                            AppCompatDelegate.setApplicationLocales(locales)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        }
    )
}

/**
 * Screen for purchasing the Premium (PRO) subscription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val proActivatedToast = stringResource(R.string.settings_pro_activated_toast)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_pro_subscription_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_coin_set_pro),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_unlock_features), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            BulletItem(stringResource(R.string.settings_premium_feature_unlimited_notes))
            BulletItem(stringResource(R.string.settings_premium_feature_hq_photos))
            BulletItem(stringResource(R.string.settings_premium_feature_market_analysis))
            BulletItem(stringResource(R.string.settings_premium_feature_priority_access))

            Spacer(Modifier.weight(1f))

            Text(stringResource(R.string.settings_price_rub), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            // Simulation: Wait for 2 seconds and succeed
                            kotlinx.coroutines.delay(2000)
                            isProcessing = false
                            Toast.makeText(context, proActivatedToast, Toast.LENGTH_LONG).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.settings_pay_activate))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_payment_simulation_notice),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
