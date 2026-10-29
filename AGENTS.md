# Agent instructions

Follow `../EmptyPlugin/AGENTS.md` for the shared rules.

## Repo-specific rules

- Config and `paper-plugin.yml` live in `src/resources/`, not `src/main/resources` (`build.gradle.kts` sets `resources.srcDir("src/resources")`).
- Update a pack's url and SHA-1 hash together (`/resourcepack update <name|id> <hash>`, plus `config.yml` on the server). Never ship the all-zero default hash.
- SneakyCharacterManager is an optional dependency (`required: false`). Touch its classes only behind the `isPluginEnabled` check in `SneakyResourcePacks.kt`.
- `ResourcepackEvents` dispatches a hardcoded console (login spell) command; don't add more hardcoded commands, move them to config.
- `ResourcepackEvents.joinedPlayers` is keyed by `Player`; remove entries and cancel tasks on quit and disable, and prefer UUID keys.
