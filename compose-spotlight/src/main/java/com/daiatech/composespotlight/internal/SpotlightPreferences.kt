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

package com.daiatech.composespotlight.internal

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

/**
 * The shared pref is called "compose-spotlight"
 * @param context the App Context
 * @property sharedPreferences The SharedPreferences, defaults to context.getSharedPreferences(...)
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SpotlightPreferences(
    context: Context,
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("compose-spotlight", Context.MODE_PRIVATE),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) {

    suspend fun getValue(
        key: String,
        defaultValue: Any?
    ): Any? {
        val value: Any?
        withContext(dispatcher) {
            value = sharedPreferences.all[key] ?: defaultValue
        }
        return value
    }

    suspend fun getAll(): Map<String, Any?> {
        val valueMap: Map<String, Any?>
        withContext(dispatcher) {
            valueMap = sharedPreferences.all.toMap()
        }
        return valueMap
    }

    suspend fun setValue(
        key: String,
        value: Any
    ) {
        val edit = sharedPreferences.edit()

        withContext(dispatcher) {
            when (value) {
                is Boolean -> {
                    edit.putBoolean(key, value)
                }

                is String -> {
                    edit.putString(key, value)
                }

                is Float -> {
                    edit.putFloat(key, value)
                }

                is Long -> {
                    edit.putLong(key, value)
                }

                is Int -> {
                    edit.putInt(key, value)
                }
            }

            edit.commit()
        }
    }

    suspend fun setStringList(key: String, list: List<String>) {
        val edit = sharedPreferences.edit()

        withContext(dispatcher) {
            edit.putString(key, list.joinToString(";"))

            edit.commit()
        }
    }

    suspend fun getStringList(key: String, default: List<String> = emptyList()): List<String> {
        return withContext(dispatcher) {
            val value = sharedPreferences.getString(key, null)
            if (!value.isNullOrBlank()) {
                value.split(";")
            } else {
                default
            }
        }
    }

    suspend fun remove(key: String) {
        val edit = sharedPreferences.edit()

        withContext(dispatcher) {
            edit.remove(key)

            edit.apply()
        }
    }

    suspend fun clear(except: List<String>) {
        val edit = sharedPreferences.edit()

        withContext(dispatcher) {
            val it = sharedPreferences.all.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                if (!except.contains(entry.key)) {
                    edit.remove(entry.key)
                }
            }

            edit.apply()
        }
    }
}
