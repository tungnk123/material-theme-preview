package io.github.tungnk123.materialthemepreview.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import io.github.tungnk123.mtpreview.index.ThemeIndexService
import io.github.tungnk123.mtpreview.util.KtMatchers
import org.jetbrains.kotlin.psi.KtElement
import javax.swing.JPanel
import java.awt.Dimension
import java.awt.Graphics

class MaterialThemeInlayProvider : InlayHintsProvider<NoSettings> {
    override val name = "Material Theme Preview"
    override val key = SettingsKey<NoSettings>("mtpreview.inlay")
    override val previewText = """
        val c = MaterialTheme.colorScheme.primary
        val t = MaterialTheme.typography.titleMedium
        val s = MaterialTheme.shapes.small
    """.trimIndent()
    override fun createSettings(): NoSettings = NoSettings()
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable? = null

    override fun getCollectorFor(
        file: com.intellij.psi.PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        val index = file.project.getService(ThemeIndexService::class.java)
        val factory = PresentationFactory(editor)

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val kt = element as? KtElement ?: return true
                val match = KtMatchers.matchMaterialThemeExpr(kt) ?: return true

                when (match.kind) {
                    KtMatchers.Match.Kind.COLOR -> {
                        val hex = index.getColor(match.name) ?: return true
                        val pres = factory.seq(
                            factory.smallText("  "),
                            factory.roundWithBackground(factory.smallText(hex)),
                            factory.smallText(" "),
                            factory.component(ColorDot(hex))
                        )
                        sink.addInlineElement(element.textRange.endOffset, false, pres, false)
                    }
                    KtMatchers.Match.Kind.TYPO -> {
                        val ts = index.getTextStyle(match.name) ?: return true
                        val label = buildString {
                            append("size="); append(ts.sizeSp?.toString() ?: "?"); append("sp")
                            if (ts.lineHeightSp != null) { append(", lh="); append(ts.lineHeightSp); append("sp") }
                        }
                        sink.addInlineElement(
                            element.textRange.endOffset, false,
                            factory.roundWithBackground(factory.smallText(label)), false
                        )
                    }
                    KtMatchers.Match.Kind.SHAPE -> {
                        val s = index.getShape(match.name) ?: return true
                        sink.addInlineElement(
                            element.textRange.endOffset, false,
                            factory.roundWithBackground(factory.smallText(s)), false
                        )
                    }
                }
                return true
            }
        }
    }
}

private class ColorDot(hex: String) : JPanel() {
    private val color = try { JBColor.decode(hex) } catch (_: Throwable) { JBColor.GRAY }
    init { preferredSize = Dimension(10, 10) }
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = color
        g.fillRoundRect(0, 0, width - 1, height - 1, 4, 4)
    }
}
