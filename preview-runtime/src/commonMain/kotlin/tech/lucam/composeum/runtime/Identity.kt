package tech.lucam.composeum.runtime

import tech.lucam.composeum.annotation.PreviewGroup
import tech.lucam.composeum.annotation.PreviewTag
import tech.lucam.composeum.annotation.PreviewVariantGroup
import tech.lucam.composeum.annotation.SimplePreviewTag

internal fun PreviewGroup.groupKey(): String =
    this::class.qualifiedName ?: this::class.simpleName ?: name

internal fun PreviewVariantGroup.variantGroupKey(): String =
    this::class.qualifiedName ?: this::class.simpleName ?: toString()

internal fun PreviewTag.tagKey(): String = when (this) {
    is SimplePreviewTag -> "simple:$title"
    else -> this::class.qualifiedName ?: this::class.simpleName ?: title
}
