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

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text("Login Error") },
            text = { Text(showErrorDialog!!) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    fun validate(): Boolean {
        var isValid = true
        if (username.isBlank()) {
            usernameError = "Username is required"
            isValid = false
        } else {
            usernameError = null
        }
        if (password.isBlank()) {
            passwordError = "Password is required"
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
            contentDescription = "App Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Coin Set", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                if (usernameError != null) usernameError = null
            },
            label = { Text("Username") },
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
            label = { Text("Password") },
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
                                        if (it.code() == 401) "Invalid username or password"
                                        else "Server error: ${it.code()}"
                                    }
                                    else -> it.message ?: "Connection error"
                                }
                                showErrorDialog = errorMessage
                            }
                        }
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
        
        TextButton(onClick = { navController.navigate("register") }) {
            Text("Register Account")
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

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text("Registration Error") },
            text = { Text(showErrorDialog!!) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    fun validate(): Boolean {
        var isValid = true
        
        if (nickname.trim().length < 3) {
            nicknameError = "Username must be at least 3 characters"
            isValid = false
        } else {
            nicknameError = null
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }
        
        if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
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
        Text(text = "Create Account", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = nickname,
            onValueChange = { 
                nickname = it
                if (nicknameError != null) nicknameError = null
            },
            label = { Text("Username") },
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
            label = { Text("Email") },
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
            label = { Text("Password") },
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
                                        if (it.code() == 422) "This username or email is already taken."
                                        else "Server error (${it.code()}). Please try again later."
                                    }
                                    else -> it.message ?: "Connection error. Check your internet."
                                }
                                showErrorDialog = errorMessage
                            }
                        }
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
        }
    }
}
