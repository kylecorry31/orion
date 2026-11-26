package com.kylecorry.orion.detekt

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile

class WhyComment(config: Config) : Rule(
    config,
    "Comments must start with WHY: and explain why unclear code is needed. KDoc is exempt."
) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        PsiTreeUtil.collectElementsOfType(file, PsiComment::class.java)
            .filter { it.tokenType == KtTokens.EOL_COMMENT || it.tokenType == KtTokens.BLOCK_COMMENT }
            .filterNot { it.text.removePrefix("//").removePrefix("/*").trimStart().startsWith("WHY:") }
            .forEach { report(Finding(Entity.from(it), description)) }
    }
}
