package com.ai.assistance.operit.ui.features.help.content

import com.ai.assistance.operit.ui.features.help.HelpEntry

/**
 * Element-level descriptions used by the in-place help mode.
 *
 * The screen-level text lives in [HelpTopics]; this catalog only answers
 * "what does this control do?" for the anchors placed on interactive
 * elements. Keeping it separate means a screen can ship its general guide
 * before every control has an anchor, and anchors can be added without
 * touching the long-form guide.
 */
object HelpModeCatalog {
    private fun e(id: String, title: String, body: String) = HelpEntry(id, title, body)

    private val screens: Map<String, Map<String, HelpEntry>> = mapOf(
        "aichat" to listOf(
            e(
                "history",
                "Chat history",
                "Opens the list of conversations. Search, pin, lock, rename, reorder or delete chats there. The same panel shows running background tasks."
            ),
            e(
                "character",
                "Character",
                "Opens the character or group selector. The active character supplies the role, personality, greeting and often the model binding for this chat."
            ),
            e(
                "pip",
                "Floating window",
                "Moves this chat into a small window that stays above other apps. The same button closes the floating window when it is already open."
            ),
            e(
                "composer",
                "Message field",
                "Type the message here. Use @ to mention a character or package. The expand button opens a larger editor when the text does not fit."
            ),
            e(
                "attach",
                "Attach",
                "Adds a file, photo, screenshot, notification, location, memory folder, package, skill or MCP server to the next message."
            ),
            e(
                "mic",
                "Dictate",
                "Starts voice input. The text is transcribed by the speech service selected in Settings, then placed in the message field."
            ),
            e(
                "send",
                "Send / Stop",
                "Sends the draft. While the assistant is answering, the same button stops generation. With an empty field it starts voice input; when queue mode is active it stores the draft instead."
            ),
            e(
                "queue",
                "Queue",
                "Stores the current draft for later instead of sending it. Open the queue to send, edit or delete queued messages."
            ),
            e(
                "model",
                "Model selection",
                "Rebinds the model and configuration profile used by this chat. A character card can lock the model and ask for confirmation before a change."
            ),
            e(
                "toggles",
                "Chat switches",
                "Thinking effort, maximum context, automatic memory updates, read-aloud, tool access, streaming and tool permission policy for this conversation."
            ),
            e(
                "more",
                "More options",
                "Opens memory, model, tools, behavior and plugin controls, including context length, summary threshold, tool order and shortcuts to the related settings."
            ),
            e(
                "workspace",
                "Workspace",
                "Opens the project workspace attached to this chat. The assistant can read and edit files there."
            ),
            e(
                "export",
                "Export",
                "Exports the conversation or the project for Android or Windows, with a progress dialog and an option to open or install the result."
            ),
            e(
                "computer",
                "AI computer",
                "Opens the sandboxed terminal that the assistant can drive when a task needs commands or files."
            ),
        ).associateBy { it.id },
        "memory" to listOf(
            e(
                "memory.folders",
                "Folders",
                "Toggles the folder panel. Select a folder to filter the graph, or create, rename or delete folders there."
            ),
            e(
                "memory.search",
                "Search memories",
                "Type a query and press Enter to run vector search over the current memory profile. The result highlights matching nodes."
            ),
            e(
                "memory.search_settings",
                "Search settings",
                "Opens vector search and embedding settings: save options, rebuild the search index after imports, and simulate a search."
            ),
            e(
                "memory.delete_selected",
                "Delete selected",
                "Appears in box-selection mode after nodes are selected. Opens a confirmation before deleting them."
            ),
            e(
                "memory.select",
                "Box selection",
                "Turns box selection on or off. While on, drag across the graph to select several nodes; Delete selected then removes them."
            ),
            e(
                "memory.link",
                "Link mode",
                "Turns link mode on or off. Pick two nodes, then set the link type, weight and description."
            ),
            e(
                "memory.import",
                "Import document",
                "Opens the document picker. Text, PDF, DOC and DOCX files are read and split into memory nodes."
            ),
            e(
                "memory.create",
                "Create memory",
                "Opens the new-memory dialog with title, content, type, source, credibility, importance, folder and tags."
            ),
        ).associateBy { it.id },
        "packages" to listOf(
            e(
                "packages.tab.plugins",
                "Plugins tab",
                "Shows installable tool packages. Each card can be enabled, opened for details, or replaced by a newer version."
            ),
            e(
                "packages.tab.packages",
                "Packages tab",
                "Shows script packages that add tools written in JavaScript or TypeScript. The same tab imports and updates them."
            ),
            e(
                "packages.tab.skills",
                "Skills tab",
                "Shows skill definitions that give the assistant reusable instructions and supporting resources."
            ),
            e(
                "packages.tab.mcp",
                "MCP tab",
                "Shows Model Context Protocol servers. Each entry lists its tools and whether the assistant can currently call them."
            ),
            e(
                "packages.errors",
                "Load errors",
                "Appears only when a package failed to load. Opens the failing package names and their error messages."
            ),
            e(
                "packages.env",
                "Environment variables",
                "Edits the environment values required by the imported plugins and packages. Empty optional values are allowed."
            ),
            e(
                "packages.market",
                "Artifact market",
                "Opens the artifact market to browse and import published plugins, packages, skills and MCP presets."
            ),
            e(
                "packages.import",
                "Import",
                "Opens the file picker for the current tab. Plugins and packages must match the selected tab's format."
            ),
        ).associateBy { it.id },
        "workflow" to listOf(
            e(
                "workflow.create_menu",
                "Create workflow",
                "Expands the create menu: a blank workflow, a template, or multi-select. The plus turns into a close icon while the menu is open."
            ),
            e(
                "workflow.create_blank",
                "Blank workflow",
                "Opens the editor with an empty trigger and step list. Add steps, variables and tools there."
            ),
            e(
                "workflow.create_template",
                "From template",
                "Opens the template picker. Templates are supplied by installed tool packages."
            ),
            e(
                "workflow.multi_select",
                "Multi-select",
                "Enables selection mode so several workflow cards can be deleted together."
            ),
            e(
                "workflow.delete_selected",
                "Delete selected",
                "Deletes the selected workflows after confirmation. It appears only in selection mode with at least one card selected."
            ),
            e(
                "workflow.exit_select",
                "Exit selection",
                "Clears the selection and leaves multi-select mode."
            ),
        ).associateBy { it.id },
        "toolbox" to listOf(
            e(
                "toolbox.tool_tester",
                "Tool Test Center",
                "Runs each built-in AI tool against small fixtures and shows pass or fail with the response. Opening the screen seeds the fixtures again, so a deleted test folder is restored."
            ),
            e(
                "toolbox.file_manager",
                "File Manager",
                "Browses device storage, opens and edits files, and supports multi-select, copy, move, rename, delete, archive and sharing. Root mode is available when the permission level allows it."
            ),
            e(
                "toolbox.text_to_speech",
                "Text-to-Speech",
                "Converts typed text with the selected local or cloud voice, adjustable speed and pitch. Templates and the voice test live here."
            ),
            e(
                "toolbox.speech_to_text",
                "Speech Recognition",
                "Records or imports audio and transcribes it with the selected engine. Choose the engine and language, download its model once, then use Switch Engine to change without restarting the app."
            ),
            e(
                "toolbox.app_permissions",
                "App Permission Manager",
                "Lists installed apps and their runtime and special permissions. Grant or revoke them here when a tool needs access the system dialog did not offer."
            ),
            e(
                "toolbox.default_assistant_guide",
                "Default Assistant Setup",
                "Sets Operit as the system digital assistant so the voice button or long-press gesture opens Operit instead of the stock assistant."
            ),
            e(
                "toolbox.terminal",
                "Command Terminal",
                "Opens the sandboxed Linux terminal. Environment setup runs once and installs Node.js and Python; afterwards the assistant and you can run commands and edit files there."
            ),
            e(
                "toolbox.ui_debugger",
                "UI Debug Tool",
                "Inspects the current screen hierarchy, element bounds and accessibility text, and can target elements for automation tests."
            ),
            e(
                "toolbox.ffmpeg_toolbox",
                "FFmpeg Toolbox",
                "Converts, trims, extracts audio from and inspects media files with FFmpeg presets and a custom argument field."
            ),
            e(
                "toolbox.shell_executor",
                "Command Executor",
                "Runs shell commands outside the terminal and shows stdout, stderr and the exit code. Common command snippets are provided."
            ),
            e(
                "toolbox.logcat",
                "Log Viewer",
                "Shows live logcat output with level, tag and package filters, search, pause and export."
            ),
            e(
                "toolbox.sql_viewer",
                "SQL Viewer",
                "Browses the app database tables and runs SQL. Writing statements change real data, so back up first and review the statement before running it."
            ),
            e(
                "toolbox.token_config",
                "Get API Key",
                "Opens the DeepSeek key helper. A key is optional: other providers, endpoints and models are configured in Model Configuration or during onboarding."
            ),
            e(
                "toolbox.process_limit_remover",
                "Phantom Process Limit Remover",
                "Raises the Android 12+ phantom process limit so background terminals and servers are not killed. It needs root or shell access."
            ),
            e(
                "toolbox.html_packager",
                "HTML Packager",
                "Bundles an HTML page and its local assets into one portable file for sharing or opening offline."
            ),
            e(
                "toolbox.auto_glm_tool",
                "AutoGLM Execution",
                "Runs the autoglm-phone agent for step-by-step mobile automation. It needs a configured model and accessibility or root control."
            ),
        ).associateBy { it.id },
        "workspace" to listOf(
            e(
                "workspace.tabs",
                "Workspace tabs",
                "The Preview tab shows the rendered workspace page. Each opened file gets its own tab; tap to switch and use the x on a tab to close it. Unsaved tabs show a dot."
            ),
            e(
                "workspace.save",
                "Save file",
                "Appears when the active file has unsaved changes. It writes the editor buffer to disk; closing an unsaved tab asks for confirmation first."
            ),
            e(
                "workspace.preview",
                "Preview / edit",
                "For HTML and Markdown files, switches between the rendered preview and the source editor. Preview reloads the workspace server page."
            ),
        ).associateBy { it.id },
        "settings" to listOf(
            e(
                "settings.account.github",
                "GitHub account",
                "Links the app to GitHub for marketplace publishing, gists and backup features. Logging out removes the stored token; local data stays."
            ),
            e(
                "settings.personal.profile",
                "User profile",
                "Sets your name, avatar and personal details used by characters and prompts."
            ),
            e(
                "settings.personal.language",
                "Language",
                "Chooses the app interface language. The fork ships English; changing it affects labels and help text, not the models."
            ),
            e(
                "settings.personal.theme",
                "Theme and appearance",
                "Controls colors, dark mode, fonts, chat bubbles, background image and other visual options."
            ),
            e(
                "settings.personal.display",
                "Display and behavior",
                "Global display and behavior switches that do not belong to chat, such as animations, density and status elements."
            ),
            e(
                "settings.personal.layout",
                "Layout adjustment",
                "Moves or resizes UI elements and changes margins when a control is clipped or overlaps on a small screen."
            ),
            e(
                "settings.ai.model",
                "Model and parameter configuration",
                "Creates and edits model configurations: provider, endpoint, key, model list, headers, proxy, parameters, thinking rules and the connection test."
            ),
            e(
                "settings.ai.functional",
                "Functional model configuration",
                "Assigns a separate model to summarization, memory extraction, title generation, translation and other background functions."
            ),
            e(
                "settings.ai.supervision",
                "Supervision mode",
                "Runs a second model that watches the main model and can correct or stop a risky tool call before it executes."
            ),
            e(
                "settings.ai.speech",
                "Speech services",
                "Configures text-to-speech and speech-to-text engines, local model downloads, cloud voices, language and API keys."
            ),
            e(
                "settings.prompts.system",
                "Character card editor",
                "Edits character cards: system prompt, personality, greeting, example dialogue, model binding and attached memories or tools."
            ),
            e(
                "settings.prompts.persona",
                "Persona card generation",
                "Turns a conversation or description into a reusable persona card with name, traits, speaking style and greeting."
            ),
            e(
                "settings.prompts.waifu",
                "Waifu mode",
                "Changes reply formatting for a roleplay style, including sentence-by-sentence output and voice playback."
            ),
            e(
                "settings.context.summary",
                "Context and summary",
                "Controls how much history is kept verbatim, when older turns are summarized, and which model performs the summary."
            ),
            e(
                "settings.data.tool_permissions",
                "Tool permissions",
                "Chooses which tools the assistant may call and which need confirmation. Per-tool rules override the global policy."
            ),
            e(
                "settings.data.backup",
                "Data backup and recovery",
                "Exports or imports chats, characters, memories, model configurations and the database, and restores a previous snapshot."
            ),
            e(
                "settings.data.chat_history",
                "Chat history management",
                "Deletes, archives or bulk-manages conversations and their attachments."
            ),
            e(
                "settings.data.token_usage",
                "Token usage statistics",
                "Shows token and cost totals per day, week or lifetime, with currency, pricing and exchange-rate controls."
            ),
            e(
                "settings.data.llm_log",
                "LLM I/O log",
                "Records model requests and responses for debugging. The log can contain private chats; clear it when sharing the device."
            ),
            e(
                "settings.data.performance",
                "Performance monitor",
                "Shows live CPU, memory and network use of the app, plugins and terminal."
            ),
            e(
                "settings.privacy.cookies",
                "Clear cookies",
                "Deletes cookies stored by the in-app browser and WebView components. It does not delete chats or model configurations."
            ),
            e(
                "settings.external.http",
                "External HTTP chat API",
                "Runs a local HTTP endpoint so another device or tool can send messages to Operit. It shows the address and Bearer token."
            ),
        ).associateBy { it.id },
        "terminal" to listOf(
            e(
                "terminal_output",
                "Terminal output",
                "Shows the command output. Long output scrolls; links and file paths can be used according to the terminal settings."
            ),
            e(
                "terminal_input",
                "Command input",
                "Type a command and press Enter. The input line is connected to the shell running in the selected environment."
            ),
            e(
                "terminal_extra_keys",
                "Extra keys",
                "Adds keys that a phone keyboard does not have, such as Ctrl, Alt, Tab, arrows and Esc."
            ),
            e(
                "terminal_settings",
                "Terminal settings",
                "Opens font, color, cursor, keyboard and environment options for the terminal."
            ),
            e(
                "terminal_setup",
                "Environment setup",
                "Installs and configures the Linux environment used by the terminal. Setup is required once before the shell can start."
            )
        ).associateBy { it.id }
    )

    fun entry(screenId: String, elementId: String): HelpEntry? =
        screens[screenId]?.get(elementId)
}

fun helpEntry(screenId: String, elementId: String): HelpEntry? =
    HelpModeCatalog.entry(screenId, elementId)
