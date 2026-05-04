# AGENTS.md

This document outlines the conventions and commands for working with this codebase. Adhering to these guidelines will ensure consistency and quality.

## Build, Lint, and Test Commands

### Build
To build the entire project, run:
```bash
./gradlew build
```

### Lint
To run the linter and check for code style issues, use:
```bash
./gradlew lint
```
For more specific lint checks, you can target a specific module:
```bash
./gradlew :app:lint
```

### Test
To run all unit tests in the project, use:
```bash
./gradlew test
```

To run a single test, you can use the `--tests` flag with the fully qualified name of the test class or method:
```bash
./gradlew test --tests "com.sheguard.ExampleUnitTest"
```
```bash
./gradlew test --tests "com.sheguard.ExampleUnitTest.addition_isCorrect"
```
To run all instrumentation tests, use:
```bash
./gradlew connectedAndroidTest
```

## Code Style and Conventions

### Formatting
- Follow the official [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
- Use 4 spaces for indentation.
- Keep lines under 100 characters.
- Use trailing commas where applicable.

### Imports
- Organize imports alphabetically.
- Remove unused imports.
- Avoid wildcard imports (`import foo.*`).

### Naming Conventions
- **Packages:** `com.sheguard.feature`
- **Classes/Interfaces:** `PascalCase`
- **Functions/Methods:** `camelCase`
- **Variables:** `camelCase`
- **Constants:** `UPPER_SNAKE_CASE`
- **Test Functions:** `snake_case_with_underscores`

### Types
- Use type inference (`val name = "SheGuard"`) when the type is obvious.
- Explicitly declare types when the type is not immediately clear from the context.
- Use nullable types (`?`) only when a value can truly be absent.

### Error Handling
- Use `try-catch` blocks for handling exceptions from external libraries or APIs.
- Prefer `Result<T>` or sealed classes for representing success and failure states in your own code.
- Avoid generic `catch (e: Exception)` blocks. Catch specific exceptions.

### Android Specifics
- **UI:** Use Jetpack Compose for building UI. Follow the [Compose API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md).
- **Architecture:** Follow the recommended Android app architecture using `ViewModel`, `Repository`, and `DataSource` layers.
- **Resources:** Name drawable resources using `snake_case`. For example, `ic_profile_avatar.xml`.
- **Strings:** All user-facing strings should be defined in `strings.xml` to support internationalization.

### No Existing Agent Rules
No `.cursor/rules/`, `.cursorrules`, or `.github/copilot-instructions.md` files were found in the repository.
