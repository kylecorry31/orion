package com.kylecorry.orion.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class WhyCommentTest {

    private val subject = WhyComment(Config.empty)

    @Test
    fun `reports comments without WHY`() {
        val findings = subject.lint(
            """
                // line
                /* block */
                fun a() = 1 // trailing
            """.trimIndent(),
        )

        assertEquals(3, findings.size)
    }

    @Test
    fun `does not report WHY comments`() {
        val findings = subject.lint(
            """
                // WHY: line
                //WHY: no space
                /* WHY: block
                   that spans lines */
                fun a() = 1 // WHY: trailing
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report KDoc`() {
        val findings = subject.lint(
            """
                /**
                 * Documentation
                 * @param x the value
                 */
                fun a(x: Int) = x
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report comment markers in strings`() {
        val findings = subject.lint(
            """
                val url = "https://example.com/*"
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
