package com.example.coinset.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A reusable row for displaying labeled information (e.g., Weight: 5g).
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * A list item with a checkmark icon, used for features lists.
 */
@Composable
fun BulletItem(text: String, isActive: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isActive) Color.Green else Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isActive) Color.Unspecified else Color.Gray
        )
    }
}
