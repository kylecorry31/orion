package com.kylecorry.orion.detekt

import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRecursionTest {

    private val subject = NoRecursion(io.gitlab.arturbosch.detekt.api.Config.empty)

    @Test
    fun `reports direct recursion`() {
        val findings = subject.lint(
            """
                fun factorial(n: Int): Int {
                    return if (n <= 1) 1 else n * factorial(n - 1)
                }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `reports indirect recursion`() {
        val findings = subject.lint(
            """
                fun factorial(n: Int): Int {
                    return if (n <= 1) 1 else n * wrapper(n - 1)
                }

                fun wrapper(n: Int): Int {
                    return factorial(n)
                }

            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report non-recursive function calls`() {
        val findings = subject.lint(
            """
                fun helper(n: Int): Int {
                    return n - 1
                }

                fun factorial(n: Int): Int {
                    return if (n <= 1) 1 else n * helper(n)
                }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report overload delegation`() {
        val findings = subject.lint(
            """
                object Example {
                    fun toRadians(angle: Double): Double {
                        return Math.toRadians(angle)
                    }

                    fun toRadians(angle: Float): Float {
                        return toRadians(angle.toDouble()).toFloat()
                    }
                }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report qualified calls`() {
        val findings = subject.lint(
            """
                class Example {
                    fun test() {
                        helper.test()
                    }

                    private val helper = Helper()
                }

                class Helper {
                    fun test() {
                    }
                }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report super calls`() {
        val findings = subject.lint(
            """
                open class Base {
                    open fun onCreate() {
                    }
                }

                class Child : Base() {
                    override fun onCreate() {
                        super.onCreate()
                    }
                }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
