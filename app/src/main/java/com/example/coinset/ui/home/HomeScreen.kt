package com.example.coinset.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.R
import com.example.coinset.api.CollectionRepository
import com.example.coinset.api.NewsArticleResponse
import com.example.coinset.api.NewsRepository
import com.example.coinset.api.RetrofitClient
import com.example.coinset.api.UserCoinResponse
import com.example.coinset.ui.components.CoinDisc
import com.example.coinset.ui.components.ltrIsolate
import com.example.coinset.ui.components.CoinSetLogo
import com.example.coinset.ui.components.LogoStyle
import com.example.coinset.ui.components.StatusBadge
import com.example.coinset.ui.theme.Dimens
import com.example.coinset.ui.theme.Spacing
import com.example.coinset.ui.theme.tabular

/**
 * Landing screen after login.
 *
 * The shape of the screen is deliberate: one search affordance at the top (the
 * app's single main gesture until the camera slot is lit up), then the two
 * shelves that belong to the user - what they own and what they are hunting -
 * and only then news, which belongs to the world rather than to the collector.
 *
 * What used to be here and is gone: a "see what your community is up to"
 * banner that had no data source and no destination. Coin Set is a catalog
 * with history, not a social feed, so it was deleted rather than restyled.
 *
 * The news section calls a real backend endpoint that today returns
 * manually-seeded placeholder rows - swapping in real content later is a
 * backend-only change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val collectionRepo = remember { CollectionRepository() }
    val newsRepo = remember { NewsRepository() }
    val myCoins = remember { mutableStateListOf<UserCoinResponse>() }
    val wishlist = remember { mutableStateListOf<UserCoinResponse>() }
    val news = remember { mutableStateListOf<NewsArticleResponse>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        collectionRepo.getUserCoins(status = "owned").onSuccess {
            myCoins.clear(); myCoins.addAll(it)
        }
        collectionRepo.getUserCoins(status = "wishlist").onSuccess {
            wishlist.clear(); wishlist.addAll(it)
        }
        newsRepo.getNews(limit = 5).onSuccess {
            news.clear(); news.addAll(it)
        }
        isLoading = false
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinSetLogo(
                    style = LogoStyle.IconOnly,
                    iconSize = Dimens.iconLarge - Spacing.lg,
                    modifier = Modifier.padding(end = Spacing.sm)
                )
                Text(stringResource(R.string.home_title))
            }
        })
    }) { padding ->
        if (isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SearchEntry(onClick = { navController.navigate("catalog_root") })

                CoinShelf(
                    title = stringResource(R.string.home_your_collection),
                    count = myCoins.size,
                    coins = myCoins,
                    emptyTitle = stringResource(R.string.collection_empty),
                    emptyMessage = stringResource(R.string.collection_empty_message),
                    emptyIcon = Icons.Default.AddCircle,
                    onSeeAll = { navController.navigate("my_collection") },
                    onCoinClick = { navController.navigate("coin_detail/${it.coinId}") },
                    onExploreCatalog = { navController.navigate("catalog_root") }
                )

                CoinShelf(
                    title = stringResource(R.string.home_your_wishlist),
                    count = wishlist.size,
                    coins = wishlist,
                    emptyTitle = stringResource(R.string.collection_wishlist_empty),
                    emptyMessage = stringResource(R.string.collection_wishlist_empty_message),
                    emptyIcon = Icons.Default.FavoriteBorder,
                    onSeeAll = { navController.navigate("my_collection") },
                    onCoinClick = { navController.navigate("coin_detail/${it.coinId}") },
                    onExploreCatalog = { navController.navigate("catalog_root") }
                )

                NewsSection(news)

                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

/**
 * The search affordance.
 *
 * It looks like a search field but it isn't one: this screen has no search of
 * its own, it hands off to the catalog. It used to be a read-only
 * OutlinedTextField inside a clickable Box - and a text field swallows the tap
 * itself, so the whole thing was dead on touch while still showing a caret.
 * A Surface with a real onClick is both honest and actually tappable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A section title with the count of what is in it, and a way through to the
 * full list. The count is the small thing that makes a shelf feel owned -
 * "Your Collection 24" reads as a possession, a bare title reads as a heading.
 */
@Composable
private fun ShelfHeader(title: String, count: Int, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The title takes all the room left over by the count and the link,
        // rather than sharing it: with a weight on both sides "Ваш список
        // желаний" was being clipped to "Ваш список жела…" while half the row
        // sat empty.
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = count.toString(),
                style = tabular(MaterialTheme.typography.titleMedium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0 && onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.home_see_all))
            }
        }
    }
}

