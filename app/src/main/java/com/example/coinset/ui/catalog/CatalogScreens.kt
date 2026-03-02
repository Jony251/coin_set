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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinset.*
import com.example.coinset.R // Explicitly import application R class
import com.example.coinset.ui.components.InfoRow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Screen displaying the list of countries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(navController: NavController) {
    val db = Firebase.firestore
    val countries = remember { mutableStateListOf<Country>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("countries").get().addOnSuccessListener { result ->
            countries.clear()
            for (doc in result) countries.add(doc.toObject(Country::class.java).copy(id = doc.id))
            isLoading = false
        }.addOnFailureListener { isLoading = false }
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
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(countries) { country ->
                    ListItem(
                        headlineContent = { Text(country.name) }, 
                        leadingContent = { Text("🚩") }, 
                        modifier = Modifier.clickable { 
                            navController.navigate("rulers/${country.id}/${country.name}") 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Screen displaying rulers for a specific country.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerListScreen(navController: NavController, countryId: String, countryName: String) {
    val db = Firebase.firestore
    val rulers = remember { mutableStateListOf<Ruler>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(countryId) {
        db.collection("periods").whereEqualTo("countryId", countryId).get().addOnSuccessListener { periodResult ->
            val periodIds = periodResult.documents.map { it.id }
            if (periodIds.isEmpty()) { isLoading = false; return@addOnSuccessListener }
            db.collection("rulers").whereIn("periodId", periodIds).get().addOnSuccessListener { rulerResult ->
                rulers.clear()
                for (doc in rulerResult) rulers.add(doc.toObject(Ruler::class.java).copy(id = doc.id))
                rulers.sortBy { it.startYear }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
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
                        headlineContent = { Text(ruler.name) }, 
                        supportingContent = { Text("${ruler.startYear} - ${ruler.endYear}") }, 
                        modifier = Modifier.clickable { 
                            navController.navigate("categories/${ruler.id}/${ruler.name}") 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Screen displaying metal categories (Gold, Silver, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavController, rulerId: String, rulerName: String) {
    val categories = listOf("Gold", "Silver", "Copper", "Patterns")
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
                    modifier = Modifier.clickable { 
                        navController.navigate("coins/$rulerId/$category") 
                    }
                )
            }
        }
    }
}

/**
 * Screen displaying denominations for a specific ruler and category.
 */
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
                if (category == "Patterns" || coin.metal.contains(category, ignoreCase = true)) {
                    set.add(coin.denomination)
                }
            }
            denominations.clear()
            denominations.addAll(set.sortedByDescending { it })
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(category) }, 
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
                items(denominations) { den ->
                    ListItem(
                        headlineContent = { Text(den) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable { 
                            navController.navigate("coin_type/$rulerId/$category/$den") 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Screen displaying all coins of a specific denomination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTypeScreen(navController: NavController, rulerId: String, category: String, denomination: String) {
    val db = Firebase.firestore
    val coins = remember { mutableStateListOf<Coin>() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val userId = Firebase.auth.currentUser?.uid

    LaunchedEffect(Unit) {
        db.collection("coins")
            .whereEqualTo("rulerId", rulerId)
            .whereEqualTo("denomination", denomination)
            .get().addOnSuccessListener { result ->
                coins.clear()
                for (doc in result) {
                    val coin = doc.toObject(Coin::class.java).copy(id = doc.id)
                    if (category == "Patterns" || coin.metal.contains(category, ignoreCase = true)) {
                        coins.add(coin)
                    }
                }
                coins.sortBy { it.year }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(denomination) }, 
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
            Column(Modifier.padding(padding)) {
                if (coins.isNotEmpty()) {
                    val firstCoin = coins[0]
                    Card(
                        Modifier.fillMaxWidth().padding(8.dp), 
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Type Specifications", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Metal: ${firstCoin.metal}")
                            Text("Weight: ${firstCoin.weight}g, Diameter: ${firstCoin.diameter}mm")
                            if (firstCoin.description.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(firstCoin.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                LazyColumn(Modifier.weight(1f)) {
                    items(coins) { coin ->
                        ListItem(
                            headlineContent = { Text("${coin.year} ${coin.mint}") },
                            supportingContent = { Text("Rarity: ${if(coin.rarity.isEmpty()) "Common" else coin.rarity}") },
                            trailingContent = {
                                IconButton(onClick = {
                                    if (userId != null) {
                                        val coinData = mapOf(
                                            "catalogCoinId" to coin.id, 
                                            "addedAt" to System.currentTimeMillis().toString(), 
                                            "condition" to "UNC", 
                                            "notes" to "", 
                                            "photoUrl" to ""
                                        )
                                        db.collection("collections").document(userId).update("coins", FieldValue.arrayUnion(coinData))
                                            .addOnSuccessListener { Toast.makeText(context, "Added to collection", Toast.LENGTH_SHORT).show() }
                                            .addOnFailureListener {
                                                db.collection("collections").document(userId).set(mapOf("coins" to listOf(coinData)))
                                                    .addOnSuccessListener { Toast.makeText(context, "Added to collection", Toast.LENGTH_SHORT).show() }
                                            }
                                    }
                                }) { Icon(Icons.Default.AddCircle, "Quick Add", tint = MaterialTheme.colorScheme.primary) }
                            },
                            modifier = Modifier.clickable { navController.navigate("coin_detail/${coin.id}") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen displaying detailed information about a single coin.
 */
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
                val fileName = "user_coins/${userId}_${coinId}_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child(fileName)
                ref.putFile(it).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrl = downloadUri.toString()
                        isUploading = false
                    }
                }.addOnFailureListener {
                    isUploading = false
                    Toast.makeText(context, "Upload Error", Toast.LENGTH_SHORT).show()
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
                            val coinsList = collDoc.get("coins") as? List<Map<String, Any>>
                            val foundData = coinsList?.find { it["catalogCoinId"] == coinId }
                            if (foundData != null) {
                                userCoinData = foundData
                                noteText = foundData["notes"] as? String ?: ""
                                selectedCondition = foundData["condition"] as? String ?: "UNC"
                                imageUrl = foundData["photoUrl"] as? String
                            } else {
                                userCoinData = null
                            }
                        }
                        isLoading = false
                    }.addOnFailureListener { isLoading = false }
                }.addOnFailureListener { isLoading = false }
            } else isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(coin?.name ?: "Details") }, 
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
        } else if (coin != null) {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                item {
                    Text("Characteristics:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Denomination", coin!!.denomination)
                    InfoRow("Metal", coin!!.metal)
                    InfoRow("Year", coin!!.year.toString())
                    if (coin!!.weight > 0) InfoRow("Weight", "${coin!!.weight}g")
                    if (coin!!.diameter > 0) InfoRow("Diameter", "${coin!!.diameter}mm")
                    if (coin!!.mint.isNotEmpty()) InfoRow("Mint", coin!!.mint)
                    if (coin!!.rarity.isNotEmpty()) InfoRow("Rarity", coin!!.rarity)
                    InfoRow("Estimated Value", "${coin!!.estimatedValueMin} - ${coin!!.estimatedValueMax} RUB")
                    
                    Spacer(Modifier.height(24.dp))
                }

                if (userCoinData != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable(!isUserPro) {
                                Toast.makeText(context, "Upgrade to PRO to unlock notes and photos", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Column(Modifier.padding(16.dp).alpha(if (isUserPro) 1f else 0.5f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Your Coin:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    if (!isUserPro) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(isUserPro) { launcher.launch("image/*") }, 
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUploading) CircularProgressIndicator()
                                    else if (imageUrl != null) {
                                        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Add, null, Modifier.size(48.dp))
                                            Text("Tap to add photo")
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Text("Condition:")
                                ScrollableTabRow(
                                    selectedTabIndex = conditions.indexOf(selectedCondition).coerceAtLeast(0),
                                    edgePadding = 0.dp,
                                    containerColor = Color.Transparent
                                ) {
                                    conditions.forEach { cond ->
                                        Tab(
                                            selected = selectedCondition == cond, 
                                            onClick = { if (isUserPro) selectedCondition = cond }, 
                                            text = { Text(cond) }, 
                                            enabled = isUserPro
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { if (isUserPro) noteText = it },
                                    label = { Text("Personal Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    enabled = isUserPro
                                )
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        if (isUserPro) {
                                            if (userId != null) {
                                                db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val coinsList = doc.get("coins") as? List<Map<String, Any>> ?: emptyList()
                                                    val updatedList = coinsList.map {
                                                        if (it["catalogCoinId"] == coinId) {
                                                            it.toMutableMap().apply {
                                                                put("notes", noteText)
                                                                put("condition", selectedCondition)
                                                                put("photoUrl", imageUrl ?: "")
                                                            }
                                                        } else it
                                                    }
                                                    db.collection("collections").document(userId).update("coins", updatedList)
                                                        .addOnSuccessListener { 
                                                            userCoinData = updatedList.find { it["catalogCoinId"] == coinId }
                                                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() 
                                                        }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Upgrade to PRO to save changes", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Changes")
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = {
                                if (userId != null) {
                                    db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                                        @Suppress("UNCHECKED_CAST")
                                        val coinsList = doc.get("coins") as? List<Map<String, Any>>
                                        val itemToRemove = coinsList?.find { it["catalogCoinId"] == coinId }
                                        if (itemToRemove != null) {
                                            db.collection("collections").document(userId).update("coins", FieldValue.arrayRemove(itemToRemove))
                                                .addOnSuccessListener { 
                                                    userCoinData = null
                                                    Toast.makeText(context, "Removed from collection", Toast.LENGTH_SHORT).show() 
                                                }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove from Collection")
                        }
                    }
                } else {
                    item {
                        Button(
                            onClick = {
                                if (userId != null) {
                                    val coinData = mapOf(
                                        "catalogCoinId" to coinId, 
                                        "addedAt" to System.currentTimeMillis().toString(), 
                                        "condition" to "UNC", 
                                        "notes" to "", 
                                        "photoUrl" to ""
                                    )
                                    db.collection("collections").document(userId).update("coins", FieldValue.arrayUnion(coinData))
                                        .addOnSuccessListener { userCoinData = coinData; Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show() }
                                        .addOnFailureListener {
                                            db.collection("collections").document(userId).set(mapOf("coins" to listOf(coinData)))
                                                .addOnSuccessListener { userCoinData = coinData }
                                        }
                                }
                            }, 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add to Collection")
                        }
                    }
                }
            }
        }
    }
}
