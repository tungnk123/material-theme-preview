package io.github.tungnk123.materialthemepreview.complete

import com.intellij.codeInsight.completion.*
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

                    if (!isMaterialThemeColorSchemeContext(parameters)) {
                        logger.info("[Completion] Not inside MaterialTheme.colorScheme, skipping.")
                        return
                    }

                    val themeIndex = caretLeaf.project.getService(ThemeIndexService::class.java)
                    val colorEntries = themeIndex.allColors()
                    logger.info("[Completion] Found ${colorEntries.size} colors to suggest")

                    colorEntries.forEach { (colorName, hexColor) ->
                        val lookup = MaterialColorLookup.build(colorName, hexColor)
                        result.addElement(
                            PrioritizedLookupElement.withPriority(lookup, COMPLETION_PRIORITY)
                        )
                        logger.info("[Completion] Added suggestion: $colorName = $hexColor")
                    }

                    logger.info("[Completion] Finished adding completions.")
                }
            }
        )
    }

    private fun isMaterialThemeColorSchemeContext(parameters: CompletionParameters): Boolean {
        val nameReference = parameters.position.parent as? KtNameReferenceExpression ?: return false
        var topQualified = nameReference.parent as? KtQualifiedExpression ?: return false

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
        val isColorChain = head == "MaterialTheme" && second == "colorScheme"

        logger.info("[Context] chainOK=$isColorChain (head=$head, second=$second)")
        return isColorChain
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

    private companion object {
        const val PLACEHOLDER_TOKEN = "IntellijIdeaRulezzz"
        const val COMPLETION_PRIORITY = 1000.0
    }
}