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

package com.android.build.gradle.tasks;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.build.gradle.external.cmake.server.CompileCommand;
import com.android.build.gradle.external.cmake.server.ServerUtils;
import com.android.build.gradle.external.cmake.server.receiver.InteractiveMessage;
import com.android.build.gradle.internal.SdkHandler;
import com.android.build.gradle.internal.core.Abi;
import com.android.build.gradle.internal.ndk.NdkHandler;
import com.android.builder.core.AndroidBuilder;
import com.android.repository.Revision;
import com.android.repository.api.ConsoleProgressIndicator;
import com.android.repository.api.LocalPackage;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.testutils.TestResources;
import com.android.testutils.TestUtils;
import com.android.utils.ILogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CmakeServerExternalNativeJsonGeneratorTest {
    File sdkDirectory;
    NdkHandler ndkHandler;
    int minSdkVersion;
    String variantName;
    Collection<Abi> abis;
    AndroidBuilder androidBuilder;
    File sdkFolder;
    File ndkFolder;
    File soFolder;
    File objFolder;
    File jsonFolder;
    File makeFile;
    File cmakeFolder;
    File ninjaFolder;
    boolean debuggable;
    List<String> buildArguments;
    List<String> cFlags;
    List<String> cppFlags;
    List<File> nativeBuildConfigurationsJsons;

    @Before
    public void setUp() throws Exception {
        SdkHandler.setTestSdkFolder(TestUtils.getSdk());

        sdkDirectory = TestUtils.getSdk();
        ndkHandler = Mockito.mock(NdkHandler.class);
        minSdkVersion = 123;
        variantName = "dummy variant name";
        abis = Mockito.mock(Collection.class);
        androidBuilder = Mockito.mock(AndroidBuilder.class);
        sdkFolder = TestUtils.getSdk(); //Mockito.mock(File.class);
        ndkFolder = TestUtils.getNdk();
        soFolder = Mockito.mock(File.class);
        objFolder = null;
        jsonFolder = Mockito.mock(File.class);
        makeFile = Mockito.mock(File.class);
        AndroidSdkHandler sdk = AndroidSdkHandler.getInstance(sdkDirectory);
        LocalPackage cmakePackage =
                sdk.getLatestLocalPackageForPrefix(
                        SdkConstants.FD_CMAKE, null, true, new ConsoleProgressIndicator());
        if (cmakePackage != null) {
            cmakeFolder = cmakePackage.getLocation();
        }

        ninjaFolder = new File(cmakeFolder, "bin");
        debuggable = true;
        buildArguments =
                Arrays.asList("build-argument-foo", "build-argument-bar", "build-argument-baz");
        cFlags = Arrays.asList("c-flags1", "c-flag2");
        cppFlags = Arrays.asList("cpp-flags1", "cpp-flag2");
        nativeBuildConfigurationsJsons = Mockito.mock(List.class);
    }

    @Test
    public void testGetCacheArguments() {
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();
        List<String> cacheArguments =
                cmakeServerStrategy.getProcessBuilderArgs("x86", 12, jsonFolder);

        assertThat(cacheArguments).isNotEmpty();
        assertThat(cacheArguments).contains("-DCMAKE_EXPORT_COMPILE_COMMANDS=ON");
        assertThat(cacheArguments)
                .contains(
                        String.format(
                                "-DCMAKE_ANDROID_NDK=%s", cmakeServerStrategy.getNdkFolder()));
        assertThat(cacheArguments).contains("-DCMAKE_SYSTEM_NAME=Android");
        assertThat(cacheArguments).contains("-DCMAKE_BUILD_TYPE=Debug");
        assertThat(cacheArguments).contains("-DCMAKE_C_FLAGS=c-flags1 c-flag2");
        assertThat(cacheArguments).contains("-DCMAKE_CXX_FLAGS=cpp-flags1 cpp-flag2");
        assertThat(cacheArguments).contains("build-argument-foo");
        assertThat(cacheArguments).contains("build-argument-bar");
        assertThat(cacheArguments).contains("build-argument-baz");
        assertThat(cacheArguments).contains("-G Ninja");

        // Ensure that the buildArguments (supplied by the user) is added to the end of the argument
        // list.
        // If cacheArguments = 1,2,3,4,a,b,c and buildArguments = a,b,c, we just compare where in
        // the cacheArguments does buildArguments sublist is and verify if it's indeed at the end.
        int indexOfSubset = Collections.indexOfSubList(cacheArguments, buildArguments);
        assertThat(cacheArguments.size() - indexOfSubset).isEqualTo(buildArguments.size());
    }

    @Test
    public void testInfoLoggingInteractiveMessage() {
        ILogger mockLogger = Mockito.mock(ILogger.class);
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();

        String message = "CMake random info";
        String infoMessageString1 =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage1 =
                getInteractiveMessageFromString(infoMessageString1);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage1);
        Mockito.verify(mockLogger, times(1)).info(message);

        message = "CMake error but should be logged as info";
        String infoMessageString2 =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage2 =
                getInteractiveMessageFromString(infoMessageString2);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage2);
        Mockito.verify(mockLogger, times(1)).info(message);

        message = "CMake warning but should be logged as info";
        String infoMessageString3 =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage3 =
                getInteractiveMessageFromString(infoMessageString3);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage3);
        Mockito.verify(mockLogger, times(1)).info(message);

        message = "CMake info";
        String infoMessageString4 =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"title\":\"Some title\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage4 =
                getInteractiveMessageFromString(infoMessageString4);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage4);
        Mockito.verify(mockLogger, times(1)).info(message);
    }

    @Test
    public void testWarningInMessageLoggingInteractiveMessage() {
        ILogger mockLogger = Mockito.mock(ILogger.class);
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();

        String message = "CMake Warning some random warining :|";

        String warningMessageString =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage =
                getInteractiveMessageFromString(warningMessageString);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage);
        Mockito.verify(mockLogger, times(1)).warning(message);
    }

    @Test
    public void testWarningInTitleLoggingInteractiveMessage() {
        ILogger mockLogger = Mockito.mock(ILogger.class);
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();

        String message = "CMake warning some random warning :(";

        String warningMessageString =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"title\":\"Warning\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage =
                getInteractiveMessageFromString(warningMessageString);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage);
        Mockito.verify(mockLogger, times(1)).warning(message);
    }

    @Test
    public void testErrorInMessageLoggingInteractiveMessage() {
        ILogger mockLogger = Mockito.mock(ILogger.class);
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();

        String message = "CMake Error some random error :(";

        String errorMessageString =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage = getInteractiveMessageFromString(errorMessageString);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage);
        Mockito.verify(mockLogger, times(1)).error(null, message);
    }

    @Test
    public void testErrorInTitleLoggingInteractiveMessage() {
        ILogger mockLogger = Mockito.mock(ILogger.class);
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();

        String message = "CMake error some random error :(";

        String errorMessageString =
                "{\"cookie\":\"\","
                        + "\"inReplyTo\":\"configure\","
                        + "\"message\":\""
                        + message
                        + "\","
                        + "\"title\":\"Error\","
                        + "\"type\":\"message\"}";
        InteractiveMessage interactiveMessage = getInteractiveMessageFromString(errorMessageString);

        cmakeServerStrategy.logInteractiveMessage(mockLogger, interactiveMessage);
        Mockito.verify(mockLogger, times(1)).error(null, message);
    }

    @Test
    public void testParseValidFileFromCompileCommands() throws IOException {
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();
        File compileCommandsTestFile =
                getCompileCommandsTestFile("compile_commands_valid_multiple_compilation.json");
        List<CompileCommand> compileCommands =
                ServerUtils.getCompilationDatabase(compileCommandsTestFile);

        String flags =
                cmakeServerStrategy.getAndroidGradleFileLibFlags("x86", "file.cc", compileCommands);
        assertThat(flags).isNotNull();
        assertThat(flags)
                .isEqualTo("-Irelative -DSOMEDEF=\"With spaces, quotes and \\-es.\" -c -o file.o ");
    }

    @Test
    public void testParseInvalidFileFromCompileCommands() throws IOException {
        CmakeServerExternalNativeJsonGenerator cmakeServerStrategy = getCMakeServerGenerator();
        File compileCommandsTestFile =
                getCompileCommandsTestFile("compile_commands_valid_multiple_compilation.json");
        List<CompileCommand> compileCommands =
                ServerUtils.getCompilationDatabase(compileCommandsTestFile);

        String flags =
                cmakeServerStrategy.getAndroidGradleFileLibFlags(
                        "x86", "invalid-file.cc", compileCommands);
        assertThat(flags).isNull();
    }

    /** Returns InteractiveMessage object from the given message string. */
    private static InteractiveMessage getInteractiveMessageFromString(@NonNull String messageStr) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(messageStr, InteractiveMessage.class);
    }

    /** Returns a default CmakeServerExternalNativeJsonGenerator. */
    private CmakeServerExternalNativeJsonGenerator getCMakeServerGenerator() {
        Mockito.when(ndkHandler.getRevision()).thenReturn(new Revision(15));
        Mockito.when(androidBuilder.getLogger()).thenReturn(Mockito.mock(ILogger.class));
        return new CmakeServerExternalNativeJsonGenerator(
                ndkHandler,
                minSdkVersion,
                variantName,
                abis,
                androidBuilder,
                sdkFolder,
                ndkFolder,
                soFolder,
                objFolder,
                jsonFolder,
                makeFile,
                cmakeFolder,
                debuggable,
                buildArguments,
                cFlags,
                cppFlags,
                nativeBuildConfigurationsJsons);
    }

    /**
     * Returns the test file given the test folder and file name.
     *
     * @param testFileName - test file name
     * @return test file
     */
    private File getCompileCommandsTestFile(@NonNull String testFileName) {
        final String compileCommandsTestFileDir =
                "/com/android/build/gradle/external/cmake/compile_commands/";
        return TestResources.getFile(
                CmakeServerExternalNativeJsonGeneratorTest.class,
                compileCommandsTestFileDir + testFileName);
    }
}
