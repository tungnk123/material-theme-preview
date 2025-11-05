package io.github.tungnk123.materialthemepreview.complete

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class MaterialColorIcon(private val hexColor: String) : Icon {

    private val iconSizePx = JBUI.scale(ICON_SIZE_DP)
    private val color: JBColor = parseHexColor(hexColor) ?: JBColor.GRAY

    override fun getIconWidth(): Int = iconSizePx
    override fun getIconHeight(): Int = iconSizePx

    override fun paintIcon(component: Component?, graphics: Graphics, posX: Int, posY: Int) {
        val previousColor = graphics.color
        graphics.color = color
        graphics.fillRect(posX, posY, iconSizePx, iconSizePx)
        graphics.color = previousColor
    }

    private fun parseHexColor(value: String): JBColor? = runCatching {
        val cleanHex = value.removePrefix("#")
        val argb = when (cleanHex.length) {
            6 -> (0xFF shl 24) or cleanHex.toInt(16)
            8 -> cleanHex.toLong(16).toInt()
            else -> return null
        }
        val colorValue = Color(argb, true)
        JBColor(colorValue, colorValue)
    }.getOrNull()

    private companion object {
        const val ICON_SIZE_DP = 10
    }
}