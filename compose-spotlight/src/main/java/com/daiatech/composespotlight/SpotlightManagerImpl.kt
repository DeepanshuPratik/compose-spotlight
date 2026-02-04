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

import android.content.Context
import com.daiatech.composespotlight.internal.SpotlightControllerImpl
import com.daiatech.composespotlight.internal.SpotlightPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Implementation of [SpotlightManager].
 * Sets up [SpotlightPreferences] and remote config flags.
 * Provides [SpotlightControllerImpl] via [createController].
 * [isSpotlightEnabledRemotely] and [isOnboardingEnabledRemotely] are remote config flags.
 */
class SpotlightManagerImpl
private constructor(
    private val spotlightPreferences: SpotlightPreferences,
    override val isSpotlightEnabledRemotely: Flow<Boolean>,
    override val isOnboardingEnabledRemotely: Flow<Boolean>
) : SpotlightManager {
    override fun createController(): SpotlightController {
        return SpotlightControllerImpl(spotlightPreferences)
    }

    companion object {
        private val isSpotlightEnabledRemotelyDefault = flow { emit(true) }.flowOn(Dispatchers.IO)
        private val isOnboardingEnabledRemotelyDefault = flow { emit(false) }.flowOn(Dispatchers.IO)

        fun create(
            context: Context,
            isSpotlightEnabledRemotely: Flow<Boolean> = isSpotlightEnabledRemotelyDefault,
            isOnboardingEnabledRemotely: Flow<Boolean> = isOnboardingEnabledRemotelyDefault
        ): SpotlightManager {
            return SpotlightManagerImpl(
                SpotlightPreferences(context),
                isSpotlightEnabledRemotely,
                isOnboardingEnabledRemotely
            )
        }
    }
}
