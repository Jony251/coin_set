package com.example.coinset.ui.auth

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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for user login.
 */
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
        
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        Firebase.auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { 
                                isLoading = false
                                navController.navigate("main") { popUpTo("login") { inclusive = true } } 
                            }
                            .addOnFailureListener { 
                                isLoading = false
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() 
                            }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
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
    var isLoading by remember { mutableStateOf(false) }
    val db = Firebase.firestore
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && nickname.isNotEmpty()) {
                        isLoading = true
                        Firebase.auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val userId = result.user?.uid ?: ""
                                val now = Timestamp.now()
                                
                                // Verification deadline (now + 24 hours)
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val deadlineStr = sdf.format(Date(System.currentTimeMillis() + 86400000))

                                val userData = hashMapOf(
                                    "email" to email,
                                    "nickname" to nickname,
                                    "displayName" to nickname,
                                    "createdAt" to now,
                                    "updatedAt" to now,
                                    "proActivatedAt" to now,
                                    "isPro" to false,
                                    "emailVerified" to true,
                                    "photo" to null,
                                    "verificationDeadline" to deadlineStr
                                )
                                db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener { 
                                        isLoading = false
                                        navController.navigate("main") { popUpTo("login") { inclusive = true } } 
                                    }
                                    .addOnFailureListener { 
                                        isLoading = false
                                        Toast.makeText(context, "DB Error: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { 
                                isLoading = false
                                Toast.makeText(context, "Registration Error: ${it.message}", Toast.LENGTH_LONG).show() 
                            }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
        }
    }
}
