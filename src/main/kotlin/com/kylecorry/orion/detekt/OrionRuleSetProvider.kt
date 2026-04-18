package com.kylecorry.orion.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class OrionRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("orion")

    override fun instance(): RuleSet {
        return RuleSet(ruleSetId, listOf(::NoRecursion))
    }
}
