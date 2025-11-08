package io.github.tungnk123.materialthemepreview.doc

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parents
import io.github.tungnk123.materialthemepreview.KtMatchers
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtElement

class MaterialThemeDocTargetProvider : DocumentationTargetProvider {

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val leaf = file.findElementAt(offset) ?: return emptyList()
        return buildTargetsFrom(leaf)
    }

    private fun buildTargetsFrom(element: PsiElement): List<DocumentationTarget> {
        val kt = (element as? KtElement) ?: element.parents(true).filterIsInstance<KtElement>().firstOrNull()
        ?: return emptyList()

        val match = KtMatchers.matchMaterialThemeExpr(kt) ?: return emptyList()
        val index = kt.project.getService(ThemeIndexService::class.java)

        val html = when (match.kind) {
            KtMatchers.Match.Kind.COLOR -> index.getColor(match.name)?.let {
                "<b>Material color:</b> ${match.name}<br/><code>$it</code>"
            }

            KtMatchers.Match.Kind.TYPO -> index.getTextStyle(match.name)?.let {
                "<b>Text style:</b> ${match.name}<br/>size=${it.sizeSp ?: "?"}sp, lineHeight=${it.lineHeightSp ?: "?"}sp"
            }

            KtMatchers.Match.Kind.SHAPE -> index.getShape(match.name)?.let {
                "<b>Shape:</b> ${match.name}<br/>$it"
            }
        } ?: return emptyList()

        val presentation = TargetPresentation.builder(match.name).presentation()

        val target = object : DocumentationTarget {
            override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)
            override fun computePresentation(): TargetPresentation = presentation
            override fun computeDocumentation(): DocumentationResult = DocumentationResult.documentation(html)
            override fun computeDocumentationHint(): String? = null
        }

        return listOf(target)
    }
}