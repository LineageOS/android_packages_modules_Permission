/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.permissioncontroller.permission.ui.wear.elements.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.ToggleChipColors
import androidx.wear.compose.material.ToggleChipDefaults.toggleChipColors
import androidx.wear.compose.material3.CheckboxButtonColors
import androidx.wear.compose.material3.CheckboxButtonDefaults.checkboxButtonColors
import androidx.wear.compose.material3.RadioButtonColors
import androidx.wear.compose.material3.RadioButtonDefaults.radioButtonColors
import androidx.wear.compose.material3.SwitchButtonColors
import androidx.wear.compose.material3.SwitchButtonDefaults.switchButtonColors
import com.android.permissioncontroller.permission.ui.wear.elements.toggleChipBackgroundColors
import com.android.permissioncontroller.permission.ui.wear.elements.toggleChipDisabledColors

/**
 * Defines toggle control styles, It helps in setting the right colors scheme to a toggle control.
 */
enum class WearPermissionToggleControlStyle {
    Default,
    Transparent,
    DisabledLike,
}

@Composable
internal fun WearPermissionToggleControlStyle.radioButtonColorScheme(): RadioButtonColors {
    return when (this) {
        WearPermissionToggleControlStyle.Default -> radioButtonColors()
        WearPermissionToggleControlStyle.Transparent -> radioButtonTransparentColors()
        WearPermissionToggleControlStyle.DisabledLike -> radioButtonDisabledLikeColors()
    }
}

@Composable
internal fun WearPermissionToggleControlStyle.checkboxColorScheme(): CheckboxButtonColors {
    return when (this) {
        WearPermissionToggleControlStyle.Default -> checkboxButtonColors()
        WearPermissionToggleControlStyle.Transparent -> checkButtonTransparentColors()
        WearPermissionToggleControlStyle.DisabledLike -> checkboxDisabledLikeColors()
    }
}

@Composable
internal fun WearPermissionToggleControlStyle.switchButtonColorScheme(): SwitchButtonColors {
    return when (this) {
        WearPermissionToggleControlStyle.Default -> switchButtonColors()
        WearPermissionToggleControlStyle.Transparent -> switchButtonTransparentColors()
        WearPermissionToggleControlStyle.DisabledLike -> switchButtonDisabledLikeColors()
    }
}

@Composable
internal fun WearPermissionToggleControlStyle.material2ToggleControlColors(): ToggleChipColors {
    return when (this) {
        WearPermissionToggleControlStyle.Default -> toggleChipColors()
        WearPermissionToggleControlStyle.Transparent -> toggleChipBackgroundColors()
        WearPermissionToggleControlStyle.DisabledLike -> toggleChipDisabledColors()
    }
}

@Composable
private fun checkButtonTransparentColors() =
    checkboxButtonColors(
        checkedContainerColor = Color.Transparent,
        uncheckedContainerColor = Color.Transparent,
        disabledCheckedContainerColor = Color.Transparent,
        disabledUncheckedContainerColor = Color.Transparent,
    )

@Composable
private fun radioButtonTransparentColors() =
    radioButtonColors(
        selectedContainerColor = Color.Transparent,
        unselectedContainerColor = Color.Transparent,
        disabledSelectedContainerColor = Color.Transparent,
        disabledUnselectedContainerColor = Color.Transparent,
    )

@Composable
private fun switchButtonTransparentColors() =
    switchButtonColors(
        checkedContainerColor = Color.Transparent,
        uncheckedContainerColor = Color.Transparent,
        disabledCheckedContainerColor = Color.Transparent,
        disabledUncheckedContainerColor = Color.Transparent,
    )

@Composable
private fun checkboxDisabledLikeColors(): CheckboxButtonColors {
    val defaultColors = checkboxButtonColors()
    return checkboxButtonColors(
        checkedContainerColor = defaultColors.disabledCheckedContainerColor,
        checkedContentColor = defaultColors.disabledCheckedContentColor,
        checkedSecondaryContentColor = defaultColors.disabledCheckedSecondaryContentColor,
        checkedIconColor = defaultColors.disabledCheckedIconColor,
        checkedBoxColor = defaultColors.disabledCheckedBoxColor,
        checkedCheckmarkColor = defaultColors.disabledCheckedCheckmarkColor,
        uncheckedContainerColor = defaultColors.disabledUncheckedContainerColor,
        uncheckedContentColor = defaultColors.disabledUncheckedContentColor,
        uncheckedSecondaryContentColor = defaultColors.disabledUncheckedSecondaryContentColor,
        uncheckedIconColor = defaultColors.disabledUncheckedIconColor,
        uncheckedBoxColor = defaultColors.disabledUncheckedBoxColor,
    )
}

@Composable
private fun radioButtonDisabledLikeColors(): RadioButtonColors {
    val defaultColors = radioButtonColors()
    return radioButtonColors(
        selectedContainerColor = defaultColors.disabledSelectedContainerColor,
        selectedContentColor = defaultColors.disabledSelectedContentColor,
        selectedSecondaryContentColor = defaultColors.disabledSelectedSecondaryContentColor,
        selectedIconColor = defaultColors.disabledSelectedIconColor,
        selectedControlColor = defaultColors.disabledSelectedControlColor,
        unselectedContentColor = defaultColors.disabledUnselectedContentColor,
        unselectedContainerColor = defaultColors.disabledUnselectedContainerColor,
        unselectedSecondaryContentColor = defaultColors.disabledUnselectedSecondaryContentColor,
        unselectedIconColor = defaultColors.disabledUnselectedIconColor,
        unselectedControlColor = defaultColors.disabledUnselectedControlColor,
    )
}

@Composable
private fun switchButtonDisabledLikeColors(): SwitchButtonColors {
    val defaultColors = switchButtonColors()
    return switchButtonColors(
        checkedContainerColor = defaultColors.disabledCheckedContainerColor,
        checkedContentColor = defaultColors.disabledCheckedContentColor,
        checkedSecondaryContentColor = defaultColors.disabledCheckedSecondaryContentColor,
        checkedIconColor = defaultColors.disabledCheckedIconColor,
        checkedThumbColor = defaultColors.disabledCheckedThumbColor,
        checkedThumbIconColor = defaultColors.disabledCheckedThumbIconColor,
        checkedTrackColor = defaultColors.disabledCheckedTrackColor,
        checkedTrackBorderColor = defaultColors.disabledCheckedTrackBorderColor,
        uncheckedContainerColor = defaultColors.disabledUncheckedContainerColor,
        uncheckedContentColor = defaultColors.disabledUncheckedContentColor,
        uncheckedSecondaryContentColor = defaultColors.disabledUncheckedSecondaryContentColor,
        uncheckedIconColor = defaultColors.disabledUncheckedIconColor,
        uncheckedThumbColor = defaultColors.disabledUncheckedThumbColor,
        uncheckedTrackColor = defaultColors.checkedTrackColor.run { copy(alpha = alpha * 0.12f) },
        uncheckedTrackBorderColor = defaultColors.disabledUncheckedTrackBorderColor,
    )
}
