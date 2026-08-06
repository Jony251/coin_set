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
import com.example.coinset.api.CollectionRepository
import com.example.coinset.api.UserCoinResponse

/**
 * Screen displaying the user's personal coin collection and statistics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val repository = remember { CollectionRepository() }
    val coinsWithDetails = remember { mutableStateListOf<UserCoinResponse>() }
    var totalPurchaseValue by remember { mutableStateOf(0.0) }
    var totalSellingValue by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch collection data on launch
    LaunchedEffect(Unit) {
        repository.getUserCoins().onSuccess { result ->
            coinsWithDetails.clear()
            coinsWithDetails.addAll(result)
            
            repository.getCollectionStats().onSuccess { stats ->
                totalPurchaseValue = stats.totalPurchaseValue
                totalSellingValue = stats.totalSellingValue
            }
            isLoading = false
        }.onFailure { isLoading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Collection") }) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Calculate collection statistics
            val totalCoins = coinsWithDetails.size

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
                            Text("Total Purchase Value:")
                            Text("$totalPurchaseValue RUB", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (coinsWithDetails.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Collection is empty") }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(coinsWithDetails) { userCoin ->
                            CollectionItem(userCoin) {
                                navController.navigate("coin_detail/${userCoin.coinId}")
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
fun CollectionItem(userCoin: UserCoinResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val userPhoto = userCoin.images.firstOrNull()
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
                        text = userCoin.coinName ?: "Unknown Coin", 
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
                            text = userCoin.condition, 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                            color = Color.White, 
                            fontSize = 12.sp
                        )
                    }
                }
                val note = userCoin.notes ?: ""
                if (note.isNotEmpty()) {
                    Text(
                        text = "Note: $note", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.secondary, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("${userCoin.purchasePrice ?: 0.0} RUB", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
