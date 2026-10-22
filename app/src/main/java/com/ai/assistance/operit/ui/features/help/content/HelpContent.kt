package com.ai.assistance.operit.ui.features.help.content

/**
 * Built-in offline help for the English-only fork. Every main screen gets
 * one topic; each topic documents every control on that screen.
 *
 * Why code, not strings.xml: help bodies are long-form documentation, not
 * UI labels — they never go through the runtime translator, and keeping
 * them next to the Help screen avoids 12 new string-array resources.
 */

/** Pending topic requested by the "?" button before navigating to Help. */
object HelpTopicHolder {
    var topic: String = ""
}

data class HelpSection(val header: String, val body: String)

data class HelpTopic(val id: String, val title: String, val intro: String, val sections: List<HelpSection>)

private fun s(header: String, body: String) = HelpSection(header, body)

val HelpTopics: List<HelpTopic> = listOf(
    HelpTopic(
        id = "aichat",
        title = "Chat",
        intro = "The main conversation screen. Pick a character, type a message, attach files — the assistant replies using the model bound to the CHAT function.",
        sections = listOf(
            s(
                "History (clock icon)",
                "Opens the chat-history drawer. Inside: a search field filters conversations; the display-mode dropdown regroups the list (by folder, by character card, current card only); auto-switch-character and auto-switch-chat decide whether opening a chat also switches the active character. Tap a row to open it; long-press for pin, lock, rename, delete; drag rows to reorder. Arrow buttons jump to top/bottom of the list."
            ),
            s(
                "Character chip (avatar + name)",
                "Opens the character / character-group selector. The active character defines personality, default model binding and greeting. Some character cards lock the model — the model row then shows a confirm dialog before any change."
            ),
            s(
                "Message field, attach (+), mic, send",
                "Type text (@-mention works inside). Attach (+) opens the attachment panel: file, photo / take photo, screen content (screenshot + on-device OCR), notifications, location, memory folders, package / skill / MCP. Attached items appear as chips — tap x to remove, tap the chip to insert a reference. The mic button dictates instead of typing; the expand button opens a large input dialog. Send dispatches the message; while the model streams, Send becomes Stop — tapping it cancels generation. If the draft exceeds the context limit, a token-limit dialog offers Continue send or Cancel."
            ),
            s(
                "Queue button",
                "Defers the current draft into the pending queue instead of sending. Expand the queue to Send now, Edit or Delete each item."
            ),
            s(
                "Model row and toggles",
                "Two dropdowns rebind the model used by the CHAT function: model/config profile and model index. Below them: Thinking (reasoning effort), Max-context, Memory auto-update (writes new facts to Memory Base after chats), Auto-read / TTS (reads replies aloud), Tools on/off, Stream output off (wait for the full reply), No user preferences, and the permission-level chips Allow / Ask / Forbid (tool-call policy for this chat). The ... popup and the gear open Memory / Model / Tools / Behavior / Plugins: context-length slider, summary-threshold slider, thinking-quality control, per-tool visibility manager and order, manual memory update, and shortcuts to Memory, Model config, Prompts and Packages."
            ),
            s(
                "Workspace button (</>) and Export",
                "Opens the workspace overlay (built-in web page) or its setup. Export opens the platform dialog (Android / Windows), then progress (Cancel) and a completion dialog (Open / Install file)."
            ),
            s(
                "Computer and PiP buttons",
                "The terminal icon opens the AI computer (sandboxed shell the assistant can drive). The picture-in-picture button detaches the chat into the floating window; the running-tasks pill (visible with 2+ background tasks) jumps to the history drawer."
            )
        )
    ),
    HelpTopic(
        id = "workspace",
        title = "Workspace",
        intro = "A per-chat project folder shown as a full-screen overlay. The assistant reads and edits the same files; Preview renders the built page in a WebView.",
        sections = listOf(
            s(
                "Tabs and files",
                "The Preview tab is always first. Opening a file from the file browser adds a tab; the active tab shows the editor or preview. A dot means unsaved changes. The x closes the tab after a confirmation when the buffer is dirty."
            ),
            s(
                "Top bar actions",
                "Save appears only for a dirty file. For HTML and Markdown, Preview switches between rendered output and source. While a browser or command preview is open, back, forward and reload act on that WebView. The ? button turns on workspace help."
            ),
            s(
                "Floating menu",
                "The draggable menu opens the file browser, undo, redo, format (when the language is supported), rename, unbind and export. Export is available when the workspace config allows it and the folder is not a SAF environment."
            )
        )
    ),
    HelpTopic(
        id = "memory",
        title = "Memory Base",
        intro = "A knowledge graph of facts, documents and their links. The assistant reads it for context and, with auto-update on, writes new facts here after chats.",
        sections = listOf(
            s(
                "Search field and gear",
                "Type a query and press Search (or Enter) for vector search over memories. The gear opens search / embedding settings: save options, rebuild the search index (needed after imports), simulate a search."
            ),
            s(
                "Graph canvas",
                "Tap a node for the memory info dialog (Edit / Delete). Tap an edge to edit or delete the link. Drag a box while box-select mode is on to multi-select nodes."
            ),
            s(
                "Floating buttons",
                "Create Memory (+) opens the new-memory dialog (title, content, type, source, credibility, importance, folder, tags). Import Document takes a text / PDF / DOC file and chunks it into nodes. Link mode: pick two nodes, then set link type, weight and description. Box-select mode enables drag selection, after which Delete-selected removes them with confirmation."
            ),
            s(
                "Folder drawer and documents",
                "The folder icon shows the folder navigator: select, create, rename, delete and refresh folders; the profile dropdown switches memory spaces. Opening a document gives title and per-chunk text fields, in-document search, Save, Delete and Close."
            )
        )
    ),
    HelpTopic(
        id = "packages",
        title = "Packages (Plugins / Packages / Skills / MCP)",
        intro = "Installable capability packs: plugins (.toolpkg), script packages (.js/.ts/.hjson), skills, and MCP servers. Disabled packs stay on disk but the assistant cannot call them.",
        sections = listOf(
            s(
                "Tabs and search",
                "Four tabs — Plugins, Packages, Skills, MCP — switch the list. The top search field filters the current tab only."
            ),
            s(
                "Floating buttons",
                "Import (+) opens a file picker (.toolpkg for plugins; .js / .ts / .hjson for packages). The store button opens the artifact market. The gear opens environment variables (one text field per key, Save applies). The error button appears only when something failed to load and shows the load-errors dialog."
            ),
            s(
                "Rows, details, enable switch",
                "Tap a row for the details dialog: description, per-tool Run script (opens the execution dialog with input fields and Run), plugin configuration, Delete. The switch enables or disables the pack. Skills additionally support drag-to-reorder and a Market button; the MCP tab has its own market button."
            )
        )
    ),
    HelpTopic(
        id = "toolbox",
        title = "Toolbox",
        intro = "A grid of built-in utilities and diagnostics. Each card opens its tool; plugin-provided tools appear here too.",
        sections = listOf(
            s(
                "Shell and system",
                "Terminal — interactive shell (needs the terminal environment set up, see its own help). Shell executor — run one command and see output. Logcat — live system log viewer. SQL viewer — inspect the app database. UI debugger — dump and inspect the current screen hierarchy."
            ),
            s(
                "Files and media",
                "File manager — browse app and shared storage. HTML packager — build a web workspace for export. FFmpeg toolbox — audio/video conversion. Text-to-speech and Speech recognition — test the voice engines directly."
            ),
            s(
                "App and testing",
                "Permission manager — the same permission center as the setup wizard. Tool test center — exercise any tool with manual inputs. Token config — inspect token accounting. Process limit remover, Default assistant guide, User agreement — one-shot setup and legal screens."
            )
        )
    ),
    HelpTopic(
        id = "settings",
        title = "Settings",
        intro = "All app configuration in one list. Rows marked below navigate to dedicated screens, each with its own help topic where it matters.",
        sections = listOf(
            s(
                "Account and personalization",
                "GitHub account / Login / Logout manage update-check identity. User preferences, Language (English-only build: only English is shipped), Theme/appearance, Global display and Layout adjustment control look and layout."
            ),
            s(
                "AI model",
                "Model parameters and Functional model choose which endpoint serves each built-in function. Supervision mode switch enables the supervision overlay (toast cards on tool calls). Speech services configures TTS/STT engines."
            ),
            s(
                "Prompts and context",
                "System prompts and Persona card generation shape assistant behavior; Waifu mode is a presentation style. Context summary sets the context-length and auto-summary threshold."
            ),
            s(
                "Data and permissions",
                "Tool permissions (global Allow / Ask / Forbid plus per-tool lists), Data backup, Chat history management, Token usage statistics, LLM IO log (every request/response with latency), Subpack storage — Change moves the template directory between internal, external and SD card and reports moving/failed states. Clear cookies wipes WebView cookies after confirmation. External HTTP chat exposes the assistant over LAN."
            )
        )
    ),
    HelpTopic(
        id = "workflow",
        title = "Workflows",
        intro = "Saved automation chains: trigger → steps → actions. Disabled workflows are kept but never fire.",
        sections = listOf(
            s(
                "Create and templates",
                "The + button expands a speed-dial: blank workflow, from template, or multi-select mode. Templates include intent chat broadcast, plain chat, condition, logic AND/OR, extract, error branch, speech trigger, plus one row per ToolPkg template. The create dialog needs a non-empty Name; Description is optional."
            ),
            s(
                "Cards and selection",
                "Tap a card to open its detail; the switch enables/disables it. In multi-select mode checkboxes appear with Select all / Clear selection on the selection bar, and Delete selected asks for confirmation. The empty state offers a single New button."
            )
        )
    ),
    HelpTopic(
        id = "permissions",
        title = "Permission center",
        intro = "Five cumulative levels — STANDARD, ACCESSIBILITY, ADMIN, DEBUGGER, ROOT. Higher levels unlock more tools but need more setup. Refresh re-checks every status row.",
        sections = listOf(
            s(
                "STANDARD rows",
                "Storage permission jumps to the system All-files access page; Overlay to the draw-over-apps page; Battery exemption to the unrestricted-battery page; Location triggers the permission request. Operit terminal opens the terminal-setup wizard."
            ),
            s(
                "Higher levels",
                "ACCESSIBILITY adds Enable service (system accessibility page) and Install/Update provider (installs the helper APK). ADMIN adds no actions of its own. DEBUGGER adds the Shizuku row (setup wizard: install from store, install bundled APK, open the Shizuku app, tutorial, request permission, update). ROOT adds the Root row (request root, video tutorial). Each wizard card expands/collapses to show its steps."
            ),
            s(
                "Terminal environments",
                "The OperitTerminal card has Open TerminalSetup plus expand. Setup offers Node.js and Python categories — tick at least these two, then Start Setup; the status row turns green only when pnpm, python and pip are all present. Without ticking anything, Start Setup does almost nothing."
            )
        )
    ),
    HelpTopic(
        id = "terminal",
        title = "Terminal",
        intro = "A full shell inside the app (proot Linux). Needs the environment installed via the setup wizard; without it commands fail and the permission row stays red.",
        sections = listOf(
            s(
                "Terminal view",
                "Output plus a command input line — type and run. The extra-keys row injects Tab, Esc, arrows, Ctrl combinations and friends that phones lack."
            ),
            s(
                "Home menu, setup, settings",
                "The overflow menu holds Settings (gear) and Setup. Setup has a setup-commands field with Setup/Back — it provisions Node.js/Python and returns home. Settings: Back, font-size +/- , switches (extra keys, bell, keep screen on), mirror/source lists with Add (name + URL, Save), radio select, Delete, and Reset buttons."
            )
        )
    ),
    HelpTopic(
        id = "modelconfig",
        title = "Model parameters",
        intro = "Connection profiles for LLM endpoints. The active profile serves the CHAT function unless a character card overrides it.",
        sections = listOf(
            s(
                "Profiles",
                "The dropdown switches profiles (check mark = active). New creates one (name required), Rename renames, Delete removes. Test connection runs chat/model checks and lists per-item Success/Failed rows with Cancel to abort."
            ),
            s(
                "API section",
                "Provider dropdown sets provider plus default model/endpoint. Endpoint field with preset dropdown; API Key (masked); Model name with Fetch/Refresh list and model dropdown. Codex Login/Logout with usage refresh; MNN download button fetches the on-device model."
            ),
            s(
                "Context, thinking, parameters",
                "Context-length and summary-threshold numeric fields with switches (autosaved). Thinking rules: Add rule, per-row Edit / Move up-down / enable dot; the editor has Enable, Control and Match-field dropdowns, Match-models and Param-path fields, Always-on, On/Off actions and Levels editors. Parameters tabs (Generation / Creativity / Repetition / Custom): per-parameter Enable plus slider or text field, temperature recommendation row, Reset, Add custom (name/type/default/min/max/category). Custom headers: key/value rows with Delete, preset inserter (Android/Win browser, zh/en language, CMCC, US-LA), Add header."
            ),
            s(
                "Advanced and key pool",
                "Request-limit/min and Max-concurrent numeric fields (autosaved). API-key-pool switch reveals the pool: Add key, per-key Edit/Delete, Test start/Pause, Clear marks, Batch import from file, Export to file, Clear pool with confirmation."
            )
        )
    ),
    HelpTopic(
        id = "toolpermission",
        title = "Tool permissions",
        intro = "Default policy for every assistant tool call, with per-tool exceptions. Applies to the whole app, not one chat.",
        sections = listOf(
            s(
                "Global chips and lists",
                "Allow / Ask / Forbid set the default for all tools. The Allow card (+) and Forbid card (+) open the selector: search field filters tools, checkbox rows toggle membership, Done closes. Per-tool chips carry an x that returns the tool to Ask."
            )
        )
    ),
    HelpTopic(
        id = "speech",
        title = "Speech services",
        intro = "Voice in and out. TTS speaks replies; STT transcribes the mic button. Tabs TTS / STT share the profile mechanism.",
        sections = listOf(
            s(
                "Profiles and TTS engines",
                "Per-tab profile dropdown with Create / Rename / Delete and name dialogs. Engine dropdown: Simple (system voice), HTTP, OpenAI-WS, OpenAI, VITS — each shows its own fields. Rate (0.5–2.0) and Pitch (0.5–2.0) sliders. Simple adds Locale dropdown, Voice field with Select dialog and Clear; cleaner regex rows with Delete, Add and a template menu; HTTP adds URL template, API key, Headers JSON, Method (GET/POST), Body, Content-type, Locale, Voice, Model, Pipeline JSON and an Advanced expander."
            ),
            s(
                "STT and test tools",
                "STT tab: engine dropdown, Endpoint URL, API key, Model; local tuning via VAD switch + Sensitivity slider; GigaAM model section with Download/Delete (about 225 MB, on-demand with consent). The Speech recognition tool: microphone-permission button, Copy result, engine label with Switch engine (cycles GigaAM → OpenAI → Deepgram), Language dropdown, Start (mic) / Stop. The Text-to-speech tool: input field, Locale, Voice + Select + Clear, Rate/Pitch sliders, Play / Stop."
            )
        )
    ),
    HelpTopic(
        id = "about",
        title = "About",
        intro = "Version, storage, project links and updates. This fork states its identity under the version: English-only community fork, ALPHA build.",
        sections = listOf(
            s(
                "Updates",
                "Check for updates checks now or reopens the update dialog (spinner while checking). Beta plan switch opts into beta builds. The update dialog offers Download / Patch update / OK and Close; mirror-choice dialogs pick a download mirror; progress dialogs Cancel; an install prompt finishes the flow."
            ),
            s(
                "Storage and links",
                "Total / internal / SD-card rows show megabytes and free space (display only). Project URL and Star on GitHub open the repository; Update log opens history; Open source licenses shows the licenses dialog. Contact and Developer rows are display-only except the tappable developer link."
            )
        )
    )
)

fun helpTopic(id: String): HelpTopic? = HelpTopics.find { it.id == id }

/** Screen class name → help topic. Screens without a topic show no "?" button. */
fun helpTopicForScreen(screen: Any): String? {
    val name = screen.javaClass.simpleName
    return when (name) {
        "AiChat" -> "aichat"
        "MemoryBase" -> "memory"
        "Packages", "PackageManager" -> "packages"
        "Toolbox" -> "toolbox"
        "Settings" -> "settings"
        "Workflow" -> "workflow"
        "ShizukuCommands", "ShizukuDemo" -> "permissions"
        "AppPermissions" -> "permissions"
        "Terminal", "TerminalSetup", "TerminalAutoConfig" -> "terminal"
        "ModelConfig", "ModelConfigOnboarding" -> "modelconfig"
        "ToolPermission" -> "toolpermission"
        "SpeechServicesSettings", "SpeechToText", "TextToSpeech" -> "speech"
        "About" -> "about"
        else -> null
    }
}
