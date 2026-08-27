package com.example.coinset.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.coinset.ui.theme.MetalPalette
import com.example.coinset.ui.theme.Spacing
import com.example.coinset.ui.theme.tabular

/**
 * Shared numismatic UI vocabulary: the coin disc, the ruler portrait, the
 * spec grid and the empty state.
 *
 * These exist because the same three ideas were being re-typed (badly) on
 * every screen: "show a coin", "show a person", "show a labelled number".
 * A coin was a line of text, a ruler was a generic person glyph, and a
 * number was a sentence. One component each, used everywhere.
 */

// ---------------------------------------------------------------- coin disc

/**
 * A coin, drawn as a coin: a circle.
 *
 * Catalog photos (Numista) are square with the round coin inscribed, so
 * ContentScale.Fit inside a CircleShape clip lands the coin edge-to-edge and
 * throws away the square's corners - no cropping of the coin itself.
 *
 * When there is no photo (602 of 2465 catalog rows, plus every collection
 * item until the backend returns coin_image_url) it falls back to a disc
 * tinted with the coin's own metal rather than a grey box. The screen still
 * reads as a shelf of coins instead of a shelf of missing images.
 */
@Composable
fun CoinDisc(
    imageUrl: String?,
    metalType: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val metal = MetalPalette.of(metalType)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (imageUrl.isNullOrBlank()) {
                    Brush.linearGradient(listOf(metal.highlight, metal.base, metal.shadow))
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            // Inner relief ring - enough to read as a struck disc, not a dot.
            Box(
                Modifier
                    .fillMaxSize(0.56f)
                    .clip(CircleShape)
                    .background(metal.shadow.copy(alpha = 0.35f))
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * The coin as hero of its own screen - the single biggest thing the coin
 * detail screen was missing. Sits on a soft radial plinth so a photo shot on
 * white doesn't float unanchored on the page background.
 */
@Composable
fun CoinHero(
    imageUrl: String?,
    metalType: String?,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CoinDisc(
            imageUrl = imageUrl,
            metalType = metalType,
            size = height - Spacing.xxxl * 2
        )
    }
}

// ----------------------------------------------------------- ruler portrait

/**
 * A ruler portrait at 3:4, NOT a circular avatar.
 *
 * These are painted state portraits: a circular crop cuts the crown off the
 * top and the shoulders off the sides, which is exactly the part that tells
 * one emperor from another. Falls back to a monogram on the ruler's initials
 * - still individual - instead of the identical generic person glyph that
 * made all fourteen rulers look like the same row repeated.
 */
@Composable
fun RulerPortrait(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp
) {
    val height = width * 4 / 3
    Surface(
        modifier = modifier.size(width = width, height = height),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = monogramOf(name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * "Пётр I" -> "ПI", "Анна Иоанновна" -> "АИ", "Elizabeth" -> "E".
 * Roman numerals are kept whole because they are what distinguishes
 * Peter I from Peter II - dropping them would defeat the point.
 */
private fun monogramOf(name: String): String {
    val words = name.trim().split(' ', '-').filter { it.isNotBlank() }
    if (words.isEmpty()) return "?"
    val first = words[0].take(1).uppercase()
    val second = words.getOrNull(1)?.let { w ->
        if (w.all { it in "IVXLCDM" }) w else w.take(1).uppercase()
    } ?: ""
    return (first + second).take(4)
}

// -------------------------------------------------------------- spec grid

data class Spec(val label: String, val value: String?)

/**
 * Labelled numbers in a two-column grid with tabular figures, replacing the
 * run of full-width InfoRow lines where a value sat a whole screen-width
 * away from its own label.
 *
 * Specs whose value is missing are dropped rather than rendered - this is
 * what removes the literal "nullmm" that reached the screen.
 */
@Composable
fun SpecGrid(specs: List<Spec>, modifier: Modifier = Modifier) {
    val present = specs.filter { !it.value.isNullOrBlank() }
    if (present.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        present.chunked(2).forEach { row ->
            // IntrinsicSize.Min makes both cells in a row as tall as the taller
            // one, so a value that wraps to two lines doesn't leave its
            // neighbour floating at a different height.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                row.forEach { spec -> SpecCell(spec, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpecCell(spec: Spec, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(
                text = spec.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = spec.value.orEmpty(),
                style = tabular(MaterialTheme.typography.titleSmall),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A single number with its caption, for the collection header. */
@Composable
fun StatFigure(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
        Text(text = value, style = tabular(MaterialTheme.typography.titleLarge))
        Text(
            text = caption.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Groups digits for readability ("27600013" -> "27 600 013"). Anything that
 * isn't a plain run of digits (the backend sends mintage as free text) is
 * passed through untouched.
 */
fun groupDigits(raw: String?): String? {
    if (raw.isNullOrBlank()) return raw
    val t = raw.trim()
    if (t.length < 5 || !t.all { it.isDigit() }) return t
    return t.reversed().chunked(3).joinToString(" ").reversed()
}

// --------------------------------------------------------------- bidi

/**
 * Forces a fragment to lay out left-to-right inside a right-to-left paragraph.
 *
 * Catalog data is Latin ("2 Marks - William I", "1682 — 1725") while the UI
 * paragraph in Hebrew is right-to-left, and the connecting characters - the
 * dash, the middle dot, a leading digit - are bidi-neutral. Without an isolate
 * they get reordered and the string comes out backwards: Hebrew showed
 * "Marks - William I 2" and, worse, a reign as "1725 — 1682", which is a wrong
 * fact on screen rather than merely an ugly one.
 *
 * LRI/PDI is a no-op in a left-to-right paragraph, so call sites don't need to
 * ask which language is running.
 */
private const val LTR_ISOLATE = '⁦'
private const val POP_DIRECTIONAL_ISOLATE = '⁩'

fun ltrIsolate(text: String): String = LTR_ISOLATE + text + POP_DIRECTIONAL_ISOLATE

// ------------------------------------------------------------- empty state

/**
 * One empty state for the whole app, replacing four different shades of
 * "grey sentence centred on a blank screen". Always says what is missing,
 * why, and offers the way out.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            androidx.compose.material3.Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

// ------------------------------------------------------------------ chips

/**
 * A small factual chip (weight, metal, rarity). Deliberately not a
 * Material FilterChip: those carry selection semantics this doesn't have,
 * and per project rule we build our own look rather than recolouring a
 * library component into something it isn't.
 */
@Composable
fun FactChip(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (emphasized) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (emphasized) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = tabular(MaterialTheme.typography.labelSmall),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
            maxLines = 1
        )
    }
}
