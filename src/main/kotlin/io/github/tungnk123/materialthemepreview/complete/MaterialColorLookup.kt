package io.github.tungnk123.materialthemepreview.complete

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

object MaterialColorLookup {
    fun build(name: String, hex: String): LookupElement =
        LookupElementBuilder.create(name)
            .withTypeText(hex, true)
            .withIcon(MaterialColorIcon(hex))
}
