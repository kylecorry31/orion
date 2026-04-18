package com.kylecorry.orion.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class NoRecursion(config: Config) : Rule(
    config,
    "Recursive functions are not allowed.",
), RequiresAnalysisApi {

    private data class FunctionId(
        val qualifiedName: String,
        val receiverType: String?,
        val parameterTypes: List<String?>,
    )

    private val functionStack = ArrayDeque<FunctionId>()
    private val callGraph = mutableMapOf<FunctionId, MutableList<Pair<FunctionId, KtCallExpression>>>()

    override fun visitKtFile(file: KtFile) {
        callGraph.clear()
        super.visitKtFile(file)
        detectIndirectRecursion()
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val functionId = function.toFunctionId() ?: run {
            super.visitNamedFunction(function)
            return
        }

        functionStack.addLast(functionId)
        super.visitNamedFunction(function)
        functionStack.removeLast()
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val currentFunction = functionStack.lastOrNull()
        val calledFunction = expression.toFunctionId()

        if (currentFunction != null && calledFunction != null) {
            if (calledFunction == currentFunction) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Function '${currentFunction.qualifiedName}' calls itself recursively.",
                    ),
                )
            } else {
                callGraph.getOrPut(currentFunction) { mutableListOf() }
                    .add(Pair(calledFunction, expression))
            }
        }

        super.visitCallExpression(expression)
    }

    private fun detectIndirectRecursion() {
        val visited = mutableSetOf<FunctionId>()
        val reportedCycles = mutableSetOf<Set<FunctionId>>()

        fun dfs(node: FunctionId, path: MutableList<FunctionId>, inPath: MutableMap<FunctionId, Int>) {
            if (node in inPath) {
                val cycleStart = inPath[node]!!
                val cycle = path.subList(cycleStart, path.size).toSet()
                if (reportedCycles.add(cycle)) {
                    val caller = path.last()
                    val callExpr = callGraph[caller]?.firstOrNull { it.first == node }?.second
                    if (callExpr != null) {
                        report(
                            Finding(
                                entity = Entity.from(callExpr),
                                message = "Indirect recursion detected involving '${node.qualifiedName}'.",
                            ),
                        )
                    }
                }
                return
            }
            if (node in visited) return
            visited.add(node)
            inPath[node] = path.size
            path.add(node)
            for ((callee, _) in callGraph[node] ?: emptyList()) {
                dfs(callee, path, inPath)
            }
            path.removeLast()
            inPath.remove(node)
        }

        for (fn in callGraph.keys.toSet()) {
            if (fn !in visited) {
                dfs(fn, mutableListOf(), mutableMapOf())
            }
        }
    }

    private fun KtNamedFunction.toFunctionId(): FunctionId? {
        val name = fqName?.asString() ?: this.name ?: return null
        return analyze(this) {
            FunctionId(
                qualifiedName = name,
                receiverType = receiverTypeReference?.type?.toStableString(),
                parameterTypes = valueParameters.map { parameter ->
                    if (parameter.isVarArg) {
                        "vararg ${parameter.returnType.arrayElementType?.toStableString()}"
                    } else {
                        parameter.returnType.toStableString()
                    }
                },
            )
        }
    }

    private fun KtCallExpression.toFunctionId(): FunctionId? = analyze(this) {
        val symbol = resolveToCall()?.singleFunctionCallOrNull()?.symbol as? KaFunctionSymbol ?: return@analyze null
        FunctionId(
            qualifiedName = symbol.callableId?.asSingleFqName()?.asString()
                ?: calleeExpression?.text
                ?: return@analyze null,
            receiverType = symbol.receiverParameter?.returnType?.toStableString(),
            parameterTypes = symbol.valueParameters.map { parameter ->
                if (parameter.isVararg) {
                    "vararg ${parameter.returnType.toStableString()}"
                } else {
                    parameter.returnType.toStableString()
                }
            },
        )
    }

    private fun KaType.toStableString(): String =
        toString().replace('/', '.').removeSuffix("!")
}
