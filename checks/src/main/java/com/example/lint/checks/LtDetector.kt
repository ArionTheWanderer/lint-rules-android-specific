package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.uast.*

class LtDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        listOf<Class<out UElement>>(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitClass(node: UClass) {
            val evaluator = context.evaluator
            val doesExtendActivity = evaluator.inheritsFrom(node, ACTIVITY, false)
            if (!doesExtendActivity) {
                return
            }

            val threadFields = mutableListOf<UField>()
            val fields = node.fields
            fields.forEach { field ->
                if (field.type.equalsToText(THREAD)) {
                    threadFields.add(field)
                }
            }

            for (threadField in threadFields) {
                if (isCreated(node, threadField)) {
                    if (isStartedButNotInterrupted(node, threadField)) {
                        reportUsage(context, node, threadField)
                    }
                }
            }

        }

        private fun isCreated(node: UClass, threadField: UField): Boolean {
            val methods = node.methods
            val threadFieldName = threadField.name
            val initializer = threadField.uastInitializer
            val initString = initializer.toString()
            for (method in methods) {
                val expressions = (method.uastBody as? UBlockExpression)?.expressions ?: continue
                for (expression in expressions) {
                    if (expression !is UBinaryExpression) {
                        continue
                    }
                    if (expression.operator !is UastBinaryOperator.AssignOperator) {
                        continue
                    }

                    val leftOperand = expression.leftOperand
                    if (leftOperand.toString() == threadFieldName) {
                        return true
                    } else {
                        if (leftOperand !is UReferenceExpression) {
                            return false
                        }
                        val leftOperandSource = leftOperand.resolve() ?: return false

                        val leftOperandField = leftOperandSource.toUElementOfType<UField>()
                        if (leftOperandField != null && leftOperandField == threadField) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun isStartedButNotInterrupted(node: UClass, threadField: UField): Boolean {
            var isStarted = false
            var isInterrupted = false
            if (isJava(node.sourcePsi)) {
                val methods = node.methods
                for (method in methods) {
                    val methodCalls = PsiTreeUtil.findChildrenOfType(method.javaPsi, PsiMethodCallExpression::class.java)
                    for (methodCall in methodCalls) {
                        val methodExpression = methodCall.methodExpression
                        val callName = methodExpression.referenceName
                        if (!methodCall.argumentList.isEmpty || !callName.equals(START) &&
                                !callName.equals(INTERRUPT)) {
                            continue
                        }

                        val fieldRef: PsiReferenceExpression
                        val possibleNextReference =
                            methodExpression.firstChild as? PsiReferenceExpression
                        if (possibleNextReference != null) {
                            fieldRef = possibleNextReference
                        } else {
                            continue
                        }

                        val fieldSource = fieldRef.resolve() ?: continue

                        val field = fieldSource as? PsiField

                        if (field != null) {
                            if (!field.type.equalsToText(THREAD)) {
                                continue
                            }

                            if (field.name == threadField.name) {
                                when (callName) {
                                    START -> isStarted = true
                                    INTERRUPT -> isInterrupted = true
                                    else -> continue
                                }
                            }
                        } else {
                            continue
                        }
                    }
                }
            } else if (isKotlin(node.sourcePsi)) {
                val methods = node.methods
                for (method in methods) {
                    val methodCalls = PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtCallExpression::class.java)
                    for (methodCall in methodCalls) {
                        val callName = methodCall.text
                        val argumentsCount = methodCall.valueArguments.size
                        if ((argumentsCount > 0) || !callName.equals(START_KT) &&
                                !callName.equals(INTERRUPT_KT)) {
                            continue
                        }

                        val thread: KtNameReferenceExpression?
                        val possibleDotParent =
                            methodCall.parent as? KtDotQualifiedExpression
                        if (possibleDotParent != null) {
                            thread = PsiTreeUtil.findChildOfType(
                                possibleDotParent.toUElementOfType<UElement>()?.sourcePsi,
                                KtNameReferenceExpression::class.java
                            )
                        } else {
                            continue
                        }

                        val threadReference = thread.toUElementOfType<UReferenceExpression>()
                        val threadSource = threadReference?.resolve() ?: continue

                        val threadMethodSource = threadSource.toUElementOfType<UMethod>()?.sourcePsi
                        val threadFieldResolved = threadMethodSource.toUElementOfType<UField>()

                        val threadFieldSource = threadSource.toUElementOfType<UField>()

                        if (threadFieldResolved != null) {
                            if (!threadFieldResolved.type.equalsToText(THREAD)) {
                                continue
                            }
                            if (threadFieldResolved.name == threadField.name) {
                                when (callName) {
                                    START_KT -> isStarted = true
                                    INTERRUPT_KT -> isInterrupted = true
                                    else -> continue
                                }
                            }
                        } else if (threadFieldSource != null){
                            if (!threadFieldSource.type.equalsToText(THREAD)) {
                                continue
                            }
                            if (threadFieldSource.name == threadField.name) {
                                when (callName) {
                                    START_KT -> isStarted = true
                                    INTERRUPT_KT -> isInterrupted = true
                                    else -> continue
                                }
                            }
                        }
                    }
                }
            }
            if (isStarted && !isInterrupted) {
                return true
            }
            return false
        }

    }

    private fun reportUsage(context: JavaContext, containingClass: UClass, threadField: UField) {
        var onDestroyMethod: UMethod? = null
        val classMethods = containingClass.methods
        for (method in classMethods) {
            val doesNameMatch = method.name == ON_DESTROY_NAME
            val hasNoParameters = method.uastParameters.isEmpty()
            val isNotStatic = !method.isStatic
            val isOverride = context.evaluator.isOverride(method)
            val isVoidReturnType = method.returnType?.equalsToText(VOID) ?: false
            val isNotConstructor = !method.isConstructor
            if (
                doesNameMatch && hasNoParameters &&
                isNotStatic && isOverride &&
                isVoidReturnType && isNotConstructor
            ) {
                onDestroyMethod = method
                break
            }
        }

        val leakingThreadFix: LintFix

        if (isJava(threadField.sourcePsi)) {
            val range: PsiElement?
            if (onDestroyMethod != null) {
                val lastExpression = (onDestroyMethod.uastBody as? UBlockExpression)?.expressions?.last()
                if (lastExpression != null) {
                    range = lastExpression.sourcePsi
                    val threadInterruptExpression = "${threadField.name}.interrupt();"
                    leakingThreadFix = fix()
                        .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                        .family("Add threadField.interrupt() expression in onDestroy() method")
                        .replace()
                        .range(context.getLocation(range))
                        .end()
                        .with("\n$threadInterruptExpression")
                        .reformat(true)
                        .autoFix()
                        .build()
                } else {
                    val onDestroyText = createOnDestroy("${threadField.name}.interrupt();")
                    leakingThreadFix = fix()
                        .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                        .family("Add threadField.interrupt() expression in onDestroy() method")
                        .replace()
                        .range(context.getLocation(onDestroyMethod))
                        .all()
                        .with(onDestroyText)
                        .reformat(true)
                        .autoFix()
                        .build()
                }
            } else {
                range = if (classMethods.isEmpty()) {
                    containingClass.lBrace
                } else {
                    classMethods.last().sourcePsi
                }

                val onDestroyText = createOnDestroy("${threadField.name}.interrupt();")
                leakingThreadFix = fix()
                    .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                    .family("Add threadField.interrupt() expression in onDestroy() method")
                    .replace()
                    .range(context.getLocation(range))
                    .end()
                    .with("\n$onDestroyText")
                    .reformat(true)
                    .autoFix()
                    .build()
            }
        } else if (isKotlin(threadField.sourcePsi)) {
            val range: PsiElement?
            if (onDestroyMethod != null) {
                val lastExpression = (onDestroyMethod.uastBody as? UBlockExpression)?.expressions?.last()
                if (lastExpression != null) {
                    range = lastExpression.sourcePsi
                    val threadInterruptExpression = "${threadField.name}.interrupt()"
                    leakingThreadFix = fix()
                        .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                        .family("Add threadField.interrupt() expression in onDestroy() method")
                        .replace()
                        .range(context.getLocation(range))
                        .end()
                        .with("\n$threadInterruptExpression")
                        .reformat(true)
                        .autoFix()
                        .build()
                } else {
                    val onDestroyText = createOnDestroyKt("${threadField.name}.interrupt()")
                    leakingThreadFix = fix()
                        .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                        .family("Add threadField.interrupt() expression in onDestroy() method")
                        .replace()
                        .range(context.getLocation(onDestroyMethod))
                        .all()
                        .with(onDestroyText)
                        .reformat(true)
                        .autoFix()
                        .build()
                }
            } else {
                val ktClass = containingClass.sourcePsi as? KtClass
                val lBrace = ktClass?.body?.lBrace

                val onDestroyText: String
                if (lBrace != null) {
                    range = lBrace
                    onDestroyText = createOnDestroyKt("${threadField.name}.interrupt()")
                } else if (ktClass != null) {
                    range = ktClass
                    onDestroyText = createOnDestroyKtWithBrackets("${threadField.name}.interrupt()")
                } else throw IllegalArgumentException("Body error")

                leakingThreadFix = fix()
                    .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
                    .family("Add threadField.interrupt() expression in onDestroy() method")
                    .replace()
                    .range(context.getLocation(range))
                    .end()
                    .with("\n$onDestroyText")
                    .reformat(true)
                    .autoFix()
                    .build()
            }
        } else throw IllegalArgumentException("Unsupported language")


        val incident = Incident(context, ISSUE_LEAKING_THREAD)
            .message("Thread field ${threadField.name} is started and not interrupted.")
            .at(threadField)
            .fix(leakingThreadFix)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_LEAKING_THREAD = Issue.create(
            "Leaking Thread",
            "Activity starts a thread and does not stop it.",
            "Interrupt a thread in onDestroy() method.",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(LtDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val VOID = "void"
        private const val ACTIVITY = "android.app.Activity"
        private const val THREAD = "java.lang.Thread"
        private const val START = "start"
        private const val START_KT = "start()"
        private const val INTERRUPT = "interrupt"
        private const val INTERRUPT_KT = "interrupt()"
        private const val ON_DESTROY_NAME = "onDestroy"
        private fun createOnDestroy(expression: String) = """@Override
                                                            .protected void onDestroy() {
                                                            .    super.onDestroy();
                                                            .    $expression
                                                            .}""".trimMargin(".")
        private fun createOnDestroyKt(expression: String) = """override fun onDestroy() {
                                                              .    super.onDestroy()
                                                              .    $expression
                                                              .}""".trimMargin(".")
        private fun createOnDestroyKtWithBrackets(expression: String) = """{override fun onDestroy() {
                                                              .    super.onDestroy()
                                                              .    $expression
                                                              .}}""".trimMargin(".")
    }
}
