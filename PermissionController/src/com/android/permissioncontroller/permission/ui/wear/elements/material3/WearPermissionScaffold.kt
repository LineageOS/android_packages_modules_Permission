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

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.android.permissioncontroller.permission.ui.wear.elements.AnnotatedText
import com.android.permissioncontroller.permission.ui.wear.elements.Wear2Scaffold
import com.android.permissioncontroller.permission.ui.wear.elements.layout.ScalingLazyColumn
import com.android.permissioncontroller.permission.ui.wear.elements.layout.ScalingLazyColumnState
import com.android.permissioncontroller.permission.ui.wear.elements.layout.rememberResponsiveColumnState
import com.android.permissioncontroller.permission.ui.wear.elements.rememberDrawablePainter
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion.MATERIAL2_5
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionTheme

/**
 * This component is wrapper on material scaffold component. It helps with time text, scroll
 * indicator and standard list elements like title, icon and subtitle.
 */
@Composable
internal fun WearPermissionScaffold(
    materialUIVersion: WearPermissionMaterialUIVersion = MATERIAL2_5,
    showTimeText: Boolean,
    title: String?,
    subtitle: CharSequence?,
    image: Any?,
    isLoading: Boolean,
    content: ScalingLazyListScope.() -> Unit,
    titleTestTag: String? = null,
    subtitleTestTag: String? = null,
) {

    if (materialUIVersion == MATERIAL2_5) {
        Wear2Scaffold(
            showTimeText,
            title,
            subtitle,
            image,
            isLoading,
            content,
            titleTestTag,
            subtitleTestTag,
        )
    } else {
        WearPermissionScaffoldInternal(
            showTimeText,
            title,
            subtitle,
            image,
            isLoading,
            content,
            titleTestTag,
            subtitleTestTag,
        )
    }
}

@Composable
private fun WearPermissionScaffoldInternal(
    showTimeText: Boolean,
    title: String?,
    subtitle: CharSequence?,
    image: Any?,
    isLoading: Boolean,
    content: ScalingLazyListScope.() -> Unit,
    titleTestTag: String? = null,
    subtitleTestTag: String? = null,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val paddingDefaults =
        WearPermissionScaffoldPaddingDefaults(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            titleNeedsLargePadding = subtitle == null,
        )
    val columnState =
        rememberResponsiveColumnState(contentPadding = { paddingDefaults.scrollContentPadding })
    WearPermissionTheme(version = WearPermissionMaterialUIVersion.MATERIAL3) {
        AppScaffold(timeText = wearPermissionTimeText(showTimeText && !isLoading)) {
            ScreenScaffold(
                scrollInfoProvider = ScrollInfoProvider(columnState.state),
                scrollIndicator = wearPermissionScrollIndicator(!isLoading, columnState),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        ScrollingView(
                            columnState = columnState,
                            icon = painterFromImage(image),
                            title = title,
                            titleTestTag = titleTestTag,
                            titlePaddingValues = paddingDefaults.titlePaddingValues,
                            subtitle = subtitle,
                            subtitleTestTag = subtitleTestTag,
                            subTitlePaddingValues = paddingDefaults.subTitlePaddingValues,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

private class WearPermissionScaffoldPaddingDefaults(
    screenWidth: Int,
    screenHeight: Int,
    titleNeedsLargePadding: Boolean,
) {
    private val firstSpacerItemHeight = 0.dp
    private val scrollContentHorizontalPadding = (screenWidth * 0.052).dp
    private val titleHorizontalPadding = (screenWidth * 0.0884).dp
    private val subtitleHorizontalPadding = (screenWidth * 0.0416).dp
    private val scrollContentTopPadding = (screenHeight * 0.1456).dp - firstSpacerItemHeight
    private val scrollContentBottomPadding = (screenHeight * 0.3636).dp
    private val defaultItemPadding = 4.dp
    private val largeItemPadding = 8.dp
    val titlePaddingValues =
        PaddingValues(
            start = titleHorizontalPadding,
            top = defaultItemPadding,
            bottom = if (titleNeedsLargePadding) largeItemPadding else defaultItemPadding,
            end = titleHorizontalPadding,
        )
    val subTitlePaddingValues =
        PaddingValues(
            start = subtitleHorizontalPadding,
            top = defaultItemPadding,
            bottom = largeItemPadding,
            end = subtitleHorizontalPadding,
        )
    val scrollContentPadding =
        PaddingValues(
            start = scrollContentHorizontalPadding,
            end = scrollContentHorizontalPadding,
            top = scrollContentTopPadding,
            bottom = scrollContentBottomPadding,
        )
}

@Composable
private fun BoxScope.ScrollingView(
    columnState: ScalingLazyColumnState,
    icon: Painter?,
    title: String?,
    titleTestTag: String?,
    subtitle: CharSequence?,
    subtitleTestTag: String?,
    titlePaddingValues: PaddingValues,
    subTitlePaddingValues: PaddingValues,
    content: ScalingLazyListScope.() -> Unit,
) {
    ScalingLazyColumn(columnState = columnState) {
        iconItem(icon, Modifier.size(24.dp))
        titleItem(text = title, testTag = titleTestTag, contentPaddingValues = titlePaddingValues)
        subtitleItem(
            text = subtitle,
            testTag = subtitleTestTag,
            modifier = Modifier.align(Alignment.Center).padding(subTitlePaddingValues),
        )
        content()
    }
}

private fun wearPermissionTimeText(showTime: Boolean): @Composable () -> Unit {
    return if (showTime) {
        { TimeText { time() } }
    } else {
        {}
    }
}

private fun wearPermissionScrollIndicator(
    showIndicator: Boolean,
    columnState: ScalingLazyColumnState,
): @Composable (BoxScope.() -> Unit)? {
    return if (showIndicator) {
        {
            ScrollIndicator(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = columnState.state,
            )
        }
    } else {
        null
    }
}

@Composable
private fun painterFromImage(image: Any?): Painter? {
    return when (image) {
        is Int -> painterResource(id = image)
        is Drawable -> rememberDrawablePainter(image)
        else -> null
    }
}

private fun Modifier.optionalTestTag(tag: String?): Modifier {
    if (tag == null) {
        return this
    }
    return this then testTag(tag)
}

private fun ScalingLazyListScope.iconItem(painter: Painter?, modifier: Modifier = Modifier) =
    painter?.let {
        item {
            Image(
                painter = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
    }

private fun ScalingLazyListScope.titleItem(
    text: String?,
    testTag: String?,
    contentPaddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) =
    text?.let {
        item {
            ListHeader(
                modifier = modifier.requiredHeightIn(1.dp), // We do not want default min height
                contentPadding = contentPaddingValues,
            ) {
                Text(
                    text = it,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.optionalTestTag(testTag),
                )
            }
        }
    }

private fun ScalingLazyListScope.subtitleItem(
    text: CharSequence?,
    testTag: String?,
    modifier: Modifier = Modifier,
) =
    text?.let {
        item {
            AnnotatedText(
                text = it,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                modifier = modifier.optionalTestTag(testTag),
                shouldCapitalize = true,
            )
        }
    }
