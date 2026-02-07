# Contributing to Compose Spotlight

First off, thank you for considering contributing to Compose Spotlight! It's people like you that make this library better for everyone.

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to deepanshupratik@gmail.com.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

* **Use a clear and descriptive title**
* **Describe the exact steps to reproduce the problem**
* **Provide specific examples** - Include code snippets, screenshots, or GIFs
* **Describe the behavior you observed** and explain which behavior you expected to see instead
* **Include device/emulator details** - Android version, device model, etc.
* **Mention the library version** you're using

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

* **Use a clear and descriptive title**
* **Provide a detailed description** of the suggested enhancement
* **Explain why this enhancement would be useful** to most users
* **List some examples** of how it would be used

### Pull Requests

* Fill in the required template
* Follow the Kotlin coding style (use `ktlint` for formatting)
* Include thoughtful comments in your code
* Write meaningful commit messages
* Update the README.md if you're adding features
* Add tests for your changes
* Ensure all tests pass before submitting

## Development Setup

1. **Fork the repository**

2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/compose-spotlight.git
   cd compose-spotlight
   ```

3. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/DeepanshuPratik/compose-spotlight.git
   ```

4. **Create a branch**
   ```bash
   git checkout -b feature/my-new-feature
   ```

5. **Make your changes**

6. **Run tests**
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

7. **Commit your changes**
   ```bash
   git commit -m "Add some feature"
   ```

8. **Push to your fork**
   ```bash
   git push origin feature/my-new-feature
   ```

9. **Create a Pull Request**

## Coding Standards

### Kotlin Style Guide

* Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
* Use 4 spaces for indentation
* Maximum line length: 120 characters
* Use meaningful variable and function names
* Add KDoc comments for public APIs

### Code Formatting

We use `ktlint` for code formatting. Before committing:

```bash
./gradlew ktlintFormat
```

### Testing

* Write unit tests for all new functionality
* Ensure existing tests pass
* Aim for high test coverage
* Use descriptive test names

```kotlin
@Test
fun `enqueue should add spotlight to queue when not persistent`() {
    // Test implementation
}
```

## Project Structure

```
composespotlight/
├── src/
│   ├── main/
│   │   └── java/com/daiatech/composespotlight/
│   │       ├── SpotlightManager.kt       # Public API
│   │       ├── SpotlightController.kt    # Public API
│   │       ├── SpotlightZone.kt         # Composable
│   │       ├── DimmingGround.kt         # Composable
│   │       ├── SpotlightDefaults.kt     # Constants
│   │       ├── FakeSpotlightController.kt # Testing
│   │       ├── internal/                # Internal implementation
│   │       └── models/                  # Data models
│   ├── test/                            # Unit tests
│   └── androidTest/                     # Instrumentation tests
├── build.gradle.kts                     # Build configuration
├── LICENSE                              # Apache 2.0 License
├── README.md                            # Documentation
└── CONTRIBUTING.md                      # This file
```

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

* `feat:` A new feature
* `fix:` A bug fix
* `docs:` Documentation changes
* `style:` Code style changes (formatting, etc.)
* `refactor:` Code refactoring
* `test:` Adding or updating tests
* `chore:` Maintenance tasks

Examples:
```
feat: add support for custom shapes in spotlight
fix: tooltip not showing on first spotlight
docs: update README with audio narration example
test: add tests for persistent spotlight queue
```

## Documentation

* Update README.md for new features
* Add KDoc comments for all public APIs
* Include code examples in documentation
* Keep documentation up to date with code changes

## Testing

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew test --tests "SpotlightControllerTest.enqueue*"
```

### Writing Tests

* Test both success and failure scenarios
* Mock external dependencies
* Use descriptive test names
* Keep tests focused and independent

## Release Process

(For maintainers only)

1. Update version in `build.gradle.kts`
2. Update CHANGELOG.md
3. Create a git tag: `git tag v1.0.0`
4. Push tag: `git push origin v1.0.0`
5. Publish to Maven Central: `./gradlew publishReleasePublicationToSonatypeRepository`
6. Create GitHub release with changelog

## Questions?

Feel free to:
* Open an issue for questions
* Join discussions in GitHub Discussions
* Email: deepanshupratik@gmail.com

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to Compose Spotlight! 🎉
