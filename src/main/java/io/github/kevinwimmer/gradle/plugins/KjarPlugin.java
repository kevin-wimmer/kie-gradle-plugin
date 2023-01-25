/*
 * Copyright 2023 Kevin Wimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package io.github.kevinwimmer.gradle.plugins;

import java.util.Arrays;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;

import io.github.kevinwimmer.gradle.tasks.CompileDrools;
import io.github.kevinwimmer.gradle.tasks.GenerateKmoduleXml;
import io.github.kevinwimmer.gradle.tasks.GeneratePomProperties;

/**
 * A Gradle plugin that extends the {@code JavaPlugin} to compile Drools source files and assemble a
 * Kie JAR artifact.
 */
public abstract class KjarPlugin implements Plugin<Project> {

    public static final String COMPILE_DROOLS_TASK_NAME = "compileDrools";
    public static final String GENERATE_POM_PROPERTIES_TASK_NAME = "generatePomProperties";
    public static final String GENERATE_KMODULE_XML_TASK_NAME = "generateKmoduleXml";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        project.getTasks().register(COMPILE_DROOLS_TASK_NAME, CompileDrools.class,
                task -> task.setDescription("Compiles the Drools source files."));
        project.getTasks().register(GENERATE_KMODULE_XML_TASK_NAME, GenerateKmoduleXml.class,
                task -> task.setDescription("Generates a kmodule.xml file."));
        project.getTasks().register(GENERATE_POM_PROPERTIES_TASK_NAME, GeneratePomProperties.class,
                task -> task.setDescription("Generates a Maven pom.properties file."));

        configureBuild(project);
    }

    private static void configureBuild(Project project) {
        final TaskContainer tasks = project.getTasks();
        tasks.named(JavaPlugin.CLASSES_TASK_NAME,
                task -> task.dependsOn(tasks.named(COMPILE_DROOLS_TASK_NAME)));
        tasks.named(COMPILE_DROOLS_TASK_NAME,
                task -> task.setMustRunAfter(Arrays.asList(tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))));
        tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, task -> {
            task.finalizedBy(tasks.named(GENERATE_KMODULE_XML_TASK_NAME));
            task.finalizedBy(tasks.named(GENERATE_POM_PROPERTIES_TASK_NAME));
        });
    }
}
