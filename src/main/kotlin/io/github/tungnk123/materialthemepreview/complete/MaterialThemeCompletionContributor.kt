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

    private val log = Logger.getInstance(MaterialThemeCompletionContributor::class.java)

    init {
        log.info("[MaterialThemeCompletionContributor] Initialized")
        extend(
            CompletionType.BASIC,
            psiElement().withParent(KtNameReferenceExpression::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
                ) {
                    val position = parameters.position
                    log.info("[Completion] Triggered for element: ${position.text}")

                    if (!isInColorSchemeContext(parameters)) {
                        log.info("[Completion] Not inside MaterialTheme.colorScheme, skipping.")
                        return
                    }

                    val service = position.project.getService(ThemeIndexService::class.java)
                    val colors = service.allColors()
                    log.info("[Completion] Found ${colors.size} colors to suggest")

                    colors.forEach { (name, hex) ->
                        val element = MaterialColorLookup.build(name, hex)
                        result.addElement(PrioritizedLookupElement.withPriority(element, COMPLETION_PRIORITY))
                        log.info("[Completion] Added suggestion: $name = $hex")
                    }

                    log.info("[Completion] Finished adding completions.")
                }
            })
    }

    private fun isInColorSchemeContext(p: CompletionParameters): Boolean {
        val nameRef = p.position.parent as? KtNameReferenceExpression ?: return false
        var qualified = nameRef.parent as? KtQualifiedExpression ?: return false
        while (qualified.parent is KtQualifiedExpression) {
            qualified = qualified.parent as KtQualifiedExpression
        }

        val raw = collectQualifiedNames(qualified)
        val parts = raw.map { it.replace(COMPLETION_TOKEN, "") }.filter { it.isNotEmpty() }
        log.info("[Context] parts=$parts, raw=$raw, expr='${qualified.text}'")

        val ok = parts.getOrNull(0) == "MaterialTheme" && parts.getOrNull(1) == "colorScheme"
        log.info("[Context] chainOK=$ok (head=${parts.getOrNull(0)}, second=${parts.getOrNull(1)})")
        return ok
    }

    private fun collectQualifiedNames(q: KtQualifiedExpression): List<String> {
        val out = mutableListOf<String>()
        fun visit(e: KtExpression?) {
            when (e) {
                is KtQualifiedExpression -> {
                    visit(e.receiverExpression); visit(e.selectorExpression)
                }

                is KtNameReferenceExpression -> out += e.getReferencedName()
                is KtCallExpression -> (e.calleeExpression as? KtNameReferenceExpression)?.let { out += it.getReferencedName() }
            }
        }
        visit(q)
        return out
    }

    private companion object {
        const val COMPLETION_TOKEN = "IntellijIdeaRulezzz"
        const val COMPLETION_PRIORITY = 1000.0
    }
}