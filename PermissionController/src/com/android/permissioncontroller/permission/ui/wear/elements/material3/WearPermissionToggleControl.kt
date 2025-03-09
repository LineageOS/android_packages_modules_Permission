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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.android.permissioncontroller.permission.ui.wear.elements.ToggleChip
import com.android.permissioncontroller.permission.ui.wear.elements.ToggleChipToggleControl
import com.android.permissioncontroller.permission.ui.wear.elements.toggleControlSemantics
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion

/**
 * The custom component is a wrapper on different material3 toggle controls.
 * 1. It provides an unified interface for RadioButton,CheckButton and SwitchButton.
 * 2. It takes icon, primary, secondary label resources and construct them applying permission app
 *    defaults
 * 3. Applies custom semantics for based on the toggle control type
 */
@Composable
fun WearPermissionToggleControl(
    toggleControl: ToggleChipToggleControl,
    label: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelMaxLines: Int? = null,
    materialUIVersion: WearPermissionMaterialUIVersion =
        WearPermissionMaterialUIVersion.MATERIAL2_5,
    iconBuilder: WearPermissionIconBuilder? = null,
    secondaryLabel: String? = null,
    secondaryLabelMaxLines: Int? = null,
    enabled: Boolean = true,
    style: WearPermissionToggleControlStyle = WearPermissionToggleControlStyle.Default,
) {
    if (materialUIVersion == WearPermissionMaterialUIVersion.MATERIAL2_5) {
        ToggleChip(
            toggleControl = toggleControl,
            label = label,
            labelMaxLine = labelMaxLines,
            checked = checked,
            onCheckedChanged = onCheckedChanged,
            modifier = modifier,
            icon = iconBuilder?.iconResource,
            secondaryLabel = secondaryLabel,
            secondaryLabelMaxLine = secondaryLabelMaxLines,
            enabled = enabled,
            colors = style.material2ToggleControlColors(),
        )
    } else {
        WearPermissionToggleControlInternal(
            label = label,
            toggleControl = toggleControl,
            checked = checked,
            onCheckedChanged = onCheckedChanged,
            modifier = modifier,
            iconBuilder = iconBuilder,
            labelMaxLines = labelMaxLines,
            secondaryLabel = secondaryLabel,
            secondaryLabelMaxLines = secondaryLabelMaxLines,
            enabled = enabled,
            style = style,
        )
    }
}

@Composable
private fun WearPermissionToggleControlInternal(
    label: String,
    toggleControl: ToggleChipToggleControl,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconBuilder: WearPermissionIconBuilder? = null,
    labelMaxLines: Int? = null,
    secondaryLabel: String? = null,
    secondaryLabelMaxLines: Int? = null,
    enabled: Boolean = true,
    style: WearPermissionToggleControlStyle = WearPermissionToggleControlStyle.Default,
) {
    val labelParam: (@Composable RowScope.() -> Unit) = {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            maxLines = labelMaxLines ?: LocalTextConfiguration.current.maxLines,
        )
    }

    val secondaryLabelParam: (@Composable RowScope.() -> Unit)? =
        secondaryLabel?.let {
            {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = secondaryLabelMaxLines ?: LocalTextConfiguration.current.maxLines,
                )
            }
        }

    val iconParam: (@Composable BoxScope.() -> Unit)? = iconBuilder?.let { { it.build() } }

    val updatedModifier =
        modifier
            .fillMaxWidth()
            // .heightIn(min = 58.dp) // TODO(b/370783358): This should be a overlaid value
            .toggleControlSemantics(toggleControl, checked)

    when (toggleControl) {
        ToggleChipToggleControl.Radio ->
            RadioButton(
                selected = checked,
                onSelect = { onCheckedChanged(true) },
                modifier = updatedModifier,
                enabled = enabled,
                icon = iconParam,
                secondaryLabel = secondaryLabelParam,
                label = labelParam,
                colors = style.radioButtonColorScheme(),
            )

        ToggleChipToggleControl.Checkbox ->
            CheckboxButton(
                checked = checked,
                onCheckedChange = onCheckedChanged,
                modifier = updatedModifier,
                enabled = enabled,
                icon = iconParam,
                secondaryLabel = secondaryLabelParam,
                label = labelParam,
                colors = style.checkboxColorScheme(),
            )

        ToggleChipToggleControl.Switch ->
            SwitchButton(
                checked = checked,
                onCheckedChange = onCheckedChanged,
                modifier = updatedModifier,
                enabled = enabled,
                icon = iconParam,
                secondaryLabel = secondaryLabelParam,
                label = labelParam,
                colors = style.switchButtonColorScheme(),
            )
    }
}
