# Compose Spotlight 🎯

A powerful and flexible feature spotlight library for Jetpack Compose that helps you create beautiful onboarding experiences and highlight important UI elements with dimmed overlays, tooltips, and optional audio narration.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.DeepanshuPratik/compose-spotlight)](https://central.sonatype.com/artifact/io.github.DeepanshuPratik/compose-spotlight)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

## ✨ Features

- 🎨 **Visual Spotlighting** - Dim the background and highlight specific UI elements
- 💬 **Customizable Tooltips** - Add rich composable content in tooltips with flexible positioning
- 🔊 **Audio Narration** - Built-in ExoPlayer support for audio-guided tours
- 📋 **Queue Management** - Queue multiple spotlights and show them sequentially
- 💾 **Persistent State** - Save and restore spotlight progress across app sessions
- 🎭 **Shape Support** - Highlight elements with Circle or Rectangle shapes
- 🔒 **Touch Blocking** - Optional touch blocking during spotlight sequences
- 🎯 **Lifecycle Aware** - Automatically handles audio playback based on app lifecycle
- 🧪 **Testing Support** - Includes fake implementations for testing
- 🎬 **Remote Control** - Optional remote config integration for feature flags

## 📦 Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.DeepanshuPratik:compose-spotlight:1.0.0")
}
```

### Requirements

- **Minimum SDK**: 24
- **Target SDK**: 34+
- **Kotlin**: 1.9+
- **Jetpack Compose**: BOM 2024.01.00+

## 🚀 Quick Start

### 1. Set up SpotlightManager

Create a `SpotlightManager` instance (typically in your DI setup):

```kotlin
val spotlightManager = SpotlightManagerImpl.create(context)
```

### 2. Create and Configure a Controller

```kotlin
val controller = spotlightManager.createController()

LaunchedEffect(Unit) {
    // Simple setup
    controller.configure("onboarding_tour")

    // Or with options using the builder DSL
    controller.configure("onboarding_tour") {
        persistent = true
        initialQueue = listOf("welcome", "profile", "settings")
    }
}
```

### 3. Wrap Your Screen with DimmingGround

```kotlin
@Composable
fun MyScreen() {
    val controller = // ... get controller

    DimmingGround(controller = controller) {
        // Your screen content
        MyScreenContent()
    }
}
```

### 4. Mark Elements with SpotlightZone

#### Simple String Message (New in v1.0.0!)

```kotlin
@Composable
fun MyScreenContent() {
    val controller = // ... get controller

    Column {
        // Simplest way - just a string message
        SpotlightZone(
            key = "profile_button",
            controller = controller,
            message = "Tap here to view your profile"
        ) {
            Button(onClick = { /* ... */ }) {
                Text("Profile")
            }
        }

        // String message with audio
        SpotlightZone(
            key = "settings_button",
            controller = controller,
            message = "Access settings here",
            audioResId = R.raw.settings_audio
        ) {
            Button(onClick = { /* ... */ }) {
                Text("Settings")
            }
        }
    }
}
```

#### Using Builder Pattern (New in v1.0.0!)

```kotlin
SpotlightZone(
    key = "welcome_zone",
    controller = controller,
    message = spotlightMessage {
        content { Text("Welcome to our app!") }
        audioResource(context, R.raw.welcome_audio)
        delay(5000)
    }
) {
    WelcomeCard()
}
```

#### Traditional Approach (Still Supported)

```kotlin
SpotlightZone(
    key = "profile_button",
    controller = controller,
    messages = listOf(
        SpotlightMessage(
            content = { Text("Tap here to view your profile") },
            defaultDelayMillis = 3000
        )
    ),
    tooltipPosition = TooltipPosition.BOTTOM
) {
    Button(onClick = { /* ... */ }) {
        Text("Profile")
    }
}
```

### 5. Control the Spotlight Flow

```kotlin
LaunchedEffect(Unit) {
    // Enqueue spotlights
    controller.enqueueAll(listOf("profile_button", "settings_button"))

    // Start showing spotlights
    controller.dequeueAndSpotlight(groundDimming = true)
}

// Or enqueue them one by one
LaunchedEffect(showTutorial) {
    if (showTutorial) {
        controller.enqueue("profile_button")
        controller.dequeueAndSpotlight()
    }
}
```

### 6. Handle Spotlight Completion

```kotlin
SpotlightZone(
    key = "last_item",
    controller = controller,
    messages = messages,
    onFinish = {
        // Spotlight finished, move to next or close
        scope.launch {
            controller.dequeueAndSpotlight()
        }
    }
) {
    // Your content
}
```

## 🎯 Builder Pattern API (v1.0.0)

The new builder pattern API provides a more flexible and readable way to create spotlight messages and configure spotlight zones.

### SpotlightMessage Builder

Create messages with a fluent, declarative API:

```kotlin
// Simple text-only message
val msg1 = spotlightMessage {
    content { Text("Welcome!") }
}

