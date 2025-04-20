package com.github.qczone.switch2cursor.actions

import com.github.qczone.switch2cursor.settings.AppSettingsState
import com.github.qczone.switch2cursor.utils.WindowUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread

class OpenFileInCursorAction : AnAction() {
    private val logger = Logger.getInstance(OpenFileInCursorAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        
        val line = editor?.caretModel?.logicalPosition?.line?.plus(1) ?: 1
        val column = editor?.caretModel?.logicalPosition?.column?.plus(1) ?: 1
        
        val filePath = virtualFile.path
        val settings = AppSettingsState.getInstance()
        val cursorPath = settings.cursorPath
        
        // Command to open file and position cursor
        val fileCommand = when {
            System.getProperty("os.name").lowercase().contains("mac") -> {
                arrayOf("open", "-a", "$cursorPath", "cursor://file$filePath:$line:$column")
            }
            System.getProperty("os.name").lowercase().contains("windows") -> {
                arrayOf("cmd", "/c", "$cursorPath", "--goto", "$filePath:$line:$column")
            }
            else -> {
                arrayOf(cursorPath, "--goto", "$filePath:$line:$column")
            }
        }
        
        if (settings.openProjectWithFile) {
            // Command to open project
            val projectPath = project.basePath ?: return
            val projectCommand = when {
                System.getProperty("os.name").lowercase().contains("mac") -> {
                    arrayOf("open", "-a", "$cursorPath", projectPath)
                }
                System.getProperty("os.name").lowercase().contains("windows") -> {
                    arrayOf("cmd", "/c", "$cursorPath", projectPath)
                }
                else -> {
                    arrayOf(cursorPath, projectPath)
                }
            }
            
            try {
                logger.info("Executing project command: ${projectCommand.joinToString(" ")}")
                ProcessBuilder(*projectCommand).start()
                
                // Give some time for the project to open, then open the file and position the cursor
                Thread.sleep(1000)
                
                // Then open the file and position the cursor
                logger.info("Executing file command: ${fileCommand.joinToString(" ")}")
                ProcessBuilder(*fileCommand).start()
            } catch (ex: Exception) {
                logger.error("Failed to execute cursor command: ${ex.message}", ex)
                showErrorDialog(project, ex)
                return
            }
        } else {
            // Only open the file and position the cursor
            try {
                logger.info("Executing file command: ${fileCommand.joinToString(" ")}")
                ProcessBuilder(*fileCommand).start()
            } catch (ex: Exception) {
                logger.error("Failed to execute cursor command: ${ex.message}", ex)
                showErrorDialog(project, ex)
                return
            }
        }

        WindowUtils.activeWindow()
    }
    
    private fun showErrorDialog(project: Project, ex: Exception) {
        com.intellij.openapi.ui.Messages.showErrorDialog(
            project,
            """
            ${ex.message}
            
            Please check:
            1. Cursor path is correctly configured in Settings > Tools > Switch2Cursor
            2. Cursor is properly installed on your system
            3. The configured path points to a valid Cursor executable
            """.trimIndent(),
            "Error"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        e.presentation.isEnabledAndVisible = project != null && 
                                           virtualFile != null && 
                                           !virtualFile.isDirectory
    }
} 