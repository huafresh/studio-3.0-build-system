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

package com.android.build.gradle.integration.common.utils;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.utils.FileUtils;
import com.android.utils.StringHelper;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper to help verify content of a file.
 */
public class TestFileUtils {

    /** Return a list of relative path of the files in a directory. */
    public static List<String> listFiles(@NonNull Path base) throws IOException {
        assertThat(base).isDirectory();

        return Files.walk(base)
                .filter(Files::isRegularFile)
                .map(path -> path.toString().substring(base.toString().length()))
                .collect(Collectors.toList());
    }

    public static void checkContent(File file, String expectedContent) throws IOException {
        checkContent(file, Collections.singleton(expectedContent));
    }

    public static void checkContent(File file, Iterable<String> expectedContents) throws IOException {
        assertThat(file).isFile();

        String contents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        for (String expectedContent : expectedContents) {
            assertTrue("File '" + file.getAbsolutePath() + "' does not contain: " + expectedContent,
                    contents.contains(expectedContent));
        }
    }

    public static void searchAndReplace(
            @NonNull File file,
            @NonNull String search,
            @NonNull String replace) throws IOException {
        searchAndReplace(file.toPath(), search, replace);
    }

    public static void searchAndReplace(
            @NonNull Path file, @NonNull String search, @NonNull String replace)
            throws IOException {

        String content = new String(java.nio.file.Files.readAllBytes(file));
        // Handle patterns that use unix-style line endings even on Windows where the test
        // projects are sometimes checked out with Windows-style endings depending on the .gitconfig
        // "autocrlf" property
        if (content.contains("\r\n")) {
            search = StringHelper.toSystemLineSeparator(search);
            replace = StringHelper.toSystemLineSeparator(replace);
        }

        String newContent = content.replaceAll(search, replace);
        assertNotEquals(
                "No match in file"
                        + "\n - File:   " + file + "\n - Search: " + search
                        + "\n - Replace: " + replace + "\n",
                content,
                newContent);

        // Gradle has a bug, where it may not notice rapid changes to build.gradle if the length of
        // the file has not changed. Work around this by appending a space at the end.
        if (file.getFileName().toString().equals(SdkConstants.FN_BUILD_GRADLE)
                && content.length() == newContent.length()) {
            newContent += ' ';
        }

        java.nio.file.Files.write(file, newContent.getBytes());
    }

    /**
     * Replace a line from a file with another line.
     * @param file file to change
     * @param lineNumber the line number, starting at 1
     * @param line the line to replace with
     */
    public static void replaceLine(
            @NonNull  File file,
            int lineNumber,
            @NonNull String line) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), Charsets.UTF_8);

        lines.add(lineNumber, line);
        lines.remove(lineNumber - 1);

        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    public static void addMethod(
            @NonNull File javaFile,
            @NonNull String methodCode) throws IOException {
        // Put the method code before the last closing brace.
        searchAndReplace(javaFile, "\n}\\s*$", methodCode + "\n\n}");
    }

    public static void appendToFile(@NonNull File file, @NonNull String content) throws IOException {
        Files.write(
                file.toPath(),
                (System.lineSeparator() + content).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE);
    }

    /**
     * Return a list of path folders and file (if applicable)
     */
    public static List<String> splitPath(@NonNull File path) {
        return Arrays.asList(
                FileUtils.toSystemIndependentPath(
                        path.getPath()).split("/"));
    }

    @NonNull
    public static String sha1NormalizedLineEndings(@NonNull File file) throws IOException {
        return sha1NormalizedLineEndings(file.toPath());
    }

    @NonNull
    public static String sha1NormalizedLineEndings(@NonNull Path file) throws IOException {
        String content =
                java.nio.file.Files.readAllLines(file, Charsets.UTF_8)
                        .stream()
                        .collect(Collectors.joining("\n"));
        return Hashing.sha1().hashString(content + "\n", Charsets.UTF_8).toString();
    }
}
