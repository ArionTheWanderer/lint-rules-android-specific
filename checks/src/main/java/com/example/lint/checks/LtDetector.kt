package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
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
//        val classMethods = containingClass.methods
//        val leakingThreadFix: LintFix
//
//        if (isJava(threadField.sourcePsi)) {
//
//        } else if (isKotlin(threadField.sourcePsi)) {
//
//        } else throw IllegalArgumentException("Unsupported language")
//
//        val leakingThreadFix = fix()
//            .name("Add ${threadField.name}.interrupt() expression in onDestroy() method")
//            .family("Add threadField.interrupt() expression in onDestroy() method")
//            .replace()
//            .range(context.getLocation(getterInvocation))
//            .all()
//            .with(field.name)
//            .reformat(true)
//            .autoFix()
//            .build()

        val incident = Incident(context, ISSUE_LEAKING_THREAD)
            .message("Thread field ${threadField.name} is started and not interrupted.")
            .at(threadField)
//            .fix(leakingThreadFix)
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
        private const val ACTIVITY = "android.app.Activity"
        private const val THREAD = "java.lang.Thread"
        private const val START = "start"
        private const val START_KT = "start()"
        private const val INTERRUPT = "interrupt"
        private const val INTERRUPT_KT = "interrupt()"
        private const val ON_DESTROY = """@Override
                                         .protected void onDestroy() {
                                         .    super.onDestroy();
                                         .}"""
        private const val ON_DESTROY_KT = """override fun onDestroy() {
                                            .    super.onDestroy()
                                            .}"""
    }
}