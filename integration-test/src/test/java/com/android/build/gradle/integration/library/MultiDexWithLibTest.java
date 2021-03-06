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

package com.android.build.gradle.integration.library;

import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.AssumeUtil;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Assemble tests for multiDexWithLib. */
public class MultiDexWithLibTest {
    @ClassRule
    public static GradleTestProject project =
            GradleTestProject.builder()
                    .fromTestProject("multiDexWithLib")
                    .withHeap("2048M")
                    .create();

    @BeforeClass
    public static void setUp() throws IOException, InterruptedException {
        AssumeUtil.assumeBuildToolsAtLeast(21);
        project.execute("clean", "assembleDebug", "assembleDebugAndroidTest");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
    }

    @Test
    public void lint() throws IOException, InterruptedException {
        project.execute("lint");
    }

    @Test
    @Category(DeviceTests.class)
    public void connectedCheck() throws IOException, InterruptedException {
        project.executeConnectedCheck();
    }
}
