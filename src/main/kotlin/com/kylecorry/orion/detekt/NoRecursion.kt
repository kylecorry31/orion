package com.kylecorry.orion.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class NoRecursion(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "Recursive functions are not allowed.",
        debt = Debt.FIVE_MINS,
    )

    private val functionStack = ArrayDeque<KtNamedFunction>()
    private val allFunctions = mutableListOf<KtNamedFunction>()
    private data class CallInfo(val calledName: String, val argCount: Int, val expression: KtCallExpression)
    private val pendingCalls = mutableMapOf<KtNamedFunction, MutableList<CallInfo>>()

    override fun visitKtFile(file: KtFile) {
        allFunctions.clear()
        pendingCalls.clear()
        super.visitKtFile(file)
        detectIndirectRecursion()
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        allFunctions.add(function)
        functionStack.addLast(function)
        super.visitNamedFunction(function)
        functionStack.removeLast()
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val currentFunction = functionStack.lastOrNull() ?: return super.visitCallExpression(expression)
        if (hasOverloadInScope(currentFunction)) return super.visitCallExpression(expression)
        val calledFunction = expression.calleeExpression?.text
        val receiver = getReceiverText(expression)

        if (
            calledFunction != null &&
            (receiver == null || receiver == "this" || receiver.startsWith("this@"))
        ) {
            if (
                calledFunction == currentFunction.name &&
                expression.valueArguments.size == currentFunction.valueParameters.size
            ) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(expression),
                        message = "Function '${currentFunction.name}' calls itself recursively.",
                    ),
                )
            }
            pendingCalls.getOrPut(currentFunction) { mutableListOf() }
                .add(CallInfo(calledFunction, expression.valueArguments.size, expression))
        }

        super.visitCallExpression(expression)
    }

    private fun detectIndirectRecursion() {
        val functionByName = allFunctions.groupBy { it.name }

        // Build call graph: function -> list of (callee, call expression), excluding self-calls
        val graph = mutableMapOf<KtNamedFunction, MutableList<Pair<KtNamedFunction, KtCallExpression>>>()
        for ((caller, calls) in pendingCalls) {
            for (callInfo in calls) {
                val callees = functionByName[callInfo.calledName]?.filter { callee ->
                    callee !== caller && callee.valueParameters.size == callInfo.argCount
                } ?: continue
                for (callee in callees) {
                    graph.getOrPut(caller) { mutableListOf() }.add(Pair(callee, callInfo.expression))
                }
            }
        }

        // DFS cycle detection
        val visited = mutableSetOf<KtNamedFunction>()
        val reportedCycles = mutableSetOf<Set<KtNamedFunction>>()

        fun dfs(node: KtNamedFunction, path: MutableList<KtNamedFunction>, inPath: MutableSet<KtNamedFunction>) {
            if (node in inPath) {
                val cycleStart = path.indexOf(node)
                val cycle = path.subList(cycleStart, path.size).toSet()
                if (reportedCycles.add(cycle)) {
                    val callExpr = graph[path.last()]?.firstOrNull { it.first == node }?.second
                    if (callExpr != null) {
                        report(
                            CodeSmell(
                                issue = issue,
                                entity = Entity.from(callExpr),
                                message = "Indirect recursion detected involving '${node.name}'.",
                            ),
                        )
                    }
                }
                return
            }
            if (node in visited) return
            visited.add(node)
            inPath.add(node)
            path.add(node)
            for ((neighbor, _) in graph[node] ?: emptyList()) {
                dfs(neighbor, path, inPath)
            }
            path.removeLast()
            inPath.remove(node)
        }

        for (function in allFunctions) {
            if (function !in visited) {
                dfs(function, mutableListOf(), mutableSetOf())
            }
        }
    }

    private fun getReceiverText(expression: KtCallExpression): String? {
        val calleeOffset = expression.calleeExpression?.textOffset ?: return null
        val source = expression.containingFile.text
        var index = calleeOffset - 1

        while (index >= 0 && source[index].isWhitespace()) {
            index--
        }

        if (index < 0 || source[index] != '.') {
            return null
        }

        index--

        while (index >= 0 && source[index].isWhitespace()) {
            index--
        }

        if (index < 0) {
            return null
        }

        val end = index

        while (index >= 0 && (source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '@')) {
            index--
        }

        return source.substring(index + 1, end + 1)
    }

    private fun hasOverloadInScope(function: KtNamedFunction): Boolean {
        return function.parent
            ?.collectDescendantsOfType<KtNamedFunction>()
            ?.any { it !== function && it.parent == function.parent && it.name == function.name } == true
    }
}
