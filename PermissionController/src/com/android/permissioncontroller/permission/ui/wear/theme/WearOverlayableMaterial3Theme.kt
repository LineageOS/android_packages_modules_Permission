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
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Theme wrapper providing Material 3 styling while maintaining compatibility with Runtime Resource
 * Overlay (RRO).
 *
 * Uses the tonal palette from the previous Material Design version until dynamic color tokens are
 * available in SDK 36.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal class WearOverlayableMaterial3Theme(context: Context) {
    val colorScheme =
        if (Build.VERSION.SDK_INT >= 36) {
            WearComposeMaterial3ColorScheme.dynamicColorScheme(context)
        } else {
            WearComposeMaterial3ColorScheme.tonalColorScheme(context)
        }

    val typography = WearComposeMaterial3Typography.dynamicTypography(context)

    val shapes = WearComposeMaterial3Shapes.dynamicShapes(context)
}
