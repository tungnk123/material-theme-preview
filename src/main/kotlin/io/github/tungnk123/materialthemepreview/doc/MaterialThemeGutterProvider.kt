package io.github.tungnk123.materialthemepreview.doc

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.GutterIconRenderer
import com.intellij.psi.PsiElement
import io.github.tungnk123.mtpreview.index.ThemeIndexService
import io.github.tungnk123.mtpreview.util.KtMatchers
import org.jetbrains.kotlin.psi.KtElement
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

class MaterialThemeGutterProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val kt = element as? KtElement ?: return null
        val match = KtMatchers.matchMaterialThemeExpr(kt) ?: return null
        if (match.kind != KtMatchers.Match.Kind.COLOR) return null

        val hex = kt.project.getService(ThemeIndexService::class.java).getColor(match.name) ?: return null
        val icon = colorIcon(hex)
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            ({ "Material color: ${match.name} = $hex" }),
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "MaterialTheme Color" }
    }

    private fun colorIcon(hex: String): Icon {
        val c = try { Color(Integer.parseInt(hex.removePrefix("#"), 16)) } catch (_: Throwable) { Color.GRAY }
        val img = BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = c; g.fillRoundRect(0, 0, 12, 12, 4, 4)
        g.dispose()
        return ImageIcon(img)
    }
}
