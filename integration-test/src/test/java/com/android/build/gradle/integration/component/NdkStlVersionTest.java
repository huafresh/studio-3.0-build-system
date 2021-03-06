package com.android.build.gradle.integration.component;

import static com.android.build.gradle.integration.common.fixture.GradleTestProject.DEFAULT_BUILD_TOOL_VERSION;
import static com.android.build.gradle.integration.common.fixture.GradleTestProject.DEFAULT_COMPILE_SDK_VERSION;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.app.HelloWorldJniApp;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.ndk.internal.NativeCompilerArgsUtil;
import com.android.repository.Revision;
import com.android.utils.FileUtils;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test STL version.
 *
 * This test will need to be updated as we update NDK version.
 */
public class NdkStlVersionTest {

    @Rule
    public GradleTestProject project = GradleTestProject.builder()
            .fromTestApp(HelloWorldJniApp.builder().useCppSource().build())
            .useExperimentalGradleVersion(true).create();

    @Before
    public void setUp() throws Exception {
        TestFileUtils.appendToFile(
                project.getBuildFile(),
                "apply plugin: 'com.android.model.application'\n"
                        + "model {\n"
                        + "    android {\n"
                        + "        compileSdkVersion " + DEFAULT_COMPILE_SDK_VERSION + "\n"
                        + "        buildToolsVersion \"" + DEFAULT_BUILD_TOOL_VERSION + "\"\n"
                        + "        ndk {\n"
                        + "            moduleName \"hello-jni\"\n"
                        + "            abiFilters.addAll([\"x86\", \"armeabi-v7a\", \"mips\"])\n"
                        + "            stl \"gnustl_shared\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n");
    }

    @Test
    public void checkDefaultStlVersion() throws Exception {
        project.execute("clean", "assembleDebug");
        File cppOptions =
                project.file(
                        "build/tmp/compileHello-jniX86DebugSharedLibraryHello-jniX86DebugSharedLibraryMainCpp/"
                                + "options.txt");
        assertThat(cppOptions).containsAllOf(escape("gnu-libstdc++/4.9/"));
    }

    @Test
    public void checkCustomStlVersion() throws Exception {
        File libstdc =
                FileUtils.join(
                        GradleTestProject.ANDROID_NDK_HOME, "sources", "cxx-stl", "gnu-libstdc++");
        assertThat(libstdc).isDirectory();
        assertThat(libstdc.listFiles()).named("libstdc++ files list").isNotNull();

        List<String> stlVersions = Lists.newArrayList();
        //noinspection ConstantConditions - listFiles() will never return null
        for (File f : libstdc.listFiles()) {
            if (f.isDirectory()) {
                try {
                    Revision r = Revision.parseRevision(f.getName());
                    stlVersions.add(r.toString());
                } catch (NumberFormatException nfe) {
                    // do nothing, this is not a dir with an stl version
                }
            }
        }
        // there should be at least one version
        assertThat(stlVersions).isNotEmpty();

        TestFileUtils.appendToFile(
                project.getBuildFile(), "apply plugin: 'com.android.model.application'\n");
        for (String stlVersion : stlVersions) {
            TestFileUtils.appendToFile(
                    project.getBuildFile(),
                    "model {\n"
                            + "    android {"
                            + "        ndk {\n"
                            + "            stlVersion = \""
                            + stlVersion
                            + "\"\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n");
            project.execute("clean", "assembleDebug");
            File cppOptions =
                    project.file(
                            "build/tmp/compileHello-jniX86DebugSharedLibraryHello-jniX86DebugSharedLibraryMainCpp/"
                                    + "options.txt");
            assertThat(cppOptions).containsAllOf(escape("gnu-libstdc++/" + stlVersion + "/"));
        }
    }

    private static String escape(String arg) {
        return NativeCompilerArgsUtil.transform(FileUtils.toSystemDependentPath(arg));
    }
}
