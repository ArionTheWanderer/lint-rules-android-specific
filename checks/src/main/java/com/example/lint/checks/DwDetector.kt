package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*

class DwDetector : Detector(), Detector.UastScanner {

    data class MutablePair(var acquireInvocation: UCallExpression? = null, var releaseInvocation: UCallExpression? = null)

    // stores data for an analyze, then releases
    private val wlFieldsMap: MutableMap<String, MutableMap<String, MutableMap<UMethod, MutablePair>>> = mutableMapOf()


    override fun afterCheckFile(context: Context) {
        for ((_, map) in wlFieldsMap.entries) {
            for ((_, methodInvocationsMap) in map.entries) {
                for ((method, acquireReleasePair) in methodInvocationsMap.entries) {
                    val acquireInvocation = acquireReleasePair.acquireInvocation
                    if (acquireInvocation != null && acquireReleasePair.releaseInvocation == null) {
                        reportUsage(context, acquireInvocation, method)
                    }
                }
            }
        }
    }

    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitInitializer(node: UClassInitializer) {

        }

        override fun visitClass(node: UClass) {
            val fields = node.fields
            for (field in fields) {
                if (field.type.equalsToText(POWER_MANAGER_WAKELOCK)) {
                    val wlClass = node.qualifiedName ?: continue
                    val wlFieldName = field.name

                    if (wlFieldsMap[wlClass] == null) {
                        wlFieldsMap[wlClass] = mutableMapOf()
                    }
                    wlFieldsMap[wlClass]?.set(wlFieldName, mutableMapOf())
                }
            }
        }

        override fun visitMethod(node: UMethod) {
//            KtClassBody
//            KtBlockExpression
            if (isJava(node.sourcePsi)) {
                val methodCalls = PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiMethodCallExpression::class.java)
                for ((index, methodCall) in methodCalls.withIndex()) {
                    val parentBlock = PsiTreeUtil.getParentOfType(methodCall, PsiCodeBlock::class.java)
                    val lBrace = parentBlock?.lBrace
                    val rBrace = parentBlock?.rBrace

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

                        if (!wlField.type.equalsToText(POWER_MANAGER_WAKELOCK)) {
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
                            ?: continue
                        if (wlFieldStructure[node] == null) {
                            wlFieldStructure[node] = MutablePair()
                        }
                        if (callName.equals(ACQUIRE)) {
                            wlFieldStructure[node]?.acquireInvocation = methodCall.toUElementOfType()
                        } else if (callName.equals(RELEASE)) {
                            wlFieldStructure[node]?.releaseInvocation = methodCall.toUElementOfType()
                        }
                        continue

                    } else if (wlLocalVariable != null) {

                        if (!callName.equals(ACQUIRE)) {
                            continue
                        }

                        if (!wlLocalVariable.type.equalsToText(POWER_MANAGER_WAKELOCK)) {
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

                        var isReleased = false

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
                                    isReleased = true
                                    break
                                }
                            }

                        }
                        if (isReleased) {
                            continue
                        }
                        methodCall.toUElementOfType<UCallExpression>()
                            ?.let { reportUsage(context, it, node) }
                    } else if (wlParameter != null) {

                        if (!callName.equals(ACQUIRE)) {
                            continue
                        }

                        if (!wlParameter.type.equalsToText(POWER_MANAGER_WAKELOCK)) {
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

                        var isReleased = false

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
                                    isReleased = true
                                    break
                                }
                            }

                        }
                        if (isReleased) {
                            continue
                        }
                        methodCall.toUElementOfType<UCallExpression>()
                            ?.let { reportUsage(context, it, node) }
                    } else {
                        continue
                    }
                }
            } else if (isKotlin(node.sourcePsi)) {
                val callExpressions = PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtCallExpression::class.java)
                for ((index, callExpression) in callExpressions.withIndex()) {
                    val parentBlock = PsiTreeUtil.getParentOfType(callExpression, KtBlockExpression::class.java)
                    val lBrace = parentBlock?.lBrace
                    val rBrace = parentBlock?.rBrace

                    val callName = callExpression.text
                    val argumentsCount = callExpression.valueArguments.size
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

                            callExpression.toUElementOfType<UCallExpression>()
                                ?.let { reportUsage(context, it, node) }
                        } else if (isInClass) {
                            val clazz = node.containingClass?.toUElementOfType<UClass>()?.qualifiedName ?: continue
                            val wlFieldStructure = wlFieldsMap[clazz]?.get(sourceName)
                                ?: continue

                            if (wlFieldStructure[node] == null) {
                                wlFieldStructure[node] = MutablePair()
                            }
                            if (callName.equals(ACQUIRE_KT)) {
                                wlFieldStructure[node]?.acquireInvocation = callExpression.toUElementOfType()
                            } else if (callName.equals(RELEASE_KT)) {
                                wlFieldStructure[node]?.releaseInvocation = callExpression.toUElementOfType()
                            }
                            continue
                        }

                    }
                }
            }
        }
    }

    private fun reportUsage(context: Context, acquireInvocation: UCallExpression, node: UMethod) {
        val receiverText = acquireInvocation.receiver?.sourcePsi?.text ?: ""
        val acquireInvocationText: String? = if (receiverText != "") {
            "$receiverText.${acquireInvocation.methodName}"
        } else {
            acquireInvocation.methodName
        }


        val newAcquireInvocationText = "$acquireInvocationText(10*60*1000L)"

        val durableWakelockFix = fix()
            .name("Set timeout to acquire invocation in ${node.name} method")
            .family("Set timeout to acquire invocation")
            .replace()
            .range(context.getLocation(acquireInvocation))
            .all()
            .with(newAcquireInvocationText)
            .reformat(true)
            .autoFix()
            .build()


        val incident = Incident(context, ISSUE_DURABLE_WAKELOCK)
            .message("Release the WakeLock in 10 minutes.")
            .at(acquireInvocation)
            .fix(durableWakelockFix)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_DURABLE_WAKELOCK = Issue.create(
            "Durable Wakelock",
            "WakeLock won't be released.",
            "Set timeout to acquire() invocation",
            ConstantHolder.ANDROID_QUALITY_SMELLS, 5, Severity.WARNING,
            Implementation(DwDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        private const val ACQUIRE = "acquire"
        private const val ACQUIRE_KT = "acquire()"
        private const val RELEASE = "release"
        private const val RELEASE_KT = "release()"
        private const val POWER_MANAGER_WAKELOCK = "android.os.PowerManager.WakeLock"
    }

}
