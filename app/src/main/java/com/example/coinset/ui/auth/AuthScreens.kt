package com.example.coinset.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.api.AuthRepository
import com.example.coinset.ui.components.CoinSetLogo
import com.example.coinset.ui.components.LogoStyle
import com.example.coinset.ui.components.SectionCard
import com.example.coinset.ui.theme.Dimens
import com.example.coinset.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Screen for user login.
 */
@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    val usernameRequiredMsg = stringResource(R.string.auth_error_username_required)
    val passwordRequiredMsg = stringResource(R.string.auth_error_password_required)
    val invalidCredentialsMsg = stringResource(R.string.auth_error_invalid_credentials)
    val serverErrorTemplate = stringResource(R.string.auth_error_server)
    val connectionErrorMsg = stringResource(R.string.auth_error_connection)
    val showPasswordDesc = stringResource(R.string.auth_show_password)
    val hidePasswordDesc = stringResource(R.string.auth_hide_password)

    fun validate(): Boolean {
        var isValid = true
        if (username.isBlank()) {
            usernameError = usernameRequiredMsg
            isValid = false
        } else {
            usernameError = null
        }
        if (password.isBlank()) {
            passwordError = passwordRequiredMsg
            isValid = false
        } else {
            passwordError = null
        }
        return isValid
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.xxxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoinSetLogo(style = LogoStyle.Full)
            Spacer(modifier = Modifier.height(Spacing.xxl))

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (usernameError != null) usernameError = null
                    },
                    label = { Text(stringResource(R.string.auth_label_username)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = usernameError != null,
                    supportingText = { if (usernameError != null) Text(usernameError!!) }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = { Text(stringResource(R.string.auth_label_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) hidePasswordDesc else showPasswordDesc
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = { if (passwordError != null) Text(passwordError!!) }
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxl))

            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            repository.login(username.trim(), password).onSuccess {
                                isLoading = false
                                navController.navigate("main") { popUpTo("login") { inclusive = true } }
                            }.onFailure {
                                isLoading = false
                                val errorMessage = when (it) {
                                    is retrofit2.HttpException -> {
                                        if (it.code() == 401) invalidCredentialsMsg
                                        else String.format(serverErrorTemplate, it.code())
                                    }
                                    else -> it.message ?: connectionErrorMsg
                                }
                                scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.auth_action_login))
                }
            }

            TextButton(onClick = { navController.navigate("register") }) {
                Text(stringResource(R.string.auth_action_register_account))
            }
        }
    }
}

/**
 * Screen for new user registration.
 */
@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nicknameError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    val nicknameMinLengthMsg = stringResource(R.string.auth_error_username_min_length)
    val emailInvalidMsg = stringResource(R.string.auth_error_email_invalid)
    val passwordMinLengthMsg = stringResource(R.string.auth_error_password_min_length)
    val usernameEmailTakenMsg = stringResource(R.string.auth_error_username_email_taken)
    val serverErrorRetryTemplate = stringResource(R.string.auth_error_server_retry)
    val connectionErrorCheckInternetMsg = stringResource(R.string.auth_error_connection_check_internet)
    val showPasswordDesc = stringResource(R.string.auth_show_password)
    val hidePasswordDesc = stringResource(R.string.auth_hide_password)

    fun validate(): Boolean {
        var isValid = true

        if (nickname.trim().length < 3) {
            nicknameError = nicknameMinLengthMsg
            isValid = false
        } else {
            nicknameError = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = emailInvalidMsg
            isValid = false
        } else {
            emailError = null
        }

        if (password.length < 6) {
            passwordError = passwordMinLengthMsg
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.xxxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoinSetLogo(style = LogoStyle.Full)
            Spacer(modifier = Modifier.height(Spacing.xxl))

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it
                        if (nicknameError != null) nicknameError = null
                    },
                    label = { Text(stringResource(R.string.auth_label_username)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nicknameError != null,
                    supportingText = { if (nicknameError != null) Text(nicknameError!!) }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text(stringResource(R.string.auth_label_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = { if (emailError != null) Text(emailError!!) }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = { Text(stringResource(R.string.auth_label_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) hidePasswordDesc else showPasswordDesc
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = { if (passwordError != null) Text(passwordError!!) }
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxl))

            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            repository.register(nickname.trim(), email.trim(), password).onSuccess {
                                repository.login(nickname.trim(), password).onSuccess {
                                    isLoading = false
                                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                                }.onFailure {
                                    isLoading = false
                                    navController.navigate("login") { popUpTo("register") { inclusive = true } }
                                }
                            }.onFailure {
                                isLoading = false
                                val errorMessage = when (it) {
                                    is retrofit2.HttpException -> {
                                        if (it.code() == 422) usernameEmailTakenMsg
                                        else String.format(serverErrorRetryTemplate, it.code())
                                    }
                                    else -> it.message ?: connectionErrorCheckInternetMsg
                                }
                                scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.auth_action_register))
                }
            }
        }
    }
}
