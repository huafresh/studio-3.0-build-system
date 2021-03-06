package com.android.build.gradle.integration.common.fixture.app;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.integration.common.fixture.TestProject;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * A TestProject containing multiple TestProject as modules.
 */
public class MultiModuleTestProject implements TestProject {

    private Map<String, TestProject> subprojects;

    /**
     * Creates a MultiModuleTestProject.
     *
     * @param subprojects a map with gradle project path as key and the corresponding TestProject as
     *                    value.
     */
    public MultiModuleTestProject(@NonNull Map<String, ? extends TestProject> subprojects) {
        this.subprojects = Maps.newHashMap(subprojects);
    }

    /**
     * Creates a MultiModuleTestProject with multiple subproject of the same TestProject.
     *
     * @param baseName Base name of the subproject.  Actual project name will be baseName + index.
     * @param subproject A TestProject.
     * @param count Number of subprojects to create.
     */
    public MultiModuleTestProject(
            @NonNull String baseName,
            @NonNull TestProject subproject,
            int count) {
        subprojects = Maps.newHashMapWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            subprojects.put(baseName + i, subproject);
        }
    }

    /**
     * Return the test project with the given project path.
     */
    public TestProject getSubproject(String subprojectPath) {
        return subprojects.get(subprojectPath);
    }

    @Override
    public void write(
            @NonNull final File projectDir,
            @Nullable final String buildScriptContent)  throws IOException {
        for (Map.Entry<String, ? extends TestProject> entry : subprojects.entrySet()) {
            String subprojectPath = entry.getKey();
            TestProject subproject = entry.getValue();
            File subprojectDir = new File(projectDir, convertGradlePathToDirectory(subprojectPath));
            if (!subprojectDir.exists()) {
                subprojectDir.mkdirs();
                assert subprojectDir.isDirectory();
            }
            subproject.write(subprojectDir, null);
        }

        StringBuilder builder = new StringBuilder();
        for (String subprojectName : subprojects.keySet()) {
            builder.append("include '").append(subprojectName).append("'\n");
        }
        Files.write(builder.toString(),
                new File(projectDir, "settings.gradle"),
                Charset.defaultCharset());

        Files.write(buildScriptContent,
                new File(projectDir, "build.gradle"),
                Charset.defaultCharset());
    }

    @Override
    public boolean containsFullBuildScript() {
        return false;
    }

    private static String convertGradlePathToDirectory(String gradlePath) {
        return gradlePath.replace(":", "/");
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private BiMap<String, AndroidTestApp> projects = HashBiMap.create();

        @NonNull
        public Builder subproject(@NonNull String name, @NonNull AndroidTestApp testProject) {
            projects.put(name, testProject);
            return this;
        }

        @NonNull
        public Builder dependency(@NonNull AndroidTestApp from, @NonNull AndroidTestApp to) {
            return dependency("implementation", from, to);
        }

        @NonNull
        public Builder dependency(
                @NonNull String configuration,
                @NonNull AndroidTestApp from,
                @NonNull AndroidTestApp to) {
            String snippet =
                    "\ndependencies {\n    "
                            + configuration
                            + " project('"
                            + projects.inverse().get(to)
                            + "')\n}\n";
            from.replaceFile(from.getFile("build.gradle", "").appendContent(snippet));
            return this;
        }

        @NonNull
        public MultiModuleTestProject build() {
            return new MultiModuleTestProject(projects);
        }
    }

}
