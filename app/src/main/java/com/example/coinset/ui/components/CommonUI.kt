package com.example.coinset.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.coinset.R
import com.example.coinset.ui.theme.Dimens
import com.example.coinset.ui.theme.Spacing

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
            // tertiary = patina green = the app's one semantic "active" accent - see Color.kt
            tint = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isActive) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class SectionCardEmphasis { Neutral, Brand }

/**
 * Shared "highlight card" - replaces the four independently-styled highlight
 * cards that had drifted across Home/Collection/Catalog (different container
 * colors, one with a one-off alpha hack) with one consistent component.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    emphasis: SectionCardEmphasis = SectionCardEmphasis.Neutral,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = when (emphasis) {
        SectionCardEmphasis.Neutral -> MaterialTheme.colorScheme.surfaceContainer
        SectionCardEmphasis.Brand -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Shared status/condition badge - replaces two independently-reimplemented
 * copies of this exact pattern (Collection + Home) that had drifted to
 * slightly different padding/font-size values.
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = Dimens.badgeHorizontalPadding,
                vertical = Dimens.badgeVerticalPadding
            ),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Circular avatar - shows a real photo when available, else a monogram
 * fallback. Replaces reusing the raster launcher icon (R.drawable.icon) as a
 * fake "profile picture" on Settings. No user-profile-photo feature exists
 * yet - imageUrl is here so one can light this up later with zero call-site
 * changes.
 */
@Composable
fun CollectorAvatar(
    modifier: Modifier = Modifier,
    size: Dp = Dimens.avatarLarge,
    imageUrl: String? = null,
    displayName: String? = null
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = displayName?.trim()?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

enum class LogoStyle { Full, IconOnly, TextOnly }

/**
 * The app's logo lockup: the existing full-color launcher icon (a gold coin
 * badge - res/drawable/icon.png, kept as-is, NOT tinted since it's a
 * full-color raster asset) plus a styled wordmark. One shared component so
 * every place that wants "app branding" uses the same lockup instead of an
 * ad hoc Image(painterResource(R.drawable.icon)) call - R.drawable.icon
 * itself is unchanged and still doubles as the launcher icon.
 *
 * (res/drawable/image.xml was considered as the mark instead, but it turned
 * out to be a single solid-filled rounded-square path, not a coin outline -
 * it renders as a plain blob, not usable as a logo mark.)
 */
@Composable
fun CoinSetLogo(
    modifier: Modifier = Modifier,
    style: LogoStyle = LogoStyle.Full,
    iconSize: Dp = Dimens.logoMark
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (style != LogoStyle.TextOnly) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
        if (style != LogoStyle.IconOnly) {
            if (style == LogoStyle.Full) Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.app_wordmark),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
