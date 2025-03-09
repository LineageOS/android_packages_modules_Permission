/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.permissioncontroller.permission.ui.wear.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.android.permissioncontroller.R

enum class ToggleChipToggleControl {
    Switch,
    Radio,
    Checkbox,
}

@Composable
fun Modifier.toggleControlSemantics(
    toggleControl: ToggleChipToggleControl,
    checked: Boolean,
): Modifier {
    val semanticsRole =
        when (toggleControl) {
            ToggleChipToggleControl.Switch -> Role.Switch
            ToggleChipToggleControl.Radio -> Role.RadioButton
            ToggleChipToggleControl.Checkbox -> Role.Checkbox
        }
    val stateDescriptionSemantics =
        stringResource(
            if (checked) {
                R.string.on
            } else {
                R.string.off
            }
        )

    return semantics {
        role = semanticsRole
        stateDescription = stateDescriptionSemantics
    }
}
