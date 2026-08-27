package com.example.coinset.ui.catalog

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.R
import com.example.coinset.api.*
import com.example.coinset.ui.components.*
import com.example.coinset.ui.theme.Spacing
import com.example.coinset.ui.theme.tabular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * The backend's metal_type/rarity fields are fixed enums always returned in
 * English (e.g. "gold", "extremely_rare") regardless of the ?lang= the app
 * sends for catalog text - only the label needs stringResource(), the enum
 * *value* itself needs mapping to a resource, or it shows English text
 * inside an otherwise-translated screen.
 */
@Composable
private fun localizedMetalType(metalType: String): String = when (metalType.lowercase()) {
    "gold" -> stringResource(R.string.metal_gold)
    "silver" -> stringResource(R.string.metal_silver)
    "copper" -> stringResource(R.string.metal_copper)
    "bronze" -> stringResource(R.string.metal_bronze)
    "brass" -> stringResource(R.string.metal_brass)
    else -> stringResource(R.string.metal_other)
}

// Catalog category browsing uses stable English keys ("gold"/"silver"/"copper"/
// "other"/"trial") in routes and matching logic - never the localized label - so
// switching the app language in Settings can't silently break which coins
// show up under a category. Only the displayed text is localized.
private val CATALOG_CATEGORY_KEYS = listOf("gold", "silver", "copper", "other", "trial")

@Composable
private fun categoryDisplayName(categoryKey: String): String = when (categoryKey) {
    "gold" -> stringResource(R.string.metal_gold)
    "silver" -> stringResource(R.string.metal_silver)
    "copper" -> stringResource(R.string.metal_copper)
    "other" -> stringResource(R.string.metal_other)
    "trial" -> stringResource(R.string.catalog_category_trial)
    else -> categoryKey
}

/** The metal a category tile should be tinted with; "trial" has no metal. */
private fun categoryMetal(categoryKey: String): String? = when (categoryKey) {
    "gold" -> "gold"
    "silver" -> "silver"
    "copper" -> "copper"
    else -> null
}

/**
 * Single source of truth for "does this coin belong under that category tile",
 * shared by the denomination list and the coin list so the two can't drift.
 *
 * Two things this fixes. "other" is a new tile: 476 Soviet and modern Russian
 * coins carry metal_type "other" and had no category at all, so a third of that
 * catalog was effectively unreachable. And "trial" used to be a catch-all that
 * matched every coin regardless of metal - it now actually looks for pattern
 * and trial strikes, which Numista marks in the title ("(Pattern)", "Obverse
 * Trial"). Those are ~89 coins out of ~2465, not all of them.
 *
 * A pattern coin still also appears under its own metal, which is intended -
 * the tiles are browsing paths, not an exclusive partition.
 */
private fun coinMatchesCategory(name: String, metalType: String?, category: String): Boolean {
    val metal = (metalType ?: "").lowercase()
    return when (category) {
        "gold" -> metal == "gold"
        "silver" -> metal == "silver"
        "copper" -> metal == "copper" || metal == "bronze"
        "other" -> metal == "other" || metal == "brass" || metal.isBlank()
        "trial" -> name.lowercase().let { it.contains("pattern") || it.contains("trial") }
        else -> false
    }
}

@Composable
private fun localizedRarity(rarity: String): String = when (rarity.lowercase()) {
    "uncommon" -> stringResource(R.string.rarity_uncommon)
    "rare" -> stringResource(R.string.rarity_rare)
    "very_rare" -> stringResource(R.string.rarity_very_rare)
    "extremely_rare" -> stringResource(R.string.rarity_extremely_rare)
    else -> stringResource(R.string.rarity_common)
}

/** Anything above "common" earns the highlighted chip treatment. */
private fun isNotableRarity(rarity: String) = rarity.lowercase() !in setOf("", "common")

/**
 * The part of a coin's catalog name that says which *variety* it is.
 *
 * Numista names read "Denga - Peter I / Ivan V (with the name of Peter)":
 * denomination, then the thing that tells two coins of the same year apart.
 * Showing `series` here instead printed "Стандартные обращаемые монеты" on
 * every single row, which distinguished nothing - two 1682 dengas looked
 * identical. Strips the denomination prefix and keeps the rest.
 */
