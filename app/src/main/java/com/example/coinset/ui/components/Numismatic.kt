package com.example.coinset.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// -------------------------------------------------------------- era emblem

/**
 * A historical era, drawn as its own flag in a circle.
 *
 * The period picker used to be three identical grey slabs carrying the same
 * generic clock glyph, where the only thing separating the Empire from the
 * USSR was a pair of dates. A flag is what people actually recognise an era
 * by, and a circle is the shape this app already speaks in - the ruler
 * avatars, the coin disc, the metal tiles.
 *
 * A flag is a wide rectangle, so it is cropped rather than fitted: the
 * horizontal bands that carry a tricolour's identity survive a centre crop
 * intact, while fitting would leave the flag as a thin strip floating in a
 * circle of background.
 *
 * Two things the ring is doing, not decoration: the imperial and the modern
 * Russian flags both contain a white band, and without a border they would
 * bleed into a light background and lose their edge; and the same ring, drawn
 * around the fallback, keeps a flagless era exactly the same shape and size as
 * a flagged one, so a half-populated screen still reads as one row of circles.
 */
@Composable
fun EraEmblem(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // The monogram is drawn first and the flag over it, rather than one or
        // the other. A missing flag and a flag that failed to load look the
        // same to the reader and deserve the same answer: this way the circle
        // is never an empty disc, whether the URL is absent, still loading, or
        // pointing at a file that isn't there.
        Text(
            text = eraMonogram(name),
            // The monogram is sized off the circle it sits in, not fixed: the
            // same emblem is drawn at 104dp in the era list and at 28dp in the
            // strip on a country row, and a titleLarge monogram in a 28dp
            // circle is a letter with its sides sheared off.
            style = if (size >= 64.dp) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * "Российская Империя" -> "РИ", "СССР" -> "СССР", "Weimar Republic" -> "WR".
 *
 * A single word is kept whole when it is short, because an acronym is already
 * the name people use - reducing "СССР" to "С" would be worse than useless.
 * Multi-word names collapse to initials, which is what makes the Empire and
 * the Federation tell each other apart at a glance.
 */
private fun eraMonogram(name: String): String {
    val words = name.trim().split(' ', '-', '–').filter { it.isNotBlank() }
    if (words.isEmpty()) return "?"
    if (words.size == 1) {
        val only = words[0]
        return if (only.length <= 4) only.uppercase() else only.take(2).uppercase()
    }
    return words.take(3).joinToString("") { it.take(1).uppercase() }
}

/** One era as the flag strip needs it: what to draw, and what to draw instead. */
data class EraChip(val name: String, val imageUrl: String?)

/**
 * The eras of one country as a row of small flags.
 *
 * This replaces "Периодов: 3" on the country row. "Период" is our own word -
 * a column name that leaked onto the screen - and the number told a reader
 * nothing they could act on: three *what*, and three of which? The flags say
 * the thing the counter was standing in for, which is that opening Russia
 * leads to the Empire, the USSR and the Federation.
 *
 * Sized to the space it is actually given rather than to a fixed count.
 * Germany has seven eras and Britain has one, and a hard cap picked for
 * Germany would waste the row for everyone else while still breaking on the
 * next long country name, a larger font scale, or a narrower phone. So the
 * strip measures: as many whole circles as fit, and if they don't all fit, the
 * last slot becomes "+N" so the count is never silently wrong.
 *
 * contentDescription is passed in rather than assembled from the flags: a
 * screen reader announcing seven wordless images is worse than the counter
 * this replaced, so the row keeps saying "7 эпох" out loud while showing
 * flags on screen.
 */
@Composable
fun EraFlagStrip(
    eras: List<EraChip>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    diameter: Dp = 28.dp,
    gap: Dp = Spacing.xs
) {
    if (eras.isEmpty()) return
    // The explicit height is load-bearing, not styling. BoxWithConstraints is a
    // SubcomposeLayout, and Material3's ListItem measures its supporting slot by
    // asking for its intrinsic height first - a question a SubcomposeLayout
    // answers by throwing. A size modifier lets that question be answered
    // without subcomposing, which is exactly what the exception message asks
    // for. The strip is a row of fixed-diameter circles, so its height was
    // never in doubt anyway.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(diameter)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            }
    ) {
        val slot = diameter + gap
        // n circles occupy n*diameter + (n-1)*gap, i.e. n*slot - gap.
        val fits = ((maxWidth + gap) / slot).toInt().coerceAtLeast(1)
        val shown = if (eras.size > fits) eras.take(fits - 1) else eras
        val hidden = eras.size - shown.size
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            shown.forEach { era ->
                EraEmblem(imageUrl = era.imageUrl, name = era.name, size = diameter)
            }
            if (hidden > 0) {
                Box(
                    modifier = Modifier
                        .size(diameter)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // ltrIsolate: "+2" is a sign and a digit, both
                        // bidi-neutral, and in Hebrew an unisolated one renders
                        // as "2+".
                        text = ltrIsolate("+$hidden"),
                        style = tabular(MaterialTheme.typography.labelSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
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
 * A spec value longer than this cannot sit in a half-width cell without either
 * wrapping mid-number or being cut off, so it is given the whole row instead.
 *
 * The mintage columns are what forced this. They are not numbers but source
 * text - "12,3 млн (1826–31)", "не менее 555 510 192 (1897–1917)" - and every
 * word of it is load-bearing: "не менее" means a year with no surviving figures
 * was left out of the sum, so it is an exact statement, not a hedge. An
 * ellipsis there would turn a fact into a wrong fact, which is why the grid
 * widens the cell rather than trimming the value.
 */
private const val WIDE_VALUE_CHARS = 16

/** Same idea for the caption: a label that would ellipsize stops being a label. */
private const val WIDE_LABEL_CHARS = 18

/**
 * Bidi isolates are invisible, so they must not count towards the width a
 * value needs - otherwise an isolated value would jump to a full row two
 * characters before an identical bare one.
 */
private fun visibleLength(text: String?): Int =
    text?.count { !it.isBidiControl() } ?: 0

// U+2066..U+2069 are the isolates, U+202A..U+202E the older embeddings.
// Compared by code point rather than by character literal: these
// characters are invisible, so a literal range reads as an empty string
// in the source and the next person cannot tell what it covers.
private fun Char.isBidiControl(): Boolean =
    code in 0x2066..0x2069 || code in 0x202A..0x202E

private val Spec.needsFullWidth: Boolean
    get() = visibleLength(value) > WIDE_VALUE_CHARS || label.length > WIDE_LABEL_CHARS

/**
 * Labelled numbers in a two-column grid with tabular figures, replacing the
 * run of full-width InfoRow lines where a value sat a whole screen-width
 * away from its own label.
 *
 * Specs whose value is missing are dropped rather than rendered - this is
 * what removes the literal "nullmm" that reached the screen, and it is also
 * what lets the same list of specs serve the Empire's gold (mint, edge,
 * mintage and mint master all filled) and a coin from any other country
 * (all four empty): the grid simply closes up around the gaps.
 */
@Composable
fun SpecGrid(specs: List<Spec>, modifier: Modifier = Modifier) {
    val present = specs.filter { !it.value.isNullOrBlank() }
    if (present.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        packSpecs(present).forEach { row ->
            // IntrinsicSize.Min makes both cells in a row as tall as the taller
            // one, so a value that wraps to two lines doesn't leave its
            // neighbour floating at a different height.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // A lone cell takes the whole row rather than sitting in half
                // of one with a hole beside it: the block stays a clean
                // rectangle, and a short value next to a long one no longer
                // makes the grid look like it failed to load the other half.
                row.forEach { spec ->
                    SpecCell(spec, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/**
 * Lays the specs out into rows: two short ones side by side, a long one alone
 * across the full width. Order is never rearranged to pack tighter - the
 * sequence is the reading order of the coin (what it is, then what it is made
 * of, then how many were made), and a gap is cheaper than a shuffled fact.
 */
private fun packSpecs(specs: List<Spec>): List<List<Spec>> {
    val rows = mutableListOf<List<Spec>>()
    var pending: Spec? = null
    for (spec in specs) {
        if (spec.needsFullWidth) {
            pending?.let { rows += listOf(it) }
            pending = null
            rows += listOf(spec)
        } else if (pending == null) {
            pending = spec
        } else {
            rows += listOf(pending, spec)
            pending = null
        }
    }
    pending?.let { rows += listOf(it) }
    return rows
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
                // A full-width cell holds the longest value the column can
                // store (50 characters) in two lines with room to spare, so
                // three lines means nothing is ever actually cut there.
                maxLines = if (spec.needsFullWidth) 3 else 2,
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

/**
 * A mintage figure, ready to drop into a spec cell in any language.
 *
 * A bare run of digits gets thousands separators; anything the source phrased
 * itself - "12,3 млн (1826–31)", "не менее 555 510 192 (1897–1917)" - is left
 * standing exactly as written. "не менее" in particular is an exact statement,
 * not a hedge: the years with no surviving figures were left out of the sum, so
 * rounding it or cutting it back to a number would print something untrue.
 *
 * The result is isolated left-to-right, because in Hebrew the digits, comma,
 * dash and brackets are all bidi-neutral and the years in the trailing bracket
 * would otherwise swap places, showing a range that never existed.
 */
fun mintageValue(raw: String?): String? =
    groupDigits(raw)?.takeIf { it.isNotBlank() }?.let(::ltrIsolate)

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

/**
 * The mint master's initials, drawn the way they appear on the coin: a small
 * punched mark, outlined rather than filled.
 *
 * This is not a FactChip on purpose. Weight and rarity are facts *about* a
 * coin; the mint master's initials are part of its *identity* - they are the
 * only thing that tells a 1899 "10 рублей" struck under Аполлон Грасгоф from
 * the one struck under Эликум Бабаянц. Two rows with the same denomination
 * and the same year must not read as a duplicate, so the mark gets the
 * accent colour and sits on the headline row, not down among the chips.
 *
 * Initials are Cyrillic or Latin ("АГ", "BS", "НФ, ДС, АГ") and always run
 * left-to-right, so they are isolated to survive a Hebrew RTL layout.
 */
@Composable
fun MintMark(initials: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Text(
            text = ltrIsolate(initials),
            style = tabular(MaterialTheme.typography.labelMedium).copy(letterSpacing = 0.6.sp),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
