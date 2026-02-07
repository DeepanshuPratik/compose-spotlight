package com.daiatech.composespotlight.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daiatech.composespotlight.DimmingGround
import com.daiatech.composespotlight.FakeSpotlightController
import com.daiatech.composespotlight.SpotlightController
import com.daiatech.composespotlight.SpotlightDefaults
import com.daiatech.composespotlight.SpotlightManagerImpl
import com.daiatech.composespotlight.SpotlightZone
import com.daiatech.composespotlight.configure
import com.daiatech.composespotlight.models.DimState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotlightSampleTheme {
                val context = LocalContext.current
                val manager = remember { SpotlightManagerImpl.create(context) }
                val controller = remember { manager.createController() }

                LaunchedEffect(Unit) {
                    controller.configure("sample_onboarding") {
                        initialQueue = listOf("search", "notifications", "fab", "profile")
                        autoDim = true
                    }
                    controller.dequeueAndSpotlight(groundDimming = true)
                }

                SampleScreen(controller = controller)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(controller: SpotlightController) {
    val scope = rememberCoroutineScope()
    val dimState by controller.dimState.collectAsState()
    var rippleIntensity by rememberSaveable { mutableFloatStateOf(SpotlightDefaults.RippleIntensity) }

    Box(modifier = Modifier.fillMaxSize()) {
        DimmingGround(
            controller = controller,
            modifier = Modifier.fillMaxSize(),
            rippleIntensity = rippleIntensity
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Spotlight Sample") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        actions = {
                            SpotlightZone(
                                key = "search",
                                controller = controller,
                                message = "Step 1/4: Tap here to search across all your content"
                            ) {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }

                            SpotlightZone(
                                key = "notifications",
                                controller = controller,
                                message = "Step 2/4: Check your latest notifications here"
                            ) {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = true,
                            onClick = { }
                        )

                        SpotlightZone(
                            key = "profile",
                            controller = controller,
                            message = "Step 4/4: View and edit your profile from here"
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                selected = false,
                                onClick = { }
                            )
                        }

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = false,
                            onClick = { }
                        )
                    }
                },
                floatingActionButton = {
                    SpotlightZone(
                        key = "fab",
                        controller = controller,
                        message = "Step 3/4: Create something new!"
                    ) {
                        FloatingActionButton(
                            onClick = { },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Welcome to Compose Spotlight",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tap anywhere during the tour to progress.\n" +
                                "Use the slider below to adjust ripple intensity.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Ripple intensity control
                    Text(
                        text = "Ripple Intensity: ${"%.0f".format(rippleIntensity * 100)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = rippleIntensity,
                        onValueChange = { rippleIntensity = it },
                        valueRange = 0f..1f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Smooth", style = MaterialTheme.typography.labelSmall)
                        Text("Max ripple", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                controller.enqueueAll(listOf("search", "notifications", "fab", "profile"))
                                controller.dequeueAndSpotlight(groundDimming = true)
                            }
                        }
                    ) {
                        Text("Restart Tour")
                    }
                }
            }
        }

        // Tap overlay: visible only while the spotlight tour is active.
        if (dimState == DimState.RUNNING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            scope.launch {
                                controller.dequeueAndSpotlight(groundDimming = true)
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun SpotlightSampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SampleScreenPreview() {
    SpotlightSampleTheme {
        SampleScreen(controller = FakeSpotlightController())
    }
}
