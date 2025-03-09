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

package com.android.permissioncontroller.permission.ui.wear.elements.material3

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import com.android.permissioncontroller.permission.ui.wear.elements.chipDefaultColors
import com.android.permissioncontroller.permission.ui.wear.elements.chipDisabledColors
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButtonStyle.DisabledLike
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButtonStyle.Primary
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButtonStyle.Secondary
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButtonStyle.Transparent

/**
 * This component is wrapper on material control colors, It applies the right colors based material
 * ui version.
 */
enum class WearPermissionButtonStyle {
    Primary,
    Secondary,
    Transparent,
    DisabledLike,
}

@Composable
internal fun WearPermissionButtonStyle.material2ChipColors(): ChipColors {
    return when (this) {
        Primary -> chipDefaultColors()
        Secondary -> ChipDefaults.secondaryChipColors()
        Transparent -> ChipDefaults.childChipColors()
        DisabledLike -> chipDisabledColors()
    }
}

@Composable
internal fun WearPermissionButtonStyle.material3ButtonColors(): ButtonColors {
    return when (this) {
        Primary -> ButtonDefaults.buttonColors()
        Secondary -> ButtonDefaults.filledTonalButtonColors()
        Transparent -> ButtonDefaults.childButtonColors()
        DisabledLike -> ButtonDefaults.disabledLikeColors()
    }
}

@Composable
private fun ButtonDefaults.disabledLikeColors() =
    filledTonalButtonColors().run {
        ButtonColors(
            containerPainter = disabledContainerPainter,
            contentColor = disabledContentColor,
            secondaryContentColor = disabledSecondaryContentColor,
            iconColor = disabledIconColor,
            disabledContainerPainter = disabledContainerPainter,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )
    }