private fun varietyOf(coin: CoinResponse): String? {
    val name = coin.name.trim()
    // Numista separates denomination from variety with " - ". Matching on the
    // separator rather than on the denomination string is what works here:
    // denomination is "1 Denga" while the name begins "Denga", so a prefix
    // comparison never fires.
    val separator = name.indexOf(" - ")
    if (separator >= 0) {
        return name.substring(separator + 3).trim().ifBlank { null }
    }
    val denomination = coin.denomination?.trim()
    if (!denomination.isNullOrEmpty() && name.startsWith(denomination, ignoreCase = true)) {
        return name.removeRange(0, denomination.length).trimStart(' ', '-', '–', '—')
            .ifBlank { null }
    }
    return coin.series
}

/**
 * Trims a measurement to at most one decimal and uses the locale's decimal
 * separator, so a Russian screen shows "0,2 г" rather than Double.toString()'s
 * "0.2" - and a whole number shows as "5", not "5.0".
 */
private fun formatMeasure(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) {
        rounded.toLong().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", rounded)
    }
}

/**
 * A reign or era as a range. An open-ended period ("1991 - " with a dangling
 * dash on screen today) reads as "с 1991" instead, and a one-year reign
 * collapses to a single year rather than "1762 - 1762".
 */
@Composable
private fun yearRange(start: Int, end: Int?): String = when {
    start <= 0 -> ""
    end == null || end <= 0 -> stringResource(R.string.catalog_year_since, start)
    end == start -> start.toString()
    else -> ltrIsolate(stringResource(R.string.catalog_year_range, start, end))
}