// Message with audio from URI
val msg2 = spotlightMessage {
    content { Text("Tap here to continue") }
    audioUri("android.resource://com.example/123")
    delay(5000)
}

// Message with audio from raw resource
val msg3 = spotlightMessage {
    content {
        Column {
            Text("Profile Section", fontWeight = FontWeight.Bold)
            Text("Manage your account here")
        }
    }
    audioResource(context, R.raw.profile_audio)
}

// Silent message (no audio)
val msg4 = spotlightMessage {
    content { Text("Quick tip") }
    disableAudio()
    delay(2000)
}
```

### AudioSource Abstraction

Use the `AudioSource` sealed class for type-safe audio configuration:

```kotlin
spotlightMessage {
    content { Text("Welcome") }
    audio(AudioSource.Resource(context, R.raw.welcome))
}

spotlightMessage {
    content { Text("Tutorial") }
    audio(AudioSource.Uri("android.resource://..."))
}

spotlightMessage {
    content { Text("Silent tip") }
    audio(AudioSource.None)
}

spotlightMessage {
    content { Text("From file") }
    audio(AudioSource.File("/path/to/audio.mp3"))
}
```

### SpotlightZone Configuration Builder

For complex multi-message zones, use the configuration builder:

```kotlin
SpotlightZone(
    key = "tutorial_zone",
    controller = controller,
    config = {
        message {
            content { Text("Step 1: Introduction") }
            audioResource(context, R.raw.step1)
            delay(3000)
        }
        message {
            content { Text("Step 2: Action required") }
            audioResource(context, R.raw.step2)
        }
        message {
            content { Text("Step 3: Complete") }
            disableAudio()
            delay(2000)
        }
        tooltipPosition = TooltipPosition.BOTTOM
        shape = RoundedCornerShape(16.dp)
        onFinish = {
            viewModel.markTutorialComplete()
        }
    }
) {
    TutorialContent()
}
```

### Progressive Disclosure

Start simple and add complexity as needed:

```kotlin
// Level 1: Simplest - just a string
SpotlightZone(
    key = "button1",
    controller = controller,
    message = "Tap here"
) {
    MyButton()
}

// Level 2: Add audio
SpotlightZone(
    key = "button2",
    controller = controller,
    message = "Tap here",
    audioResId = R.raw.tap_audio
) {
    MyButton()
}

// Level 3: Custom message with builder
SpotlightZone(
    key = "button3",
    controller = controller,
    message = spotlightMessage {
        content {
            Column {
                Text("Feature", fontWeight = FontWeight.Bold)
                Text("Learn more")
            }
        }
        audioResource(context, R.raw.feature)
        delay(4000)
    }
) {
    MyButton()
}

// Level 4: Full configuration with multiple messages
SpotlightZone(
    key = "button4",
    controller = controller,
    config = {
        message { content { Text("First tip") } }
        message { content { Text("Second tip") } }
        tooltipPosition = TooltipPosition.BOTTOM
        shape = CircleShape
    }
) {
    MyButton()
}
```

## 📚 Advanced Usage

### Audio Narration

Add audio files to guide users through the spotlight.

**New Builder Way (Recommended):**

```kotlin
spotlightMessage {
    content { Text("Welcome to our app!") }
    audioResource(context, R.raw.welcome_audio)
    delay(5000) // Fallback if audio fails
}
```

**Traditional Way (Still Supported):**

```kotlin
SpotlightMessage(
    content = { Text("Welcome to our app!") },
    audioFilePath = "android.resource://${context.packageName}/${R.raw.welcome_audio}",
    defaultDelayMillis = 5000
)
```

### Persistent Spotlights

Save spotlight queue state across app restarts:

```kotlin
controller.setup("my_tour")

// Check if already persistent
if (!controller.isPersistent()) {
    controller.enqueueAll(listOf("step1", "step2", "step3"))
    controller.setPersistent() // Save queue to disk
}

// Queue is automatically restored on next setup()
controller.dequeueAndSpotlight()
```

### Custom Shapes

```kotlin
SpotlightZone(
    key = "circular_button",
    controller = controller,
    shape = CircleShape, // or RectangleShape
    messages = messages
) {
    FloatingActionButton(onClick = { }) {
        Icon(Icons.Default.Add, contentDescription = null)
    }
}
```

### Disable Touch During Spotlight

```kotlin
SpotlightZone(
    key = "important_info",
    controller = controller,
    disableTouch = true, // User can't interact with rest of screen
    messages = messages
) {
    InfoCard()
}
```

### Custom Tooltip Styling

```kotlin
SpotlightZone(
    key = "styled_zone",
    controller = controller,
    messages = listOf(
        SpotlightMessage(
            content = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Custom Title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your custom content here")
                }
            }
        )
    ),
    tooltipPosition = TooltipPosition.TOP,
    toolTipMaxWidth = 300.dp
) {
    // Your content
}
```

### Remote Configuration

Integrate with Firebase Remote Config or similar:

```kotlin
val spotlightManager = SpotlightManagerImpl.create(
    context = context,
    isSpotlightEnabledRemotely = remoteConfig.getBoolean("enable_spotlights"),
    isOnboardingEnabledRemotely = remoteConfig.getBoolean("enable_onboarding")
)

