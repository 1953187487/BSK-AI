package com.bskai

import android.app.Application
import android.content.Context
import com.bskai.agent.AgentEngine
import com.bskai.agent.Coordinator
import com.bskai.agent.slash.ClearCommand
import com.bskai.agent.slash.HelpCommand
import com.bskai.agent.slash.ModelPickCommand
import com.bskai.agent.slash.SlashRegistry
import com.bskai.agent.slash.WorkspaceToggleCommand
import com.bskai.agent.tools.ListFilesTool
import com.bskai.agent.tools.ReadFileTool
import com.bskai.agent.tools.RunShellTool
import com.bskai.agent.tools.ToolRegistry
import com.bskai.agent.tools.WriteFileTool
import com.bskai.data.SettingsRepository
import com.bskai.i18n.LocaleManager
import com.bskai.music.MusicEngine
import com.bskai.permission.ShizukuBridge
import com.bskai.terminal.TerminalEngine
import com.bskai.workspace.WorkspaceManager

class AuraApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var agent: AgentEngine
        private set
    lateinit var coordinator: Coordinator
        private set
    lateinit var shizuku: ShizukuBridge
        private set
    lateinit var terminal: TerminalEngine
        private set
    lateinit var workspace: WorkspaceManager
        private set
    lateinit var toolRegistry: ToolRegistry
        private set
    lateinit var slashRegistry: SlashRegistry
        private set
    lateinit var music: MusicEngine
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        applyLocale()
        shizuku = ShizukuBridge()
        terminal = TerminalEngine(shizuku)
        workspace = WorkspaceManager(this, settings)
        workspace.ensureDefault()
        music = MusicEngine(this)

        // 注册 AI 工具
        toolRegistry = ToolRegistry().apply {
            register(RunShellTool(terminal))
            register(ListFilesTool(workspace))
            register(ReadFileTool(workspace))
            register(WriteFileTool(workspace))
        }

        // 注册斜杠命令
        slashRegistry = SlashRegistry().apply {
            register(WorkspaceToggleCommand(
                isEnabled = { agent.workspaceEnabled },
                setEnabled = { agent.setWorkspaceEnabled(it) }
            ))
            register(ModelPickCommand(
                current = { settings.settings.value.apiModel },
                setModel = { model -> settings.update { it.copy(apiModel = model) } }
            ))
            register(ClearCommand(clear = { agent.clearConversation() }))
            register(HelpCommand(registry = this))
        }

        agent = AgentEngine(this, settings).also {
            it.workspace = workspace
            it.toolRegistry = toolRegistry
            it.slashRegistry = slashRegistry
        }
        coordinator = Coordinator(settings, agent)
    }

    fun applyLocale() {
        LocaleManager.apply(this, settings)
    }

    companion object {
        fun of(context: Context): AuraApp = context.applicationContext as AuraApp
    }
}
