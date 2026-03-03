package com.example.coinset.ui.catalog

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.*
import com.example.coinset.R // Using application resources
import com.example.coinset.ui.components.InfoRow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Screen displaying the list of countries with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(navController: NavController) {
    val db = Firebase.firestore
    val countries = remember { mutableStateListOf<Country>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Fetch all countries
    LaunchedEffect(Unit) {
        db.collection("countries").get().addOnSuccessListener { result ->
            countries.clear()
            for (doc in result) countries.add(doc.toObject(Country::class.java).copy(id = doc.id))
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    // Client-side filtering for better UX
    val filteredCountries = countries.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(topBar = { 
        TopAppBar(
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.icon), 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                    )
                    Text("Catalog") 
                }
            }
        ) 
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search country (e.g. Italy, Germany)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (filteredCountries.isEmpty() && searchQuery.isNotBlank()) {
                // If country not found, allow user to request it
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Country \"$searchQuery\" will appear in the near future!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(onClick = {
                        val trimmed = searchQuery.trim()
                        db.collection("find_arr").document("find_arr")
                            .set(mapOf("find" to FieldValue.arrayUnion(trimmed)), SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(context, "Request for '$trimmed' saved!", Toast.LENGTH_SHORT).show()
                                searchQuery = ""
                            }
                    }) {
                        Text("Notify me when added")
                    }
                }
            } else {
                LazyColumn {
                    items(filteredCountries) { country ->
                        ListItem(
                            headlineContent = { Text(country.name) }, 
                            supportingContent = { Text(country.id) }, // Shows the slug
                            leadingContent = { Text("🚩", fontSize = 24.sp) }, 
                            modifier = Modifier.clickable { 
                                navController.navigate("rulers/${country.id}/${country.name}") 
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen displaying rulers based on Country -> Period -> Ruler relationship.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerListScreen(navController: NavController, countryId: String, countryName: String) {
    val db = Firebase.firestore
    val rulers = remember { mutableStateListOf<Ruler>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(countryId) {
        // According to schema: countries -> periods (by countryId) -> rulers (by periodId)
        db.collection("periods").whereEqualTo("countryId", countryId).get().addOnSuccessListener { periodResult ->
            val periodIds = periodResult.documents.map { it.id }
            if (periodIds.isEmpty()) {
                // Fallback for direct issuer_period ID
                val fallbackPeriodId = "${countryId}_period"
                db.collection("rulers").whereEqualTo("periodId", fallbackPeriodId).get().addOnSuccessListener { rResult ->
                    rulers.clear()
                    for (doc in rResult) rulers.add(doc.toObject(Ruler::class.java).copy(id = doc.id))
                    isLoading = false
                }.addOnFailureListener { isLoading = false }
            } else {
                db.collection("rulers").whereIn("periodId", periodIds).get().addOnSuccessListener { rulerResult ->
                    rulers.clear()
                    for (doc in rulerResult) rulers.add(doc.toObject(Ruler::class.java).copy(id = doc.id))
                    isLoading = false
                }.addOnFailureListener { isLoading = false }
            }
        }.addOnFailureListener { isLoading = false }
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
                items(rulers) { ruler ->
                    ListItem(
                        headlineContent = { Text(ruler.name, fontWeight = FontWeight.Bold) }, 
                        supportingContent = { 
                            if (ruler.startYear > 0) Text("${ruler.startYear} - ${ruler.endYear}") 
                            else Text("Details")
                        }, 
                        leadingContent = {
                            if (!ruler.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ruler.imageUrl,
                                    contentDescription = ruler.name,
                                    modifier = Modifier.size(50.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp))
                                }
                            }
                        },
                        modifier = Modifier.clickable { 
                            navController.navigate("categories/${ruler.id}/${ruler.name}") 
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavController, rulerId: String, rulerName: String) {
    val categories = listOf("Золото", "Серебро", "Медь", "Пробные")
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(rulerName) }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category) }, 
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { 
                        navController.navigate("coins/$rulerId/$category") 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(navController: NavController, rulerId: String, category: String) {
    val db = Firebase.firestore
    val denominations = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(rulerId, category) {
        db.collection("coins").whereEqualTo("rulerId", rulerId).get().addOnSuccessListener { result ->
            val set = mutableSetOf<String>()
            for (doc in result) {
                val coin = doc.toObject(Coin::class.java)
                val isPattern = category == "Пробные" || category == "Patterns"
                val metalMatches = coin.metal.contains(category, ignoreCase = true) || 
                                   (category == "Серебро" && coin.metal.contains("Silver", ignoreCase = true)) ||
                                   (category == "Золото" && coin.metal.contains("Gold", ignoreCase = true)) ||
                                   (category == "Медь" && coin.metal.contains("Copper", ignoreCase = true))
                
                if (isPattern || metalMatches) {
                    set.add(coin.denomination.ifEmpty { coin.name })
                }
            }
            denominations.clear()
            denominations.addAll(set.sortedByDescending { it })
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(topBar = { 
        TopAppBar(
            title = { Text(category) }, 
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        ) 
    }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
    val db = Firebase.firestore
    val coins = remember { mutableStateListOf<Coin>() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val userId = Firebase.auth.currentUser?.uid

    LaunchedEffect(Unit) {
        db.collection("coins").whereEqualTo("rulerId", rulerId).get().addOnSuccessListener { result ->
            coins.clear()
            for (doc in result) {
                val coin = doc.toObject(Coin::class.java).copy(id = doc.id)
                val currentDenom = coin.denomination.ifEmpty { coin.name }
                if (currentDenom == denomination) {
                    val isPattern = category == "Пробные" || category == "Patterns"
                    val metalMatches = coin.metal.contains(category, ignoreCase = true) || 
                                       (category == "Серебро" && coin.metal.contains("Silver", ignoreCase = true)) ||
                                       (category == "Золото" && coin.metal.contains("Gold", ignoreCase = true)) ||
                                       (category == "Медь" && coin.metal.contains("Copper", ignoreCase = true))
                    if (isPattern || metalMatches) coins.add(coin)
                }
            }
            coins.sortBy { it.year }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
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
                Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Specifications", fontWeight = FontWeight.Bold)
                        Text("Metal: ${first.metal}")
                        Text("Weight: ${first.weight}g | Diameter: ${first.diameter}mm")
                    }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(coins) { coin ->
                    ListItem(
                        headlineContent = { Text("${coin.year} ${coin.mint}") },
                        supportingContent = { Text("Rarity: ${coin.rarity.ifEmpty { "Common" }}") },
                        trailingContent = {
                            IconButton(onClick = {
                                if (userId != null) {
                                    val coinData = mapOf("catalogCoinId" to coin.id, "addedAt" to System.currentTimeMillis().toString(), "condition" to "UNC")
                                    db.collection("collections").document(userId).update("coins", FieldValue.arrayUnion(coinData))
                                        .addOnSuccessListener { Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show() }
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
    val db = Firebase.firestore
    val storage = Firebase.storage
    val userId = Firebase.auth.currentUser?.uid
    var coin by remember { mutableStateOf<Coin?>(null) }
    var userCoinData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isUserPro by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var noteText by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("UNC") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val conditions = listOf("UNC", "AU", "XF", "VF", "F", "VG", "G")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isUserPro && userId != null) {
                isUploading = true
                val ref = storage.reference.child("user_coins/${userId}_${coinId}.jpg")
                ref.putFile(it).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrl = downloadUri.toString()
                        isUploading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(coinId) {
        db.collection("coins").document(coinId).get().addOnSuccessListener { doc ->
            coin = doc.toObject(Coin::class.java)?.copy(id = doc.id)
            if (userId != null) {
                db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                    isUserPro = userDoc.getBoolean("isPro") ?: false
                    db.collection("collections").document(userId).get().addOnSuccessListener { collDoc ->
                        if (collDoc.exists()) {
                            @Suppress("UNCHECKED_CAST")
                            val list = collDoc.get("coins") as? List<Map<String, Any>>
                            val data = list?.find { it["catalogCoinId"] == coinId }
                            if (data != null) {
                                userCoinData = data
                                noteText = data["notes"] as? String ?: ""
                                selectedCondition = data["condition"] as? String ?: "UNC"
                                imageUrl = data["photoUrl"] as? String
                            }
                        }
                        isLoading = false
                    }.addOnFailureListener { isLoading = false }
                }.addOnFailureListener { isLoading = false }
            } else isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(coin?.name ?: "Details") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (coin != null) {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                item {
                    Text("Characteristics:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    InfoRow("Denomination", coin!!.denomination)
                    InfoRow("Metal", coin!!.metal)
                    InfoRow("Year", coin!!.year.toString())
                    InfoRow("Value", "${coin!!.estimatedValueMin} - ${coin!!.estimatedValueMax} RUB")
                    Spacer(Modifier.height(24.dp))
                }
                if (userCoinData != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Your Coin:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    if (!isUserPro) Icon(Icons.Default.Lock, null, Modifier.padding(start = 8.dp).size(18.dp))
                                }
                                Box(Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).clickable(isUserPro) { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                                    if (isUploading) CircularProgressIndicator()
                                    else if (imageUrl != null) AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Default.Add, null, Modifier.size(48.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(value = noteText, onValueChange = { if(isUserPro) noteText = it }, label = { Text("Notes") }, enabled = isUserPro, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    if (isUserPro && userId != null) {
                                        db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                                            @Suppress("UNCHECKED_CAST")
                                            val list = (doc.get("coins") as? List<Map<String, Any>> ?: emptyList()).map {
                                                if (it["catalogCoinId"] == coinId) it.toMutableMap().apply { put("notes", noteText); put("condition", selectedCondition); put("photoUrl", imageUrl ?: "") }
                                                else it
                                            }
                                            db.collection("collections").document(userId).update("coins", list).addOnSuccessListener { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Save") }
                            }
                        }
                    }
                }
            }
        }
    }
}
