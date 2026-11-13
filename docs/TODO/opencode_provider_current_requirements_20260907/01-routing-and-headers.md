# Routing and request headers

## [DONE] Model list endpoint

Zen uses `https://opencode.ai/zen/v1/models`, and Go uses `https://opencode.ai/zen/go/v1/models`. The endpoint version segment is uniformly completed by the existing `OpenCodeRouting.normalizedBase`.

## [DONE] Protocol routing

- GPT, Grok and Muse Spark use OpenAI Responses.
- Claude and Qwen use Anthropic Messages.
- Gemini uses Google Generative Language.
- DeepSeek, GLM, Kimi, MiMo, MiniMax Zen and the other open models of Go use OpenAI-compatible Chat Completions.
- MiniMax Go uses Anthropic Messages; the free-compatible MiniMax Zen models are still handled by the catalog's Anthropic protocol.

## [DONE] Go client identity

- `User-Agent` uses `Operit/<version>`.
- `x-opencode-session` uses a stable value derived from the model config ID, so requests created from the same config stay consistent.

The current `AIService` interface does not pass the chat conversation ID to the provider, so a stable model config identifier is used here; if the public chat interface later exposes the conversation ID, this value should be replaced with the real chat session identifier.
