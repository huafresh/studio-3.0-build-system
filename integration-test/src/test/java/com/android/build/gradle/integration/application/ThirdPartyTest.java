/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.integration.application;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

/** Assemble tests for 3rdPartyTests. */
public class ThirdPartyTest {
    @ClassRule
    public static GradleTestProject project =
            GradleTestProject.builder().fromTestProject("3rdPartyTests").create();

    @AfterClass
    public static void cleanUp() {
        project = null;
    }

    @Test
    public void deviceCheck() throws IOException, InterruptedException {
        // Run deviceCheck even without devices, since we use a fake DeviceProvider that doesn't
        // use a device, but only record the calls made to the DeviceProvider and the
        // DeviceConnector.
        project.execute("clean", "deviceCheck");
    }
}
