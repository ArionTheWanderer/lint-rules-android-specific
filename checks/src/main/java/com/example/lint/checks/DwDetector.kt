package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import java.lang.IllegalArgumentException

class DwDetector : Detector(), Detector.UastScanner {

    data class WlFieldStructure(
        val uField: UField,
        var isAcquired: Boolean = false,
        var isReleased: Boolean = false
    )

    // stores data for an analyze, then releases
    private val wlFieldsMap = mutableMapOf<String, MutableMap<String, WlFieldStructure>>()


    override fun afterCheckFile(context: Context) {
        for ((clazz, map) in wlFieldsMap.entries) {
            for ((fieldName, wlFieldStructure) in map.entries) {
                if (wlFieldStructure.isAcquired && !wlFieldStructure.isReleased) {
                    // TODO report
                    println("ЕСТЬ!")
                }
            }
        }
    }

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitClass(node: UClass) {
            // node.allFields?
            val fields = node.fields
            for (field in fields) {
                if (field.type.equalsToText(POWER_MANAGER_WAKELOCK) ||
                    field.type.equalsToText(MY_DATA_STRUCTURE) ||
                    field.type.equalsToText(MY_DATA_STRUCTURE_KT)) {
                    val wlClass = node.qualifiedName ?: continue
                    val wlFieldName = field.name

                    if (wlFieldsMap[wlClass] == null) {
                        wlFieldsMap[wlClass] = mutableMapOf()
                    }
                    wlFieldsMap[wlClass]?.set(wlFieldName, WlFieldStructure(field))
                }
            }
        }

