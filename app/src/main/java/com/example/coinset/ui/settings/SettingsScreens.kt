package com.example.coinset.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.ui.components.BulletItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Screen for user settings and account status.
 */
@Composable
fun SettingsScreen(navController: NavController, rootNavController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    var isPro by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                isPro = userDoc.getBoolean("isPro") ?: false
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = "Profile Picture",
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "Your Profile", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Email: ${Firebase.auth.currentUser?.email}", 
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
                        Text(text = "Account Status", fontWeight = FontWeight.Bold)
                        Text(text = if (isPro) "PRO Version Active" else "Free Version")
                    }
                    if (!isPro && !isLoading) {
                        Button(onClick = { navController.navigate("premium") }) {
                            Text("Upgrade to PRO")
                        }
                    } else if (isPro) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                
                // Feature List
                Text(text = "PRO Features:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                BulletItem("Add personal notes to coins", isActive = isPro)
                BulletItem("Upload photos of your coins", isActive = isPro)
                BulletItem("Total collection value estimation", isActive = isPro)
                BulletItem("Priority support", isActive = isPro)
            }
        }

        Spacer(Modifier.weight(1f))
        
        // Sign Out Button
        Button(
            onClick = { 
                Firebase.auth.signOut()
                rootNavController.navigate("login") { 
                    popUpTo(0) { inclusive = true } 
                } 
            }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer, 
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Sign Out")
        }
    }
}

/**
 * Screen for purchasing the Premium (PRO) subscription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("PRO Subscription") }, 
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
                text = "Coin Set PRO", 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            
            Text("Unlock all features:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            BulletItem("Unlimited notes for your collection")
            BulletItem("High-quality photo uploads")
            BulletItem("Market value analysis")
            BulletItem("Priority access to new features")
            
            Spacer(Modifier.weight(1f))
            
            Text("Price: 499 RUB / One-time", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            
            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (userId != null) {
                            isProcessing = true
                            db.collection("users").document(userId).update(
                                "isPro", true,
                                "proActivatedAt", Timestamp.now(),
                                "updatedAt", Timestamp.now()
                            ).addOnSuccessListener {
                                Toast.makeText(context, "Success! PRO Activated.", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }.addOnFailureListener { e ->
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Pay & Activate")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This is a payment simulation for demonstration purposes", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray
            )
        }
    }
}
