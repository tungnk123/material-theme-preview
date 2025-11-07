package io.github.tungnk123.materialthemepreview.complete

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class MaterialThemeCompletionContributor : CompletionContributor() {

    private val logger = Logger.getInstance(MaterialThemeCompletionContributor::class.java)

    init {
        logger.info("[MaterialThemeCompletionContributor] Initialized")

        extend(
            CompletionType.BASIC,
            psiElement().withParent(KtNameReferenceExpression::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val caretLeaf = parameters.position
                    logger.info("[Completion] Triggered for element: ${caretLeaf.text}")

                    val contextKind = resolveMaterialThemeContext(parameters) ?: run {
                        logger.info("[Completion] Not inside MaterialTheme.{colorScheme|typography|shapes}, skipping.")
                        return
                    }

                    val themeIndex = caretLeaf.project.getService(ThemeIndexService::class.java)

                    when (contextKind) {
                        ContextKind.COLOR -> {
                            val colors = themeIndex.allColors()
                            logger.info("[Completion] COLOR context: ${colors.size} items")
                            colors.forEach { (name, hex) ->
                                val element = MaterialColorLookup.build(name, hex)
                                result.addElement(PrioritizedLookupElement.withPriority(element, COMPLETION_PRIORITY))
                                logger.info("[Completion] + color: $name = $hex")
                            }
                        }

                        ContextKind.TYPOGRAPHY -> {
                            val styles = themeIndex.allTextStyles()
                            logger.info("[Completion] TYPOGRAPHY context: ${styles.size} items")
                            styles.forEach { (name, style) ->
                                val size = style.sizeSp?.let { "${it}sp" } ?: "?"
                                val lineHeight = style.lineHeightSp?.let { "${it}sp" } ?: "?"
                                val typeText = "size=$size, line-height=$lineHeight"
                                val element = LookupElementBuilder
                                    .create(name)
                                    .withTypeText(typeText, true)
                                    .withTailText("  Material text style", true)
                                    .bold()
                                result.addElement(PrioritizedLookupElement.withPriority(element, COMPLETION_PRIORITY))
                                logger.info("[Completion] + typo: $name ($typeText)")
                            }
                        }

                        ContextKind.SHAPES -> {
                            val shapes = themeIndex.allShapes()
                            logger.info("[Completion] SHAPES context: ${shapes.size} items")
                            shapes.forEach { (name, shapeDesc) ->
                                val shapeSize = "$shapeDesc dp"
                                val element = LookupElementBuilder
                                    .create(name)
                                    .withTypeText(shapeSize, true)
                                    .withTailText("  Material shape", true)
                                    .bold()
                                result.addElement(PrioritizedLookupElement.withPriority(element, COMPLETION_PRIORITY))
                                logger.info("[Completion] + shape: $name = $shapeDesc")
                            }
                        }
                    }

                    logger.info("[Completion] Finished adding completions.")
                }
            }
        )
    }

    private fun resolveMaterialThemeContext(parameters: CompletionParameters): ContextKind? {
        val nameReference = parameters.position.parent as? KtNameReferenceExpression ?: return null
        var topQualified = nameReference.parent as? KtQualifiedExpression ?: return null
        while (topQualified.parent is KtQualifiedExpression) {
            topQualified = topQualified.parent as KtQualifiedExpression
        }

        val rawSegments = collectQualifiedNameSegments(topQualified)
        val cleanedSegments = rawSegments
            .map { it.replace(PLACEHOLDER_TOKEN, "") }
            .filter { it.isNotEmpty() }

        logger.info("[Context] parts=$cleanedSegments, raw=$rawSegments, expr='${topQualified.text}'")

        val head = cleanedSegments.getOrNull(0)
        val second = cleanedSegments.getOrNull(1)
        if (head != "MaterialTheme") return null

        val kind = when (second) {
            "colorScheme" -> ContextKind.COLOR
            "typography"  -> ContextKind.TYPOGRAPHY
            "shapes"      -> ContextKind.SHAPES
            else          -> null
        }

        logger.info("[Context] chainOK=${kind != null} (head=$head, second=$second, kind=$kind)")
        return kind
    }

    private fun collectQualifiedNameSegments(qualified: KtQualifiedExpression): List<String> {
        val segments = mutableListOf<String>()
        fun visit(expression: KtExpression?) {
            when (expression) {
                is KtQualifiedExpression -> {
                    visit(expression.receiverExpression)
                    visit(expression.selectorExpression)
                }
                is KtNameReferenceExpression -> segments += expression.getReferencedName()
                is KtCallExpression ->
                    (expression.calleeExpression as? KtNameReferenceExpression)
                        ?.let { segments += it.getReferencedName() }
            }
        }
        visit(qualified)
        return segments
    }

    private enum class ContextKind { COLOR, TYPOGRAPHY, SHAPES }

    private companion object {
        const val PLACEHOLDER_TOKEN = "IntellijIdeaRulezzz"
        const val COMPLETION_PRIORITY = 1000.0
    }
}
