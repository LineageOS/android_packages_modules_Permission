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
import androidx.annotation.DimenRes
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Shapes
import com.android.permissioncontroller.R

// TODO(b/324928718): Use system defined symbols.
internal object WearComposeMaterial3Shapes {
    private fun CornerBasedShape.updatedShape(
        context: Context,
        @DimenRes cornerSizeRes: Int,
    ): CornerBasedShape {
        val size = ResourceHelper.getDimen(context, cornerSizeRes)?.dp ?: return this
        return copy(CornerSize(size))
    }

    fun dynamicShapes(context: Context): Shapes {
        val defaultShapes = Shapes()
        return Shapes(
            extraLarge =
                defaultShapes.extraLarge.updatedShape(
                    context,
                    R.dimen.wear_compose_material3_shape_corner_extra_large_size,
                ),
            large =
                defaultShapes.large.updatedShape(
                    context,
                    R.dimen.wear_compose_material3_shape_corner_large_size,
                ),
            medium =
                defaultShapes.medium.updatedShape(
                    context,
                    R.dimen.wear_compose_material3_shape_corner_medium_size,
                ),
            small =
                defaultShapes.small.updatedShape(
                    context,
                    R.dimen.wear_compose_material3_shape_corner_small_size,
                ),
            extraSmall =
                defaultShapes.extraSmall.updatedShape(
                    context,
                    R.dimen.wear_compose_material3_shape_corner_extra_small_size,
                ),
        )
    }
}
