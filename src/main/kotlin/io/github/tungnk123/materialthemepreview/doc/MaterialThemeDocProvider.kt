package io.github.tungnk123.materialthemepreview.doc

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import io.github.tungnk123.materialthemepreview.KtMatchers
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtElement

class MaterialThemeDocProvider : DocumentationProvider {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val kt = originalElement as? KtElement ?: return null
        val match = KtMatchers.matchMaterialThemeExpr(kt) ?: return null
        val idx = kt.project.getService(ThemeIndexService::class.java)

        return when (match.kind) {
            KtMatchers.Match.Kind.COLOR ->
                idx.getColor(match.name)?.let {
                    "<b>Material color:</b> ${match.name}<br/><code>$it</code>"
                }
            KtMatchers.Match.Kind.TYPO ->
                idx.getTextStyle(match.name)?.let {
                    "<b>Text style:</b> ${match.name}<br/>" +
                            "size=${it.sizeSp ?: "?"}sp, lineHeight=${it.lineHeightSp ?: "?"}sp"
                }
            KtMatchers.Match.Kind.SHAPE ->
                idx.getShape(match.name)?.let {
                    "<b>Shape:</b> ${match.name}<br/>$it"
                }
        }
    }
}