package com.example.lint.checks

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Category.Companion.PERFORMANCE
import com.android.tools.lint.detector.api.Severity.WARNING
import com.intellij.psi.*


import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import java.lang.IllegalArgumentException
import java.util.stream.Collectors

class MimDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitMethod(node: UMethod) {
            val evaluator = context.evaluator

//            val importList = PsiTreeUtil.findChildrenOfType(node.containingFile.toUElementOfType<UFile>()?.sourcePsi, KtImportList::class.java).elementAt(0)
//            val importStartOffset = importList.startOffset
//            val importEndOffset = importList.endOffset
//            val importStartOffsetInParent = importList.startOffsetInParent
//            val importStartOffsetInAncestor = importList.getStartOffsetIn(node.containingFile)
//
//            val classStartOffset = node.containingClass?.startOffset
//            val classEndOffset = node.containingClass?.endOffset
//            val classStartOffsetInParent = node.containingClass?.startOffsetInParent
//            val classOffsetInAncestor = node.containingClass?.getStartOffsetIn(node.containingFile)

            val containingClass = node.javaPsi.containingClass
            val allFieldsNames =
                containingClass?.allFields?.toList()?.stream()?.map { field -> field.name }
                    ?.collect(Collectors.toList()) ?: listOf()
            val allMethodsNames =
                containingClass?.allMethods?.toList()?.stream()?.map { method -> method.name }
                    ?.collect(Collectors.toList()) ?: listOf()

