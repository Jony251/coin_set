package com.example.coinset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coinset.ui.theme.CoinSetTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoinSetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CoinList(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CoinList(modifier: Modifier = Modifier) {
    val db = Firebase.firestore
    val coins = remember { mutableStateListOf<Coin>() }

    LaunchedEffect(Unit) {
        db.collection("coins")
            .get()
            .addOnSuccessListener { result ->
                coins.clear()
                for (document in result) {
                    val coin = document.toObject(Coin::class.java).copy(id = document.id)
                    coins.add(coin)
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(coins) { coin ->
            CoinCard(coin)
        }
    }
}

@Composable
fun CoinCard(coin: Coin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = coin.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "Номинал: ${coin.denomination}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Металл: ${coin.metal}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Редкость: ${coin.rarity}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Цена: ${coin.estimatedValueMin} - ${coin.estimatedValueMax} руб.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
