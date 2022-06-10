package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.*

class IsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {

            if (!isJava(node.sourcePsi)) {
                return
            }

            val fields = node.fields
            val setterToFieldPairs = getSetters(node, fields.toList())
            val classMethods = node.methods
            for (classMethod in classMethods) {
                if (classMethod.isStatic) {
                    continue
                }
                val methodExpressions = PsiTreeUtil.findChildrenOfType(classMethod.javaPsi, PsiMethodCallExpression::class.java)
                for (methodExpression in methodExpressions) {
                    val methodReferenceSource = methodExpression.methodExpression.resolve() ?: continue
                    val resolvedMethod = methodReferenceSource.toUElementOfType<UMethod>() ?: continue
                    for (setterToFieldPair in setterToFieldPairs) {
                        if (setterToFieldPair.first == resolvedMethod) {
                            reportUsage(
                                context = context,
                                setterInvocation = methodExpression,
                                setter = setterToFieldPair.first,
                                field = setterToFieldPair.second
                            )
                        }
                    }
                }
            }
        }

        private fun setterSignatureCheck(method: UMethod): Boolean {
            val returnType = method.returnType
            if (returnType != null && returnType.equalsToText(VOID)) {
                return method.uastParameters.size == 1
            }
            return false
        }

        private fun setterAssignmentCheck(method: UMethod, field: UField): Boolean {
//                UElement.isAssignment()
//                UExpression - Represents an expression or statement (which is considered as an expression in Uast).
            val fieldName = field.name
            val expressions = (method.uastBody as? UBlockExpression)?.expressions ?: return false
            if (expressions.size != 1) {
                return false
            }
            val expression = expressions[0]
            val binaryExpression = expression as? UBinaryExpression ?: return false
            val leftOperand = binaryExpression.leftOperand
            val rightOperand = binaryExpression.rightOperand
            val operatorIdentifier = binaryExpression.operator
            if (operatorIdentifier !is UastBinaryOperator.AssignOperator) {
                return false
            }
            if (rightOperand.toString() != fieldName)  {
                return false
            }
            if (leftOperand.toString() == fieldName) {
                return true
            } else {
                if (leftOperand !is UReferenceExpression) {
                    return false
                }
                val leftOperandSource = leftOperand.resolve() ?: return false

                val leftOperandField = leftOperandSource.toUElementOfType<UField>()
                if (leftOperandField != null && leftOperandField == field) {
                    return true
                }
            }
            return false
        }

        private fun getSetters(node: UClass, fields: List<UField>): List<Pair<UMethod, UField>> {
            val setterToFieldPairs = mutableListOf<Pair<UMethod, UField>>()
            val classMethods = node.methods
            for (classMethod in classMethods) {
                if (classMethod.isStatic) {
                    continue
                }
                if (!setterSignatureCheck(classMethod)) {
                    continue
                }
                val methodParam = classMethod.uastParameters[0]
                val methodName = classMethod.name
                for (field in fields) {
                    val fieldType = field.type
                    if (methodParam.type == fieldType) {
                        val fieldName = field.name
                        if (methodName ==
                            "set${fieldName[0].uppercaseChar()}${fieldName.substring(1)}") {
                            if (setterAssignmentCheck(classMethod, field)) {
                                setterToFieldPairs.add(classMethod to field)
                                break
                            }
                        }
                    }
                }
            }
            return setterToFieldPairs
        }
    }

    private fun reportUsage(context: JavaContext, setterInvocation: PsiMethodCallExpression, setter: UMethod, field: UField) {
        val setterInvocationArgumentText = setterInvocation.argumentList.expressions[0].text

        val setterUsageFix = fix()
            .name("Replace ${setter.name} invocation with field access")
            .family("Replace setter invocation with field access")
            .replace()
            .range(context.getLocation(setterInvocation))
            .all()
            .with("this.${field.name} = $setterInvocationArgumentText")
            .reformat(true)
            .autoFix()
            .build()


        val incident = Incident(context, ISSUE_INTERNAL_SETTER)
            .message("Set the field ${field.name} not through a setter, but directly.")
            .at(setterInvocation)
            .fix(setterUsageFix)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_INTERNAL_SETTER = Issue.create(
            "Internal Setter",
            "Internal field is accessed via setter. That increases memory consumption.",
            "Consider accessing the field directly and only use setter in public API.",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(IsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val VOID = "void"
    }
}
