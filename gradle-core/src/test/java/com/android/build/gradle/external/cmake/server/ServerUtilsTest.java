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

package com.android.build.gradle.external.cmake.server;

import static com.google.common.truth.Truth.assertThat;

import com.android.annotations.NonNull;
import com.android.testutils.TestResources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class ServerUtilsTest {
    @Test
    public void testValidOneCompilationDatabase() throws IOException {
        File file = getCompileCommandsTestFile("compile_commands_valid_one_compilation.json");
        assertThat(file).isNotNull();

        List<CompileCommand> compileCommands = ServerUtils.getCompilationDatabase(file);
        assertThat(compileCommands).isNotNull();
        assertThat(compileCommands).hasSize(1);

        CompileCommand compileCommand = compileCommands.get(0);
        assertThat(isCompileCommandValid(compileCommand)).isTrue();

        assertThat(compileCommand.directory).isEqualTo("/home/user/test/build");
        assertThat(compileCommand.command)
                .isEqualTo(
                        "/usr/bin/clang++ -Irelative -DSOMEDEF=\"With spaces, quotes and \\-es.\" -c -o file.o file.cc");
        assertThat(compileCommand.file).isEqualTo("file.cc");
    }

    @Test
    public void testValidMultiCompilationDatabase() throws IOException {
        File file = getCompileCommandsTestFile("compile_commands_valid_multiple_compilation.json");
        assertThat(file).isNotNull();

        List<CompileCommand> compileCommands = ServerUtils.getCompilationDatabase(file);
        assertThat(compileCommands).isNotNull();
        assertThat(compileCommands).hasSize(3);

        for (CompileCommand compileCommand : compileCommands) {
            assertThat(isCompileCommandValid(compileCommand)).isTrue();
        }
    }

    @Test(expected = JsonSyntaxException.class)
    public void testInvalidBadJson() throws IOException {
        File file = getCompileCommandsTestFile("compile_commands_invalid_bad_json.json");
        assertThat(file).isNotNull();

        List<CompileCommand> compileCommands = ServerUtils.getCompilationDatabase(file);
        assertThat(compileCommands).isNull();
    }

    @Test(expected = JsonSyntaxException.class)
    public void testInvalidCompilation() throws IOException {
        File file = getCompileCommandsTestFile("compile_commands_invalid_compilation.json");
        assertThat(file).isNotNull();

        List<CompileCommand> compileCommands = ServerUtils.getCompilationDatabase(file);
        assertThat(compileCommands).isNull();
    }

    @Test
    public void testInvalidMissingFieldsCompilation() throws IOException {
        File file = getCompileCommandsTestFile("compile_commands_invalid_missing_fields.json");
        assertThat(file).isNotNull();

        List<CompileCommand> compileCommands = ServerUtils.getCompilationDatabase(file);
        assertThat(compileCommands).isNotNull();
        assertThat(compileCommands).hasSize(1);

        CompileCommand compileCommand = compileCommands.get(0);
        assertThat(isCompileCommandValid(compileCommand)).isFalse();
    }

    @Test
    public void testValidDefaultCompilationDatabase() throws IOException {
        final String compileCommandsTestFileDir =
                "/com/android/build/gradle/external/cmake/compile_commands/";
        List<CompileCommand> compileCommands =
                ServerUtils.getCompilationDatabase(
                        TestResources.getDirectory(compileCommandsTestFileDir),
                        "compile_commands_valid_one_compilation.json");
        assertThat(compileCommands).isNotNull();
        assertThat(compileCommands).hasSize(1);

        CompileCommand compileCommand = compileCommands.get(0);
        assertThat(isCompileCommandValid(compileCommand)).isTrue();

        assertThat(compileCommand.directory).isEqualTo("/home/user/test/build");
        assertThat(compileCommand.command)
                .isEqualTo(
                        "/usr/bin/clang++ -Irelative -DSOMEDEF=\"With spaces, quotes and \\-es.\" -c -o file.o file.cc");
        assertThat(compileCommand.file).isEqualTo("file.cc");
    }

    @Test
    public void testValidCodeModel() throws IOException {
        assertThat(
                        ServerUtils.isCodeModelValid(
                                getCodeModelFromString(getValidCodeModelResponseString())))
                .isTrue();
    }

    @Test
    public void testValidCodeModelTargetMissingArtifact() throws IOException {
        assertThat(
                        ServerUtils.isCodeModelValid(
                                getCodeModelFromString(
                                        getValidCodeModelTargetMissingArtifactResponseString())))
                .isTrue();
    }

    @Test
    public void testValidCodeModelTargetMissingFileGroup() throws IOException {
        assertThat(
                        ServerUtils.isCodeModelValid(
                                getCodeModelFromString(
                                        getValidCodeModelTargetMissingFileGroupResponseString())))
                .isTrue();
    }

    @Test
    public void testInvalidCodeModel() throws IOException {
        assertThat(
                        ServerUtils.isCodeModelValid(
                                getCodeModelFromString(getInvalidCodeModelResponseString())))
                .isFalse();
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
                ServerUtilsTest.class, compileCommandsTestFileDir + testFileName);
    }

    /**
     * Validates if the given CompileCommand object is valid.
     *
     * @param compileCommand - given CompileCommand object
     * @return true if the object is valid
     */
    private boolean isCompileCommandValid(@NonNull CompileCommand compileCommand) {
        return (compileCommand.directory != null)
                && (compileCommand.command != null)
                && (compileCommand.file != null);
    }

    /**
     * Returns a valid code model response string.
     *
     * @return code model json response string
     */
    private String getValidCodeModelResponseString() {
        return "{\"configurations\": [{\n"
                + "\"name\": \"\",\n"
                + "\"projects\": [{\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"name\": \"CMAKE_FORM\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"targets\": [{\n"
                + "\"artifacts\": [\"/tmp/build/Source/CursesDialog/form/libcmForm.a\"],\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"fileGroups\": [{\n"
                + "\"compileFlags\": \"  -std=gnu11\",\n"
                + "\"defines\": [\"CURL_STATICLIB\", \"LIBARCHIVE_STATIC\"],\n"
                + "\"includePath\": [{\n"
                + "\"path\": \"/tmp/build/Utilities\"\n"
                + "}],\n"
                + "\"isGenerated\": false,\n"
                + "\"language\": \"C\",\n"
                + "\"sources\": [\"fld_arg.c\"]\n"
                + "}],\n"
                + "\"fullName\": \"libcmForm.a\",\n"
                + "\"linkerLanguage\": \"C\",\n"
                + "\"name\": \"cmForm\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"type\": \"STATIC_LIBRARY\"\n"
                + "}]\n"
                + "}]\n"
                + "}],\n"
                + "\"cookie\": \"\",\n"
                + "\"inReplyTo\": \"codemodel\",\n"
                + "\"type\": \"reply\"\n"
                + "}";
    }

    /**
     * Returns a valid code model response string.
     *
     * @return code model json response string
     */
    private String getValidCodeModelTargetMissingArtifactResponseString() {
        return "{\"configurations\": [{\n"
                + "\"name\": \"\",\n"
                + "\"projects\": [{\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"name\": \"CMAKE_FORM\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"targets\": [{\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"fileGroups\": [{\n"
                + "\"compileFlags\": \"  -std=gnu11\",\n"
                + "\"defines\": [\"CURL_STATICLIB\", \"LIBARCHIVE_STATIC\"],\n"
                + "\"includePath\": [{\n"
                + "\"path\": \"/tmp/build/Utilities\"\n"
                + "}],\n"
                + "\"isGenerated\": false,\n"
                + "\"language\": \"C\",\n"
                + "\"sources\": [\"fld_arg.c\"]\n"
                + "}],\n"
                + "\"fullName\": \"libcmForm.a\",\n"
                + "\"linkerLanguage\": \"C\",\n"
                + "\"name\": \"cmForm\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"type\": \"STATIC_LIBRARY\"\n"
                + "}]\n"
                + "}]\n"
                + "}],\n"
                + "\"cookie\": \"\",\n"
                + "\"inReplyTo\": \"codemodel\",\n"
                + "\"type\": \"reply\"\n"
                + "}";
    }

    /**
     * Returns a valid code model response string.
     *
     * @return code model json response string
     */
    private String getValidCodeModelTargetMissingFileGroupResponseString() {
        return "{\"configurations\": [{\n"
                + "\"name\": \"\",\n"
                + "\"projects\": [{\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"name\": \"CMAKE_FORM\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"targets\": [{\n"
                + "\"artifacts\": [\"/tmp/build/Source/CursesDialog/form/libcmForm.a\"],\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"fullName\": \"libcmForm.a\",\n"
                + "\"linkerLanguage\": \"C\",\n"
                + "\"name\": \"cmForm\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"type\": \"STATIC_LIBRARY\"\n"
                + "}]\n"
                + "}]\n"
                + "}],\n"
                + "\"cookie\": \"\",\n"
                + "\"inReplyTo\": \"codemodel\",\n"
                + "\"type\": \"reply\"\n"
                + "}";
    }

    /**
     * Returns a invalid code model response string.
     *
     * @return code model json response string
     */
    private String getInvalidCodeModelResponseString() {
        return "{\"configurations\": [{\n"
                + "\"name\": \"\",\n"
                + "\"projects\": [{\n"
                + "\"name\": \"CMAKE_FORM\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"targets\": [{\n"
                + "\"artifacts\": [\"/tmp/build/Source/CursesDialog/form/libcmForm.a\"],\n"
                + "\"buildDirectory\": \"/tmp/build/Source/CursesDialog/form\",\n"
                + "\"fileGroups\": [{\n"
                + "\"compileFlags\": \"  -std=gnu11\",\n"
                + "\"defines\": [\"CURL_STATICLIB\", \"LIBARCHIVE_STATIC\"],\n"
                + "\"includePath\": [{\n"
                + "\"path\": \"/tmp/build/Utilities\"\n"
                + "}],\n"
                + "\"isGenerated\": false,\n"
                + "\"language\": \"C\",\n"
                + "\"sources\": [\"fld_arg.c\"]\n"
                + "}],\n"
                + "\"fullName\": \"libcmForm.a\",\n"
                + "\"linkerLanguage\": \"C\",\n"
                + "\"name\": \"cmForm\",\n"
                + "\"sourceDirectory\": \"/home/code/src/cmake/Source/CursesDialog/form\",\n"
                + "\"type\": \"STATIC_LIBRARY\"\n"
                + "}]\n"
                + "}]\n"
                + "}],\n"
                + "\"cookie\": \"\",\n"
                + "\"inReplyTo\": \"codemodel\",\n"
                + "\"type\": \"reply\"\n"
                + "}";
    }

    @NonNull
    private CodeModel getCodeModelFromString(@NonNull String codeModelString) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(codeModelString, CodeModel.class);
    }
}
