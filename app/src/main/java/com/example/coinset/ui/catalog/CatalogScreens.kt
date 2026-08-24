package com.example.coinset.ui.catalog

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.R
import com.example.coinset.api.*
import com.example.coinset.ui.components.CoinSetLogo
import com.example.coinset.ui.components.CountryFlag
import com.example.coinset.ui.components.InfoRow
import com.example.coinset.ui.components.LogoStyle
import com.example.coinset.ui.components.SectionCard
import com.example.coinset.ui.components.SectionCardEmphasis
import com.example.coinset.ui.theme.Spacing
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
// "trial") in routes and matching logic - never the localized label - so
// switching the app language in Settings can't silently break which coins
// show up under a category. Only the displayed text is localized.
private val CATALOG_CATEGORY_KEYS = listOf("gold", "silver", "copper", "trial")

@Composable
private fun categoryDisplayName(categoryKey: String): String = when (categoryKey) {
    "gold" -> stringResource(R.string.metal_gold)
    "silver" -> stringResource(R.string.metal_silver)
    "copper" -> stringResource(R.string.metal_copper)
    "trial" -> stringResource(R.string.catalog_category_trial)
    else -> categoryKey
}

@Composable
private fun localizedRarity(rarity: String): String = when (rarity.lowercase()) {
    "uncommon" -> stringResource(R.string.rarity_uncommon)
    "rare" -> stringResource(R.string.rarity_rare)
    "very_rare" -> stringResource(R.string.rarity_very_rare)
    "extremely_rare" -> stringResource(R.string.rarity_extremely_rare)
    else -> stringResource(R.string.rarity_common)
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(stringResource(R.string.catalog_search_country_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (filteredCountries.isEmpty() && searchQuery.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.catalog_country_not_found, searchQuery),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn {
                    items(filteredCountries) { country ->
                        ListItem(
                            headlineContent = { Text(country.name, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(country.code) },
                            leadingContent = { CountryFlag(code = country.code) },
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
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(periods) { period ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { navController.navigate("rulers/${period.id}/${period.name}") }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.History, null, Modifier.padding(12.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(period.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val endLabel = period.periodEnd?.toString() ?: ""
                                Text("${period.periodStart} - $endLabel", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lists the rulers within one historical period.
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
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(rulers) { ruler ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { navController.navigate("categories/${ruler.id}/${ruler.name}") }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Person, null, Modifier.padding(12.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(ruler.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (ruler.periodStart > 0) Text("${ruler.periodStart} - ${ruler.periodEnd}", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavController, rulerId: String, rulerName: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rulerName) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(CATALOG_CATEGORY_KEYS) { categoryKey ->
                ListItem(
                    headlineContent = { Text(categoryDisplayName(categoryKey)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { navController.navigate("coins/$rulerId/$categoryKey") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(navController: NavController, rulerId: String, category: String) {
    val repository = remember { CatalogRepository() }
    val denominations = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(rulerId, category) {
        val rId = rulerId.toIntOrNull()
        if (rId != null) {
            repository.getCoins(rulerId = rId).onSuccess { result ->
                val set = mutableSetOf<String>()
                for (coin in result) {
                    val m = (coin.metalType ?: "").lowercase()
                    val matches = when (category) {
                        "gold" -> m == "gold"
                        "silver" -> m == "silver"
                        "copper" -> m == "copper" || m == "bronze"
                        else -> false
                    }

                    if (category == "trial" || matches) {
                        set.add(coin.denomination ?: coin.name)
                    }
                }
                denominations.clear()
                denominations.addAll(set.sorted())
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
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (denominations.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.catalog_no_denominations_found)) }
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(denominations) { den ->
                ListItem(
                    headlineContent = { Text(den) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { navController.navigate("coin_type/$rulerId/$category/$den") }
                )
            }
        }
    }
}

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
                    
                    if (currentDenomination == denomination) {
                        val m = coin.metalType.lowercase()
                        val matches = when (category) {
                            "gold" -> m == "gold"
                            "silver" -> m == "silver"
                            "copper" -> m == "copper" || m == "bronze"
                            else -> false
                        }

                        if (category == "trial" || matches) coins.add(coin)
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
            title = { Text(denomination) }, 
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        ) 
    }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.padding(padding)) {
            if (coins.isNotEmpty()) {
                val first = coins[0]
                SectionCard(Modifier.fillMaxWidth().padding(Spacing.sm)) {
                        Text(stringResource(R.string.catalog_specifications), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.catalog_composition, localizedMetalType(first.metalType)))
                        Text(stringResource(R.string.catalog_weight_diameter, first.weight.toString(), first.diameter.toString()))
                        if (first.rarity.isNotEmpty()) {
                            Text(stringResource(R.string.catalog_rarity_scale, localizedRarity(first.rarity)), color = MaterialTheme.colorScheme.primary)
                        }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(coins) { coin ->
                    ListItem(
                        headlineContent = { Text("${coin.year ?: ""} ${coin.description ?: ""}") },
                        supportingContent = {
                            Text(stringResource(R.string.catalog_rarity_label, localizedRarity(coin.rarity)))
                        },
                        trailingContent = {
                    val addedText = stringResource(R.string.catalog_toast_added)
                    val errorTemplate = stringResource(R.string.catalog_toast_error)
                    IconButton(onClick = {
                        scope.launch {
                            collectionRepo.addCoinToCollection(coin.id, "UNC").onSuccess { _: UserCoinResponse ->
                                Toast.makeText(context, addedText, Toast.LENGTH_SHORT).show()
                            }.onFailure { e: Throwable ->
                                Toast.makeText(context, String.format(errorTemplate, e.message), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                        },
                        modifier = Modifier.clickable { navController.navigate("coin_detail/${coin.id}") }
                    )
                }
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(coin?.name ?: stringResource(R.string.catalog_details_title)) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (coin != null) {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    Text(stringResource(R.string.catalog_characteristics), style = MaterialTheme.typography.titleLarge)
                    InfoRow(stringResource(R.string.catalog_label_denomination), coin!!.denomination ?: "")
                    InfoRow(stringResource(R.string.catalog_label_metal), localizedMetalType(coin!!.metalType))
                    InfoRow(stringResource(R.string.catalog_label_year), coin!!.year?.toString() ?: "")
                    InfoRow(stringResource(R.string.catalog_label_rarity), localizedRarity(coin!!.rarity))
                    coin!!.series?.let { InfoRow(stringResource(R.string.catalog_label_series), it) }
                    coin!!.rarityCode?.let { InfoRow(stringResource(R.string.catalog_label_rarity_code), it) }
                    coin!!.mintageSpmd?.let { InfoRow(stringResource(R.string.catalog_label_mintage_spmd), it) }
                    coin!!.mintageMmd?.let { InfoRow(stringResource(R.string.catalog_label_mintage_mmd), it) }
                    coin!!.priceEstimate?.let { InfoRow(stringResource(R.string.catalog_label_estimated_price), it) }

                    coin!!.description?.let { InfoRow(stringResource(R.string.catalog_label_description), it) }

                    Spacer(Modifier.height(24.dp))
                }
                if (userCoinData != null) {
                    item {
                        SectionCard(emphasis = SectionCardEmphasis.Brand) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.catalog_your_coin), style = MaterialTheme.typography.titleMedium)
                                    // if (!isUserPro) Icon(Icons.Default.Lock, null, Modifier.padding(start = 8.dp).size(18.dp))
                                }
                                Box(Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).clickable(true) { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                                    if (isUploading) CircularProgressIndicator()
                                    else if (imageUrl != null) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Default.Add, null, Modifier.size(48.dp))
                                }
                                Spacer(Modifier.height(16.dp))
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
                                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.common_save)) }
                        }
                    }
                } else {
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    collectionRepo.addCoinToCollection(coin!!.id, "UNC", status = "owned")
                                        .onSuccess { userCoinData = it }
                                        .onFailure { snackbarHostState.showSnackbar(saveFailedMsg) }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AddCircle, null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.catalog_action_add_to_collection))
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    collectionRepo.addCoinToCollection(coin!!.id, "UNC", status = "wishlist")
                                        .onSuccess { userCoinData = it }
                                        .onFailure { snackbarHostState.showSnackbar(saveFailedMsg) }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FavoriteBorder, null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.catalog_action_add_to_wishlist))
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
