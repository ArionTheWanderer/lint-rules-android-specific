package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

class NlmrDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {
            val evaluator = context.evaluator
            val doesExtendActivity = evaluator.inheritsFrom(node, ACTIVITY, false)
            if (!doesExtendActivity) {
                return
            }
            val methods = node.methods
            var isOnLowMemoryMethodFound = false
            for (method in methods) {
                val doesNameMatch = method.name == ON_LOW_MEMORY_NAME
                val hasNoParameters = method.uastParameters.isEmpty()
                val isNotStatic = !method.isStatic
                val isOverride = evaluator.isOverride(method)
                val isVoidReturnType = method.returnType?.equalsToText(VOID) ?: false
                val isNotConstructor = !method.isConstructor
                if (
                    doesNameMatch && hasNoParameters &&
                    isNotStatic && isOverride &&
                    isVoidReturnType && isNotConstructor
                ) {
                    isOnLowMemoryMethodFound = true
                    break
                }
            }
            if (isOnLowMemoryMethodFound) {
                return
            }
            reportUsage(context, node)
        }
    }

    private fun reportUsage(context: JavaContext, node: UClass) {
        val classMethods = node.methods
        val lintFix: LintFix

        if (isJava(node.sourcePsi)) {
            val onLowMemoryText = ON_LOW_MEMORY_JAVA.trimMargin(".")

            val range: PsiElement? = if (classMethods.isEmpty()) {
                node.lBrace
            } else {
                classMethods.last().sourcePsi
            }

            lintFix =
                fix()
                    .name("Add overridden onLowMemory() method in ${node.name} class")
                    .family("Add overridden onLowMemory() method")
                    .replace()
                    .range(context.getLocation(range))
                    .end()
                    .with("\n$onLowMemoryText")
                    .reformat(true)
                    .autoFix()
                    .build()

        } else if (isKotlin(node.sourcePsi)) {
            var onLowMemoryText = ON_LOW_MEMORY_KOTLIN.trimMargin(".")
            val ktClass = node.sourcePsi as? KtClass
            val rBrace = ktClass?.body?.rBrace
            val lBrace = ktClass?.body?.lBrace

            val range: PsiElement? = lBrace ?: ktClass

            if (lBrace == null) {
                onLowMemoryText = "{$onLowMemoryText"
            }
            if (rBrace == null) {
                onLowMemoryText = "$onLowMemoryText}"
            }

            lintFix =
                fix()
                    .name("Add overridden onLowMemory() method in ${node.name} class")
                    .family("Add overridden onLowMemory() method")
                    .replace()
                    .range(context.getLocation(range))
                    .end()
                    .with("\n$onLowMemoryText")
                    .reformat(true)
                    .autoFix()
                    .build()

        } else throw IllegalArgumentException("Unsupported language")

        val incident = Incident(context, ISSUE_NO_LOW_MEMORY_RESOLVER)
            .message("Override onLowMemory()")
            .at(node)
            .fix(lintFix)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_NO_LOW_MEMORY_RESOLVER = Issue.create(
            "No Low Memory Resolver",
            "This method should clean caches or unnecessary resources.",
            "Override onLowMemory() callback.",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(NlmrDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val ACTIVITY = "android.app.Activity"
        private const val VOID = "void"
        private const val ON_LOW_MEMORY_NAME = "onLowMemory"
        private const val ON_LOW_MEMORY_JAVA = """@Override
                                                 .public void onLowMemory() {
                                                 .    super.onLowMemory();
                                                 .    // TODO clean caches or unnecessary resources
                                                 .}"""
        private const val ON_LOW_MEMORY_KOTLIN = """override fun onLowMemory() {
                                                   .    super.onLowMemory()
                                                   .    // TODO clean caches or unnecessary resources
                                                   .}"""
    }
}
