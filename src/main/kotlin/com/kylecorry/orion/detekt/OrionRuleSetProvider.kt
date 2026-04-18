package com.kylecorry.orion.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class OrionRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "orion"

    override fun instance(config: Config): RuleSet {
        return RuleSet(ruleSetId, listOf(NoRecursion(config)))
    }
}
