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
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import androidx.wear.compose.material3.MaterialTheme as Material3Theme
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion.MATERIAL2_5
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion.MATERIAL3

/** This enum is used to specify the material version used for a specific screen */
enum class WearPermissionMaterialUIVersion {
    MATERIAL2_5,
    MATERIAL3,
}

/**
 * Supports both Material 3 and Material 2_5 theme. default version for permission theme will be 2_5
 * until we migrate enough screens to 3. 2_5 version will use material 3 overlay resources if we
 * enable material3 for even one screen (Permission screens will be migrated in phases).
 */
@Composable
fun WearPermissionTheme(
    version: WearPermissionMaterialUIVersion = MATERIAL2_5,
    content: @Composable () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        WearPermissionLegacyTheme(content)
    } else {
        // Whether we are ready to use material3 for any screen.
        val useBridgedTheme = ResourceHelper.material3Enabled

        // Material3 UI controls are still being used in the screen that the theme is applied
        if (version == MATERIAL3) {
            val material3Theme = WearOverlayableMaterial3Theme(LocalContext.current)
            Material3Theme(
                colorScheme = material3Theme.colorScheme,
                typography = material3Theme.typography,
                shapes = material3Theme.shapes,
                content = content,
            )
        }
        // Material2_5 UI controls are still being used in the screen that the theme is applied,
        // But some in-app screens(like permission grant screen) are migrated to material3.
        // To avoid having two set of overlay resources, we will use material3 overlay resources to
        // support material2_5 UI controls as well.
        else if (version == MATERIAL2_5 && useBridgedTheme) {
            val material3Theme = WearOverlayableMaterial3Theme(LocalContext.current)
            val bridgedLegacyTheme = WearMaterialBridgedLegacyTheme.createFrom(material3Theme)
            MaterialTheme(
                colors = bridgedLegacyTheme.colors,
                typography = bridgedLegacyTheme.typography,
                shapes = bridgedLegacyTheme.shapes,
                content = content,
            )
        }
        // We are not ready for material3 yet in any screens.
        else {
            WearPermissionLegacyTheme(content)
        }
    }
}

/**
 * The Material 2.5 Theme Wrapper for Supporting RRO with legacy resources. This theme is kept here
 * for backward compatibility. When grant screen is updated to material3 will clean up legacy
 * resources.
 */
@Composable
fun WearPermissionLegacyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            overlayColors(context)
                .copy(error = MaterialTheme.colors.error, onError = MaterialTheme.colors.onError)
        } else {
            MaterialTheme.colors
        }
    MaterialTheme(colors = colors, typography = deviceDefaultTypography(context), content = content)
}

/**
 * Creates a dynamic color maps that can be overlaid. 100 - Lightest shade; 0 - Darkest Shade; In
 * wear we only support dark theme for the time being. Thus the fill colors and variants are dark
 * and anything on top is light. We will use this custom redirection until wear compose material
 * supports color scheming.
 *
 * The mapping is best case match on wear material color tokens from
 * /android/clockwork/common/wearable/wearmaterial/color/res/values/color-tokens.xml
 *
 * @param context The context required to get system resource data.
 */
@RequiresApi(Build.VERSION_CODES.S)
@VisibleForTesting
internal fun overlayColors(context: Context): Colors {
    val tonalPalette = dynamicTonalPalette(context)
    return Colors(
        background = Color.Black,
        onBackground = Color.White,
        primary = tonalPalette.primary90,
        primaryVariant = tonalPalette.primary80,
        onPrimary = tonalPalette.primary10,
        secondary = tonalPalette.tertiary90,
        secondaryVariant = tonalPalette.tertiary60,
        onSecondary = tonalPalette.tertiary10,
        surface = tonalPalette.neutral20,
        onSurface = tonalPalette.neutral95,
        onSurfaceVariant = tonalPalette.neutralVariant80,
    )
}

private fun fontFamily(context: Context, @StringRes id: Int): FontFamily {
    val typefaceName = context.resources.getString(id)
    val font = Font(familyName = DeviceFontFamilyName(typefaceName))
    return FontFamily(font)
}

/*
 Only customizes font family. The material 3 roles to 2.5 are mapped to the best case matching of
 google3/java/com/google/android/wearable/libraries/compose/theme/GoogleMaterialTheme.kt
*/
internal fun deviceDefaultTypography(context: Context): Typography {
    val defaultTypography = Typography()
    return Typography(
        display1 =
            defaultTypography.display1.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_display_1_font_family)
            ),
        display2 =
            defaultTypography.display2.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_display_2_font_family)
            ),
        display3 =
            defaultTypography.display3.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_display_3_font_family)
            ),
        title1 =
            defaultTypography.title1.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_title_1_font_family)
            ),
        title2 =
            defaultTypography.title2.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_title_2_font_family)
            ),
        title3 =
            defaultTypography.title3.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_title_3_font_family)
            ),
        body1 =
            defaultTypography.body1.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_body_1_font_family)
            ),
        body2 =
            defaultTypography.body2.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_body_2_font_family)
            ),
        button =
            defaultTypography.button.copy(
                fontFamily = fontFamily(context, R.string.wear_material_compose_button_font_family)
            ),
        caption1 =
            defaultTypography.caption1.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_caption_1_font_family)
            ),
        caption2 =
            defaultTypography.caption2.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_caption_2_font_family)
            ),
        caption3 =
            defaultTypography.caption3.copy(
                fontFamily =
                    fontFamily(context, R.string.wear_material_compose_caption_3_font_family)
            ),
    )
}
