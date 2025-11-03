package io.github.tungnk123.materialthemepreview.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.*

@Service(Service.Level.PROJECT)
class ThemeIndexService(private val project: Project) {

    data class ColorScheme(val colors: Map<String, String>)
    data class Typography(val textStyles: Map<String, TextStyle>)
    data class TextStyle(val sizeSp: Float?, val weight: Int?, val lineHeightSp: Float?)
    data class Shapes(val corners: Map<String, String>)

    @Volatile
    private var colorScheme = ColorScheme(emptyMap())
    @Volatile
    private var typography = Typography(emptyMap())
    @Volatile
    private var shapes = Shapes(emptyMap())

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {
            override fun childrenChanged(event: PsiTreeChangeEvent) {
                rebuild()
            }
        }, project)
        rebuild()
    }

    fun getColor(name: String) = colorScheme.colors[name]
    fun getTextStyle(name: String) = typography.textStyles[name]
    fun getShape(name: String) = shapes.corners[name]

    private fun rebuild() {
        val newColors = mutableMapOf<String, String>()
        val newText = mutableMapOf<String, TextStyle>()
        val newShapes = mutableMapOf<String, String>()

        val scope = GlobalSearchScope.projectScope(project)
        val vfs = FilenameIndex.getAllFilesByExt(project, "kt", scope)

        vfs.forEach { vf ->
            val ktFile = PsiManager.getInstance(project).findFile(vf) as? KtFile ?: return@forEach
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    val callee = expression.calleeExpression?.text ?: return
                    when (callee) {
                        "lightColorScheme", "darkColorScheme" -> {
                            expression.valueArguments.forEach { arg ->
                                val name = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                val value = arg.getArgumentExpression()?.text ?: return@forEach
                                val hex = Regex("0x([0-9A-Fa-f]{6,8})").find(value)?.groupValues?.get(1)
                                if (hex != null) newColors[name] = "#" + hex.takeLast(6).uppercase()
                            }
                        }

                        "Typography" -> {
                            expression.valueArguments.forEach { arg ->
                                val name = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                val body = arg.getArgumentExpression()?.text ?: return@forEach
                                fun readSp(key: String): Float? =
                                    Regex("$key\\s*=\\s*(\\d+(?:\\.\\d+)?)\\.sp").find(body)?.groupValues?.get(1)
                                        ?.toFloat()

                                val size = readSp("fontSize")
                                val lh = readSp("lineHeight")
                                newText[name] = TextStyle(sizeSp = size, weight = null, lineHeightSp = lh)
                            }
                        }

                        "Shapes" -> {
                            expression.valueArguments.forEach { arg ->
                                val name = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                val body = arg.getArgumentExpression()?.text ?: return@forEach
                                val corner = Regex("RoundedCornerShape\\(([^)]+)\\)").find(body)?.groupValues?.get(1)
                                if (corner != null) newShapes[name] = corner.trim()
                            }
                        }
                    }
                }
            })
        }

        colorScheme = ColorScheme(newColors)
        typography = Typography(newText)
        shapes = Shapes(newShapes)
    }
}
