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

package com.android.build.gradle.integration.component;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.app.HelloWorldJniApp;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.internal.core.Abi;
import com.android.build.gradle.internal.ndk.NdkHandler;
import com.android.repository.Revision;
import com.android.testutils.apk.Apk;
import com.android.utils.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test unified headers in NDK. */
public class NdkUnifiedHeadersTest {
    @Rule
    public GradleTestProject project =
            GradleTestProject.builder()
                    .fromTestApp(HelloWorldJniApp.builder().build())
                    .useExperimentalGradleVersion(true)
                    .create();

    @Before
    public void setUp() throws Exception {
        TestFileUtils.appendToFile(
                project.getBuildFile(),
                "apply plugin: 'com.android.model.application'\n"
                        + "\n"
                        + "model {\n"
                        + "    android {\n"
                        + "        compileSdkVersion "
                        + GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
                        + "\n"
                        + "        buildToolsVersion \""
                        + GradleTestProject.DEFAULT_BUILD_TOOL_VERSION
                        + "\"\n"
                        + "        ndk {\n"
                        + "            moduleName \"hello-jni\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n");
    }

    @Test
    public void testUseUnifiedHeaders() throws Exception {

        Revision ndkRevision = NdkHandler.findRevision(GradleTestProject.ANDROID_NDK_HOME);
        Assume.assumeTrue(
                "Unified headers is supported from NDK r14", ndkRevision.getMajor() >= 14);
        TestFileUtils.appendToFile(
                project.getBuildFile(),
                "model {\n"
                        + "    android {\n"
                        + "        ndk {\n"
                        + "            platformVersion 19\n"
                        + "            useUnifiedHeaders true\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n");

        project.executor().run("assembleDebug");

        // Verify .so are built for all platform.
        Apk apk = project.getApk("debug");
        assertThat(apk).contains("lib/x86/libhello-jni.so");

        assertThat(
                        project.file(
                                "build/tmp/compileHello-jniX86DebugSharedLibraryHello-jniX86DebugSharedLibraryMainC/options.txt"))
                .contains(
                        "--sysroot="
                                + FileUtils.join(
                                                GradleTestProject.ANDROID_NDK_HOME.getPath(),
                                                "sysroot")
                                        .replace("\\", "\\\\"));

        assertThat(project.file("build/tmp/linkHello-jniX86DebugSharedLibrary/options.txt"))
                .contains(
                        "--sysroot="
                                + FileUtils.join(
                                                GradleTestProject.ANDROID_NDK_HOME.getPath(),
                                                "platforms",
                                                "android-19",
                                                "arch-" + Abi.X86.getArchitecture())
                                        .replace("\\", "\\\\"));
    }
}
