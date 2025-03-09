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

package com.android.ecm;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.annotation.TargetApi;
import android.os.Build;
import android.permission.flags.Flags;
import android.telecom.Call;

/**
 * @hide
 *
 * In-process API for the Enhanced Confirmation Service
 */
@FlaggedApi(Flags.FLAG_ENHANCED_CONFIRMATION_IN_CALL_APIS_ENABLED)
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
@TargetApi(Build.VERSION_CODES.BAKLAVA)
public interface EnhancedConfirmationManagerLocal {
    /**
     * Inform the enhanced confirmation service of an ongoing call
     *
     * @param call The call to potentially track
     *
     */
    void addOngoingCall(@NonNull Call call);

    /**
     * Inform the enhanced confirmation service that a call has ended
     *
     * @param callId The ID of the call to stop tracking
     *
     */
    void removeOngoingCall(@NonNull String callId);

    /**
     * Informs the enhanced confirmation service it should clear out any ongoing calls
     */
    void clearOngoingCalls();
}
