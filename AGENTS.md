# Project conventions

## Formatting

- Keep all edited frontend and backend source files formatted and easy to read.
- Frontend: use the pinned Prettier configuration in `FE/.prettierrc.json`, with 2-space indentation. Run `npm.cmd run format` and `npm.cmd run format:check` from `FE` after frontend edits.
- Backend: use the pinned Spotless configuration in `BE/pom.xml`, with Google Java Format GOOGLE and 2-space Java indentation. Run `.\mvnw.cmd spotless:apply` and `.\mvnw.cmd spotless:check` from `BE` after Java or POM edits.
- Use 2 spaces for indentation throughout both projects, including Java, TypeScript, Angular templates, CSS, JSON, YAML and XML. Do not use tabs.
- Follow the root `.editorconfig` for UTF-8, whitespace and line endings.
- Do not reformat existing Flyway SQL migrations that have already been applied; add a new migration when database changes are needed.
- Use JDK 21 for backend Maven commands. Use `npm` and `./mvnw` equivalents on macOS/Linux.
