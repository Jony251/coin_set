package com.example.coinset.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.coinset.ui.components.CoinSetLogo
import com.example.coinset.ui.components.LogoStyle
import com.example.coinset.ui.components.SectionCard
import com.example.coinset.ui.components.SectionCardEmphasis
import com.example.coinset.ui.components.StatusBadge
import com.example.coinset.ui.theme.Spacing

/**
 * Landing screen after login (Vivino-style "Home" tab): a search shortcut into
 * the catalog, a decorative community banner, horizontally-scrolling carousels
 * of the user's collection and wishlist, and a news section. The news section
 * calls a real backend endpoint that today returns manually-seeded placeholder
 * rows - swapping in real PCGS content later is a backend-only change.
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
                CoinSetLogo(style = LogoStyle.IconOnly, iconSize = 32.dp, modifier = Modifier.padding(end = Spacing.sm))
                Text(stringResource(R.string.home_title))
            }
        })
    }) { padding ->
        if (isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                // Search shortcut - visual only, hands off to the real catalog search
                Box(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .clickable { navController.navigate("catalog_root") }
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Decorative community banner - no data source
                SectionCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    emphasis = SectionCardEmphasis.Brand
                ) {
                        Text(stringResource(R.string.home_stories_banner_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.home_stories_banner_subtitle), style = MaterialTheme.typography.bodyMedium)
                }

                HomeCarousel(
                    title = stringResource(R.string.home_your_collection),
                    coins = myCoins,
                    emptyHint = stringResource(R.string.home_empty_collection_hint),
                    onCoinClick = { navController.navigate("coin_detail/${it.coinId}") }
                )

                HomeCarousel(
                    title = stringResource(R.string.home_your_wishlist),
                    coins = wishlist,
                    emptyHint = stringResource(R.string.home_empty_wishlist_hint),
                    onCoinClick = { navController.navigate("coin_detail/${it.coinId}") }
                )

                NewsSection(news)

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HomeCarousel(
    title: String,
    coins: List<UserCoinResponse>,
    emptyHint: String,
    onCoinClick: (UserCoinResponse) -> Unit
) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (coins.isEmpty()) {
            Text(
                emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(coins) { coin -> HomeCoinCard(coin, onClick = { onCoinClick(coin) }) }
            }
        }
    }
}

@Composable
private fun HomeCoinCard(userCoin: UserCoinResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable { onClick() }
    ) {
        Column {
            val userPhoto = RetrofitClient.resolveImageUrl(userCoin.images.firstOrNull())
            Box(Modifier.fillMaxWidth().height(100.dp)) {
                if (!userPhoto.isNullOrEmpty()) {
                    AsyncImage(
                        model = userPhoto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (userCoin.status != "wishlist") {
                    StatusBadge(
                        text = userCoin.condition,
                        modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs)
                    )
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    text = userCoin.coinName ?: stringResource(R.string.collection_unknown_coin),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewsSection(news: List<NewsArticleResponse>) {
    val context = LocalContext.current
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            stringResource(R.string.home_news_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            news.forEach { article ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = article.url != null) {
                        article.url?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(article.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            article.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            article.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
