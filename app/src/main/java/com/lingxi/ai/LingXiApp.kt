package com.lingxi.ai

import android.app.Application
import android.content.Context
import com.lingxi.ai.agent.AgentEngine
import com.lingxi.ai.agent.Coordinator
import com.lingxi.ai.agent.VideoGenCoordinator
import com.lingxi.ai.agent.slash.ClearCommand
import com.lingxi.ai.agent.slash.CompactCommand
import com.lingxi.ai.agent.slash.HelpCommand
import com.lingxi.ai.agent.slash.HistoryCommand
import com.lingxi.ai.agent.slash.ModelPickCommand
import com.lingxi.ai.agent.slash.SlashRegistry
import com.lingxi.ai.agent.slash.WorkspaceToggleCommand
import com.lingxi.ai.agent.tools.ListFilesTool
import com.lingxi.ai.agent.tools.ReadFileTool
import com.lingxi.ai.agent.tools.RunShellTool
import com.lingxi.ai.agent.tools.SystemInfoTool
import com.lingxi.ai.agent.tools.ToolRegistry
import com.lingxi.ai.agent.tools.DateTimeTool
import com.lingxi.ai.agent.tools.WriteFileTool
import com.lingxi.ai.data.SettingsRepository
import com.lingxi.ai.i18n.LocaleManager
import com.lingxi.ai.music.MusicEngine
import com.lingxi.ai.permission.DhizukuBridge
import com.lingxi.ai.permission.ShizukuBridge
import com.lingxi.ai.terminal.TerminalEngine
import com.lingxi.ai.workspace.WorkspaceManager

class LingXiApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var agent: AgentEngine
        private set
    lateinit var coordinator: Coordinator
        private set
    lateinit var videoGen: VideoGenCoordinator
        private set
    lateinit var shizuku: ShizukuBridge
        private set
    lateinit var dhizuku: DhizukuBridge
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
        dhizuku = DhizukuBridge()
        // Dhizuku.init performs a synchronous provider call, keep it off the main thread
        Thread {
            dhizuku.init(this)
        }.apply { isDaemon = true; start() }
        terminal = TerminalEngine(shizuku, dhizuku)
        workspace = WorkspaceManager(this, settings)
        workspace.ensureDefault()
        music = MusicEngine(this)

        // 注册 AI 工具
        toolRegistry = ToolRegistry().apply {
            register(RunShellTool(terminal))
            register(ListFilesTool(workspace))
            register(ReadFileTool(workspace))
            register(WriteFileTool(workspace))
            register(SystemInfoTool(this@LingXiApp))
            register(DateTimeTool())
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
        videoGen = VideoGenCoordinator(this, settings, agent)

        // /video 斜杠命令（依赖 videoGen，注册在 slashRegistry 上供 UI 使用）
        slashRegistry.register(
            com.lingxi.ai.agent.slash.VideoGenCommand(
                generate = { prompt -> videoGen.generateVideo(prompt) }
            )
        )

        // /compact 与 /history（依赖 agent，注册在 slashRegistry 上供 UI 菜单显示）
        slashRegistry.register(CompactCommand { agent.compactConversation() })
        slashRegistry.register(HistoryCommand { agent.historySummary() })
    }

    fun applyLocale() {
        LocaleManager.apply(this, settings)
    }

    companion object {
        fun of(context: Context): LingXiApp = context.applicationContext as LingXiApp
    }
}
