package io.github.tungnk123.materialthemepreview.inlay

import com.intellij.codeInsight.hints.InlayHintsProviderFactory
import com.intellij.codeInsight.hints.ProviderInfo
import org.jetbrains.kotlin.idea.KotlinLanguage

class MaterialThemeInlayProviderFactory : InlayHintsProviderFactory {
    override fun getProvidersInfo(): List<ProviderInfo<out Any>> {
        return listOf(
            ProviderInfo(KotlinLanguage.INSTANCE, MaterialThemeInlayProvider())
        )
    }
}