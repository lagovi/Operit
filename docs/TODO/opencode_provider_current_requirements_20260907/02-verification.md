# Verification record

Per the repository execution rules, no build, Gradle or test command was run for this change.

Static checks:

- The OpenCode model list URL contains `/v1/models`.
- The MiniMax protocol for Zen and Go is distinguished by endpoint.
- The OpenCode-specific request headers are injected only on the OpenCode route.
- The shared providers gain no new OpenCode conditional branches.

Static check completed: `git diff --check` reports no errors. No build, Gradle or test command was executed.
