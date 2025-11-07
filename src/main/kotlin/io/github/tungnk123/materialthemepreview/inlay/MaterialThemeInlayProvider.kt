package io.github.tungnk123.materialthemepreview.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import io.github.tungnk123.materialthemepreview.KtMatchers
import io.github.tungnk123.materialthemepreview.complete.MaterialColorIcon
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtElement
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class MaterialThemeInlayProvider : InlayHintsProvider<NoSettings> {

    override val name: String = "Material Theme Preview"
    override val key: SettingsKey<NoSettings> = SettingsKey("mtpreview.inlay")
    override val previewText: String = """
        val c = MaterialTheme.colorScheme.primary
        val t = MaterialTheme.typography.titleMedium
        val s = MaterialTheme.shapes.small
    """.trimIndent()

    override fun createSettings(): NoSettings = NoSettings()

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun getCollectorFor(
        file: com.intellij.psi.PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
    ): InlayHintsCollector {
        val themeIndex = file.project.getService(ThemeIndexService::class.java)

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val ktElement = element as? KtElement ?: return true
                val match = KtMatchers.matchMaterialThemeExpr(ktElement) ?: return true

                when (match.kind) {
                    KtMatchers.Match.Kind.COLOR -> {
                        val hex = themeIndex.getColor(match.name) ?: return true
                        val presentation = factory.seq(
                            factory.text("  "),
                            factory.roundWithBackground(factory.text(hex)),
                            factory.text(" "),
                            factory.icon(MaterialColorIcon(hex))
                        )
                        sink.addInlineElement(element.textRange.endOffset, false, presentation, false)
                    }

                    KtMatchers.Match.Kind.TYPO -> {
                        val textStyle = themeIndex.getTextStyle(match.name) ?: return true
                        val label = buildString {
                            append("size=")
                            append(textStyle.sizeSp?.toString() ?: "?")
                            append("sp")
                            if (textStyle.lineHeightSp != null) {
                                append(", line-height=")
                                append(textStyle.lineHeightSp)
                                append("sp")
                            }
                        }
                        sink.addInlineElement(
                            offset = element.textRange.endOffset,
                            relatesToPrecedingText = false,
                            presentation = factory.roundWithBackground(factory.text(label)),
                            placeAtTheEndOfLine = false
                        )
                    }

                    KtMatchers.Match.Kind.SHAPE -> {
                        val shapeText = themeIndex.getShape(match.name) ?: return true
                        sink.addInlineElement(
                            element.textRange.endOffset,
                            false,
                            factory.roundWithBackground(factory.text("$shapeText dp")),
                            false
                        )
                    }
                }
                return true
            }
        }
    }
}