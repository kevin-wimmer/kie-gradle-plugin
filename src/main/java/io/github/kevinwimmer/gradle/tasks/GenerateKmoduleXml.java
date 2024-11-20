/*
 * Copyright 2023-2024 Kevin Wimmer
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

package io.github.kevinwimmer.gradle.tasks;

import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Not worth caching")
public class GenerateKmoduleXml extends DefaultTask {

    @TaskAction
    public void generatePomProperties() throws IOException {
        try (FileOutputStream writer = new FileOutputStream(createDestination(getProject()))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\"/>".getBytes(StandardCharsets.UTF_8));
        }
    }

    private File createDestination(Project project) {
        File dest = new File(
                project
                        .getExtensions()
                        .getByType(JavaPluginExtension.class)
                        .getSourceSets()
                        .getByName(MAIN_SOURCE_SET_NAME)
                        .getOutput()
                        .getResourcesDir(),
                        "META-INF/kmodule.xml");
        dest.getParentFile().mkdirs();
        return dest;
    }
}