/**
 * One horizontal shelf of the user's coins.
 *
 * Empty, it keeps the same height as a filled shelf and offers the way out
 * instead of collapsing into a stray grey sentence - the old version put a
 * tiny icon and a line of secondary-coloured text loose on the background,
 * which read as an error rather than an invitation.
 */
@Composable
private fun CoinShelf(
    title: String,
    count: Int,
    coins: List<UserCoinResponse>,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: ImageVector,
    onSeeAll: () -> Unit,
    onCoinClick: (UserCoinResponse) -> Unit,
    onExploreCatalog: () -> Unit
) {
    Column(Modifier.padding(top = Spacing.lg)) {
        ShelfHeader(title = title, count = count, onSeeAll = onSeeAll)
        Spacer(Modifier.height(Spacing.md))
        if (coins.isEmpty()) {
            ShelfInvite(
                icon = emptyIcon,
                title = emptyTitle,
                message = emptyMessage,
                onAction = onExploreCatalog
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(coins) { coin -> HomeCoinCard(coin, onClick = { onCoinClick(coin) }) }
            }
        }
    }
}

/** The empty shelf: a card of the same weight as the coins that will fill it. */
@Composable
private fun ShelfInvite(
    icon: ImageVector,
    title: String,
    message: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        // The action sits under the sentence, not beside it: side by side, the
        // button ate a third of the width and "Монеты, добавленные из
        // каталога..." wrapped into five short ragged lines.
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
                Spacer(Modifier.width(Spacing.md))
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            TextButton(
                onClick = onAction,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.collection_empty_action))
            }
        }
    }
}

@Composable
private fun HomeCoinCard(userCoin: UserCoinResponse, onClick: () -> Unit) {
    // Same three-step fallback as the collection shelf: own photo, catalog
    // photo, metal disc. Previously a coin with no uploaded photo rendered as
    // an empty grey rectangle on the app's landing screen.
    val image = RetrofitClient.resolveImageUrl(userCoin.images.firstOrNull())
        ?.takeIf { it.isNotBlank() }
        ?: RetrofitClient.resolveImageUrl(userCoin.coinImageUrl)?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.width(Dimens.carouselCardWidth).clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoinDisc(imageUrl = image, metalType = userCoin.coinMetalType, size = 72.dp)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    // Latin catalog name inside a possibly-RTL paragraph.
                    text = shortCoinName(userCoin.coinName)?.let { ltrIsolate(it) }
                        ?: stringResource(R.string.collection_unknown_coin),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                userCoin.coinYear?.let {
                    Text(
                        text = it.toString(),
                        style = tabular(MaterialTheme.typography.labelSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

/** Denomination out of the full Numista title - see CollectionScreens. */
private fun shortCoinName(fullName: String?): String? {
    val name = fullName?.trim().orEmpty()
    if (name.isEmpty()) return null
    val separator = name.indexOf(" - ")
    return if (separator > 0) name.substring(0, separator) else name
}

/**
 * News.
 *
 * The endpoint has always returned an image_url and the screen has always
 * ignored it, so every article was a wall of three text sizes. A thumbnail
 * makes the list scannable; articles without one keep their text full-width
 * rather than reserving an empty grey square.
 *
 * Hidden entirely when there is nothing to show: an empty "News" heading over
 * blank space is worse than no section.
 */
@Composable
private fun NewsSection(news: List<NewsArticleResponse>) {
    if (news.isEmpty()) return
    val context = LocalContext.current
    Column(Modifier.padding(top = Spacing.xxl)) {
        ShelfHeader(
            title = stringResource(R.string.home_news_title),
            count = 0,
            onSeeAll = null
        )
        Spacer(Modifier.height(Spacing.md))
        Column(
            Modifier.padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            news.forEach { article -> NewsCard(article, context) }
        }
    }
}

@Composable
private fun NewsCard(article: NewsArticleResponse, context: android.content.Context) {
    val image = RetrofitClient.resolveImageUrl(article.imageUrl)?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = article.url != null) {
            article.url?.let { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(Spacing.md)) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(Spacing.sm))
                )
                Spacer(Modifier.width(Spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = listOfNotNull(
                        article.source.takeIf { it.isNotBlank() },
                        shortDate(article.publishedAt)
                    ).joinToString(" · "),
                    style = tabular(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * "2026-08-26T09:12:00Z" -> "2026-08-26". Deliberately not locale-formatted:
 * the value is an ISO timestamp from the API and the only part worth showing
 * is the date, cut without pulling a formatter (and without risking a parse
 * exception on a shape the backend might change).
 */
private fun shortDate(raw: String?): String? {
    val t = raw?.trim().orEmpty()
    if (t.length < 10) return t.ifBlank { null }
    return t.substring(0, 10)
}
