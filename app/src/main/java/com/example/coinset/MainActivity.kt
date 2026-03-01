package com.example.coinset

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.coinset.ui.theme.CoinSetTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        enableEdgeToEdge()
        setContent {
            CoinSetTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentUser = Firebase.auth.currentUser

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "main" else "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("main") { MainScreen(navController) }
        
        composable("rulers/{countryId}/{countryName}") { backStackEntry ->
            RulerListScreen(navController, backStackEntry.arguments?.getString("countryId") ?: "", backStackEntry.arguments?.getString("countryName") ?: "")
        }
        composable("categories/{rulerId}/{rulerName}") { backStackEntry ->
            CategoryListScreen(navController, backStackEntry.arguments?.getString("rulerId") ?: "", backStackEntry.arguments?.getString("rulerName") ?: "")
        }
        composable("coins/{rulerId}/{category}") { backStackEntry ->
            CoinListScreen(navController, backStackEntry.arguments?.getString("rulerId") ?: "", backStackEntry.arguments?.getString("category") ?: "")
        }
        composable("coin_detail/{coinId}") { backStackEntry ->
            CoinDetailScreen(navController, backStackEntry.arguments?.getString("coinId") ?: "")
        }
        composable("premium") { PremiumScreen(navController) }
    }
}

@Composable
fun MainScreen(parentNavController: NavController) {
    val bottomNavController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBarItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text("Каталог") }, selected = currentRoute == "catalog_root", onClick = { bottomNavController.navigate("catalog_root") })
                NavigationBarItem(icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Моя") }, selected = currentRoute == "my_collection", onClick = { bottomNavController.navigate("my_collection") })
                NavigationBarItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Настройки") }, selected = currentRoute == "settings", onClick = { bottomNavController.navigate("settings") })
            }
        }
    ) { innerPadding ->
        NavHost(navController = bottomNavController, startDestination = "my_collection", modifier = Modifier.padding(innerPadding)) {
            composable("catalog_root") { CountryListScreen(parentNavController) }
            composable("my_collection") { MyCollectionScreen(parentNavController) }
            composable("settings") { SettingsScreen(parentNavController) }
        }
    }
}

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
                    Image(painter = painterResource(id = R.drawable.icon), contentDescription = null, modifier = Modifier.size(32.dp).padding(end = 8.dp))
                    Text("Каталог") 
                }
            }
        ) 
    }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(countries) { country ->
                ListItem(headlineContent = { Text(country.name) }, leadingContent = { Text("🚩") }, modifier = Modifier.clickable { navController.navigate("rulers/${country.id}/${country.name}") })
            }
        }
    }
}

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

    Scaffold(topBar = { TopAppBar(title = { Text(countryName) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(rulers) { ruler ->
                ListItem(headlineContent = { Text(ruler.name) }, supportingContent = { Text("${ruler.startYear} - ${ruler.endYear}") }, modifier = Modifier.clickable { navController.navigate("categories/${ruler.id}/${ruler.name}") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavController, rulerId: String, rulerName: String) {
    val categories = listOf("Золото", "Серебро", "Медь", "Пробные")
    Scaffold(topBar = { TopAppBar(title = { Text(rulerName) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(categories) { category ->
                ListItem(headlineContent = { Text(category) }, modifier = Modifier.clickable { navController.navigate("coins/$rulerId/$category") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(navController: NavController, rulerId: String, category: String) {
    val db = Firebase.firestore
    val coins = remember { mutableStateListOf<Coin>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(rulerId, category) {
        db.collection("coins").whereEqualTo("rulerId", rulerId).get().addOnSuccessListener { result ->
            coins.clear()
            for (doc in result) {
                val coin = doc.toObject(Coin::class.java).copy(id = doc.id)
                if (category == "Пробные" || coin.metal.contains(category, ignoreCase = true)) coins.add(coin)
            }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(category) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(coins) { coin ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { navController.navigate("coin_detail/${coin.id}") }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(coin.name, style = MaterialTheme.typography.titleMedium)
                        Text("Металл: ${coin.metal}", style = MaterialTheme.typography.bodySmall)
                        Text("Год: ${coin.year}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(navController: NavController, coinId: String) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    var coin by remember { mutableStateOf<Coin?>(null) }
    var userCoinData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    var noteText by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("UNC") }
    val conditions = listOf("UNC", "AU", "XF", "VF", "F", "VG", "G")

    LaunchedEffect(coinId) {
        db.collection("coins").document(coinId).get().addOnSuccessListener { doc ->
            coin = doc.toObject(Coin::class.java)?.copy(id = doc.id)
            if (userId != null) {
                db.collection("collections").document(userId).get().addOnSuccessListener { collDoc ->
                    if (collDoc.exists()) {
                        val coinsList = collDoc.get("coins") as? List<Map<String, Any>>
                        val foundData = coinsList?.find { it["catalogCoinId"] == coinId }
                        if (foundData != null) {
                            userCoinData = foundData
                            noteText = foundData["notes"] as? String ?: ""
                            selectedCondition = foundData["condition"] as? String ?: "UNC"
                        } else {
                            userCoinData = null
                        }
                    }
                    isLoading = false
                }.addOnFailureListener { isLoading = false }
            } else isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(coin?.name ?: "Детали") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (coin != null) {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                item {
                    Text("Характеристики:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Номинал", coin!!.denomination)
                    InfoRow("Металл", coin!!.metal)
                    InfoRow("Год", coin!!.year.toString())
                    if (coin!!.weight > 0) InfoRow("Вес", "${coin!!.weight} г")
                    if (coin!!.diameter > 0) InfoRow("Диаметр", "${coin!!.diameter} мм")
                    if (coin!!.mint.isNotEmpty()) InfoRow("Монетный двор", coin!!.mint)
                    if (coin!!.rarity.isNotEmpty()) InfoRow("Редкость", coin!!.rarity)
                    InfoRow("Примерная цена", "${coin!!.estimatedValueMin} - ${coin!!.estimatedValueMax} руб.")
                    
                    Spacer(Modifier.height(24.dp))
                }

                if (userCoinData != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Ваша монета:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                
                                Text("Состояние:")
                                ScrollableTabRow(
                                    selectedTabIndex = conditions.indexOf(selectedCondition).coerceAtLeast(0),
                                    edgePadding = 0.dp,
                                    containerColor = Color.Transparent
                                ) {
                                    conditions.forEach { cond ->
                                        Tab(selected = selectedCondition == cond, onClick = { selectedCondition = cond }, text = { Text(cond) })
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    label = { Text("Заметки юзера") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        if (userId != null) {
                                            db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                                                val coinsList = doc.get("coins") as? List<Map<String, Any>> ?: emptyList()
                                                val updatedList = coinsList.map {
                                                    if (it["catalogCoinId"] == coinId) {
                                                        it.toMutableMap().apply {
                                                            put("notes", noteText)
                                                            put("condition", selectedCondition)
                                                        }
                                                    } else it
                                                }
                                                db.collection("collections").document(userId).update("coins", updatedList)
                                                    .addOnSuccessListener { 
                                                        userCoinData = updatedList.find { it["catalogCoinId"] == coinId }
                                                        Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show() 
                                                    }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Сохранить изменения") }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = {
                                if (userId != null) {
                                    db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                                        val coinsList = doc.get("coins") as? List<Map<String, Any>>
                                        val itemToRemove = coinsList?.find { it["catalogCoinId"] == coinId }
                                        if (itemToRemove != null) {
                                            db.collection("collections").document(userId).update("coins", FieldValue.arrayRemove(itemToRemove))
                                                .addOnSuccessListener { userCoinData = null; Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Удалить из коллекции") }
                    }
                } else {
                    item {
                        Button(onClick = {
                            if (userId != null) {
                                val coinData = mapOf("catalogCoinId" to coinId, "addedAt" to System.currentTimeMillis().toString(), "condition" to "UNC", "notes" to "")
                                db.collection("collections").document(userId).update("coins", FieldValue.arrayUnion(coinData))
                                    .addOnSuccessListener { userCoinData = coinData; Toast.makeText(context, "Добавлено", Toast.LENGTH_SHORT).show() }
                                    .addOnFailureListener {
                                        db.collection("collections").document(userId).set(mapOf("coins" to listOf(coinData)))
                                            .addOnSuccessListener { userCoinData = coinData }
                                    }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Добавить в коллекцию") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val coinsWithDetails = remember { mutableStateListOf<Pair<Coin, Map<String, Any>>>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("collections").document(userId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val userCoinsList = doc.get("coins") as? List<Map<String, Any>> ?: emptyList()
                    val coinIds = userCoinsList.mapNotNull { it["catalogCoinId"] as? String }
                    if (coinIds.isNotEmpty()) {
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
                    } else { coinsWithDetails.clear(); isLoading = false }
                } else { coinsWithDetails.clear(); isLoading = false }
            }.addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Моя коллекция") }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (coinsWithDetails.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Коллекция пуста") }
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(coinsWithDetails) { (coin, userData) ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { navController.navigate("coin_detail/${coin.id}") }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(coin.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                                Text(userData["condition"] as? String ?: "UNC", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 12.sp)
                            }
                        }
                        val note = userData["notes"] as? String ?: ""
                        if (note.isNotEmpty()) {
                            Text("Заметка: $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    var isPro by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                isPro = doc.getBoolean("isPro") ?: false
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text("Ваш профиль", style = MaterialTheme.typography.headlineMedium)
        Text("Email: ${Firebase.auth.currentUser?.email}", color = MaterialTheme.colorScheme.secondary)
        
        Spacer(Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Статус аккаунта", fontWeight = FontWeight.Bold)
                    Text(if (isPro) "PRO версия активна" else "Бесплатная версия")
                }
                if (!isPro && !isLoading) {
                    Button(onClick = { navController.navigate("premium") }) { Text("Купить PRO") }
                } else if (isPro) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = { Firebase.auth.signOut(); navController.navigate("login") { popUpTo(0) { inclusive = true } } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Выйти из аккаунта") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("PRO подписка") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Coin Set PRO", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            Text("Возможности PRO:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            BulletItem("Добавление заметок к монетам")
            BulletItem("Загрузка фотографий ваших монет")
            BulletItem("Выставление монет на продажу")
            BulletItem("Приоритетная поддержка")
            
            Spacer(Modifier.weight(1f))
            
            Text("Стоимость: 499 руб / единоразово", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            
            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (userId != null) {
                            isProcessing = true
                            // ВАЖНО: Текущие правила Firestore запрещают клиенту менять isPro.
                            // Для теста нужно временно разрешить update или использовать Backend.
                            db.collection("users").document(userId).update(
                                "isPro", true,
                                "proActivatedAt", Timestamp.now(),
                                "updatedAt", Timestamp.now()
                            ).addOnSuccessListener {
                                Toast.makeText(context, "Оплата успешна! PRO активирован.", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }.addOnFailureListener { e ->
                                isProcessing = false
                                Toast.makeText(context, "Ошибка (проверьте правила Firestore): ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Оплатить и активировать") }
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Это симуляция оплаты для демонстрации", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun BulletItem(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Green)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = R.drawable.icon), contentDescription = null, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(24.dp))
        Text("Coin Set", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        TextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        TextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        Firebase.auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { 
                                isLoading = false
                                navController.navigate("main") { popUpTo("login") { inclusive = true } } 
                            }
                            .addOnFailureListener { 
                                isLoading = false
                                Toast.makeText(context, "Ошибка: ${it.message}", Toast.LENGTH_LONG)
                                    .show() 
                            }
                    } else {
                        Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) { Text("Войти") }
        }
        
        TextButton(onClick = { navController.navigate("register") }) { Text("Регистрация") }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val db = Firebase.firestore
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Создать аккаунт", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        TextField(nickname, { nickname = it }, label = { Text("Никнейм") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        TextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        TextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && nickname.isNotEmpty()) {
                        isLoading = true
                        Firebase.auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val userId = result.user?.uid ?: ""
                                val now = Timestamp.now()
                                
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val deadlineStr = sdf.format(Date(System.currentTimeMillis() + 86400000))

                                val userData = hashMapOf(
                                    "email" to email,
                                    "nickname" to nickname,
                                    "displayName" to nickname,
                                    "createdAt" to now,
                                    "updatedAt" to now,
                                    "proActivatedAt" to now,
                                    "isPro" to false,
                                    "emailVerified" to true,
                                    "photo" to null,
                                    "verificationDeadline" to deadlineStr
                                )
                                db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener { 
                                        isLoading = false
                                        navController.navigate("main") { popUpTo("login") { inclusive = true } } 
                                    }
                                    .addOnFailureListener { 
                                        isLoading = false
                                        Toast.makeText(context, "Ошибка БД: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { 
                                isLoading = false
                                Toast.makeText(context, "Ошибка регистрации: ${it.message}", Toast.LENGTH_LONG).show() 
                            }
                    } else {
                        Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) { Text("Зарегистрироваться") }
        }
    }
}
