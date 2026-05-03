package tech.lucam.composeum.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import tech.lucam.composeum.ksp.codegen.ParamFormGenerator
import tech.lucam.composeum.ksp.codegen.RegistryGenerator
import tech.lucam.composeum.ksp.model.ModelBuilder
import tech.lucam.composeum.ksp.validation.Validator

/** KSP processor for @ComposePreview — generates preview registries and param forms. */
internal class ComposeumProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val logger = environment.logger
    private val strictTypes = environment.options["composeum.strictTypes"]
        ?.toBooleanStrictOrNull() ?: true
    private val includeAndroidPreview = environment.options["composeum.includeAndroidPreview"]
        ?.toBooleanStrictOrNull() ?: false
    private val enableKdoc = environment.options["composeum.enableKdoc"]
        ?.toBooleanStrictOrNull() ?: false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val composeModels = resolver
            .getSymbolsWithAnnotation("tech.lucam.composeum.annotation.ComposePreview")
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { Validator.validate(it, logger, resolver, strictTypes) }
            .map { ModelBuilder.build(it, enableKdoc) }

        val viewModels = resolver
            .getSymbolsWithAnnotation("tech.lucam.composeum.annotation.ViewPreview")
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { Validator.validateViewPreview(it, logger, resolver) }
            .map { ModelBuilder.buildViewPreview(it, enableKdoc) }

        val androidPreviewModels = if (includeAndroidPreview) {
            resolver
                .getSymbolsWithAnnotation("androidx.compose.ui.tooling.preview.Preview")
                .filterIsInstance<KSFunctionDeclaration>()
                .filter { Validator.validateAndroidPreview(it, logger, resolver, strictTypes) }
                .map { ModelBuilder.buildAndroidPreview(it, enableKdoc) }
        } else {
            emptySequence()
        }

        val models = (composeModels + viewModels + androidPreviewModels).toList()

        if (models.isNotEmpty() && !reportDuplicateKeys(models) && !reportVariantGroupIssues(models)) {
            val fnPkg = models.first().functionPackage
            val defaultPackage = if (fnPkg.isEmpty()) "generated" else "$fnPkg.generated"
            val registryPackage = environment.options["composeum.registryPackage"] ?: defaultPackage
            val registryName =
                environment.options["composeum.registryName"] ?: "GeneratedPreviewRegistry"
            RegistryGenerator.generate(
                models,
                environment.codeGenerator,
                registryPackage,
                registryName
            )
            for (model in models.filter { !it.isViewPreview }) {
                ParamFormGenerator.generate(model, environment.codeGenerator, registryPackage)
            }
        }

        return emptyList()
    }

    private fun reportDuplicateKeys(models: List<tech.lucam.composeum.ksp.model.PreviewModel>): Boolean {
        var hasDuplicates = false
        models.groupBy { it.key }
            .filterValues { it.size > 1 }
            .forEach { (key, duplicates) ->
                hasDuplicates = true
                val sources = duplicates.joinToString { model ->
                    when {
                        model.isAndroidPreview -> "@Preview"
                        model.isViewPreview -> "@ViewPreview"
                        else -> "@ComposePreview"
                    }
                }
                logger.error(
                    "Duplicate preview key '$key' detected from $sources. " +
                            "Each discovered preview must have a unique fully-qualified function name.",
                )
            }
        return hasDuplicates
    }

    private fun reportVariantGroupIssues(models: List<tech.lucam.composeum.ksp.model.PreviewModel>): Boolean {
        var hasErrors = false
        models.groupBy { it.variantGroupImport.takeIf(String::isNotEmpty) ?: return@groupBy null }
            .filterKeys { it != null }
            .values
            .forEach { family ->
                val defaults = family.count { it.isDefaultVariant }
                if (defaults > 1) {
                    hasErrors = true
                    logger.error(
                        "Variant group '${family.first().variantGroupImport}' declares $defaults default previews. " +
                                "Mark exactly one preview with isDefaultVariant=true.",
                    )
                }
                val distinctGroups =
                    family.map { it.groupImport.ifEmpty { it.groupExpression } }.distinct()
                if (distinctGroups.size > 1) {
                    hasErrors = true
                    logger.error(
                        "Variant group '${family.first().variantGroupImport}' is used across multiple preview groups " +
                                distinctGroups.joinToString(prefix = "[", postfix = "]") +
                                ". Keep a variant family inside a single PreviewGroup.",
                    )
                }
            }
        return hasErrors
    }
}
