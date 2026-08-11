package com.example.coinset.ui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.api.AuthRepository
import kotlinx.coroutines.launch

/**
 * Screen for user login.
 */
@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }

    val usernameRequiredMsg = stringResource(R.string.auth_error_username_required)
    val passwordRequiredMsg = stringResource(R.string.auth_error_password_required)
    val invalidCredentialsMsg = stringResource(R.string.auth_error_invalid_credentials)
    val serverErrorTemplate = stringResource(R.string.auth_error_server)
    val connectionErrorMsg = stringResource(R.string.auth_error_connection)

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text(stringResource(R.string.auth_login_error_title)) },
            text = { Text(showErrorDialog!!) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = stringResource(R.string.auth_app_icon_description),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.auth_app_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                if (usernameError != null) usernameError = null
            },
            label = { Text(stringResource(R.string.auth_label_username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = usernameError != null,
            supportingText = { if (usernameError != null) Text(usernameError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            label = { Text(stringResource(R.string.auth_label_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError!!) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
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
                                showErrorDialog = errorMessage
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_action_login))
            }
        }

        TextButton(onClick = { navController.navigate("register") }) {
            Text(stringResource(R.string.auth_action_register_account))
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
    var nickname by remember { mutableStateOf("") }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nicknameError by remember { mutableStateOf<String?>(null) }
    
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }

    val nicknameMinLengthMsg = stringResource(R.string.auth_error_username_min_length)
    val emailInvalidMsg = stringResource(R.string.auth_error_email_invalid)
    val passwordMinLengthMsg = stringResource(R.string.auth_error_password_min_length)
    val usernameEmailTakenMsg = stringResource(R.string.auth_error_username_email_taken)
    val serverErrorRetryTemplate = stringResource(R.string.auth_error_server_retry)
    val connectionErrorCheckInternetMsg = stringResource(R.string.auth_error_connection_check_internet)

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text(stringResource(R.string.auth_register_error_title)) },
            text = { Text(showErrorDialog!!) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.auth_create_account_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = {
                nickname = it
                if (nicknameError != null) nicknameError = null
            },
            label = { Text(stringResource(R.string.auth_label_username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nicknameError != null,
            supportingText = { if (nicknameError != null) Text(nicknameError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) emailError = null
            },
            label = { Text(stringResource(R.string.auth_label_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            label = { Text(stringResource(R.string.auth_label_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError!!) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
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
                                showErrorDialog = errorMessage
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_action_register))
            }
        }
    }
}
