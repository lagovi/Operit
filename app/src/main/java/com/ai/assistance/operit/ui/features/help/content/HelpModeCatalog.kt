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
