# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Compose Multiplatform support (iOS, Desktop, Web)
- Animation customization API
- Analytics hooks for tracking spotlight views
- Priority queue support
- Conditional spotlights based on user behavior
- DataStore support (replacing SharedPreferences)

## [1.1.0] - 2026-03-01

### Added
- **Hand Gesture Overlay**: Animated Lottie-based hand gesture (swipe/tap) displayed over spotlight zones to guide users
- **Shape-Adaptive Ripple Effects**: Ripple animations that match the spotlight shape (circle or rectangle)
- **Animated Traveling Wave**: Spotlight ripple now uses an animated traveling wave effect for a more polished look
- **Spotlight Padding**: New padding configuration for spotlight zones, giving control over the highlight area around the target element
- **Enhanced Ripple Configuration**: Configurable ripple color, stroke width, and animation parameters

### Fixed
- **Improved Recomposition**: Core data classes are now properly marked as immutable (`@Immutable`), reducing unnecessary recompositions in Compose

### Known Issues
- Caret drawing for tooltips is currently disabled (see issue #1160)
- `SpotlightControlMode` enum is defined but not yet used
- Dimming overlay color and opacity are not customizable (hardcoded to 50% black)
- ExoPlayer is directly exposed in public API (abstraction deferred to v1.2.0)
- SharedPreferences only (DataStore support planned for v1.2.0)

## [1.0.0] - 2026-02-07

### Added
- **Builder Pattern API**: New flexible message creation with `spotlightMessage {}` DSL
  - Fluent API for creating `SpotlightMessage` instances
  - Support for multiple audio source types (URI, resource ID, file path)
  - Optional audio with `disableAudio()` method
  - Configurable delay durations
- **AudioSource Sealed Class**: Type-safe audio source abstraction
  - `AudioSource.Uri` - Audio from URI strings
  - `AudioSource.Resource` - Audio from raw resource IDs
  - `AudioSource.File` - Audio from file paths
  - `AudioSource.None` - Explicitly disable audio
- **SpotlightZone Overloads**: Multiple convenience functions for simpler use cases
  - Simple string message overload: `SpotlightZone(key, controller, message = "Text")`
  - Single message overload: `SpotlightZone(key, controller, message = spotlightMessage {...})`
  - Builder-style configuration with `SpotlightZoneConfig`
- **Controller Configuration DSL**: Flexible controller setup
  - `controller.configure(id) {}` DSL function
  - `SpotlightControllerConfig` class for declarative configuration
  - Support for persistent queues, initial queue setup
- Initial public release
- Core spotlight functionality with `SpotlightManager` and `SpotlightController`
- `DimmingGround` composable for dimmed overlay effect
- `SpotlightZone` composable for marking highlight zones
- Queue-based spotlight management system
- Tooltip support with customizable positioning (TOP/BOTTOM)
- Audio narration support using ExoPlayer
- Multi-message support with auto-advance
- Persistent spotlight queue (survives app restarts)
- Lifecycle-aware audio playback (pause on background)
- Touch blocking during spotlight sequences
- Shape support (Circle and Rectangle)
- Remote configuration support for feature flags
- `FakeSpotlightController` for testing
- Inspection mode bypass for Compose previews
- Thread-safe zone registration and queue management
- SharedPreferences-based persistence layer
- Comprehensive KDoc documentation
- Apache 2.0 license

### Fixed
- **Critical**: Zone unregistration memory leak - zones are now properly unregistered when composable is disposed
- **Critical**: Proper cleanup in `DisposableEffect` to prevent memory accumulation

### Breaking Changes
- None - new API is additive, all existing code continues to work

### Features
- **Visual Spotlighting**: Dim background and highlight specific UI elements
- **Customizable Tooltips**: Rich composable content in tooltips
- **Audio Narration**: Optional audio guidance with ExoPlayer
- **Queue Management**: Sequential spotlight presentation
- **Persistent State**: Save/restore progress across sessions
- **Shape Support**: Circle or Rectangle spotlight cutouts
- **Touch Control**: Optional touch blocking during tours
- **Lifecycle Integration**: Proper audio handling with app lifecycle
- **Testing Support**: Fake implementations for unit tests
- **Remote Config**: Integration with remote configuration services
- **Progressive Disclosure**: Start with simple API, add complexity as needed

### Dependencies
- Jetpack Compose BOM 2024.01.00+
- Material 3
- Lifecycle Runtime Compose
- Media3 ExoPlayer
- Android SDK 24+ (minimum)

### Migration Guide

**For Existing Users**: All existing code continues to work without changes. The new builder pattern API is optional and additive.

**Recommended Updates**:

1. **Simple messages** - Use the new string overload:
   ```kotlin
   // Before
   SpotlightZone(
       key = "button",
       controller = controller,
       messages = listOf(SpotlightMessage(content = { Text("Click") }))
   ) { Button() }

   // After (simpler)
   SpotlightZone(
       key = "button",
       controller = controller,
       message = "Click"
   ) { Button() }
   ```

2. **Messages with audio** - Use builder pattern:
   ```kotlin
   // Before
   SpotlightMessage(
       content = { Text("Welcome") },
       audioFilePath = "android.resource://${context.packageName}/${R.raw.audio}"
   )

   // After (cleaner)
   spotlightMessage {
       content { Text("Welcome") }
       audioResource(context, R.raw.audio)
   }
   ```

3. **Controller setup** - Use configure DSL:
   ```kotlin
   // Before
   controller.setup("id")
   controller.setPersistent()
   controller.enqueueAll(list)

   // After (declarative)
   controller.configure("id") {
       persistent = true
       initialQueue = list
   }
   ```

### Known Issues
- Caret drawing for tooltips is currently disabled (see issue #1160)
- `SpotlightControlMode` enum is defined but not yet used
- Dimming overlay color and opacity are not customizable (hardcoded to 50% black)
- ExoPlayer is directly exposed in public API (will be abstracted in v1.1.0)
- SharedPreferences only (DataStore support planned for v1.2.0)

---

## Version History

### Version Number Format
We use [Semantic Versioning](https://semver.org/):
- **MAJOR** version for incompatible API changes
- **MINOR** version for backwards-compatible functionality additions
- **PATCH** version for backwards-compatible bug fixes

### Support
- Each major version is supported until the next major version is released
- Bug fixes are backported to the current minor version only
- Security fixes are backported to all supported versions

---

[Unreleased]: https://github.com/DeepanshuPratik/compose-spotlight/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/DeepanshuPratik/compose-spotlight/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/DeepanshuPratik/compose-spotlight/releases/tag/v1.0.0
