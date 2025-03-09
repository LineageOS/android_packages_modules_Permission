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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Shapes
import androidx.wear.compose.material.Typography

/**
 * This exists to support Permission Controller screens that may still use Material 2.5 components
 * to maintain consistency with the settings screens.
 *
 * However to avoid maintaining two sets of resources for overlays, this class construct 2.5 theme
 * from 3.0
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal class WearMaterialBridgedLegacyTheme
private constructor(newTheme: WearOverlayableMaterial3Theme) {

    val colors =
        newTheme.colorScheme.run {
            Colors(
                background = background,
                onBackground = onBackground,
                primary = onPrimaryContainer, // primary90
                primaryVariant = primaryDim, // primary80
                onPrimary = onPrimary, // primary10
                secondary = tertiary, // Tertiary90
                secondaryVariant = tertiaryDim, // Tertiary60 - Tertiary80 BestFit.
                onSecondary = onTertiary, // Tertiary10
                surface = surfaceContainer, // neutral20
                onSurface = onSurface, // neutral95
                onSurfaceVariant = onSurfaceVariant, // neutralVariant80
            )
        }

    // Based on:
    // Material 2:
    // wear/compose/compose-material/src/main/java/androidx/wear/compose/material/Typography.kt
    // Material 3:
    // wear/compose/compose-material3/src/main/java/androidx/wear/compose/material3/tokens/TypeScaleTokens.kt
    val typography =
        newTheme.typography.run {
            Typography(
                display1 = displayLarge, // 40.sp
                display2 = displayMedium.copy(fontSize = 34.sp, lineHeight = 40.sp),
                display3 = displayMedium, // 30.sp
                title1 = displaySmall, // 24.sp
                title2 = titleLarge, // 20.sp
                title3 = titleMedium, // 16.sp
                body1 = bodyLarge, // 16.sp
                body2 = bodyMedium, // 14.sp
                caption1 = bodyMedium, // 14.sp
                caption2 = bodySmall, // 12.sp
                caption3 = bodyExtraSmall, // 10.sp
                button = labelMedium, // 15.sp
            )
        }

    val shapes = newTheme.shapes.run { Shapes(large = large, medium = medium, small = small) }

    companion object {
        fun createFrom(newTheme: WearOverlayableMaterial3Theme) =
            WearMaterialBridgedLegacyTheme(newTheme)
    }
}
