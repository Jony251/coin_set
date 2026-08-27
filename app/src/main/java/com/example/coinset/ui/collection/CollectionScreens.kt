package com.example.coinset.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinset.R
import com.example.coinset.api.CollectionRepository
import com.example.coinset.api.RetrofitClient
import com.example.coinset.api.UserCoinResponse
import com.example.coinset.ui.components.CoinDisc
import com.example.coinset.ui.components.EmptyState
import com.example.coinset.ui.components.StatFigure
import com.example.coinset.ui.components.StatusBadge
import com.example.coinset.ui.components.ltrIsolate
import com.example.coinset.ui.theme.Spacing
import com.example.coinset.ui.theme.tabular
import kotlin.math.roundToLong

/**
 * The user's collection as a shelf rather than a ledger.
 *
 * It used to be a LazyColumn of text rows that showed an image only when the
 * user had uploaded their own photo - so a fresh collection looked like a bank
 * statement. Now every entry is a coin, with a three-step image fallback:
 * the owner's own photo, then the catalog photo, then a disc tinted with the
 * coin's metal. Something is always shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val repository = remember { CollectionRepository() }
    val coinsWithDetails = remember { mutableStateListOf<UserCoinResponse>() }
    var totalCoins by remember { mutableStateOf(0) }
    var totalPurchaseValue by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    // Stats always reflect owned coins only (backend filters status == "owned"),
    // so they stay correct regardless of which tab is showing - fetched once here.
    LaunchedEffect(Unit) {
        repository.getCollectionStats().onSuccess { stats ->
            totalCoins = stats.totalCoins
            totalPurchaseValue = stats.totalPurchaseValue
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

            // Three figures instead of two label:value sentences - the numbers
            // are the point, so they lead.
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatFigure(
                        value = totalCoins.toString(),
                        caption = stringResource(R.string.collection_stat_coins),
                        modifier = Modifier.weight(1f)
                    )
                    StatFigure(
                        value = coinsWithDetails.mapNotNull { it.rulerName }.distinct().size.toString(),
                        caption = stringResource(R.string.collection_stat_rulers),
                        modifier = Modifier.weight(1f)
                    )
                    StatFigure(
                        value = stringResource(R.string.collection_stat_value_rub, totalPurchaseValue.roundToLong().toString()),
                        caption = stringResource(R.string.collection_stat_spent),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
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
                EmptyState(
                    icon = if (selectedTab == 0) Icons.Default.AddCircle else Icons.Default.FavoriteBorder,
                    title = stringResource(
                        if (selectedTab == 0) R.string.collection_empty else R.string.collection_wishlist_empty
                    ),
                    message = stringResource(
                        if (selectedTab == 0) R.string.collection_empty_message
                        else R.string.collection_wishlist_empty_message
                    ),
                    actionLabel = stringResource(R.string.collection_empty_action),
                    onAction = { navController.navigate("catalog_root") }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(coinsWithDetails) { userCoin ->
                        CollectionTile(userCoin) {
                            navController.navigate("coin_detail/${userCoin.coinId}")
                        }
                    }
                }
            }
        }
    }
}

/**
 * The denomination out of a full catalog name.
 *
 * The backend stores the whole Numista title ("Denga - Peter I / Ivan V (with
 * the name of Peter)"), which on a half-width tile wraps to four lines and
 * pushes the year off the card. Everything before the " - " is the part that
 * belongs on a shelf label; the ruler is already the tile's caption.
 */
private fun shortCoinName(fullName: String?): String? {
    val name = fullName?.trim().orEmpty()
    if (name.isEmpty()) return null
    val separator = name.indexOf(" - ")
    return if (separator > 0) name.substring(0, separator) else name
}

/**
 * One coin on the shelf.
 */
@Composable
fun CollectionTile(userCoin: UserCoinResponse, onClick: () -> Unit) {
    // Own photo first (it's the one the collector cares about), then the
    // catalog photo, then nothing - CoinDisc draws a metal disc for null.
    val image = RetrofitClient.resolveImageUrl(userCoin.images.firstOrNull())
        ?.takeIf { it.isNotBlank() }
        ?: RetrofitClient.resolveImageUrl(userCoin.coinImageUrl)?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoinDisc(imageUrl = image, metalType = userCoin.coinMetalType, size = 84.dp)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    // Latin catalog name inside a possibly-RTL paragraph.
                    text = shortCoinName(userCoin.coinName)?.let { ltrIsolate(it) }
                        ?: stringResource(R.string.collection_unknown_coin),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val caption = listOfNotNull(
                    userCoin.coinYear?.toString(),
                    userCoin.rulerName
                ).joinToString(" · ")
                if (caption.isNotEmpty()) {
                    Text(
                        text = ltrIsolate(caption),
                        style = tabular(MaterialTheme.typography.labelSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (userCoin.status != "wishlist") {
                StatusBadge(
                    text = userCoin.condition,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm)
                )
            }
        }
    }
}