/**
 * Screen displaying countries with advanced search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(navController: NavController) {
    val repository = remember { CatalogRepository() }
    val countries = remember { mutableStateListOf<CountryResponse>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.getCountries().onSuccess { result ->
            countries.clear()
            countries.addAll(result)
            isLoading = false
        }.onFailure { isLoading = false }
    }

    val filteredCountries = countries.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    // Country search is client-side filtering over the already-fetched list (no
    // server round trip per keystroke), so a miss needs its own report. Debounced
    // so we log once per pause in typing, not on every character.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && countries.isNotEmpty()) {
            delay(1000)
            val stillMissing = countries.none { it.name.contains(searchQuery, ignoreCase = true) }
            if (stillMissing) {
                repository.logCountrySearchMiss(searchQuery)
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinSetLogo(style = LogoStyle.IconOnly, iconSize = 32.dp, modifier = Modifier.padding(end = Spacing.sm))
                    Text(stringResource(R.string.catalog_title))
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                placeholder = { Text(stringResource(R.string.catalog_search_country_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (filteredCountries.isEmpty() && searchQuery.isNotBlank()) {
                EmptyState(
                    icon = Icons.Default.TravelExplore,
                    title = stringResource(R.string.catalog_country_not_found, searchQuery),
                    message = stringResource(R.string.catalog_country_not_found_hint)
                )
            } else {
                LazyColumn {
                    items(filteredCountries) { country ->
                        ListItem(
                            headlineContent = { Text(country.name, style = MaterialTheme.typography.titleMedium) },
                            // Was the raw ISO code ("RU") - a database column shown to
                            // the user where something useful belongs.
                            supportingContent = {
                                if (country.periodsCount > 0) {
                                    Text(
                                        text = stringResource(R.string.catalog_periods_count, country.periodsCount),
                                        style = tabular(MaterialTheme.typography.bodySmall),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingContent = { CountryFlag(code = country.code) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) },
                            modifier = Modifier.clickable {
                                navController.navigate("periods/${country.id}/${country.name}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Period picker between Country and Ruler - lets a country span multiple
 * historical eras (e.g. Russia: Empire / USSR / Federation) without those
 * eras being modeled as separate top-level countries. A country with only
 * one period skips straight to its rulers instead of showing a 1-item list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodListScreen(navController: NavController, countryId: String, countryName: String) {
    val repository = remember { CatalogRepository() }
    val periods = remember { mutableStateListOf<PeriodResponse>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(countryId) {
        val id = countryId.toIntOrNull()
        if (id != null) {
            repository.getPeriods(id).onSuccess { result ->
                if (result.size == 1) {
                    val only = result[0]
                    navController.navigate("rulers/${only.id}/${only.name}") {
                        popUpTo("periods/$countryId/$countryName") { inclusive = true }
                    }
                    return@onSuccess
                }
                periods.clear()
                periods.addAll(result)
                isLoading = false
            }.onFailure { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(countryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(periods) { period ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { navController.navigate("rulers/${period.id}/${period.name}") },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                Icon(Icons.Default.History, null, Modifier.padding(Spacing.md), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(period.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = yearRange(period.periodStart, period.periodEnd),
                                    style = tabular(MaterialTheme.typography.bodyMedium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (period.rulersCount > 0) {
                                    Text(
                                        text = stringResource(R.string.catalog_rulers_count, period.rulersCount),
                                        style = tabular(MaterialTheme.typography.labelSmall),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lists the rulers within one historical period.
 *
 * Previously fourteen identical grey slabs carrying an identical generic
 * person glyph, where the only thing telling Peter I from Ivan VI was a pair
 * of years. Now each ruler is a chapter heading: portrait, reign, and the
 * opening of their description.
 *
 * Order comes from the server (chronological) and is deliberately not
 * re-sorted here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerListScreen(navController: NavController, periodId: String, periodName: String) {
    val repository = remember { CatalogRepository() }
    val rulers = remember { mutableStateListOf<RulerResponse>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(periodId) {
        val id = periodId.toIntOrNull()
        if (id != null) {
            repository.getPeriodWithRulers(id).onSuccess { result ->
                rulers.clear()
                rulers.addAll(result.rulers)
                isLoading = false
            }.onFailure { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(periodName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (rulers.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                icon = Icons.Default.Person,
                title = stringResource(R.string.catalog_no_rulers_title),
                message = stringResource(R.string.catalog_no_rulers_message)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(rulers) { ruler -> RulerCard(ruler) { navController.navigate("ruler/${ruler.id}") } }
            }
        }
    }
}

@Composable
private fun RulerCard(ruler: RulerResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(Spacing.md)) {
            RulerPortrait(
                imageUrl = ruler.imageUrl,
                name = ruler.name,
                width = 64.dp
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ruler.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val reign = yearRange(ruler.periodStart, ruler.periodEnd)
                if (reign.isNotEmpty()) {
                    Text(
                        text = reign,
                        style = tabular(MaterialTheme.typography.labelLarge),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!ruler.description.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = ruler.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (ruler.coinsCount > 0) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.catalog_coins_count, ruler.coinsCount),
                        style = tabular(MaterialTheme.typography.labelSmall),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * The ruler's own screen, which used to be a bare five-row list of metal
 * names with the ruler reduced to a title-bar string.
 *
 * The portrait is a card, not a full-bleed header: the backend's portraits are
 * 250px wide, and stretching one across a 1080px screen would be visibly soft.
 * When larger originals land at the same URLs this layout only gets sharper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerScreen(navController: NavController, rulerId: String) {
    val repository = remember { CatalogRepository() }
    var ruler by remember { mutableStateOf<RulerResponse?>(null) }
    val coins = remember { mutableStateListOf<CoinResponse>() }
    var isLoading by remember { mutableStateOf(true) }
    var descriptionExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(rulerId) {
        val id = rulerId.toIntOrNull()
        if (id != null) {
            repository.getRuler(id).onSuccess { ruler = it }
            // Paged fetch: the per-metal counts must cover the whole catalog for
            // this ruler, not just the first page.
            repository.getCoins(rulerId = id).onSuccess {
                coins.clear(); coins.addAll(it)
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ruler?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val current = ruler
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (current != null) {
                    item {
                        Row {
                            RulerPortrait(
                                imageUrl = current.imageUrl,
                                name = current.name,
                                width = 108.dp
                            )
                            Spacer(Modifier.width(Spacing.lg))
                            Column(Modifier.weight(1f)) {
                                if (!current.countryName.isNullOrBlank()) {
                                    Text(
                                        text = current.countryName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(current.name, style = MaterialTheme.typography.headlineSmall)
                                val reign = yearRange(current.periodStart, current.periodEnd)
                                if (reign.isNotEmpty()) {
                                    Text(
                                        text = reign,
                                        style = tabular(MaterialTheme.typography.titleMedium),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (coins.isNotEmpty()) {
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        text = stringResource(R.string.catalog_coins_count, coins.size),
                                        style = tabular(MaterialTheme.typography.bodySmall),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // The backend now returns 3-5 sentences here, so this can't
                    // assume a single line - it collapses to four and expands.
                    if (!current.description.isNullOrBlank()) {
                        item {
                            Column(Modifier.animateContentSize()) {
                                Text(
                                    text = current.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { descriptionExpanded = !descriptionExpanded },
                                    contentPadding = PaddingValues(vertical = Spacing.xs, horizontal = 0.dp)
                                ) {
                                    Text(
                                        stringResource(
                                            if (descriptionExpanded) R.string.catalog_show_less
                                            else R.string.catalog_show_more
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.catalog_browse_by_metal),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Category tiles carry their own count, so a dead end is visible
                // before you tap it rather than after.
                items(CATALOG_CATEGORY_KEYS.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        pair.forEach { key ->
                            val count = coins.count { coinMatchesCategory(it.name, it.metalType, key) }
                            MetalTile(
                                categoryKey = key,
                                label = categoryDisplayName(key),
                                count = count,
                                modifier = Modifier.weight(1f)
                            ) { navController.navigate("coins/$rulerId/$key") }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetalTile(
    categoryKey: String,
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val enabled = count > 0
    Card(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(Spacing.md).graphicsLayer { alpha = if (enabled) 1f else 0.45f },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinDisc(imageUrl = null, metalType = categoryMetal(categoryKey), size = 28.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.catalog_coins_count, count),
                    style = tabular(MaterialTheme.typography.labelSmall),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(navController: NavController, rulerId: String, category: String) {
    val repository = remember { CatalogRepository() }
    val denominations = remember { mutableStateListOf<Pair<String, Int>>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(rulerId, category) {
        val rId = rulerId.toIntOrNull()
        if (rId != null) {
            repository.getCoins(rulerId = rId).onSuccess { result ->
                val counts = linkedMapOf<String, Int>()
                for (coin in result) {
                    if (coinMatchesCategory(coin.name, coin.metalType, category)) {
                        val key = coin.denomination ?: coin.name
                        counts[key] = (counts[key] ?: 0) + 1
                    }
                }
                denominations.clear()
                denominations.addAll(counts.toList().sortedBy { it.first })
                isLoading = false
            }.onFailure { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(categoryDisplayName(category)) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (denominations.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.catalog_no_denominations_found),
                message = stringResource(R.string.catalog_no_denominations_hint),
                actionLabel = stringResource(R.string.catalog_back_to_metals),
                onAction = { navController.popBackStack() }
            )
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(denominations) { (den, count) ->
                    ListItem(
                        headlineContent = { Text(den, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.catalog_coins_count, count),
                                style = tabular(MaterialTheme.typography.bodySmall),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = { CoinDisc(imageUrl = null, metalType = categoryMetal(category), size = 36.dp) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) },
                        modifier = Modifier.clickable { navController.navigate("coin_type/$rulerId/$category/$den") }
                    )
                }
            }
        }
    }
}

/**
 * All coins of one denomination.
 *
 * This screen used to render each coin as its raw Numista description - six
 * lines of English catalogue prose beginning with the year, three coins to a
 * screenful. The year is what people scan by, so it leads as a tabular figure;
 * the prose moves to the coin's own screen, where there is room for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTypeScreen(navController: NavController, rulerId: String, category: String, denomination: String) {
    val repository = remember { CatalogRepository() }
    val collectionRepo = remember { CollectionRepository() }
    val coins = remember { mutableStateListOf<CoinResponse>() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val rId = rulerId.toIntOrNull()
        if (rId != null) {
            repository.getCoins(rulerId = rId).onSuccess { result ->
                coins.clear()
                for (coin in result) {
                    val currentDenomination = coin.denomination ?: coin.name
                    if (currentDenomination == denomination &&
                        coinMatchesCategory(coin.name, coin.metalType, category)
                    ) {
                        coins.add(coin)
                    }
                }
                coins.sortBy { it.year }
                isLoading = false
            }.onFailure { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(denomination, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (coins.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.catalog_no_coins_title),
                message = stringResource(R.string.catalog_no_coins_message),
                actionLabel = stringResource(R.string.catalog_back_to_metals),
                onAction = { navController.popBackStack() }
            )
        } else {
            val addedText = stringResource(R.string.catalog_toast_added)
            val errorTemplate = stringResource(R.string.catalog_toast_error)
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(coins) { coin ->
                    CoinRowCard(
                        coin = coin,
                        onClick = { navController.navigate("coin_detail/${coin.id}") },
                        onAdd = {
                            scope.launch {
                                collectionRepo.addCoinToCollection(coin.id, "UNC").onSuccess { _: UserCoinResponse ->
                                    Toast.makeText(context, addedText, Toast.LENGTH_SHORT).show()
                                }.onFailure { e: Throwable ->
                                    Toast.makeText(context, String.format(errorTemplate, e.message), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinRowCard(coin: CoinResponse, onClick: () -> Unit, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            CoinDisc(imageUrl = coin.imageUrl, metalType = coin.metalType, size = 52.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = coin.year?.toString() ?: coin.denomination.orEmpty(),
                    style = tabular(MaterialTheme.typography.titleLarge)
                )
                varietyOf(coin)?.let { variety ->
                    Text(
                        text = variety,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    coin.weight?.takeIf { it > 0 }?.let {
                        FactChip(stringResource(R.string.catalog_value_grams, formatMeasure(it)))
                    }
                    if (isNotableRarity(coin.rarity)) {
                        FactChip(localizedRarity(coin.rarity), emphasized = true)
                    }
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.AddCircle, contentDescription = stringResource(R.string.catalog_action_add_to_collection), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(navController: NavController, coinId: String) {
    val repository = remember { CatalogRepository() }
    val collectionRepo = remember { CollectionRepository() }
    val authRepository = remember { AuthRepository() }
    val context = LocalContext.current

    var coin by remember { mutableStateOf<CoinResponse?>(null) }
    var userCoinData by remember { mutableStateOf<UserCoinResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var descriptionExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // rememberSaveable so an in-progress note survives an Activity recreation
    // (e.g. screen rotation) instead of being silently wiped.
    var noteText by rememberSaveable { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    val vipRequiredMsg = stringResource(R.string.catalog_vip_feature_message)
    val upgradeActionLabel = stringResource(R.string.catalog_upgrade_action)
    val saveFailedMsg = stringResource(R.string.catalog_save_failed)

    suspend fun handleVipGate() {
        val result = snackbarHostState.showSnackbar(
            message = vipRequiredMsg,
            actionLabel = upgradeActionLabel
        )
        if (result == SnackbarResult.ActionPerformed) {
            navController.navigate("premium")
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pickedUri ->
            val currentUserCoin = userCoinData ?: return@let
            scope.launch {
                isUploading = true
                val part = withContext(Dispatchers.IO) { uriToMultipart(context, pickedUri) }
                if (part == null) {
                    isUploading = false
                    snackbarHostState.showSnackbar(saveFailedMsg)
                    return@launch
                }
                collectionRepo.uploadImage(currentUserCoin.id, part).onSuccess { updated ->
                    userCoinData = updated
                    imageUrl = RetrofitClient.resolveImageUrl(updated.images.firstOrNull())
                    isUploading = false
                }.onFailure { e ->
                    isUploading = false
                    if (e is retrofit2.HttpException && e.code() == 403) handleVipGate()
                    else snackbarHostState.showSnackbar(saveFailedMsg)
                }
            }
        }
    }

    LaunchedEffect(coinId) {
        val id = coinId.toIntOrNull()
        if (id != null) {
            repository.getCoin(id).onSuccess { coinResult: CoinResponse ->
                coin = coinResult

                authRepository.getCurrentUser().onSuccess { _ ->
                    collectionRepo.getUserCoins().onSuccess { userCoins: List<UserCoinResponse> ->
                        val data = userCoins.find { it.coinId == id }
                        if (data != null) {
                            userCoinData = data
                            noteText = data.notes ?: ""
                            imageUrl = RetrofitClient.resolveImageUrl(data.images.firstOrNull())
                        }
                        isLoading = false
                    }.onFailure { isLoading = false }
                }.onFailure { isLoading = false }
            }.onFailure { isLoading = false }
        } else {
            isLoading = false
        }
    }

    val current = coin
    // Title bar carries the short human name; the long raw catalogue string
    // used to wrap to two lines in the app bar.
    val shortTitle = current?.let { c ->
        listOfNotNull(c.denomination ?: c.name, c.year?.toString()).joinToString(" ")
    } ?: stringResource(R.string.catalog_details_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shortTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (current != null) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                // The coin itself, finally. coins.image_url has been populated for
                // 1863 of 2465 rows the whole time and no screen ever showed it.
                item { CoinHero(imageUrl = current.imageUrl, metalType = current.metalType) }

                item {
                    Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                        val breadcrumb = listOfNotNull(current.countryName, current.rulerName).joinToString(" · ")
                        if (breadcrumb.isNotEmpty()) {
                            Text(
                                text = breadcrumb.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = current.denomination ?: current.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        varietyOf(current)?.let { variety ->
                            Text(
                                text = variety,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    SpecGrid(
                        specs = listOf(
                            Spec(stringResource(R.string.catalog_label_year), current.year?.toString()),
                            Spec(stringResource(R.string.catalog_label_metal), localizedMetalType(current.metalType)),
                            Spec(stringResource(R.string.catalog_label_weight), current.weight?.takeIf { it > 0 }?.let { stringResource(R.string.catalog_value_grams, formatMeasure(it)) }),
                            Spec(stringResource(R.string.catalog_label_diameter), current.diameter?.takeIf { it > 0 }?.let { stringResource(R.string.catalog_value_mm, formatMeasure(it)) }),
                            Spec(stringResource(R.string.catalog_label_rarity), localizedRarity(current.rarity)),
                            Spec(stringResource(R.string.catalog_label_rarity_code), current.rarityCode),
                            Spec(stringResource(R.string.catalog_label_mintage_spmd), groupDigits(current.mintageSpmd)),
                            Spec(stringResource(R.string.catalog_label_mintage_mmd), groupDigits(current.mintageMmd)),
                            Spec(stringResource(R.string.catalog_label_series), current.series),
                            Spec(stringResource(R.string.catalog_label_estimated_price), current.priceEstimate)
                        ),
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                }

                if (!current.description.isNullOrBlank()) {
                    item {
                        Column(Modifier.padding(Spacing.lg).animateContentSize()) {
                            Text(
                                text = stringResource(R.string.catalog_label_description),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = current.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = { descriptionExpanded = !descriptionExpanded },
                                contentPadding = PaddingValues(vertical = Spacing.xs, horizontal = 0.dp)
                            ) {
                                Text(
                                    stringResource(
                                        if (descriptionExpanded) R.string.catalog_show_less
                                        else R.string.catalog_show_more
                                    )
                                )
                            }
                        }
                    }
                }

                if (userCoinData != null) {
                    item {
                        SectionCard(
                            modifier = Modifier.padding(Spacing.lg),
                            emphasis = SectionCardEmphasis.Brand
                        ) {
                            Text(stringResource(R.string.catalog_your_coin), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(Spacing.sm))
                            Box(
                                Modifier.fillMaxWidth().height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(true) { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading) CircularProgressIndicator()
                                else if (imageUrl != null) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, null, Modifier.size(32.dp))
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        stringResource(R.string.catalog_add_your_photo),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(Modifier.height(Spacing.lg))
                            OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text(stringResource(R.string.catalog_notes_label)) }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                val currentUserCoin = userCoinData ?: return@Button
                                scope.launch {
                                    collectionRepo.updateUserCoin(currentUserCoin.id, UserCoinUpdate(notes = noteText)).onSuccess { updated ->
                                        userCoinData = updated
                                    }.onFailure { e ->
                                        if (e is retrofit2.HttpException && e.code() == 403) handleVipGate()
                                        else snackbarHostState.showSnackbar(saveFailedMsg)
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) { Text(stringResource(R.string.common_save)) }
                        }
                    }
                } else {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Button(onClick = {
                                scope.launch {
                                    collectionRepo.addCoinToCollection(current.id, "UNC", status = "owned")
                                        .onSuccess { userCoinData = it }
                                        .onFailure { snackbarHostState.showSnackbar(saveFailedMsg) }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.catalog_action_own), maxLines = 1)
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    collectionRepo.addCoinToCollection(current.id, "UNC", status = "wishlist")
                                        .onSuccess { userCoinData = it }
                                        .onFailure { snackbarHostState.showSnackbar(saveFailedMsg) }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FavoriteBorder, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text(stringResource(R.string.catalog_action_want), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Copies a picked content:// image into a cache file and wraps it as a
 * multipart form part - upload-image endpoints need a real file, not a
 * content Uri. Must be called off the main thread (file I/O).
 */
private fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val file = File.createTempFile("upload", ".jpg", context.cacheDir)
    inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestBody)
}
