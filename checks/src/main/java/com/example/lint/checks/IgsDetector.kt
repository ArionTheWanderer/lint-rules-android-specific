package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*

class IgsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {

            fun setterSignatureCheck(method: UMethod): Boolean {
                val returnType = method.returnType
                if (returnType != null && returnType.equalsToText(VOID)) {
                    return method.uastParameters.size == 1
                }
                return false
            }

            fun setterAssignmentCheck(method: UMethod, field: UField): Boolean {
//                UElement.isAssignment()
//                UExpression - Represents an expression or statement (which is considered as an expression in Uast).
                val fieldName = field.name
                val expressions = (method.uastBody as? UBlockExpression)?.expressions ?: return false
                for (expression in expressions) {
                    val binaryExpression = expression as? UBinaryExpression
                    val leftOperand = binaryExpression?.leftOperand
                    val rightOperand = binaryExpression?.rightOperand
                    val operatorIdentifier = binaryExpression?.operator
                    if (operatorIdentifier !is UastBinaryOperator.AssignOperator) {
                        continue
                    }
                    if (rightOperand.toString() != fieldName)  {
                        continue
                    }
                    if (leftOperand.toString() == fieldName) {
                        return true
                    } else {
                        if (leftOperand !is UReferenceExpression) {
                            continue
                        }
                        val leftOperandSource = leftOperand.resolve() ?: continue

                        val leftOperandField = leftOperandSource.toUElementOfType<UField>()
                        val leftOperandFieldKotlin = leftOperandSource.toUElementOfType<UMethod>()?.sourcePsi.toUElementOfType<UField>()
                        if (leftOperandField != null && leftOperandField == field) {
                            return true
                        } else if (leftOperandFieldKotlin != null && (leftOperandFieldKotlin == field)) {
                            return true
                        }
                    }
                }
                return false
            }

            fun getSetters(fields: List<UField>): List<Pair<UMethod, UField>> {
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


            val fields = node.fields
            val setterToFieldPairs = getSetters(fields.toList())
            val classMethods = node.methods
            for (classMethod in classMethods) {
                if (classMethod.isStatic) {
                    continue
                }
                val methodExpressions = (classMethod.uastBody as? UBlockExpression)?.expressions ?: continue
                for (methodExpression in methodExpressions) {
                    if (methodExpression !is UCallExpression) {
                        continue
                    }
                    val methodReferenceSource = methodExpression.resolve() ?: continue
                    val resolvedMethod = methodReferenceSource.toUElementOfType<UMethod>() ?: continue
                    for (setterToFieldPair in setterToFieldPairs) {
                        if (setterToFieldPair.first == resolvedMethod) {
                            // TODO report
                            print("Igs has been found")
                        }
                    }
                }
            }

        }
    }

    companion object {
        @JvmField
        val ISSUE_INTERNAL_GETTER_SETTER = Issue.create(
            "Internal Getter/Setter",
            "Internal fields are accessed via getters and setters. That increases memory consumption.",
            "Consider accessing the fields directly and only use getters and setters in public API.",
            Category.PERFORMANCE, 5, Severity.WARNING,
            Implementation(IgsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val VOID = "void"
    }
}
