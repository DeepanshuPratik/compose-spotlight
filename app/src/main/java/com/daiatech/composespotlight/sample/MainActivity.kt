package com.daiatech.composespotlight.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.daiatech.composespotlight.TooltipAlignment
import com.daiatech.composespotlight.TooltipPosition
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
                        initialQueue = listOf("search", "notifications", "fab", "profile", "settings")
                        autoDim = true
                    }
                    controller.dequeueAndSpotlight(groundDimming = true)
                }

                SampleScreen(controller = controller)
            }
        }
    }
}

private val colorOptions = listOf(
    "Black" to Color.Black,
    "Blue" to Color(0xFF1A237E),
    "Purple" to Color(0xFF4A148C),
    "Teal" to Color(0xFF004D40),
    "Red" to Color(0xFFB71C1C),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(controller: SpotlightController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dimState by controller.dimState.collectAsState()
    val zoneLocation by controller.zoneLocationState.collectAsState()
    var rippleIntensity by rememberSaveable { mutableFloatStateOf(SpotlightDefaults.RippleIntensity) }
    var rippleColor by remember { mutableStateOf(SpotlightDefaults.RippleColor) }
    var selectedColorIndex by rememberSaveable { mutableStateOf(0) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        DimmingGround(
            controller = controller,
            modifier = Modifier.fillMaxSize(),
            rippleIntensity = rippleIntensity,
            rippleColor = rippleColor
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
                                message = "Step 1/5: Tap here to search across all your content",
                                shape = CircleShape,
                                forcedNavigation = true
                            ) {
                                IconButton(onClick = {
                                    scope.launch {
                                        showSearchBar = true
                                        controller.enqueue("search_bar")
                                        controller.dequeueAndSpotlight(groundDimming = true)
                                    }
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }

                            SpotlightZone(
                                key = "notifications",
                                controller = controller,
                                message = "Step 2/5: Check your latest notifications here",
                                shape = CircleShape
                            ) {
                                IconButton(onClick = {
                                    Toast.makeText(context, "Notifications opened!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        SpotlightZone(
                            key = "home",
                            controller = controller,
                            message = "Home",
                            shape = RoundedCornerShape(12.dp),
                            tooltipPosition = TooltipPosition.TOP,
                            tooltipAlignment = TooltipAlignment.START,
                            modifier = Modifier.weight(1f)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                selected = true,
                                onClick = {
                                    Toast.makeText(context, "Home tapped!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        SpotlightZone(
                            key = "profile",
                            controller = controller,
                            message = "Step 4/5: View and edit your profile from here",
                            shape = CircleShape,
                            tooltipPosition = TooltipPosition.TOP,
                            modifier = Modifier.weight(1f)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                selected = false,
                                onClick = {
                                    Toast.makeText(context, "Profile tapped!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        SpotlightZone(
                            key = "settings",
                            controller = controller,
                            message = "Step 5/5: Customize your app settings",
                            shape = RectangleShape,
                            tooltipPosition = TooltipPosition.TOP,
                            tooltipAlignment = TooltipAlignment.END,
                            modifier = Modifier.weight(1f)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = false,
                                onClick = {
                                    Toast.makeText(context, "Settings tapped!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    SpotlightZone(
                        key = "fab",
                        controller = controller,
                        message = "Step 3/5: Tap the + button to continue!",
                        shape = CircleShape,
                        forcedNavigation = true
                    ) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    controller.dequeueAndSpotlight(groundDimming = true)
                                }
                            },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome to Compose Spotlight",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tap anywhere during the tour to progress.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(28.dp))

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Smooth", style = MaterialTheme.typography.labelSmall)
                            Text("Max ripple", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Ripple color picker
                        Text(
                            text = "Ripple Color: ${colorOptions[selectedColorIndex].first}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorOptions.forEachIndexed { index, (_, color) ->
                                val isSelected = index == selectedColorIndex
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color, CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            } else {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = CircleShape
                                                )
                                            }
                                        )
                                        .clickable {
                                            selectedColorIndex = index
                                            rippleColor = color
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    controller.enqueueAll(listOf("search", "notifications", "fab", "profile", "settings"))
                                    controller.dequeueAndSpotlight(groundDimming = true)
                                }
                            }
                        ) {
                            Text("Restart Tour")
                        }
                    }

                    // Floating search bar — appears when search icon is tapped
                    if (showSearchBar) {
                        SpotlightZone(
                            key = "search_bar",
                            controller = controller,
                            message = "Type here! Only this bar is active during forced navigation.",
                            shape = RoundedCornerShape(28.dp),
                            forcedNavigation = true,
                            tooltipPosition = TooltipPosition.BOTTOM,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showSearchBar = false
                                        searchQuery = ""
                                        scope.launch {
                                            controller.dequeueAndSpotlight(groundDimming = false)
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // Tap overlay: visible only while the spotlight tour is active
        // and the current zone does NOT use forced navigation.
        if (dimState == DimState.RUNNING && !zoneLocation.forcedNavigation) {
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
