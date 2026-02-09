# Compose Spotlight

A Jetpack Compose library for building guided onboarding experiences. Highlight UI elements with a dimmed overlay, shape-aware cutouts, ripple effects, tooltips, and optional audio narration.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.DeepanshuPratik/compose-spotlight)](https://central.sonatype.com/artifact/io.github.DeepanshuPratik/compose-spotlight)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

## Features

- **Shape-aware spotlight cutouts** — Circle, RoundedRectangle, Rectangle, or any custom `Shape`
- **Radial ripple dimming** — Configurable ripple intensity and color around the highlighted zone
- **Smart tooltip positioning** — Auto-detects best position (top/bottom) and alignment (start/center/end), with manual override
- **Forced navigation** — Lock interaction to only the spotlighted component, blocking all other touches
- **Queue management** — Queue multiple zones and show them sequentially
- **Audio narration** — Built-in ExoPlayer support for audio-guided tours
- **Persistent state** — Save and restore spotlight progress across sessions
- **Builder pattern API** — Fluent DSL for message and zone configuration
- **Testing support** — `FakeSpotlightController` for previews and tests
- **Remote config** — Optional feature flag integration

## Installation

```kotlin
dependencies {
    implementation("io.github.DeepanshuPratik:compose-spotlight:1.0.0")
}
```

**Requirements:** Min SDK 24, Kotlin 1.9+, Compose BOM 2024.01.00+

## Quick Start

```kotlin
// 1. Create manager + controller
val manager = SpotlightManagerImpl.create(context)
val controller = manager.createController()

// 2. Configure and start the tour
LaunchedEffect(Unit) {
    controller.configure("onboarding") {
        initialQueue = listOf("search", "profile", "settings")
        autoDim = true
    }
    controller.dequeueAndSpotlight(groundDimming = true)
}

// 3. Wrap your screen in DimmingGround
DimmingGround(controller = controller) {
    Scaffold {
        // 4. Mark elements with SpotlightZone
        SpotlightZone(
            key = "search",
            controller = controller,
            message = "Tap here to search",
            shape = CircleShape
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }
    }
}
```

## Tooltip Positioning

Tooltips auto-detect the best position based on where the highlighted element sits on screen. Override manually when needed:

```kotlin
SpotlightZone(
    key = "bottom_item",
    controller = controller,
    message = "This tooltip appears above",
    tooltipPosition = TooltipPosition.TOP,       // TOP, BOTTOM, or AUTO (default)
    tooltipAlignment = TooltipAlignment.START     // START, CENTER, END, or AUTO (default)
) {
    NavigationBarItem(/* ... */)
}
```

**Auto-detection logic:**
- **Vertical:** Element in bottom half of screen → tooltip above; top half → tooltip below
- **Horizontal:** Element in left third → start-aligned; right third → end-aligned; middle → centered
- All positions are clamped to screen bounds

## Forced Navigation

Lock interaction to only the spotlighted component during a tour step. All other touches are blocked — useful for banking app-style onboarding or step-by-step flows where you need the user to tap a specific button.

```kotlin
SpotlightZone(
    key = "required_action",
    controller = controller,
    message = "You must tap this button to continue",
    forcedNavigation = true,
    shape = CircleShape
) {
    FloatingActionButton(onClick = {
        scope.launch { controller.dequeueAndSpotlight(groundDimming = true) }
    }) {
        Icon(Icons.Default.Add, contentDescription = "Add")
    }
}
```

When `forcedNavigation = true`, four invisible touch-blocking regions are placed around the spotlight zone, leaving a hole over the highlighted component. Only touches inside the hole reach the interactive element.

## Ripple Effect

The dimming overlay uses a radial gradient with configurable ripple rings around the spotlight cutout.

```kotlin
DimmingGround(
    controller = controller,
    rippleIntensity = 0.8f,   // 0f = smooth gradient, 1f = max ripple contrast
    rippleColor = Color.Black  // Color of the dim overlay and ripple rings
) {
    // your content
}
```

## SpotlightZone Overloads

The library provides multiple ways to define a spotlight zone, from simple to fully configured:

```kotlin
// Simple string message
SpotlightZone(key = "btn", controller = ctrl, message = "Tap here") {
    MyButton()
}

// String message with audio
SpotlightZone(key = "btn", controller = ctrl, message = "Tap here", audioResId = R.raw.tap) {
    MyButton()
}

// Single SpotlightMessage object
SpotlightZone(key = "btn", controller = ctrl, message = spotlightMessage {
    content { Text("Tap here", fontWeight = FontWeight.Bold) }
    audioResource(context, R.raw.tap)
    delay(4000)
}) {
    MyButton()
}

// Full config builder with multiple messages
SpotlightZone(key = "btn", controller = ctrl, config = {
    message { content { Text("Step 1") }; audioResource(context, R.raw.s1) }
    message { content { Text("Step 2") }; delay(3000) }
    tooltipPosition = TooltipPosition.BOTTOM
    shape = RoundedCornerShape(16.dp)
    forcedNavigation = true
    onFinish = { viewModel.markDone() }
}) {
    MyButton()
}
```

## Advanced Usage

### Persistent Spotlights

Save queue state across app restarts:

```kotlin
controller.setup("my_tour")

if (!controller.isPersistent()) {
    controller.enqueueAll(listOf("step1", "step2", "step3"))
    controller.setPersistent()
}

controller.dequeueAndSpotlight()
```

### Audio Narration

```kotlin
spotlightMessage {
    content { Text("Welcome!") }
    audioResource(context, R.raw.welcome)  // From raw resource
    delay(5000)                             // Fallback if audio fails
}

spotlightMessage {
    content { Text("Quick tip") }
    audio(AudioSource.Uri("https://..."))   // From URI
}

spotlightMessage {
    content { Text("Silent") }
    disableAudio()
}
```

### Remote Configuration

```kotlin
val manager = SpotlightManagerImpl.create(
    context = context,
    isSpotlightEnabledRemotely = remoteConfig.getBoolean("enable_spotlights")
)
```

## Sample App

The `app` module contains a complete working sample that demonstrates all major features:

- **5-step onboarding tour** — Spotlights search, notifications, FAB, profile, and settings sequentially
- **Forced navigation on FAB** — Step 3 requires tapping the + button; all other touches are blocked
- **Floating search bar** — Tap the search icon to open an `OutlinedTextField` overlay spotlighted with `forcedNavigation = true`; only the text field and close button are interactive while active
- **Toast feedback on buttons** — Every nav item shows a Toast on tap, verifying that touches are blocked during forced navigation and work normally outside of it
- **Ripple customization** — Slider to adjust ripple intensity (0%–100%) and color picker (Black, Blue, Purple, Teal, Red) with live preview
- **Smart tooltip placement** — Bottom nav items use explicit `tooltipPosition = TOP` with `tooltipAlignment` set to `START`, `CENTER`, or `END` depending on position
- **Shape variety** — CircleShape on icons, RoundedCornerShape on home nav item, RectangleShape on settings
- **Restart tour button** — Re-enqueues all zones and restarts the spotlight sequence

Run it:

```bash
./gradlew :app:installDebug
```

## Architecture

```
SpotlightManager          — Factory for creating controllers
  └─ SpotlightController  — Manages zone registry, queue, and dim state
       └─ DimmingGround   — Root composable rendering the dim overlay + ripple + cutout
            └─ SpotlightZone — Wraps individual UI elements to be highlighted
```

## Testing

```kotlin
@Test
fun testSpotlight() {
    val fake = FakeSpotlightController()
    composeTestRule.setContent {
        DimmingGround(controller = fake) {
            SpotlightZone(key = "zone", controller = fake, message = "Test") {
                Text("Content")
            }
        }
    }
}
```

## Contributing

Contributions welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

```bash
git clone https://github.com/DeepanshuPratik/compose-spotlight.git
cd compose-spotlight
./gradlew build
./gradlew test
```

## License

```
Copyright 2026 Deepanshu Pratik

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

Originally developed by me as part of the Karya Android Client project by DAIA Tech. Extracting it for open sourcing it and adding more feature support 

## Roadmap

- [x] Material 3 tooltip and theming integration
- [x] Shape support (Circle, RoundedRectangle, Rectangle, custom shapes)
- [x] Smart tooltip positioning with auto-detection
- [x] Forced navigation (touch blocking outside spotlight zone)
- [x] Configurable ripple dimming effect
- [ ] Compose Multiplatform support
- [ ] Animation customization (entry/exit transitions, spotlight movement)
- [ ] Analytics hooks
- [ ] Priority queue support
- [ ] Conditional spotlights

## Contact

- GitHub: [@DeepanshuPratik](https://github.com/DeepanshuPratik)
- Email: deepanshupratik@gmail.com
