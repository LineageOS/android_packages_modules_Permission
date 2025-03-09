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

import android.annotation.NonNull;
import android.annotation.TargetApi;
import android.os.Build;
import android.permission.flags.Flags;
import android.telecom.Call;

/** @hide */
@TargetApi(Build.VERSION_CODES.BAKLAVA)
class EnhancedConfirmationManagerLocalImpl implements EnhancedConfirmationManagerLocal {

    private final EnhancedConfirmationService mService;

    EnhancedConfirmationManagerLocalImpl(EnhancedConfirmationService service) {
        if (Flags.enhancedConfirmationInCallApisEnabled()) {
            mService = service;
        } else {
            mService = null;
        }
    }

    @Override
    public void addOngoingCall(@NonNull Call call) {
        if (mService != null) {
            mService.addOngoingCall(call);
        }
    }

    @Override
    public void removeOngoingCall(@NonNull String callId) {
        if (mService != null) {
            mService.removeOngoingCall(callId);
        }
    }

    @Override
    public void clearOngoingCalls() {
        if (mService != null) {
            mService.clearOngoingCalls();
        }
    }
}
