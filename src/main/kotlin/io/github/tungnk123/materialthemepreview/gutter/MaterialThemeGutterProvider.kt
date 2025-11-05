package io.github.tungnk123.materialthemepreview.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import io.github.tungnk123.materialthemepreview.KtMatchers
import io.github.tungnk123.materialthemepreview.service.ThemeIndexService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

class MaterialThemeGutterProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val nameReference = element as? KtNameReferenceExpression ?: return null
        val match = KtMatchers.matchMaterialThemeExpr(nameReference as KtElement) ?: return null
        if (match.kind != KtMatchers.Match.Kind.COLOR) return null

        val colorHex =
            nameReference.project.getService(ThemeIndexService::class.java).getColor(match.name) ?: return null

        val icon = buildColorSquareIcon(colorHex)
        val anchor = nameReference.getReferencedNameElement()

        return LineMarkerInfo(
            anchor, anchor.textRange,
            icon, ({ "Material color: ${match.name} = $colorHex" }),
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "MaterialTheme Color" }
    }

    private fun buildColorSquareIcon(hex: String): Icon {
        val sizePx = JBUI.scale(ICON_SIZE)
        val borderWidthPx = JBUI.scale(BORDER_WIDTH)
        val fillColor = parseHexColor(hex) ?: JBColor.GRAY

        val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = image.createGraphics()
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics2D.color = fillColor
            graphics2D.fillRect(0, 0, sizePx, sizePx)

            graphics2D.color = pickBorderFor(fillColor)
            graphics2D.drawRect(0, 0, sizePx - 1, sizePx - 1)
            repeat(borderWidthPx - 1) { inset ->
                graphics2D.drawRect(inset + 1, inset + 1, sizePx - 3 - inset * 2, sizePx - 3 - inset * 2)
            }
        } finally {
            graphics2D.dispose()
        }
        return ImageIcon(image)
    }

    private fun parseHexColor(hex: String): Color? = runCatching {
        val hexColor = hex.removePrefix("#")
        val argb = when (hexColor.length) {
            6 -> (0xFF shl 24) or hexColor.toInt(16)
            8 -> hexColor.toLong(16).toInt()
            else -> return null
        }
        @Suppress("UseJBColor")
        Color(argb, true)
    }.getOrNull()

    private fun pickBorderFor(color: Color): Color {
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255.0
        return if (luminance > 0.55) JBColor(0x000000, 0x000000) else JBColor(0xFFFFFF, 0xFFFFFF)
    }

    private companion object {
        const val ICON_SIZE = 16
        const val BORDER_WIDTH = 1
    }
}