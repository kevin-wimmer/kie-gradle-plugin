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

package io.github.kevinwimmer.maven.pom;

import java.util.Collection;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.kie.api.builder.ReleaseId;
import org.kie.util.maven.support.DependencyFilter;
import org.kie.util.maven.support.PomModel;
import org.kie.util.maven.support.ReleaseIdImpl;

public class ProjectPomModel implements PomModel {

    private Project project;

    public ProjectPomModel(Project project) {
        this.project = project;
    }

    @Override
    public ReleaseId getReleaseId() {
        return new ReleaseIdImpl(project.getGroup().toString(), project.getName(), project.getVersion().toString());
    }

    @Override
    public ReleaseId getParentReleaseId() {
        Project parent = project.getParent();
        return parent == null ? null : new ReleaseIdImpl(parent.getGroup().toString(), parent.getName(), parent.getVersion().toString());
    }

    @Override
    public Collection<ReleaseId> getDependencies() {
        return project
                .getConfigurations()
                .stream()
                .flatMap(config -> getAllDependencies(config).stream())
                .toList();
    }

    private Collection<ReleaseId> getAllDependencies(Configuration config) {
        return config
                .getAllDependencies()
                .stream()
                .map(dep -> (ReleaseId) new ReleaseIdImpl(dep.getGroup(), dep.getName(), dep.getVersion()))
                .toList();
    }

    @Override
    public Collection<ReleaseId> getDependencies(DependencyFilter filter) {
        return project
                .getConfigurations()
                .getAsMap()
                .entrySet()
                .stream()
                .flatMap(entry -> getAllDependencies(entry.getValue()).stream().filter(dep -> filter.accept(dep, entry.getKey())))
                .toList();
    }
}
