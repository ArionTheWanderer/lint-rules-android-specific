package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.*

class IdsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitClass(node: UClass) {
            val fields = node.fields
            for (field in fields) {
                val fieldClassType = field.type.accept(IdsTypeVisitor()) ?: continue
                reportUsage(context, field)
            }
        }

        override fun visitMethod(node: UMethod) {
            if (isJava(node.sourcePsi)) {
                val localVars = PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiLocalVariable::class.java)
                val params = node.uastParameters
                val returnType = node.returnType
                for (localVar in localVars) {
                    val localVarClassType = localVar.type.accept(IdsTypeVisitor()) ?: continue
                    val localVarUElement = localVar.toUElementOfType<UElement>()
                    if (localVarUElement != null) {
                        reportUsage(context, localVarUElement)
                    }
                }
                for (param in params) {
                    val paramClassType = param.type.accept(IdsTypeVisitor()) ?: continue

                    reportUsage(context, param)
                }
                val returnClassType = returnType?.accept(IdsTypeVisitor())
                if (returnClassType != null) {
                    val returnTypeElement = node.returnTypeElement.toUElementOfType<UElement>()
                    if (returnTypeElement != null) {
                        reportUsage(context, returnTypeElement)
                    }
                }
            } else if (isKotlin(node.sourcePsi)) {
                val uField = node.sourcePsi.toUElementOfType<UField>()
                if (uField != null) {
                    return
                }
                val localVars = PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtProperty::class.java)
                val params = node.uastParameters
                val returnType = node.returnType
                for (localVar in localVars) {
                    val localUVar = localVar.toUElementOfType<UVariable>()
                    val localVarClassType = localUVar?.type?.accept(IdsTypeVisitor()) ?: continue
                    reportUsage(context, localUVar)
                }
                for (param in params) {
                    val paramClassType = param.type.accept(IdsTypeVisitor()) ?: continue
                    reportUsage(context, param)
                }
                val returnClassType = returnType?.accept(IdsTypeVisitor())
                if (returnClassType != null) {
                   val returnTypeElement = node.returnTypeReference
                    if (returnTypeElement != null) {
                        reportUsage(context, returnTypeElement)
                    }
                }
            }
        }

    }

    private fun reportUsage(context: JavaContext, node: UElement) {
        val incident = Incident(context, MimDetector.ISSUE_MEMBER_IGNORING_METHOD)
            .message("Replace with SparseArray")
            .at(node)
        context.report(incident)
    }

    class IdsTypeVisitor: PsiTypeVisitor<PsiClassType?>() {

        override fun visitClassType(classType: PsiClassType): PsiClassType? {
            if (classType.parameterCount != 2){
                return null
            }

            val className = classType.className
            if (!className.equals(MAP) && !className.equals(HASHMAP) && !className.equals(
                    MUTABLE_HASHMAP)) {
                return null
            }

            val parameters = classType.parameters
            if (parameters[0] !is PsiClassType) {
                return null
            }

            val intParameter = parameters[0] as PsiClassType

            if (intParameter.hasParameters()) {
                return null
            }

            if (parameters[0].presentableText != INT &&
                parameters[0].presentableText != INTEGER
            ) {
                return null
            }

            if (parameters[1] !is PsiClassType) {
                return null
            }

            return classType

        }

        override fun visitType(type: PsiType): PsiClassType? {
            return null
        }

        override fun visitPrimitiveType(primitiveType: PsiPrimitiveType): PsiClassType? {
            return null
        }

        override fun visitArrayType(arrayType: PsiArrayType): PsiClassType? {
            return null
        }

        override fun visitCapturedWildcardType(capturedWildcardType: PsiCapturedWildcardType): PsiClassType? {
            return null
        }

        override fun visitWildcardType(wildcardType: PsiWildcardType): PsiClassType? {
            return null
        }

        override fun visitEllipsisType(ellipsisType: PsiEllipsisType): PsiClassType? {
            return null
        }

        override fun visitDisjunctionType(disjunctionType: PsiDisjunctionType): PsiClassType? {
            return null
        }

        override fun visitIntersectionType(intersectionType: PsiIntersectionType): PsiClassType? {
            return null
        }

        override fun visitDiamondType(diamondType: PsiDiamondType): PsiClassType? {
            return null
        }

        override fun visitLambdaExpressionType(lambdaExpressionType: PsiLambdaExpressionType): PsiClassType? {
            return null
        }

        override fun visitMethodReferenceType(methodReferenceType: PsiMethodReferenceType): PsiClassType? {
            return null
        }

    }


    companion object {
        @JvmField
        val ISSUE_INEFFICIENT_DATA_STRUCTURE = Issue.create(
            "Inefficient data structure",
            "The use of HashMap<Integer, Object> is slow.",
            "Use SparseArray instead.",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(IdsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val MUTABLE_HASHMAP = "MutableHashMap"
        private const val HASHMAP = "HashMap"
        private const val MAP = "Map"
        private const val INT = "Int"
        private const val INTEGER = "Integer"
    }
}
