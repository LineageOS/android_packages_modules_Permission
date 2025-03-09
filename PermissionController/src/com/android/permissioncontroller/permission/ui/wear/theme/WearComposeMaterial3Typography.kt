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
import androidx.annotation.StringRes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import com.android.permissioncontroller.R

internal object WearComposeMaterial3Typography {

    private const val DEVICE_DEFAULT_FLEX_FONT_TYPE = "font-family-flex-device-default"

    fun fontFamily(
        context: Context,
        @StringRes id: Int,
        variationSettings: FontVariation.Settings? = null,
    ): FontFamily {
        val typefaceName = ResourceHelper.getString(context, id) ?: DEVICE_DEFAULT_FLEX_FONT_TYPE

        val font =
            if (variationSettings != null) {
                Font(
                    familyName = DeviceFontFamilyName(typefaceName),
                    variationSettings = variationSettings,
                )
            } else {
                Font(familyName = DeviceFontFamilyName(typefaceName))
            }
        return FontFamily(font)
    }

    private fun TextStyle.updatedTextStyle(
        context: Context,
        @StringRes fontRes: Int,
        variationSettings: FontVariation.Settings? = null,
        @DimenRes fontSizeRes: Int,
    ): TextStyle {

        val fontFamily =
            fontFamily(context = context, id = fontRes, variationSettings = variationSettings)
        val fontSize = ResourceHelper.getDimen(context = context, id = fontSizeRes)?.sp ?: fontSize

        return copy(fontFamily = fontFamily, fontSize = fontSize)
    }

    fun dynamicTypography(context: Context): Typography {
        val defaultTypography = Typography()
        return Typography(
            arcLarge =
                defaultTypography.arcLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_arc_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_arc_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.ArcLargeVariationSettings,
                ),
            arcMedium =
                defaultTypography.arcMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_arc_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_arc_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.ArcMediumVariationSettings,
                ),
            arcSmall =
                defaultTypography.arcSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_arc_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_arc_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.ArcSmallVariationSettings,
                ),
            bodyLarge =
                defaultTypography.bodyLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_body_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_body_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.BodyLargeVariationSettings,
                ),
            bodyMedium =
                defaultTypography.bodyMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_body_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_body_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.BodyMediumVariationSettings,
                ),
            bodySmall =
                defaultTypography.bodySmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_body_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_body_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.BodySmallVariationSettings,
                ),
            bodyExtraSmall =
                defaultTypography.bodyExtraSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_body_extra_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_body_extra_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.BodyExtraSmallVariationSettings,
                ),
            displayLarge =
                defaultTypography.displayLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_display_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_display_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.DisplayLargeVariationSettings,
                ),
            displayMedium =
                defaultTypography.displayMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_display_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_display_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.DisplayMediumVariationSettings,
                ),
            displaySmall =
                defaultTypography.displaySmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_display_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_display_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.DisplaySmallVariationSettings,
                ),
            labelLarge =
                defaultTypography.labelLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_label_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_label_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.LabelLargeVariationSettings,
                ),
            labelMedium =
                defaultTypography.labelMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_label_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_label_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.LabelMediumVariationSettings,
                ),
            labelSmall =
                defaultTypography.labelSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_label_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_label_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.LabelSmallVariationSettings,
                ),
            numeralExtraLarge =
                defaultTypography.numeralExtraLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_numeral_extra_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_numeral_extra_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.NumeralExtraLargeVariationSettings,
                ),
            numeralLarge =
                defaultTypography.numeralLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_numeral_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_numeral_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.NumeralLargeVariationSettings,
                ),
            numeralMedium =
                defaultTypography.numeralMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_numeral_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_numeral_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.NumeralMediumVariationSettings,
                ),
            numeralSmall =
                defaultTypography.numeralSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_numeral_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_numeral_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.NumeralSmallVariationSettings,
                ),
            numeralExtraSmall =
                defaultTypography.numeralExtraSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_numeral_extra_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_numeral_extra_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.NumeralExtraSmallVariationSettings,
                ),
            titleLarge =
                defaultTypography.titleLarge.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_title_large_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_title_large_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.TitleLargeVariationSettings,
                ),
            titleMedium =
                defaultTypography.titleMedium.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_title_medium_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_title_medium_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.TitleMediumVariationSettings,
                ),
            titleSmall =
                defaultTypography.titleSmall.updatedTextStyle(
                    context = context,
                    fontRes = R.string.wear_compose_material3_title_small_font_family,
                    fontSizeRes = R.dimen.wear_compose_material3_title_small_font_size,
                    variationSettings =
                        WearComposeMaterial3VariableFontTokens.TitleSmallVariationSettings,
                ),
        )
    }
}
