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

package io.github.kevinwimmer.maven.pom;

import static java.util.stream.Collectors.toList;

import java.util.Collection;

import org.appformer.maven.support.AFReleaseId;
import org.appformer.maven.support.AFReleaseIdImpl;
import org.appformer.maven.support.DependencyFilter;
import org.appformer.maven.support.PomModel;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class ProjectPomModel implements PomModel {

    private Project project;

    public ProjectPomModel(Project project) {
        this.project = project;
    }

    @Override
    public AFReleaseId getReleaseId() {
        return new AFReleaseIdImpl(project.getGroup().toString(), project.getName(), project.getVersion().toString());
    }

    @Override
    public AFReleaseId getParentReleaseId() {
        Project parent = project.getParent();
        return parent == null ? null : new AFReleaseIdImpl(parent.getGroup().toString(), parent.getName(), parent.getVersion().toString());
    }

    @Override
    public Collection<AFReleaseId> getDependencies() {
        return project
                .getConfigurations()
                .stream()
                .flatMap(config -> getAllDependencies(config).stream())
                .collect(toList());
    }

    private Collection<AFReleaseId> getAllDependencies(Configuration config) {
        return config
                .getAllDependencies()
                .stream()
                .map(dep -> new AFReleaseIdImpl(dep.getGroup(), dep.getName(), dep.getVersion()))
                .collect(toList());
    }

    @Override
    public Collection<AFReleaseId> getDependencies(DependencyFilter filter) {
        return project
                .getConfigurations()
                .getAsMap()
                .entrySet()
                .stream()
                .flatMap(entry -> getAllDependencies(entry.getValue()).stream().filter(dep -> filter.accept(dep, entry.getKey())))
                .collect(toList());
    }
}
