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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.Text
import com.android.permissioncontroller.permission.ui.wear.elements.Chip
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion

/**
 * This component is wrapper on material Button component
 * 1. It takes icon, primary, secondary label resources and construct them applying permission app
 *    defaults
 */
@Composable
fun WearPermissionButton(
    label: String,
    modifier: Modifier = Modifier,
    materialUIVersion: WearPermissionMaterialUIVersion =
        WearPermissionMaterialUIVersion.MATERIAL2_5,
    iconBuilder: WearPermissionIconBuilder? = null,
    labelMaxLines: Int? = null,
    secondaryLabel: String? = null,
    secondaryLabelMaxLines: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: WearPermissionButtonStyle = WearPermissionButtonStyle.Secondary,
) {
    if (materialUIVersion == WearPermissionMaterialUIVersion.MATERIAL2_5) {
        Chip(
            label = label,
            labelMaxLines = labelMaxLines,
            onClick = onClick,
            modifier = modifier,
            secondaryLabel = secondaryLabel,
            secondaryLabelMaxLines = secondaryLabelMaxLines,
            icon = { iconBuilder?.build() },
            largeIcon = false,
            colors = style.material2ChipColors(),
            enabled = enabled,
        )
    } else {
        WearPermissionButtonInternal(
            iconBuilder = iconBuilder,
            label = label,
            labelMaxLines = labelMaxLines,
            secondaryLabel = secondaryLabel,
            secondaryLabelMaxLines = secondaryLabelMaxLines,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = style.material3ButtonColors(),
        )
    }
}

@Composable
internal fun WearPermissionButtonInternal(
    modifier: Modifier = Modifier,
    label: String? = null,
    iconBuilder: WearPermissionIconBuilder? = null,
    labelMaxLines: Int? = null,
    secondaryLabel: String? = null,
    secondaryLabelMaxLines: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    requiresMinimumHeight: Boolean = true,
) {
    val minHeight: Dp =
        if (requiresMinimumHeight) {
            0.dp
        } else {
            1.dp
        }
    val iconParam: (@Composable BoxScope.() -> Unit)? = iconBuilder?.let { { it.build() } }
    val labelParam: (@Composable RowScope.() -> Unit)? =
        label?.let {
            {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = labelMaxLines ?: LocalTextConfiguration.current.maxLines,
                    style =
                        LocalTextStyle.current.copy(
                            fontWeight = FontWeight.W600,
                            hyphens = Hyphens.Auto,
                        ),
                )
            }
        }

    val secondaryLabelParam: (@Composable RowScope.() -> Unit)? =
        secondaryLabel?.let {
            {
                Text(
                    text = secondaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = secondaryLabelMaxLines ?: LocalTextConfiguration.current.maxLines,
                )
            }
        }

    Button(
        icon = iconParam,
        label = labelParam ?: {},
        secondaryLabel = secondaryLabelParam,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.requiredSizeIn(minHeight = minHeight).fillMaxWidth(),
        contentPadding = contentPadding,
        colors = colors,
    )
}
