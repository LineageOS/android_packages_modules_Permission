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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import com.android.permissioncontroller.permission.ui.wear.elements.ListFooter
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion

/** This component is creates a transparent styled button to use as a list footer. */
@Composable
fun WearPermissionListFooter(
    materialUIVersion: WearPermissionMaterialUIVersion,
    label: String,
    iconBuilder: WearPermissionIconBuilder? = null,
    onClick: (() -> Unit) = {},
) {
    if (materialUIVersion == WearPermissionMaterialUIVersion.MATERIAL2_5) {
        ListFooter(
            description = label,
            iconRes = iconBuilder?.let { it.iconResource as Int },
            onClick = onClick,
        )
    } else {
        WearPermissionButtonInternal(
            iconBuilder = iconBuilder,
            secondaryLabel = label,
            secondaryLabelMaxLines = Int.MAX_VALUE,
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.childButtonColors(),
            requiresMinimumHeight = false,
        )
    }
}
