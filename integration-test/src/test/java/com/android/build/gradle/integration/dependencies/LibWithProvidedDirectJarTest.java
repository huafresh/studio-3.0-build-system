/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.integration.dependencies;

import static com.android.build.gradle.integration.common.fixture.BuildModel.Feature.FULL_DEPENDENCIES;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static com.android.build.gradle.integration.common.utils.LibraryGraphHelper.Filter.PROVIDED;
import static com.android.build.gradle.integration.common.utils.LibraryGraphHelper.Property.GRADLE_PATH;
import static com.android.build.gradle.integration.common.utils.LibraryGraphHelper.Type.MODULE;
import static com.android.build.gradle.integration.common.utils.TestFileUtils.appendToFile;

import com.android.build.gradle.integration.common.fixture.GetAndroidModelAction.ModelContainer;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.LibraryGraphHelper;
import com.android.build.gradle.integration.common.utils.ModelHelper;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Variant;
import com.android.builder.model.level2.DependencyGraphs;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * test for provided jar in library
 */
public class LibWithProvidedDirectJarTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("projectWithModules")
            .create();
    static ModelContainer<AndroidProject> modelContainer;
    private static LibraryGraphHelper helper;

    @BeforeClass
    public static void setUp() throws Exception {
        Files.write("include 'app', 'library', 'jar'", project.getSettingsFile(), Charsets.UTF_8);

        appendToFile(project.getSubproject("app").getBuildFile(),
                "\n" +
                "dependencies {\n" +
                "    compile project(\":library\")\n" +
                "}\n");

        appendToFile(project.getSubproject("library").getBuildFile(),
                "\n" +
                "dependencies {\n" +
                "    provided project(\":jar\")\n" +
                "}\n");

        project.execute("clean", ":library:assembleDebug");
        modelContainer = project.model().withFeature(FULL_DEPENDENCIES).getMulti();
        helper = new LibraryGraphHelper(modelContainer);
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
        modelContainer = null;
        helper = null;
    }

    @Test
    public void checkProvidedJarIsNotPackaged() throws Exception {
        assertThat(project.getSubproject("library").getAar("debug"))
                .doesNotContainClass("Lcom/example/android/multiproject/person/People;");
    }

    @Test
    public void checkProvidedJarIsIntheLibCompileDeps() throws Exception {
        Variant variant = ModelHelper.getVariant(
                modelContainer.getModelMap().get(":library").getVariants(), "debug");

        DependencyGraphs graph = variant.getMainArtifact().getDependencyGraphs();

        LibraryGraphHelper.Items moduleItems = helper.on(graph).withType(MODULE);

        assertThat(moduleItems.mapTo(GRADLE_PATH)).containsExactly(":jar");
        assertThat(moduleItems.filter(PROVIDED).mapTo(GRADLE_PATH)).containsExactly(":jar");

        assertThat(graph.getProvidedLibraries())
                .containsExactly(moduleItems.asSingleGraphItem().getArtifactAddress());
    }

    @Test
    public void checkProvidedJarIsNotIntheLibPackageDeps() throws Exception {
        Variant variant = ModelHelper.getVariant(
                modelContainer.getModelMap().get(":library").getVariants(), "debug");

        DependencyGraphs dependencyGrap = variant.getMainArtifact().getDependencyGraphs();
        assertThat(dependencyGrap.getPackageDependencies()).isEmpty();
    }

    @Test
    public void checkProvidedJarIsNotInTheAppDeps() throws Exception {
        Variant variant = ModelHelper.getVariant
                (modelContainer.getModelMap().get(":app").getVariants(), "debug");

        DependencyGraphs graph = variant.getMainArtifact().getDependencyGraphs();

        // query directly the full transitive list and it should only contain :library
        assertThat(helper.on(graph).withTransitive().mapTo(GRADLE_PATH)).containsExactly(":library");
    }
}
