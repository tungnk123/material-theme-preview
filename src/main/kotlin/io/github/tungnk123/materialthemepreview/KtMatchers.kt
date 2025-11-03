package io.github.tungnk123.materialthemepreview

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

object KtMatchers {
    data class Match(val kind: Kind, val name: String) {
        enum class Kind { COLOR, TYPO, SHAPE }
    }

    fun matchMaterialThemeExpr(element: KtElement): Match? {
        val qe = element.getQualifiedExpressionForSelector() as? KtDotQualifiedExpression ?: return null
        val text = qe.text
        return when {
            text.startsWith("MaterialTheme.colorScheme.") -> {
                val name = text.removePrefix("MaterialTheme.colorScheme.").substringBefore(".")
                Match(Match.Kind.COLOR, name)
            }

            text.startsWith("MaterialTheme.typography.") -> {
                val name = text.removePrefix("MaterialTheme.typography.").substringBefore(".")
                Match(Match.Kind.TYPO, name)
            }

            text.startsWith("MaterialTheme.shapes.") -> {
                val name = text.removePrefix("MaterialTheme.shapes.").substringBefore(".")
                Match(Match.Kind.SHAPE, name)
            }

            else -> null
        }
    }
}
