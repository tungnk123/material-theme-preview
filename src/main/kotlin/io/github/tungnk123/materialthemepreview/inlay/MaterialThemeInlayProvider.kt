package io.github.tungnk123.materialthemepreview.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import io.github.tungnk123.materialthemepreview.KtMatchers
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtElement
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon
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

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JPanel()
            }
        }
    }

    override fun getCollectorFor(
        file: com.intellij.psi.PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        val index = file.project.getService(ThemeIndexService::class.java)

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
                            factory.smallText(" "), factory.icon(colorIcon(hex))
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

    private fun colorIcon(hex: String): Icon {
        val c: Color = try {
            JBColor.decode(hex)
        } catch (_: Throwable) {
            JBColor.GRAY
        }
        val img = UIUtil.createImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics = img.graphics
        g.color = c
        g.fillRoundRect(0, 0, 10, 10, 4, 4)
        g.dispose()
        return ImageIcon(img)
    }
}