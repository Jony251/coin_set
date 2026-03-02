package com.example.coinset.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.Coin
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Screen displaying the user's personal coin collection and statistics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val coinsWithDetails = remember { mutableStateListOf<Pair<Coin, Map<String, Any>>>() }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch collection data on launch
    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val userCoinsList = doc.get("coins") as? List<Map<String, Any>> ?: emptyList()
                    val coinIds = userCoinsList.mapNotNull { it["catalogCoinId"] as? String }
                    
                    if (coinIds.isNotEmpty()) {
                        // Fetch catalog details for each coin in the user's collection
                        db.collection("coins").whereIn("id", coinIds).get().addOnSuccessListener { coinResult ->
                            coinsWithDetails.clear()
                            val catalogCoins = coinResult.documents.associateBy({ it.id }, { it.toObject(Coin::class.java)!! })
                            
                            userCoinsList.forEach { userCoinMap ->
                                val cid = userCoinMap["catalogCoinId"] as String
                                catalogCoins[cid]?.let { detail ->
                                    coinsWithDetails.add(detail to userCoinMap)
                                }
                            }
                            isLoading = false
                        }
                    } else {
                        coinsWithDetails.clear()
                        isLoading = false
                    }
                } else {
                    coinsWithDetails.clear()
                    isLoading = false
                }
            }.addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Collection") }) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Calculate collection statistics
            val totalCoins = coinsWithDetails.size
            val totalPriceMin = coinsWithDetails.sumOf { it.first.estimatedValueMin }
            val totalPriceMax = coinsWithDetails.sumOf { it.first.estimatedValueMax }

            Column(Modifier.padding(padding).fillMaxSize()) {
                // Statistics Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Collection Stats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Coins:")
                            Text("$totalCoins pcs.", fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Value:")
                            Text("$totalPriceMin - $totalPriceMax RUB", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (coinsWithDetails.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Collection is empty") }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(coinsWithDetails) { (coin, userData) ->
                            CollectionItem(coin, userData) {
                                navController.navigate("coin_detail/${coin.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single item row in the collection list.
 */
@Composable
fun CollectionItem(coin: Coin, userData: Map<String, Any>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val userPhoto = userData["photoUrl"] as? String
            if (!userPhoto.isNullOrEmpty()) {
                AsyncImage(
                    model = userPhoto, 
                    contentDescription = null, 
                    modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small), 
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = coin.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        modifier = Modifier.weight(1f), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary, 
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = userData["condition"] as? String ?: "UNC", 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                            color = Color.White, 
                            fontSize = 12.sp
                        )
                    }
                }
                val note = userData["notes"] as? String ?: ""
                if (note.isNotEmpty()) {
                    Text(
                        text = "Note: $note", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.secondary, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("${coin.estimatedValueMin} - ${coin.estimatedValueMax} RUB", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
