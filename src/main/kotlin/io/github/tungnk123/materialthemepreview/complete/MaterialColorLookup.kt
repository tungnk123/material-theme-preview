package io.github.tungnk123.materialthemepreview.complete

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

object MaterialColorLookup {
    fun build(colorName: String, hexColor: String): LookupElement {
        return LookupElementBuilder.create(colorName)
            .withPresentableText(colorName)
            .withTypeText(hexColor, true)
            .withTailText("  MaterialTheme.colorScheme", true)
            .withItemTextItalic(false)
            .withStrikeoutness(false)
            .withIcon(MaterialColorIcon(hexColor))
            .withTypeIconRightAligned(true)
            .withInsertHandler { context, _ ->
                val doc = context.document
                val offset = context.tailOffset
                val textAfter = doc.charsSequence.getOrNull(offset)
                if (textAfter != ' ' && textAfter != ',' && textAfter != ')') {
                    doc.insertString(offset, " ")
                    context.editor.caretModel.moveToOffset(offset + 1)
                }
            }
    }
}
