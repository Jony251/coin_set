package com.example.coinset

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.coinset.ui.theme.CoinSetTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
            RulerListScreen(
                navController, 
                backStackEntry.arguments?.getString("countryId") ?: "",
                backStackEntry.arguments?.getString("countryName") ?: ""
            )
        }
        composable("categories/{rulerId}/{rulerName}") { backStackEntry ->
            CategoryListScreen(
                navController,
                backStackEntry.arguments?.getString("rulerId") ?: "",
                backStackEntry.arguments?.getString("rulerName") ?: ""
            )
        }
        composable("coins/{rulerId}/{category}") { backStackEntry ->
            CoinListScreen(
                navController,
                backStackEntry.arguments?.getString("rulerId") ?: "",
                backStackEntry.arguments?.getString("category") ?: ""
            )
        }
        composable("coin_detail/{coinId}") { backStackEntry ->
            CoinDetailScreen(
                navController,
                backStackEntry.arguments?.getString("coinId") ?: ""
            )
        }
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
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Каталог") },
                    selected = currentRoute == "catalog_root",
                    onClick = { bottomNavController.navigate("catalog_root") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, null) },
                    label = { Text("Моя") },
                    selected = currentRoute == "my_collection",
                    onClick = { bottomNavController.navigate("my_collection") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Настройки") },
                    selected = currentRoute == "settings",
                    onClick = { bottomNavController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "catalog_root",
            modifier = Modifier.padding(innerPadding)
        ) {
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

    Scaffold(topBar = { TopAppBar(title = { Text("Выберите страну") }) }) { padding ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerListScreen(navController: NavController, countryId: String, countryName: String) {
    val db = Firebase.firestore
    val rulers = remember { mutableStateListOf<Ruler>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(countryId) {
        db.collection("periods")
            .whereEqualTo("countryId", countryId)
            .get()
            .addOnSuccessListener { periodResult ->
                val periodIds = periodResult.documents.map { it.id }
                if (periodIds.isEmpty()) { isLoading = false; return@addOnSuccessListener }

                db.collection("rulers")
                    .whereIn("periodId", periodIds)
                    .get()
                    .addOnSuccessListener { rulerResult ->
                        rulers.clear()
                        for (doc in rulerResult) rulers.add(doc.toObject(Ruler::class.java).copy(id = doc.id))
                        rulers.sortBy { it.startYear }
                        isLoading = false
                    }
            }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(countryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category) },
                    modifier = Modifier.clickable { navController.navigate("coins/$rulerId/$category") }
                )
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
        db.collection("coins")
            .whereEqualTo("rulerId", rulerId)
            .get().addOnSuccessListener { result ->
                coins.clear()
                for (doc in result) {
                    val coin = doc.toObject(Coin::class.java).copy(id = doc.id)
                    if (category == "Пробные" || coin.metal.contains(category, ignoreCase = true)) {
                        coins.add(coin)
                    }
                }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(category) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            ) 
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(coins) { coin ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { 
                            navController.navigate("coin_detail/${coin.id}")
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(coin.name, style = MaterialTheme.typography.titleMedium)
                            Text("Металл: ${coin.metal}")
                            Text("Год: ${coin.year}")
                            Text("Номинал: ${coin.denomination}")
                        }
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
    var userCoin by remember { mutableStateOf<UserCoin?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(coinId) {
        db.collection("coins").document(coinId).get().addOnSuccessListener { doc ->
            coin = doc.toObject(Coin::class.java)?.copy(id = doc.id)
            
            if (userId != null) {
                db.collection("user_coins")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("coinId", coinId)
                    .get().addOnSuccessListener { userResult ->
                        if (!userResult.isEmpty) {
                            userCoin = userResult.documents[0].toObject(UserCoin::class.java)
                        }
                        isLoading = false
                    }
            } else {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(coin?.name ?: "Детали") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (coin != null) {
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                Text("Характеристики:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("Номинал", coin!!.denomination)
                InfoRow("Металл", coin!!.metal)
                InfoRow("Год", coin!!.year.toString())
                InfoRow("Вес", "${coin!!.weight} г")
                InfoRow("Диаметр", "${coin!!.diameter} мм")
                InfoRow("Монетный двор", coin!!.mint)
                InfoRow("Редкость", coin!!.rarity)
                InfoRow("Примерная цена", "${coin!!.estimatedValueMin} - ${coin!!.estimatedValueMax} руб.")
                
                Spacer(Modifier.weight(1f))
                
                if (userCoin == null) {
                    Button(
                        onClick = {
                            if (userId != null) {
                                val newUserCoin = UserCoin(userId = userId, coinId = coinId)
                                db.collection("user_coins").add(newUserCoin).addOnSuccessListener {
                                    userCoin = newUserCoin
                                    Toast.makeText(context, "Добавлено в коллекцию", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить в коллекцию")
                    }
                } else {
                    Button(
                        onClick = {
                            if (userId != null) {
                                db.collection("user_coins")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("coinId", coinId)
                                    .get().addOnSuccessListener { result ->
                                        for (doc in result) doc.reference.delete()
                                        userCoin = null
                                        Toast.makeText(context, "Удалено из коллекции", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить из коллекции")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(navController: NavController) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val coins = remember { mutableStateListOf<Coin>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            db.collection("user_coins").whereEqualTo("userId", userId).get().addOnSuccessListener { userResult ->
                val coinIds = userResult.documents.map { it.getString("coinId") ?: "" }.filter { it.isNotEmpty() }
                if (coinIds.isEmpty()) {
                    isLoading = false
                    return@addOnSuccessListener
                }
                
                // Firestore whereIn has 10 items limit, but for light app we can use it or chunk it
                db.collection("coins").whereIn("id", coinIds).get().addOnSuccessListener { coinResult ->
                    coins.clear()
                    for (doc in coinResult) coins.add(doc.toObject(Coin::class.java).copy(id = doc.id))
                    isLoading = false
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Моя коллекция") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (coins.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Коллекция пуста") }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(coins) { coin ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { 
                        navController.navigate("coin_detail/${coin.id}")
                    }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(coin.name, style = MaterialTheme.typography.titleMedium)
                            Text("Металл: ${coin.metal}")
                            Text("Номинал: ${coin.denomination}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val user = Firebase.auth.currentUser
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountCircle, null, Modifier.size(100.dp))
        Text("Email: ${user?.email}")
        Spacer(Modifier.height(32.dp))
        Button(onClick = { Firebase.auth.signOut(); navController.navigate("login") { popUpTo(0) } }) {
            Text("Выйти")
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Вход", style = MaterialTheme.typography.headlineLarge)
        TextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        TextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { Firebase.auth.signInWithEmailAndPassword(email, password).addOnSuccessListener { navController.navigate("main") } }, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
        TextButton(onClick = { navController.navigate("register") }) { Text("Регистрация") }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Регистрация", style = MaterialTheme.typography.headlineLarge)
        TextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        TextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { Firebase.auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { navController.navigate("main") } }, modifier = Modifier.fillMaxWidth()) { Text("Создать") }
    }
}