            val isNotConstructor = !node.isConstructor
            val statementsCount = (node.uastBody as? UBlockExpression)?.expressions?.size ?: 0
            val hasNonEmptyBody = node.uastBody != null && statementsCount > 0
            val isNonStatic = !node.isStatic
            val isNotOverride = !evaluator.isOverride(node, true)
            val hasNotInternalMemberInvocation =
                hasNotInternalMemberInvocation(evaluator, node, allFieldsNames, allMethodsNames)
            val doesNotUseThisExpression = doesNotUseThisExpression(node)
            val doesNotUseSuperExpression = doesNotUseSuperExpression(node)

//            LintFix
            if (isNotConstructor && hasNonEmptyBody && isNonStatic && isNotOverride
                && hasNotInternalMemberInvocation && doesNotUseThisExpression && doesNotUseSuperExpression
            ) {
                val debugMessage = """
                    isNotConstructor = $isNotConstructor,
                    hasNonEmptyBody = $hasNonEmptyBody,
                    isNonStatic = $isNonStatic,
                    isNotOverride = $isNotOverride,
                    hasNotInternalMemberInvocation = $hasNotInternalMemberInvocation,
                    doesNotUseThisExpression = $doesNotUseThisExpression,
                    doesNotUseSuperExpression = $doesNotUseSuperExpression
                """.trimIndent()
                reportUsage(context, node)
            }
        }

        // checks if a method has a reference to internal members (including ancestors methods)
        private fun hasNotInternalMemberInvocation(
            evaluator: JavaEvaluator,
            node: UMethod,
            allFieldsNames: List<String>,
            allMethodsNames: List<String>
        ): Boolean {

            fun isInInheritedClass(element: PsiElement?): Boolean {
                val resolvedUMethod = element.toUElementOfType<UMethod>()
                val resolvedUField = element.toUElementOfType<UField>()
                val resolvedElementsClass: PsiClass? = if (resolvedUMethod != null) {
                    resolvedUMethod.containingClass
                } else if (resolvedUField != null) {
                    resolvedUField.containingClass
                } else {
                    return false
                }
                return evaluator.inheritsFrom(
                    node.containingClass,
                    resolvedElementsClass?.qualifiedName ?: "",
                    false
                )
            }

            if (isJava(node.sourcePsi)) {
                val refExpressions =
                    PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiReferenceExpression::class.java)
                for (refExpression in refExpressions) {
                    val referenceName = refExpression.referenceName ?: ""
                    val resolvedPsiElement = refExpression.resolve() ?: continue
                    if (!(referenceName in allFieldsNames || referenceName in allMethodsNames)) {
                        continue
                    }
                    val resolvedMethod = resolvedPsiElement.toUElementOfType<UMethod>()
                    if (resolvedMethod != null && resolvedMethod.isStatic) {
                        continue
                    }
                    val isInClass =
                        PsiTreeUtil.isAncestor(node.containingClass, resolvedPsiElement, true)
                    if (isInClass || isInInheritedClass(resolvedPsiElement)) {
                        var nextReference: PsiReferenceExpression = refExpression
                        var flag = true
                        while (flag) {
                            val possibleNextReference =
                                nextReference.firstChild as? PsiReferenceExpression
                            if (possibleNextReference != null) {
                                nextReference = possibleNextReference
                            } else {
                                flag = false
                            }
                        }
                        val nextResolvedElement = nextReference.resolve()
                        if (nextResolvedElement != null) {
                            val isInMethod =
                                PsiTreeUtil.isAncestor(node.javaPsi, nextResolvedElement, true)
                            if (!isInMethod) {
                                return false
                            }
                        }
                    }
                }
            } else if (isKotlin(node.sourcePsi)) {
                val refExpressions = PsiTreeUtil.findChildrenOfType(
                    node.sourcePsi,
                    KtNameReferenceExpression::class.java
                )
                for (refExpression in refExpressions) {
                    val referenceName = refExpression.getReferencedName()
                    val resolvedPsiElement =
                        refExpression.toUElementOfType<UReferenceExpression>()?.resolve()
                            ?: continue
                    if (!(referenceName in allFieldsNames || referenceName in allMethodsNames)) {
                        continue
                    }
                    val resolvedMethod = resolvedPsiElement.toUElementOfType<UMethod>()
                    if (resolvedMethod != null && resolvedMethod.isStatic) {
                        continue
                    }
                    val isInClass =
                        PsiTreeUtil.isAncestor(node.containingClass, resolvedPsiElement, true)
                    if (isInClass || isInInheritedClass(resolvedPsiElement)) {
                        var parentReference = PsiTreeUtil.getParentOfType(
                            refExpression.toUElementOfType<UExpression>()?.sourcePsi,
                            KtDotQualifiedExpression::class.java
                        )
                        if (parentReference != null) {
                            var flag = true
                            while (flag) {
                                val possibleNextReference = PsiTreeUtil.findChildOfType(
                                    parentReference.toUElementOfType<UElement>()?.sourcePsi,
                                    KtDotQualifiedExpression::class.java
                                )
                                if (possibleNextReference != null) {
                                    parentReference = possibleNextReference
                                } else {
                                    flag = false
                                }
                            }
                            val possibleParam = PsiTreeUtil.findChildOfType(
                                parentReference.toUElementOfType<UElement>()?.sourcePsi,
                                KtNameReferenceExpression::class.java
                            )
                            val possibleParamReferenceName = possibleParam?.getReferencedName()
                            val resolvedPossibleParamPsiElement =
                                possibleParam.toUElementOfType<UReferenceExpression>()?.resolve()
                            if (resolvedPossibleParamPsiElement != null) {
                                val isInMethod = PsiTreeUtil.isAncestor(
                                    node.javaPsi,
                                    resolvedPossibleParamPsiElement,
                                    true
                                )
                                if (!isInMethod) {
                                    return false
                                }
                            }
                        } else {
                            val isInMethod =
                                PsiTreeUtil.isAncestor(node.javaPsi, resolvedPsiElement, true)
                            if (!isInMethod) {
                                return false
                            }
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Method can only analyze Java or Kotlin psi elements")
            }
            return true

        }

        // checks if a method uses 'this' expression
        private fun doesNotUseThisExpression(node: UMethod): Boolean {
            val sourcePsi = node.sourcePsi
            if (isJava(sourcePsi)) {
                val expressions =
                    PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiExpression::class.java)
                for (expression in expressions) {
                    val thisExpression = expression.toUElementOfType<UThisExpression>()
                    if (thisExpression != null) {
                        return false
                    }
                }
            } else if (isKotlin(sourcePsi)) {
                val thisExpressions =
                    PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtThisExpression::class.java)
                if (thisExpressions.isNotEmpty()) {
                    return false
                }
            }
            return true
        }

        // checks if a method uses 'super' expression
        private fun doesNotUseSuperExpression(node: UMethod): Boolean {
            val sourcePsi = node.sourcePsi
            if (isJava(sourcePsi)) {
                val expressions =
                    PsiTreeUtil.findChildrenOfType(node.javaPsi, PsiExpression::class.java)
                for (expression in expressions) {
                    val superExpression = expression.toUElementOfType<USuperExpression>()
                    if (superExpression != null) {
                        return false
                    }
                }
            } else if (isKotlin(sourcePsi)) {
                val superExpressions =
                    PsiTreeUtil.findChildrenOfType(node.sourcePsi, KtSuperExpression::class.java)
                if (superExpressions.isNotEmpty()) {
                    return false
                }
            }
            return true
        }

    }

    private fun reportUsage(context: JavaContext, node: UMethod) {
        val lintFix: LintFix
        if (isJava(node.sourcePsi)) {
            lintFix =
                fix()
                    .name("static modifier for ${node.name}")
                    .family("static modifier for method")
                    .replace()
                    .range(context.getLocation(node.modifierList))
                    .end()
                    .with(" static")
                    .autoFix()
                    .reformat(true)
                    .build()
        } else if (isKotlin(node.sourcePsi)) {
//            val importList = PsiTreeUtil.findChildrenOfType(node.containingFile.toUElementOfType<UFile>()?.sourcePsi, KtImportList::class.java).elementAt(0)
//            val startOffset = importList.startOffsetInParent
//            val dsd = importList.startOffset
            // (node.containingFile) + 1
//            context.getRangeLocation()

            lintFix =
                fix()
                    .name("static modifier for ${node.name}")
                    .family("static modifier for method")
                    .composite(
                        fix()
                            .replace()
                            .range(context.getLocation(node))
                            .all()
                            .with(null)
                            .reformat(true)
                            .autoFix()
                            .build(),
                        fix()
                            .replace()
                            .range(context.getLocation(node))
                            .end()
                            .with(" ${node.sourcePsi?.text}")
                            .reformat(true)
                            .autoFix()
                            .build()
                    )
                    .autoFix()
        } else {
            throw IllegalArgumentException("Method can only analyze Java or Kotlin psi elements")
        }
        val incident = Incident(context, ISSUE_MEMBER_IGNORING_METHOD)
            .message("[3]${node.name} method should have 'static' modifier")
            .at(node)
            .fix(lintFix)
        context.report(incident)
    }

    private fun reportUsage(context: JavaContext, node: UMethod, debugMessage: String) {
        val incident = Incident(context, ISSUE_MEMBER_IGNORING_METHOD)
            .message(debugMessage)
            .at(node)
        context.report(incident)
    }

    companion object {
        @JvmField
        val ISSUE_MEMBER_IGNORING_METHOD = Issue.create(
            "Mim",
            "Non-static method that doesn't access any property.",
            "Make the method static.",
            PERFORMANCE, 5, WARNING,
            Implementation(MimDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

}