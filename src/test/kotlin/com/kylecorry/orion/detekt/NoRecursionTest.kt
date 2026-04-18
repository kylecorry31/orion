package com.kylecorry.orion.detekt

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@KotlinCoreEnvironmentTest
class NoRecursionTest(private val env: KotlinCoreEnvironment) {

    private val subject = NoRecursion(io.gitlab.arturbosch.detekt.api.Config.empty)

    @Test
    fun `reports direct recursion`() {
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
        val findings = subject.compileAndLintWithContext(
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
