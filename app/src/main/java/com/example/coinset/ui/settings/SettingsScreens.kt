package com.example.coinset.ui.settings

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.api.AuthRepository
import com.example.coinset.api.TokenManager
import com.example.coinset.api.UserResponse
import com.example.coinset.api.VipRepository
import com.example.coinset.ui.components.BulletItem
import com.example.coinset.ui.components.CollectorAvatar
import com.example.coinset.ui.components.SectionCard
import com.example.coinset.ui.theme.Dimens
import com.example.coinset.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * A pulsing placeholder for a line of text that hasn't loaded yet - replaces
 * literally rendering the word "Loading..." inline.
 */
@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
            MaterialTheme.shapes.extraSmall
        )
    )
}

/**
 * Screen for user settings and account status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, rootNavController: NavController) {
    val authRepository = remember { AuthRepository() }
    val vipRepository = remember { VipRepository() }
    var user by remember { mutableStateOf<UserResponse?>(null) }
    var isPro by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authRepository.getCurrentUser().onSuccess { result ->
            user = result
            isLoading = false
        }.onFailure {
            isLoading = false
        }
        vipRepository.getStatus().onSuccess { status ->
            isPro = status.isVip
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CollectorAvatar(displayName = user?.email, size = Dimens.avatarLarge)
            Spacer(Modifier.height(Spacing.lg))
            Text(text = stringResource(R.string.settings_your_profile), style = MaterialTheme.typography.headlineMedium)
            if (isLoading) {
                Spacer(Modifier.height(4.dp))
                LoadingSkeleton(modifier = Modifier.width(180.dp).height(16.dp))
            } else {
                Text(
                    text = stringResource(R.string.settings_email_label, user?.email ?: ""),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            // Account Status Card
            SectionCard(modifier = Modifier.fillMaxWidth()) {
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider()
                Spacer(Modifier.height(Spacing.lg))

                // Feature List
                Text(text = stringResource(R.string.settings_pro_features_label), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.sm))
                BulletItem(stringResource(R.string.pro_feature_notes), isActive = isPro)
                BulletItem(stringResource(R.string.pro_feature_photos), isActive = isPro)
                BulletItem(stringResource(R.string.pro_feature_value_estimation), isActive = isPro)
                BulletItem(stringResource(R.string.pro_feature_priority_support), isActive = isPro)
            }

            Spacer(Modifier.height(Spacing.lg))

            // Language Selector Card
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg).clickable { showLanguageDialog = true },
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
    val vipRepository = remember { VipRepository() }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activationFailedMsg = stringResource(R.string.settings_activation_failed)

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = stringResource(R.string.settings_coin_set_pro),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.pro_one_time_purchase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // A real comparison. The previous version listed the same four
            // features twice, both times ticked, which read as "Free already
            // has everything". Only rows that genuinely differ are marked as
            // differing - the catalog and the collection really are free.
            ComparisonTable(
                rows = listOf(
                    ComparisonRow(stringResource(R.string.pro_row_catalog), free = true, pro = true),
                    ComparisonRow(stringResource(R.string.pro_row_collection), free = true, pro = true),
                    ComparisonRow(stringResource(R.string.pro_row_wishlist), free = true, pro = true),
                    ComparisonRow(stringResource(R.string.pro_feature_photos), free = false, pro = true),
                    ComparisonRow(stringResource(R.string.pro_feature_notes), free = false, pro = true),
                    ComparisonRow(stringResource(R.string.pro_feature_value_estimation), free = false, pro = true),
                    ComparisonRow(stringResource(R.string.pro_feature_priority_support), free = false, pro = true)
                )
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    isProcessing = true
                    scope.launch {
                        vipRepository.activate().onSuccess {
                            isProcessing = false
                            navController.popBackStack()
                        }.onFailure {
                            isProcessing = false
                            snackbarHostState.showSnackbar(activationFailedMsg)
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.pro_activate_button))
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

private data class ComparisonRow(val feature: String, val free: Boolean, val pro: Boolean)

/**
 * Free vs PRO, side by side.
 *
 * Built here rather than by restyling a Material list: this is a table with
 * two verdict columns, which no stock component is, and the project rule is
 * to build our own component when we want our own look.
 */
@Composable
private fun ComparisonTable(rows: List<ComparisonRow>) {
    val columnWidth = 56.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.pro_column_free),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(columnWidth)
                )
                Text(
                    text = stringResource(R.string.pro_column_pro),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(columnWidth)
                )
            }
            HorizontalDivider()
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Verdict(row.free, Modifier.width(columnWidth))
                    Verdict(row.pro, Modifier.width(columnWidth))
                }
                if (index != rows.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Verdict(included: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (included) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.pro_included),
                // tertiary = patina green = the app's one semantic "active" accent
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Dimens.iconSmall)
            )
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
