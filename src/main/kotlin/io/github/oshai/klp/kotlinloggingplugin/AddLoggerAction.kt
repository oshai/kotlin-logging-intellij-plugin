package io.github.oshai.klp.kotlinloggingplugin


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset


class AddLoggerAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Disable the action by default
        e.presentation.setEnabled(false)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (project == null || editor == null) {
            return
        }
        if (!isJavaFile(project, editor)) {
            return
        }
        // Enable the action only if it's a Java file (has .java extension)
        e.presentation.setEnabled(true)
    }

    private fun isJavaFile(        project: Project,        editor: Editor    ): Boolean {
        return PsiDocumentManager.getInstance(project).getPsiFile(editor.document) is PsiJavaFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (project == null || editor == null) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val document: com.intellij.openapi.editor.Document = editor.document
            val caret = editor.caretModel
            val lineNumber = caret.visualPosition.line
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? PsiJavaFile ?: return@runWriteCommandAction
            val element: PsiElement? = psiFile.findElementAt(lineNumber)
            val classes = psiFile.classes
            if (classes.isEmpty()) {
                return@runWriteCommandAction
            }
            val psiClass = classes[0]
            val factory = JavaPsiFacade.getElementFactory(project)
            val psiNew: PsiElement = addLoggerField(factory, psiClass)
            // Add import if needed
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass, psiNew.startOffset, psiNew.endOffset)
            logger<AddLoggerAction>().info("element: $element - ${element?.parent}")
            document.insertString(caret.offset, "logger.info(\"\");")
            caret.moveCaretRelatively(13, 0, false, false, false)
        }
    }

    private fun addLoggerField(
        factory: PsiElementFactory,
        psiClass: PsiClass
    ): PsiElement {
        val loggerClassName = "org.apache.logging.log4j.Logger"
        val loggerFieldName = "logger"
        val loggerField = factory.createField(loggerFieldName, factory.createTypeFromText(loggerClassName, null))
        loggerField.modifierList!!.setModifierProperty(PsiModifier.PRIVATE, true)
        loggerField.modifierList!!.setModifierProperty(PsiModifier.STATIC, true)
        loggerField.modifierList!!.setModifierProperty(PsiModifier.FINAL, true)
        val managerClass = "org.apache.logging.log4j.LogManager"
        val loggerInitializer = "$managerClass.getLogger()"
        val initializer: PsiExpression = factory.createExpressionFromText(loggerInitializer, null)
        loggerField.initializer = initializer
        val methods = psiClass.methods
        val fields = psiClass.fields
        val psiNew: PsiElement = when {
            methods.isNotEmpty() -> {
                psiClass.addBefore(loggerField, methods[0])
            }
            fields.isNotEmpty() -> {
                psiClass.addBefore(loggerField, fields[0])
            }
            else -> {
                psiClass.add(loggerField)
            }
        }
        return psiNew
    }


}
