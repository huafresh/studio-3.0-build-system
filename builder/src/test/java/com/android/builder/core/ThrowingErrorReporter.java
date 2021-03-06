/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.SyncIssue;
import com.android.ide.common.blame.Message;

public class ThrowingErrorReporter extends ErrorReporter {
    public ThrowingErrorReporter() {
        super(EvaluationMode.IDE);
    }

    @NonNull
    @Override
    public SyncIssue handleIssue(
            @Nullable String data, int type, int severity, @NonNull String msg) {
        throw new RuntimeException("fake");
    }

    @Override
    public boolean hasSyncIssue(int type) {
        throw new RuntimeException("fake");
    }

    @Override
    public void receiveMessage(@NonNull Message message) {
        throw new RuntimeException("fake");
    }
}
