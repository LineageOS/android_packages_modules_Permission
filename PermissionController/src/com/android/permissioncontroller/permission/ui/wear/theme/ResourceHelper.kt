/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.permissioncontroller.permission.ui.wear.theme

import android.content.Context
import android.os.SystemProperties
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DoNotInline
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

internal object ResourceHelper {

    private const val MATERIAL3_ENABLED_SYSPROP = "persist.cw_build.bluechip.enabled"

    val material3Enabled: Boolean
        get() {
            return SystemProperties.getBoolean(MATERIAL3_ENABLED_SYSPROP, false)
        }

    @DoNotInline
    fun getColor(context: Context, @ColorRes id: Int): Color? {
        return try {
            val colorInt = context.resources.getColor(id, context.theme)
            Color(colorInt)
        } catch (e: Exception) {
            null
        }
    }

    @DoNotInline
    fun getString(context: Context, @StringRes id: Int): String? {
        return try {
            context.resources.getString(id)
        } catch (e: Exception) {
            null
        }
    }

    @DoNotInline
    fun getDimen(context: Context, @DimenRes id: Int): Float? {
        return try {
            context.resources.getDimension(id) / context.resources.displayMetrics.density
        } catch (e: Exception) {
            null
        }
    }
}
