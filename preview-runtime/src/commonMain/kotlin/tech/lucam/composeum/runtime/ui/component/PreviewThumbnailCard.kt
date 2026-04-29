package tech.lucam.composeum.runtime.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import tech.lucam.composeum.runtime.PreviewEntry
import tech.lucam.composeum.runtime.withCustomTypeDefaults
import tech.lucam.composeum.runtime.config.PreviewWrapper

/** Reference render width used when scaling the preview to thumbnail size. */
private const val LOGICAL_WIDTH_PX_HINT = 1080f

/**
 * Renders a single [PreviewEntry] as a scaled-down live thumbnail card.
 *
 * The preview composable is measured at a fixed logical width and then scaled down
 * visually via [graphicsLayer] to fit the card's column cell. Long-pressing copies
 * [PreviewEntry.key] to the clipboard.
 *
 * @param entry          The preview entry to render.
 * @param previewWrapper Optional wrapper applied around the entry's composable content.
 * @param showTags       Whether to show tag chips below the name.
 * @param onClick        Called when the card is tapped.
 * @param modifier       Modifier applied to the root column.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewThumbnailCard(
    entry: PreviewEntry,
    previewWrapper: PreviewWrapper?,
    showTags: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { clipboardManager.setText(AnnotatedString(entry.key)) },
            )
            .semantics { role = Role.Button },
    ) {
        // Thumbnail preview area — provide the entry's default param state so composables
        // that read LocalPreviewParamState (including custom-type params) render correctly.
        val config = LocalPreviewConfig.current
        val thumbnailState = remember(entry.key) {
            entry.paramDefaults.toInitialState().withCustomTypeDefaults(entry, config)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clearAndSetSemantics { disabled() },
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            CompositionLocalProvider(LocalPreviewParamState provides thumbnailState) {
                ScaledPreviewContent(entry = entry, previewWrapper = previewWrapper)
            }
        }

        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (showTags && entry.tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 2.dp),
            ) {
                items(entry.tags) { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag.title, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

/**
 * Renders [entry] at a fixed logical width and scales the output down via [graphicsLayer]
 * when needed to fit the available layout bounds. Small previews keep their natural size;
 * larger previews are scaled down to fit the card. Uses a custom [layout] modifier so that
 * the parent sees the card's actual size while the composable renders at full logical resolution.
 */
@Composable
private fun ScaledPreviewContent(
    entry: PreviewEntry,
    previewWrapper: PreviewWrapper?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    // Allow content to keep its natural size up to a screen-sized logical viewport.
                    val logicalWidth = LOGICAL_WIDTH_PX_HINT.toInt().coerceAtLeast(constraints.maxWidth)
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = 0,
                            minHeight = 0,
                            maxWidth = logicalWidth,
                            maxHeight = constraints.maxHeight.coerceAtLeast(1),
                        ),
                    )
                    val widthScale = if (placeable.width > 0) {
                        constraints.maxWidth.toFloat() / placeable.width.toFloat()
                    } else {
                        1f
                    }
                    val heightScale = if (placeable.height > 0) {
                        constraints.maxHeight.toFloat() / placeable.height.toFloat()
                    } else {
                        1f
                    }
                    val scale = minOf(1f, widthScale, heightScale)
                    val scaledWidth = (placeable.width * scale).toInt().coerceAtMost(constraints.maxWidth)
                    val scaledHeight = (placeable.height * scale).toInt().coerceAtMost(constraints.maxHeight)
                    val dx = ((constraints.maxWidth - scaledWidth) / 2f).toInt()
                    val dy = ((constraints.maxHeight - scaledHeight) / 2f).toInt()

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeWithLayer(x = dx, y = dy) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                },
        ) {
            if (previewWrapper != null) {
                previewWrapper(entry) { PreviewRenderer(entry, modifier = Modifier.wrapContentSize()) }
            } else {
                PreviewRenderer(entry, modifier = Modifier.wrapContentSize())
            }
        }
    }
}
