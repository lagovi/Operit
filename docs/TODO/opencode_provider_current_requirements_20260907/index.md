---
topic: Update OpenCode Zen and Go provider requirements
status: done
---

# OpenCode provider current requirements

## Upstream changes

OpenCode now places the public APIs of Zen and Go under the versioned `/v1` path. Model IDs use `opencode/<model-id>` and `opencode-go/<model-id>` respectively, and the model protocol is decided by the official catalog rather than by a coarse prefix of the model name alone.

OpenCode Go additionally requires the client to use its own `User-Agent` and to send a stable `x-opencode-session` header per session.

References:

- [OpenCode Zen](https://opencode.ai/docs/zen)
- [OpenCode Go](https://opencode.ai/docs/go)
- [OpenCode provider source](https://github.com/anomalyco/opencode/blob/dev/packages/opencode/src/provider/provider.ts)

## Repository state

- Request routing was already isolated in `OpenCodeProvider.kt`, but the model list URL lacked `/v1`.
- MiniMax uses a different protocol on Zen and Go, and the old routing did not distinguish the endpoint.
- Qwen, Muse Spark and the current Go model set were not fully covered.
- Go requests did not declare Operit's client identity or the session header.

## Change scope

- Fix the Zen/Go model list endpoints.
- Update the Responses, Anthropic, Gemini and Chat Completions routing per the official Zen/Go endpoint table.
- Add the Operit `User-Agent` and a stable config session identifier to Go requests.
- Keep the shared providers unaware of the OpenCode-specific protocol.
