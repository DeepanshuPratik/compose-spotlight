/*
 * Copyright 2026 Deepanshu Pratik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.daiatech.composespotlight

import android.content.res.Configuration
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.composespotlight.models.SpotlightMessage
import com.daiatech.composespotlight.models.SpotlightZoneData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A composable function that creates a spotlight zone with optional tooltips and audio messages.
 *
 * @param key Unique identifier for this spotlight zone
 * @param modifier Modifier to be applied to the spotlight zone
 * @param messages Optional list of messages to be displayed in the tooltip
 * @param tooltipPosition Position of the tooltip relative to the content (TOP or BOTTOM)
 * @param disableTouch Whether touch should be disabled on this spotlight zone
 * @param shape Shape of the spotlight cutout
 * @param caretSize Size of the tooltip caret
 * @param toolTipMaxWidth Maximum width of the tooltip
 * @param controller Controller managing all spotlight zones
 * @param onFinish Callback invoked when the spotlight sequence is completed
 * @param content Composable content to be highlighted by the spotlight
 */
@Composable
fun SpotlightZone(
    key: String,
    modifier: Modifier = Modifier,
    messages: List<SpotlightMessage>? = null,
    tooltipPosition: TooltipPosition = TooltipPosition.TOP,
    disableTouch: Boolean = false,
    shape: Shape = RectangleShape,
    caretSize: DpSize = DpSize(SpotlightDefaults.caretHeight, SpotlightDefaults.caretWidth),
    toolTipMaxWidth: Dp = SpotlightDefaults.PlainTooltipMaxWidth,
    controller: SpotlightController,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (LocalInspectionMode.current) {
        Box(modifier) {
            content()
        }
    } else {
        SpotlightZoneCore(
            key = key,
            modifier = modifier,
            messages = messages,
            tooltipPosition = tooltipPosition,
            disableTouch = disableTouch,
            shape = shape,
            caretSize = caretSize,
            toolTipMaxWidth = toolTipMaxWidth,
            controller = controller,
            onFinish = onFinish,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotlightZoneCore(
    key: String,
    modifier: Modifier = Modifier,
    messages: List<SpotlightMessage>? = null,
    tooltipPosition: TooltipPosition = TooltipPosition.TOP,
    disableTouch: Boolean = false,
    shape: Shape = RectangleShape,
    caretSize: DpSize = DpSize(SpotlightDefaults.caretHeight, SpotlightDefaults.caretWidth),
    toolTipMaxWidth: Dp = SpotlightDefaults.PlainTooltipMaxWidth,
    controller: SpotlightController,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tooltipState = rememberTooltipState(isPersistent = true)
    val positionProvider = rememberTooltipPositionProvider(tooltipPosition)
    var allowTouch = remember { mutableStateOf(true) }
    val isAppInForeground = remember { mutableStateOf(true) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exoPlayer.playWhenReady = true
                isAppInForeground.value = true
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.playWhenReady = false
                isAppInForeground.value = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    DisposableEffect(key) {
        onDispose {
            controller.unregisterZone(key)
            exoPlayer.release()
        }
    }

    Box(
        modifier.onGloballyPositioned { coordinates ->
            val zone = SpotlightZoneData(
                layoutCoordinates = coordinates,
                tooltipState = tooltipState,
                audioPlayer = exoPlayer,
                shape = shape
            )
            controller.registerZone(key, zone)
        }
    ) {
        if (disableTouch && !allowTouch.value) {
            Dialog(onDismissRequest = {}) {
                (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
        if (!messages.isNullOrEmpty()) {
            TooltipBox(
                focusable = false,
                positionProvider = positionProvider,
                tooltip = {
                    var currentMessageIndex by remember { mutableIntStateOf(0) }
                    val messageContent = remember(currentMessageIndex) {
                        messages.getOrNull(currentMessageIndex)?.content ?: messages.last().content
                    }

                    LaunchedEffect(currentMessageIndex) {
                        if (currentMessageIndex > messages.lastIndex) {
                            exoPlayer.playWhenReady = false
                            allowTouch.value = true
                            onFinish.invoke()
                            return@LaunchedEffect
                        }

                        allowTouch.value = false
                        val currentMessage = messages[currentMessageIndex]
                        exoPlayer.playWhenReady = currentMessage.audioFilePath != null && isAppInForeground.value

                        if (currentMessage.audioFilePath == null && isAppInForeground.value) {
                            delay(currentMessage.defaultDelayMillis)
                            currentMessageIndex += 1
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        val mediaItems = messages.mapNotNull {
                            it.audioFilePath?.let { uri ->
                                MediaItem.fromUri(uri)
                            }
                        }
                        val listener = object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                if (currentMessageIndex < messages.lastIndex) {
                                    scope.launch {
                                        exoPlayer.playWhenReady = false
                                        delay(messages[currentMessageIndex].defaultDelayMillis)
                                        exoPlayer.seekTo(currentMessageIndex + 1, 0)
                                        exoPlayer.prepare()
                                    }
                                } else {
                                    allowTouch.value = true
                                    exoPlayer.release()
                                    onFinish.invoke()
                                }
                            }

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int
                            ) {
                                if (exoPlayer.currentMediaItemIndex == 0) return
                                currentMessageIndex += 1
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    currentMessageIndex += 1
                                }
                            }
                        }

                        exoPlayer.addListener(listener)
                        exoPlayer.playWhenReady = false
                        if (mediaItems.isNotEmpty()) {
                            exoPlayer.setMediaItems(mediaItems)
                            exoPlayer.prepare()
                        }

                        onDispose {
                            if (exoPlayer.isPlaying) exoPlayer.stop()
                            exoPlayer.removeListener(listener)
                            exoPlayer.clearMediaItems()
                        }
                    }

                    messageContent?.let {
                        Tooltip(
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                bottom = 8.dp
                            ),
                            tooltipPosition = tooltipPosition,
                            caretSize = caretSize,
                            shape = RoundedCornerShape(8.dp),
                            contentColor = Color.Black,
                            containerColor = Color.White,
                            tonalElevation = SpotlightDefaults.tonalElevation,
                            shadowElevation = SpotlightDefaults.shadowElevation,
                            maxWidth = toolTipMaxWidth
                        ) {
                            it()
                        }
                    }
                },
                state = tooltipState
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun rememberTooltipPositionProvider(
    tooltipPosition: TooltipPosition = TooltipPosition.TOP,
    spacingBetweenTooltipAndAnchor: Dp = 4.dp
): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) {
        spacingBetweenTooltipAndAnchor.roundToPx()
    }
    return remember(tooltipAnchorSpacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2

                // Tooltip prefers to be above the anchor,
                // but if this causes the tooltip to overlap with the anchor
                // then we place it below the anchor
                var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                if (tooltipPosition == TooltipPosition.BOTTOM || y < 0) {
                    y = anchorBounds.bottom + tooltipAnchorSpacing
                }
                return IntOffset(x, y)
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun TooltipScope.Tooltip(
    modifier: Modifier,
    tooltipPosition: TooltipPosition,
    caretSize: DpSize,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    maxWidth: Dp,
    content: @Composable () -> Unit
) {
    val customModifier =
        if (caretSize.isSpecified) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            Modifier
                /* Blocked by https://github.com/karya-inc/karya-android-client/issues/1160
                .drawCaret { anchorLayoutCoordinates ->
                    drawCaretWithPath(
                        density,
                        configuration,
                        containerColor,
                        caretSize,
                        tooltipPosition,
                        anchorLayoutCoordinates
                    )
                } */
                .then(modifier)
        } else {
            modifier
        }

    Surface(
        modifier = customModifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = SpotlightDefaults.TooltipMinWidth,
                    maxWidth = maxWidth,
                    minHeight = SpotlightDefaults.TooltipMinHeight
                )
                .padding(SpotlightDefaults.PlainTooltipContentPadding)
        ) {
            val textStyle = MaterialTheme.typography.bodySmall

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

@ExperimentalMaterial3Api
private fun CacheDrawScope.drawCaretWithPath(
    density: Density,
    configuration: Configuration,
    containerColor: Color,
    caretSize: DpSize,
    tooltipPosition: TooltipPosition,
    anchorLayoutCoordinates: LayoutCoordinates?
): DrawResult {
    val path = Path()

    if (anchorLayoutCoordinates != null) {
        val caretHeightPx: Int
        val caretWidthPx: Int
        val screenWidthPx: Int
        val tooltipAnchorSpacing: Int
        with(density) {
            caretHeightPx = caretSize.height.roundToPx()
            caretWidthPx = caretSize.width.roundToPx()
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx()
            tooltipAnchorSpacing = SpotlightDefaults.SpacingBetweenTooltipAndAnchor.roundToPx()
        }
        val anchorBounds = anchorLayoutCoordinates.boundsInWindow()
        val anchorLeft = anchorBounds.left
        val anchorRight = anchorBounds.right
        val anchorTop = anchorBounds.top
        val anchorMid = (anchorRight + anchorLeft) / 2
        val anchorWidth = anchorRight - anchorLeft
        val tooltipWidth = this.size.width
        val tooltipHeight = this.size.height
        val isCaretTop =
            (tooltipPosition == TooltipPosition.BOTTOM) ||
                anchorTop - tooltipHeight - tooltipAnchorSpacing < 0
        val caretY = if (isCaretTop) {
            0f
        } else {
            tooltipHeight
        }

        val position =
            if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
                val anchorMidFromRightScreenEdge =
                    screenWidthPx - anchorMid
                val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                Offset(caretX, caretY)
            } else {
                val tooltipLeft =
                    anchorLeft - (this.size.width / 2 - anchorWidth / 2)
                val caretX = anchorMid - maxOf(tooltipLeft, 0f)
                Offset(caretX, caretY)
            }

        if (isCaretTop) {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y - caretHeightPx)
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        } else {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y + caretHeightPx.toFloat())
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        }
    }

    return onDrawWithContent {
        if (anchorLayoutCoordinates != null) {
            drawContent()
            drawPath(
                path = path,
                color = containerColor
            )
        }
    }
}

/**
 * Simplified SpotlightZone composable for displaying a single text message with optional audio.
 *
 * This overload provides a simpler API for the most common use case of showing a simple text message
 * in the spotlight with optional audio from a raw resource.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param message The text message to display in the tooltip
 * @param modifier Modifier to be applied to the spotlight zone
 * @param audioResId Optional raw resource ID for audio to play with this message
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "profile",
 *     controller = controller,
 *     message = "Tap here for profile",
 *     audioResId = R.raw.profile_audio
 * ) {
 *     ProfileButton()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    message: String,
    modifier: Modifier = Modifier,
    @RawRes audioResId: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val messages = remember(message, audioResId) {
        listOf(
            spotlightMessage {
                content { androidx.compose.material3.Text(message) }
                if (audioResId != null) {
                    audioResource(context, audioResId)
                } else {
                    disableAudio()
                }
            }
        )
    }

    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = messages,
        content = content
    )
}

/**
 * SpotlightZone composable for displaying a single SpotlightMessage.
 *
 * This overload is useful when you want to use a pre-built SpotlightMessage but don't need
 * to specify a list of multiple messages.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param message The SpotlightMessage to display
 * @param modifier Modifier to be applied to the spotlight zone
 * @param tooltipPosition Position of the tooltip relative to the content (TOP or BOTTOM)
 * @param shape Shape of the spotlight cutout
 * @param onFinish Callback invoked when the spotlight sequence is completed
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * val message = spotlightMessage {
 *     content { Text("Welcome!") }
 *     audioResource(context, R.raw.welcome)
 * }
 *
 * SpotlightZone(
 *     key = "welcome",
 *     controller = controller,
 *     message = message
 * ) {
 *     WelcomeContent()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    message: SpotlightMessage,
    modifier: Modifier = Modifier,
    tooltipPosition: TooltipPosition = TooltipPosition.TOP,
    shape: Shape = RectangleShape,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = listOf(message),
        tooltipPosition = tooltipPosition,
        shape = shape,
        onFinish = onFinish,
        content = content
    )
}

/**
 * Configuration class for builder-style SpotlightZone setup.
 *
 * This class allows for a more declarative and flexible way to configure spotlight zones
 * with multiple messages and custom settings.
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "tutorial",
 *     controller = controller,
 *     config = {
 *         message {
 *             content { Text("Step 1") }
 *             audioResource(context, R.raw.step1)
 *         }
 *         message {
 *             content { Text("Step 2") }
 *             audioResource(context, R.raw.step2)
 *         }
 *         tooltipPosition = TooltipPosition.BOTTOM
 *         shape = CircleShape
 *         onFinish = { viewModel.completeTutorial() }
 *     }
 * ) {
 *     TutorialContent()
 * }
 * ```
 */
class SpotlightZoneConfig {
    /**
     * List of messages to display in sequence for this spotlight zone.
     */
    var messages: List<SpotlightMessage> = emptyList()
        private set

    /**
     * Position of the tooltip relative to the spotlight zone.
     */
    var tooltipPosition: TooltipPosition = TooltipPosition.TOP

    /**
     * Whether touch input should be disabled when this zone is spotlighted.
     */
    var disableTouch: Boolean = false

    /**
     * Shape of the spotlight cutout.
     */
    var shape: Shape = RectangleShape

    /**
     * Callback invoked when all messages have been displayed.
     */
    var onFinish: () -> Unit = {}

    /**
     * Adds a single message using the builder pattern.
     *
     * @param block Configuration lambda for the SpotlightMessageBuilder
     */
    fun message(block: SpotlightMessageBuilder.() -> Unit) {
        messages = messages + spotlightMessage(block)
    }

    /**
     * Sets multiple messages at once.
     *
     * @param msgs Variable number of SpotlightMessage instances
     */
    fun messages(vararg msgs: SpotlightMessage) {
        messages = msgs.toList()
    }
}

/**
 * SpotlightZone composable with builder-style configuration.
 *
 * This overload provides the most flexible API using a configuration DSL that allows
 * for declarative setup of multiple messages and settings.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param modifier Modifier to be applied to the spotlight zone
 * @param config Configuration lambda applied to SpotlightZoneConfig
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "feature_tour",
 *     controller = controller,
 *     config = {
 *         message {
 *             content { Text("Welcome to the new feature!") }
 *             audioResource(context, R.raw.intro)
 *             delay(5000)
 *         }
 *         message {
 *             content { Text("Tap to continue") }
 *             disableAudio()
 *         }
 *         tooltipPosition = TooltipPosition.BOTTOM
 *         shape = RoundedCornerShape(16.dp)
 *         onFinish = { analytics.trackTourComplete() }
 *     }
 * ) {
 *     FeatureButton()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    modifier: Modifier = Modifier,
    config: SpotlightZoneConfig.() -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = remember(key) { SpotlightZoneConfig().apply(config) }

    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = configuration.messages,
        tooltipPosition = configuration.tooltipPosition,
        disableTouch = configuration.disableTouch,
        shape = configuration.shape,
        onFinish = configuration.onFinish,
        content = content
    )
}

enum class TooltipPosition { TOP, BOTTOM }
