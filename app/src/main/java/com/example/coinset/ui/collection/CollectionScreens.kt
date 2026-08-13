package com.example.coinset.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.R
import com.example.coinset.api.CollectionRepository
import com.example.coinset.api.RetrofitClient
import com.example.coinset.api.UserCoinResponse
import com.example.coinset.ui.components.SectionCard
import com.example.coinset.ui.components.StatusBadge
import com.example.coinset.ui.theme.Spacing

/**
 * Screen displaying the user's personal coin collection and statistics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val repository = remember { CollectionRepository() }
    val coinsWithDetails = remember { mutableStateListOf<UserCoinResponse>() }
    var totalCoins by remember { mutableStateOf(0) }
    var totalPurchaseValue by remember { mutableStateOf(0.0) }
    var totalSellingValue by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    // Stats always reflect owned coins only (backend filters status == "owned"),
    // so they stay correct regardless of which tab is showing - fetched once here.
    LaunchedEffect(Unit) {
        repository.getCollectionStats().onSuccess { stats ->
            totalCoins = stats.totalCoins
            totalPurchaseValue = stats.totalPurchaseValue
            totalSellingValue = stats.totalSellingValue
        }
    }

    LaunchedEffect(selectedTab) {
        isLoading = true
        val status = if (selectedTab == 0) "owned" else "wishlist"
        repository.getUserCoins(status = status).onSuccess { result ->
            coinsWithDetails.clear()
            coinsWithDetails.addAll(result)
            isLoading = false
        }.onFailure { isLoading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.collection_title)) }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
                // Statistics Card
                SectionCard(modifier = Modifier.fillMaxWidth().padding(Spacing.sm)) {
                        Text(stringResource(R.string.collection_stats), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.collection_total_coins))
                            Text(stringResource(R.string.collection_total_coins_value, totalCoins), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.collection_total_purchase_value))
                            Text(stringResource(R.string.collection_total_purchase_value_rub, totalPurchaseValue.toString()), fontWeight = FontWeight.Bold)
                        }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text(stringResource(R.string.collection_tab_my_collection)) }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text(stringResource(R.string.collection_tab_wishlist)) }
                    )
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (coinsWithDetails.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.AddCircle else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(stringResource(if (selectedTab == 0) R.string.collection_empty else R.string.collection_wishlist_empty))
                            TextButton(onClick = { navController.navigate("catalog_root") }) {
                                Text(stringResource(R.string.catalog_title))
                            }
                        }
                    }
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
            val userPhoto = RetrofitClient.resolveImageUrl(userCoin.images.firstOrNull())
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
                        text = userCoin.coinName ?: stringResource(R.string.collection_unknown_coin),
                        style = MaterialTheme.typography.titleMedium, 
                        modifier = Modifier.weight(1f), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    if (userCoin.status != "wishlist") {
                        StatusBadge(text = userCoin.condition)
                    }
                }
                val note = userCoin.notes ?: ""
                if (note.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.collection_note_prefix, note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(stringResource(R.string.collection_price_rub, (userCoin.purchasePrice ?: 0.0).toString()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
