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
