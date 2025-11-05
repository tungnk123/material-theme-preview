package io.github.tungnk123.materialthemepreview.service

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.psi.*

@Service(Service.Level.PROJECT)
class ThemeIndexService(private val project: Project) : DumbAware {

    private val log = Logger.getInstance(ThemeIndexService::class.java)

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
                rebuildSafe()
            }

            override fun propertyChanged(event: PsiTreeChangeEvent) {
                rebuildSafe()
            }
        }, project)
        rebuildSafe()
    }

    fun getColor(name: String) = colorScheme.colors[name]

    fun allColors(): Map<String, String> = colorScheme.colors
    fun getTextStyle(name: String) = typography.textStyles[name]
    fun getShape(name: String) = shapes.corners[name]
    fun forceRebuild() = rebuildSafe()

    private fun rebuildSafe() {
        ReadAction.nonBlocking {
            val scope = GlobalSearchScope.projectScope(project)
            val vfs = FilenameIndex.getAllFilesByExt(project, "kt", scope)
            val psiManager = PsiManager.getInstance(project)
            log.info("[ThemeIndexService] Rebuilding index from ${vfs.size} Kotlin files...")

            val constColors = mutableMapOf<String, String>()
            val newColors = mutableMapOf<String, String>()
            val newText = mutableMapOf<String, TextStyle>()
            val newShapes = mutableMapOf<String, String>()

            vfs.forEach { vf ->
                val ktFile = psiManager.findFile(vf) as? KtFile ?: return@forEach
                ktFile.accept(object : KtTreeVisitorVoid() {
                    override fun visitProperty(property: KtProperty) {
                        val name = property.name ?: return
                        val initText = property.initializer?.text ?: return
                        val hex = parseColorLiteral(initText)
                        if (hex != null) constColors[name] = hex
                    }
                })
            }
            log.info("[ThemeIndexService] Collected color consts: ${constColors.size}")

            vfs.forEach { vf ->
                val ktFile = psiManager.findFile(vf) as? KtFile ?: return@forEach
                ktFile.accept(object : KtTreeVisitorVoid() {
                    override fun visitCallExpression(expression: KtCallExpression) {
                        when (expression.calleeExpression?.text) {
                            "lightColorScheme", "darkColorScheme" -> {
                                expression.valueArguments.forEach { arg ->
                                    val key = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                    val expr = arg.getArgumentExpression() ?: return@forEach
                                    val color = when (expr) {
                                        is KtCallExpression -> parseColorLiteral(expr.text)
                                        is KtNameReferenceExpression -> constColors[expr.getReferencedName()]
                                        else -> parseColorLiteral(expr.text ?: "")
                                    }
                                    if (color != null) {
                                        newColors[key] = color
                                    }
                                }
                            }

                            "Typography" -> {
                                expression.valueArguments.forEach { arg ->
                                    val key = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                    val body = arg.getArgumentExpression()?.text ?: return@forEach
                                    fun readSp(k: String): Float? =
                                        Regex("$k\\s*=\\s*(\\d+(?:\\.\\d+)?)\\.sp").find(body)?.groupValues?.get(1)
                                            ?.toFloat()

                                    val fs = readSp("fontSize")
                                    val lh = readSp("lineHeight")
                                    newText[key] = TextStyle(sizeSp = fs, weight = null, lineHeightSp = lh)
                                }
                            }

                            "Shapes" -> {
                                expression.valueArguments.forEach { arg ->
                                    val key = arg.getArgumentName()?.asName?.asString() ?: return@forEach
                                    val body = arg.getArgumentExpression()?.text ?: return@forEach
                                    val corner =
                                        Regex("RoundedCornerShape\\(([^)]+)\\)").find(body)?.groupValues?.get(1)?.trim()
                                    if (corner != null) newShapes[key] = corner
                                }
                            }
                        }
                    }
                })
            }

            colorScheme = ColorScheme(newColors)
            typography = Typography(newText)
            shapes = Shapes(newShapes)

            log.info("[ThemeIndexService] Indexed colors=${newColors.size}, textStyles=${newText.size}, shapes=${newShapes.size}")
            if (newColors.isEmpty()) log.warn("[ThemeIndexService] No theme colors resolved. Check that darkColorScheme/lightColorScheme exist and color args reference either Color(0x...) or top-level vals initialized with Color(0x...).")
            null
        }.inSmartMode(project).submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun parseColorLiteral(text: String?): String? {
        if (text == null) return null
        Regex("""(?:^|[\s(])(?:androidx\.compose\.ui\.graphics\.)?Color\s*\(\s*0x([0-9A-Fa-f]{6,8})\s*\)""")
            .find(text)?.groupValues?.get(1)?.let { hex -> return "#" + hex.takeLast(6).uppercase() }
        return null
    }
}