        override fun visitMethod(node: UMethod) {

//            fun hasPowerManagerWakelockType(expression: PsiReferenceExpression): Boolean {
//                val classFields = node.containingClass?.fields
//                val localVariables = PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiLocalVariable::class.java)
//                if ((classFields != null && classFields.isEmpty()) && (localVariables.isEmpty())) {
//                    return false
//                }
//
//                return false
//            }
            // TODO случай вызова acquire на методе newWakeLock() (проверка поля/переменной на тип)
            if (isJava(node.sourcePsi)) {
                val methodCalls = PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiMethodCallExpression::class.java)
                for ((index, methodCall) in methodCalls.withIndex()) {
                    val methodExpression = methodCall.methodExpression
                    val callName = methodExpression.referenceName
                    if (!methodCall.argumentList.isEmpty || !callName.equals(ACQUIRE) && !callName.equals(RELEASE)) {
                        continue
                    }

                    val wlRef: PsiReferenceExpression
                    val possibleNextReference =
                        methodExpression.firstChild as? PsiReferenceExpression
                    if (possibleNextReference != null) {
                        wlRef = possibleNextReference
                    } else {
                        continue
                    }

                    val wlSource = wlRef.resolve() ?: continue

                    val wlField = wlSource as? PsiField
                    val wlLocalVariable = wlSource as? PsiLocalVariable
                    val wlParameter = wlSource as? PsiParameter

                    if (wlField != null) {

                        if (!wlField.type.equalsToText(POWER_MANAGER_WAKELOCK) &&
                            !wlField.type.equalsToText(MY_DATA_STRUCTURE) &&
                            !wlField.type.equalsToText(MY_DATA_STRUCTURE_KT)) {
                            continue
                        }

                        val isInClass = PsiTreeUtil.isAncestor(
                            node.containingClass,
                            wlSource,
                            true
                        )
                        if (!isInClass) {
                            continue
                        }

                        val clazz = node.containingClass?.toUElementOfType<UClass>()?.qualifiedName ?: continue
                        val wlFieldStructure = wlFieldsMap[clazz]?.get(wlField.name)
                            ?: throw IllegalArgumentException("Where da feeld?")
                        if (callName.equals(ACQUIRE)) {
                            wlFieldStructure.isAcquired = true
                        } else if (callName.equals(RELEASE)) {
                            wlFieldStructure.isReleased = true
                        }
                        continue

                    } else if (wlLocalVariable != null) {

                        if (!callName.equals(ACQUIRE)) {
                            continue
                        }

                        if (!wlLocalVariable.type.equalsToText(POWER_MANAGER_WAKELOCK) &&
                            !wlLocalVariable.type.equalsToText(MY_DATA_STRUCTURE) &&
                            !wlLocalVariable.type.equalsToText(MY_DATA_STRUCTURE_KT)) {
                            continue
                        }

                        val isInMethod = PsiTreeUtil.isAncestor(
                            node.javaPsi,
                            wlSource,
                            true
                        )
                        if (!isInMethod) {
                            continue
                        }

                        for ((indexInternal, methodCallInternal) in methodCalls.withIndex()) {
                            if (indexInternal <= index) {
                                continue
                            }
                            val methodExpressionInternal = methodCallInternal.methodExpression
                            val callNameInternal = methodExpressionInternal.referenceName

                            val wlRefInternal: PsiReferenceExpression
                            val possibleNextReferenceInternal =
                                methodExpressionInternal.firstChild as? PsiReferenceExpression
                            if (possibleNextReferenceInternal != null) {
                                wlRefInternal = possibleNextReferenceInternal
                            } else {
                                continue
                            }

                            val wlSourceInternal = wlRefInternal.resolve() ?: continue
                            val wlLocalVariableInternal = wlSourceInternal as? PsiLocalVariable
                                ?: continue
                            val refComparison = wlLocalVariableInternal === wlLocalVariable
                            val dataComparison = wlLocalVariableInternal == wlLocalVariable
                            if (refComparison || dataComparison) {
                                if (callNameInternal.equals(RELEASE)) {
                                    break
                                }
                            }

                        }
                        // TODO report
                        println("ЕСТЬ!")
                    } else if (wlParameter != null) {

                        if (!callName.equals(ACQUIRE)) {
                            continue
                        }

                        if (!wlParameter.type.equalsToText(POWER_MANAGER_WAKELOCK) &&
                            !wlParameter.type.equalsToText(MY_DATA_STRUCTURE) &&
                            !wlParameter.type.equalsToText(MY_DATA_STRUCTURE_KT)) {
                            continue
                        }

                        val isInMethod = PsiTreeUtil.isAncestor(
                            node.javaPsi,
                            wlSource,
                            true
                        )
                        if (!isInMethod) {
                            continue
                        }

                        for ((indexInternal, methodCallInternal) in methodCalls.withIndex()) {
                            if (indexInternal <= index) {
                                continue
                            }
                            val methodExpressionInternal = methodCallInternal.methodExpression
                            val callNameInternal = methodExpressionInternal.referenceName

                            val wlRefInternal: PsiReferenceExpression
                            val possibleNextReferenceInternal =
                                methodExpressionInternal.firstChild as? PsiReferenceExpression
                            if (possibleNextReferenceInternal != null) {
                                wlRefInternal = possibleNextReferenceInternal
                            } else {
                                continue
                            }

                            val wlSourceInternal = wlRefInternal.resolve() ?: continue
                            val wlParameterInternal = wlSourceInternal as? PsiParameter
                                ?: continue
                            val refComparison = wlParameterInternal === wlParameter
                            val dataComparison = wlParameterInternal == wlParameter
                            if (refComparison || dataComparison) {
                                if (callNameInternal.equals(RELEASE)) {
                                    break
                                }
                            }

                        }
                        // TODO report
                        println("ЕСТЬ!")

                    } else {
                        continue
                    }
                }
            } else if (isKotlin(node.sourcePsi)) {
                val callExpressions = PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtCallExpression::class.java)
                for ((index, callExpression) in callExpressions.withIndex()) {
//                    val uCallExpression = callExpression as? UCallExpression ?: continue
                    val callName = callExpression.text
                    val argumentsCount = callExpression.valueArguments.size
//                    !methodCall.argumentList.isEmpty && (!callName.equals(ACQUIRE) || !callName.equals(RELEASE))
                    if ((argumentsCount > 0) ||
                        !callName.equals(ACQUIRE_KT) &&
                                !callName.equals(RELEASE_KT)) {
                        continue
                    }

                    val wl: KtNameReferenceExpression?
                    val possibleDotParent =
                        callExpression.parent as? KtDotQualifiedExpression
                    if (possibleDotParent != null) {
                        wl = PsiTreeUtil.findChildOfType(
                            possibleDotParent.toUElementOfType<UElement>()?.sourcePsi,
                            KtNameReferenceExpression::class.java
                        )
                    } else {
                        continue
                    }

                    val wlReference = wl.toUElementOfType<UReferenceExpression>()
                    val wlSource = wlReference?.resolve() ?: continue

                    // KtParameter - в первичном конструкторе и в методе
                    // KtProperty - локальные и класса

                    val wlLocalVariableSource = wlSource.toUElementOfType<ULocalVariable>()?.sourcePsi
                    val wlParameterSource = wlSource.toUElementOfType<UParameter>()?.sourcePsi
                    val wlMethodSource = wlSource.toUElementOfType<UMethod>()?.sourcePsi

                    if (wlLocalVariableSource != null || wlParameterSource != null || wlMethodSource != null) {
                        val isInClass: Boolean
                        val isInMethod: Boolean
                        val sourceName: String?
                        if (wlLocalVariableSource != null) {
                            sourceName = wlSource.toUElementOfType<ULocalVariable>()?.name
                            isInClass = PsiTreeUtil.isAncestor(
                                node.containingClass?.toUElementOfType<UClass>()?.sourcePsi,
                                wlLocalVariableSource,
                                true
                            )
                            isInMethod = PsiTreeUtil.isAncestor(
                                node.sourcePsi,
                                wlLocalVariableSource,
                                true
                            )
                        } else if (wlMethodSource != null) {
                            val uField = wlMethodSource.toUElementOfType<UField>()
                            sourceName = uField?.name
                            isInClass = PsiTreeUtil.isAncestor(
                                node.containingClass?.toUElementOfType<UClass>()?.sourcePsi,
                                wlMethodSource,
                                true
                            )
                            isInMethod = PsiTreeUtil.isAncestor(
                                node.sourcePsi,
                                wlMethodSource,
                                true
                            )
                        } else if (wlParameterSource != null) {
                            val uParameter = wlParameterSource.toUElementOfType<UParameter>()
                            sourceName = uParameter?.name
                            isInClass = PsiTreeUtil.isAncestor(
                                node.containingClass?.toUElementOfType<UClass>()?.sourcePsi,
                                wlParameterSource,
                                true
                            )
                            isInMethod = PsiTreeUtil.isAncestor(
                                node.sourcePsi,
                                wlParameterSource,
                                true
                            )
                        } else {
                            continue
                        }

                        var isReleased = false

                        // TODO inner method case
                        if (isInMethod) {

                            if (!callName.equals(ACQUIRE_KT)) {
                                continue
                            }

                            for ((indexInternal, callExpressionInternal) in callExpressions.withIndex()) {
                                if (indexInternal <= index) {
                                    continue
                                }
                                val callNameInternal = callExpressionInternal.text

                                val wlInternal: KtNameReferenceExpression?
                                val possibleDotParentInternal =
                                    callExpressionInternal.parent as? KtDotQualifiedExpression
                                if (possibleDotParentInternal != null) {
                                    wlInternal = PsiTreeUtil.findChildOfType(
                                        possibleDotParentInternal.toUElementOfType<UElement>()?.sourcePsi,
                                        KtNameReferenceExpression::class.java
                                    )
                                } else {
                                    continue
                                }

                                val wlReferenceInternal = wlInternal.toUElementOfType<UReferenceExpression>()
                                val wlSourceInternal = wlReferenceInternal?.resolve() ?: continue

                                val wlLocalVariableSourceInternal = wlSourceInternal.toUElementOfType<ULocalVariable>()?.sourcePsi
                                val wlParameterSourceInternal = wlSourceInternal.toUElementOfType<UParameter>()?.sourcePsi
                                val wlMethodSourceInternal = wlSourceInternal.toUElementOfType<UMethod>()?.sourcePsi

                                val refComparison: Boolean
                                val dataComparison: Boolean
                                if (wlLocalVariableSourceInternal != null) {
                                    refComparison = wlLocalVariableSource === wlLocalVariableSourceInternal
                                    dataComparison = wlLocalVariableSource == wlLocalVariableSourceInternal
                                } else if (wlMethodSourceInternal != null) {
                                    refComparison = wlMethodSource === wlMethodSourceInternal
                                    dataComparison = wlMethodSource == wlMethodSourceInternal
                                } else if (wlParameterSourceInternal != null) {
                                    refComparison = wlParameterSource === wlParameterSourceInternal
                                    dataComparison = wlParameterSource == wlParameterSourceInternal
                                } else {
                                    continue
                                }

                                if (refComparison || dataComparison) {
                                    if (callNameInternal.equals(RELEASE_KT)) {
                                        isReleased = true
                                        break
                                    }
                                }
                            }
                            if (isReleased) {
                                continue
                            }

                            // TODO report
                            println("ЕСТЬ!")
                        } else if (isInClass) {

                            val clazz = node.containingClass?.toUElementOfType<UClass>()?.qualifiedName ?: continue
                            val wlFieldStructure = wlFieldsMap[clazz]?.get(sourceName)
                                ?: continue
                            if (callName.equals(ACQUIRE_KT)) {
                                wlFieldStructure.isAcquired = true
                            } else if (callName.equals(RELEASE_KT)) {
                                wlFieldStructure.isReleased = true
                            }
                            continue

                        }

                    }
                }
            }

//            UCallExpression
//            PsiCallExpression
//            PsiMethodCallExpression
//            KtCallExpression
        }
    }

    companion object {
        @JvmField
        val ISSUE_DURABLE_WAKELOCK = Issue.create(
            "Durable Wakelock",
            "WakeLock won't be released.",
            "Release the WakeLock with release()",
            Category.PERFORMANCE, 5, Severity.WARNING,
            Implementation(DwDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val ACQUIRE = "acquire"
        private const val ACQUIRE_KT = "acquire()"
        private const val RELEASE = "release"
        private const val RELEASE_KT = "release()"
        private const val POWER_MANAGER_WAKELOCK = "android.os.PowerManager.WakeLock"
        private const val MY_DATA_STRUCTURE = "com.android.example.dw.WakelockTestJava.Wakelock"
        private const val MY_DATA_STRUCTURE_KT = "com.android.example.dw.Wakelock"
    }

}
