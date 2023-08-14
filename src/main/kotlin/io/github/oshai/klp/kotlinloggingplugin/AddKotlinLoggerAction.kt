package io.github.oshai.klp.kotlinloggingplugin

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath


class AddKotlinLoggerAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Disable the action by default
        e.presentation.setEnabled(false)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (project == null || editor == null) {
            return
        }
        if (PsiDocumentManager.getInstance(project).getPsiFile(editor.document) !is KtFile) {
            return
        }
        // Enable the action only for Kotlin files
        e.presentation.setEnabled(true)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val logger = logger<AddLoggerAction>()
        logger.info("AddLoggerAction.actionPerformed")
        val editor = getEditor(event) ?: return
        val project: Project = event.project ?: return

        val caret = editor.caretModel
        val lineNumber = caret.logicalPosition.line

//        val templateManager = TemplateManager.getInstance(project)
        val templateText = "private val logger = KotlinLogging.logger {}"

        WriteCommandAction.runWriteCommandAction(project) {
//            val caret = editor.caretModel
//            val lineNumber = caret.logicalPosition.line

            val document = editor.document
            //document.insertString(document.getLineStartOffset(lineNumber), "$templateText\n")

            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? KtFile ?: return@runWriteCommandAction
            val ktPsiFactory = KtPsiFactory(psiFile)
            val importDirective = ktPsiFactory.createImportDirective(ImportPath(FqName("io.github.oshai.kotlinlogging.KotlinLogging"), isAllUnder = false))
            psiFile.importList?.add(importDirective)
            // Move the caret to the end of the inserted line
//            caret.moveToLogicalPosition(LogicalPosition(lineNumber, templateText.length))

            val classDeclaration = psiFile.declarations.firstOrNull { it is KtClass }
            val methodDeclaration = psiFile.declarations.firstOrNull { it is KtFunction }
            val propertyDeclaration = psiFile.declarations.firstOrNull { it is KtProperty }
            // Find the last import in the file
            val lastImport = psiFile.importList?.imports?.lastOrNull()
            val property = ktPsiFactory.createProperty(templateText)

            classDeclaration?.let {
                logger.info("AddLoggerAction - add before class")
                psiFile.addBefore(property, it)
            } ?:
            methodDeclaration?.let {
                logger.info("AddLoggerAction - add before class")
                psiFile.addBefore(property, it)
            } ?:
            propertyDeclaration?.let {
                logger.info("AddLoggerAction - add before class")
                psiFile.addBefore(property, it)
            } ?:
            // Insert the KLogger field after the last import
            lastImport?.let {
                logger.info("AddLoggerAction - add after imports")
                psiFile.addAfter(property, lastImport)
            } ?:
             run {
                logger.info("AddLoggerAction - add in caret")
                document.insertString(document.getLineStartOffset(lineNumber), "$templateText\n")
                // Move the caret to the end of the inserted line
                caret.moveToLogicalPosition(LogicalPosition(lineNumber + 1, templateText.length))
            }

            // Reformat the inserted lines
            psiFile.node?.let {
                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(psiFile)
                CodeStyleManager.getInstance(project).reformat(it.psi)
            }
        }

        logger.info("AddLoggerAction.done")
    }

    private fun getEditor(event: AnActionEvent): Editor? {
        return event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
    }
}