// In your composable
val isEnabled by spotlightManager.isSpotlightEnabledRemotely.collectAsState(initial = false)

if (isEnabled) {
    // Show spotlights
}
```

### Manual Dimming Control

```kotlin
// Start dimming without spotlight
controller.startGroundDimming()

// Stop dimming
controller.stopGroundDimming()
```

### Check Audio Playback State

```kotlin
val isAudioPlaying by controller.isAudioPlaying().collectAsState(initial = false)

if (isAudioPlaying) {
    // Show audio indicator
    Icon(Icons.Default.VolumeUp, contentDescription = "Playing")
}
```

## 🏗️ Architecture

The library follows a clean architecture pattern:

```
┌─────────────────────┐
│  SpotlightManager   │  ← Factory for creating controllers
└──────────┬──────────┘
           │ creates
           ▼
┌─────────────────────┐
│ SpotlightController │  ← Manages spotlight state & queue
└──────────┬──────────┘
           │ controls
           ▼
┌─────────────────────┐
│   DimmingGround     │  ← Root container with dimming overlay
└──────────┬──────────┘
           │ contains
           ▼
┌─────────────────────┐
│   SpotlightZone     │  ← Individual spotlight zones with tooltips
└─────────────────────┘
```

### Key Components

- **SpotlightManager**: Factory for creating controller instances with optional remote config
- **SpotlightController**: Core controller managing zones, queue, and state
- **DimmingGround**: Root composable that renders the dimmed overlay
- **SpotlightZone**: Wraps individual UI elements to be highlighted
- **SpotlightPreferences**: Internal persistence layer using SharedPreferences

## 🧪 Testing

Use the provided `FakeSpotlightController` for testing:

```kotlin
@Test
fun testSpotlightBehavior() {
    val fakeController = FakeSpotlightController()

    composeTestRule.setContent {
        DimmingGround(controller = fakeController) {
            SpotlightZone(
                key = "test_zone",
                controller = fakeController
            ) {
                Text("Test Content")
            }
        }
    }

    // Your test assertions
}
```

## 🎨 Customization

### Defaults

You can customize default values through `SpotlightDefaults`:

```kotlin
SpotlightDefaults.PlainTooltipMaxWidth // 200.dp
SpotlightDefaults.caretHeight // 8.dp
SpotlightDefaults.caretWidth // 12.dp
SpotlightDefaults.tonalElevation // 4.dp
SpotlightDefaults.shadowElevation // 4.dp
```

### Dimming Overlay

The overlay uses a semi-transparent black (50% opacity) by default. To customize, you'll need to fork and modify `DimmingGroundImpl.kt`.

## 📱 Sample App

Check out the `sample` module for a complete working example demonstrating:
- Basic spotlight flow
- Audio narration
- Multiple spotlight sequences
- Persistent spotlights
- Custom tooltip designs

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

### Development Setup

1. Clone the repository
2. Open in Android Studio
3. Build the project
4. Run tests: `./gradlew test`

## 📋 Requirements for Maven Central Publishing

### For Publishers

To publish to Maven Central, you'll need:

1. **Sonatype OSSRH Account**: Sign up at https://issues.sonatype.org/
2. **GPG Key**: For signing artifacts
   ```bash
   gpg --gen-key
   gpg --list-keys
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```
3. **gradle.properties**: Add credentials
   ```properties
   ossrhUsername=your_username
   ossrhPassword=your_password
   signing.keyId=your_key_id
   signing.password=your_key_password
   signing.secretKeyRingFile=/path/to/secring.gpg
   ```
4. **Publish**: Run `./gradlew publishReleasePublicationToSonatypeRepository`

## 📄 License

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

## 🙏 Acknowledgments

Originally developed as part of the Karya Android Client project by DAIA Tech.

## 📞 Contact

- GitHub: [@DeepanshuPratik](https://github.com/DeepanshuPratik)
- Email: deepanshupratik@gmail.com

## 🗺️ Roadmap

- [ ] Compose Multiplatform support
- [ ] More shape options (custom shapes, rounded rectangles)
- [ ] Animation customization
- [ ] Analytics hooks
- [ ] Priority queue support
- [ ] Conditional spotlights
- [ ] Material 3 theming integration

---

**Made with ❤️ for the Jetpack Compose community**
