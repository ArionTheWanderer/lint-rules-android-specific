package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.*

class IgDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {

            if (!isJava(node.sourcePsi)) {
                return
            }

            val fields = node.fields
            val getterToFieldPairs = getGetters(node, fields.toList())
            val classMethods = node.methods
            for (classMethod in classMethods) {
                if (classMethod.isStatic) {
                    continue
                }
                val methodExpressions = PsiTreeUtil.findChildrenOfType(classMethod.javaPsi, PsiMethodCallExpression::class.java)
                for (methodExpression in methodExpressions) {
                    val methodReferenceSource = methodExpression.methodExpression.resolve() ?: continue
                    val resolvedMethod = methodReferenceSource.toUElementOfType<UMethod>() ?: continue
                    for (getterToFieldPair in getterToFieldPairs) {
                        if (getterToFieldPair.first == resolvedMethod) {
                            reportUsage(
                                context = context,
                                getterInvocation = methodExpression,
                                getter = getterToFieldPair.first,
                                field = getterToFieldPair.second
                            )
                        }
                    }
                }
            }
        }

        private fun getterSignatureCheck(method: UMethod): Boolean {
            val returnType = method.returnType
            if (returnType != null && !returnType.equalsToText(VOID)) {
                return method.uastParameters.isEmpty()
            }
            return false
        }

        private fun getterReturnCheck(method: UMethod, field: UField): Boolean {
            val expressions = (method.uastBody as? UBlockExpression)?.expressions ?: return false
            if (expressions.size != 1) {
                return false
            }
            val returnExpression = expressions[0] as? UReturnExpression ?: return false
            val returnExpressionEx = returnExpression.returnExpression ?: return false
            if (returnExpressionEx !is UReferenceExpression) {
                return false
            }
            val returnExpressionSource = returnExpressionEx.resolve() ?: return false
            val returnField = returnExpressionSource.toUElementOfType<UField>()
            if (returnField != null && returnField == field) {
                return true
            }
            return false
        }

        private fun getGetters(node: UClass, fields: List<UField>): List<Pair<UMethod, UField>> {
            val getterToFieldPairs = mutableListOf<Pair<UMethod, UField>>()
            val classMethods = node.methods
            for (classMethod in classMethods) {
                if (classMethod.isStatic) {
                    continue
                }
                if (!getterSignatureCheck(classMethod)) {
                    continue
                }
                val methodReturnType = classMethod.returnType
                val methodName = classMethod.name
                for (field in fields) {
                    val fieldType = field.type
                    if (methodReturnType == fieldType) {
                        val fieldName = field.name
                        if (methodName ==
                            "get${fieldName[0].uppercaseChar()}${fieldName.substring(1)}") {
                            if (getterReturnCheck(classMethod, field)) {
                                getterToFieldPairs.add(classMethod to field)
                                break
                            }
                        }
                    }
                }
            }
            return getterToFieldPairs
        }
    }

    private fun reportUsage(context: JavaContext, getterInvocation: PsiMethodCallExpression, getter: UMethod, field: UField) {

        val getterUsageFix = fix()
            .name("Replace ${getter.name} invocation with field access")
            .family("Replace getter invocation with field access")
            .replace()
            .range(context.getLocation(getterInvocation))
            .all()
            .with(field.name)
            .reformat(true)
            .autoFix()
            .build()


        val incident = Incident(context, ISSUE_INTERNAL_GETTER)
            .message("Access the field ${field.name} not through a getter, but directly.")
            .at(getterInvocation)
            .fix(getterUsageFix)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_INTERNAL_GETTER = Issue.create(
            "Internal Getter",
            "Internal field is accessed via getter. That increases memory consumption.",
            "Consider accessing the field directly and only use getter in public API.",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(IgDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val VOID = "void"
    }
}
