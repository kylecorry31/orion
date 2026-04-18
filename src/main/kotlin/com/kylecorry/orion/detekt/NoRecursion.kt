package com.kylecorry.orion.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

@RequiresTypeResolution
class NoRecursion(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "Recursive functions are not allowed.",
        debt = Debt.FIVE_MINS,
    )

    private val functionStack = ArrayDeque<CallableDescriptor>()
    private val allDescriptors = mutableListOf<CallableDescriptor>()
    private val callGraph = mutableMapOf<CallableDescriptor, MutableList<Pair<CallableDescriptor, KtCallExpression>>>()

    override fun visitKtFile(file: KtFile) {
        allDescriptors.clear()
        callGraph.clear()
        super.visitKtFile(file)
        detectIndirectRecursion()
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val functionDescriptor =
            bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, function] as? CallableDescriptor

        if (functionDescriptor == null) {
            super.visitNamedFunction(function)
            return
        }

        allDescriptors.add(functionDescriptor.original)
        functionStack.addLast(functionDescriptor.original)
        super.visitNamedFunction(function)
        functionStack.removeLast()
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val currentFunction = functionStack.lastOrNull()
        val calledFunction = resolveCallDescriptor(expression)

        if (currentFunction != null && calledFunction != null) {
            if (calledFunction == currentFunction) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(expression),
                        message = "Function '${currentFunction.name}' calls itself recursively.",
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
        val visited = mutableSetOf<CallableDescriptor>()
        val reportedCycles = mutableSetOf<Set<CallableDescriptor>>()

        fun dfs(node: CallableDescriptor, path: MutableList<CallableDescriptor>, inPath: MutableMap<CallableDescriptor, Int>) {
            if (node in inPath) {
                val cycleStart = inPath[node]!!
                val cycle = path.subList(cycleStart, path.size).toSet()
                if (reportedCycles.add(cycle)) {
                    val caller = path.last()
                    val callExpr = callGraph[caller]?.firstOrNull { it.first == node }?.second
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
            inPath[node] = path.size
            path.add(node)
            for ((neighbor, _) in callGraph[node] ?: emptyList()) {
                dfs(neighbor, path, inPath)
            }
            path.removeLast()
            inPath.remove(node)
        }

        for (descriptor in allDescriptors) {
            if (descriptor !in visited) {
                dfs(descriptor, mutableListOf(), mutableMapOf())
            }
        }
    }

    private fun resolveCallDescriptor(expression: KtCallExpression): CallableDescriptor? {
        val resolvedCall = callUtilMethod.invoke(null, expression, bindingContext) as? ResolvedCall<*>
        return resolvedCall?.resultingDescriptor?.original
    }

    private companion object {
        val callUtilMethod =
            Class.forName("org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt")
                .getMethod("getResolvedCall", KtElement::class.java, BindingContext::class.java)
    }
}
