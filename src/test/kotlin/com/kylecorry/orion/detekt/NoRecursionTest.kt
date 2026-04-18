package com.kylecorry.orion.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRecursionTest {

    private val env = createEnvironment()
    private val subject = NoRecursion(Config.empty)

    @Test
    fun `reports direct recursion`() {
        val findings = subject.lintWithContext(
            env,
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
        val findings = subject.lintWithContext(
            env,
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
        val findings = subject.lintWithContext(
            env,
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
        val findings = subject.lintWithContext(
            env,
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
        val findings = subject.lintWithContext(
            env,
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
    fun `does not report companion extension function calling another overload on companion`() {
        val findings = subject.lintWithContext(
            env,
            """
                data class Bounds(val min: Double, val max: Double) {
                    companion object {
                        fun from(value: Double): Bounds = Bounds(value, value)
                        fun from(values: List<Double>): Bounds = Bounds(values.min(), values.max())
                    }
                }

                fun Bounds.Companion.from(wrappers: List<List<Double>>): Bounds {
                    val bounds = wrappers.map { from(it) }
                    return from(bounds.flatMap { listOf(it.min, it.max) })
                }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report super calls`() {
        val findings = subject.lintWithContext(
            env,
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
