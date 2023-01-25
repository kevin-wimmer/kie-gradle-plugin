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

package io.github.kevinwimmer.gradle.tasks;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.compiler.compiler.io.memory.MemoryFile;
import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.compiler.kie.builder.impl.CompilationCacheProvider;
import org.drools.compiler.kie.builder.impl.DrlProject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieBuilderImpl;
import org.drools.compiler.kie.builder.impl.MemoryKieModule;
import org.drools.compiler.kie.builder.impl.ResultsImpl;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;

import io.github.kevinwimmer.kie.resources.DiskResourceStore;
import io.github.kevinwimmer.maven.pom.ProjectPomModel;

/**
 * Builds a KieModule from the project source files.
 * <p>
 *   This class is largely derived from the
 *   <a href="https://github.com/kiegroup/droolsjbpm-integration/tree/main/kie-maven-plugin">Kie Maven Plugin</a>
 *   project.
 * </p>
 *
 * @author Kevin Wimmer
 */
@DisableCachingByDefault(because = "Not worth caching")
public class CompileDrools extends DefaultTask {

    @TaskAction
    public void compileDrools() {
        Project project = getProject();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(createClassLoader(project));
        try {
            File sourceDir = project
                    .getExtensions()
                    .getByType(JavaPluginExtension.class)
                    .getSourceSets()
                    .getByName(MAIN_SOURCE_SET_NAME)
                    .getResources()
                    .getSrcDirs()
                    .iterator()
                    .next();
            File outputDir = project
                    .getExtensions()
                    .getByType(JavaPluginExtension.class)
                    .getSourceSets()
                    .getByName(MAIN_SOURCE_SET_NAME)
                    .getOutput()
                    .getClassesDirs()
                    .getSingleFile();

            KieServices ks = KieServices.Factory.get();
            KieBuilderImpl kieBuilder = (KieBuilderImpl) ks.newKieBuilder(project.getProjectDir());
            kieBuilder.setPomModel(new ProjectPomModel(project));
            kieBuilder.buildAll(DrlProject.SUPPLIER,
                    s -> s.contains(sourceDir.getAbsolutePath()) || s.endsWith("pom.xml"));
            InternalKieModule kModule = (InternalKieModule) kieBuilder.getKieModule();
            ResultsImpl messages = (ResultsImpl) kieBuilder.getResults();

            List<Message> errors = messages != null ? messages.filterMessages(Message.Level.ERROR) : Collections.emptyList();

            CompilationCacheProvider.get().writeKieModuleMetaInfo(kModule, new DiskResourceStore(outputDir));

            if (!errors.isEmpty()) {
                for (Message error : errors) {
                    getLogger().error(error.toString());
                }
                throw new GradleException("Build failed!");
            } else {
                writeClassFiles(kModule, outputDir);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        getLogger().info("KieModule successfully built!");
    }

    private ClassLoader createClassLoader(Project project) {
        Set<URL> urls = new HashSet<>();
        project
                .getConfigurations()
                .getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME)
                .getResolvedConfiguration()
                .getFiles()
                .forEach(dependency -> urls.add(urlOf(dependency)));
        project
                .getExtensions()
                .getByType(JavaPluginExtension.class)
                .getSourceSets()
                .getByName(MAIN_SOURCE_SET_NAME)
                .getOutput()
                .getClassesDirs()
                .forEach(dir -> urls.add(urlOf(dir)));
        return URLClassLoader.newInstance(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    private URL urlOf(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeClassFiles(InternalKieModule kModule, File outputDir) {
        MemoryFileSystem mfs = ((MemoryKieModule) kModule).getMemoryFileSystem();
        kModule.getFileNames()
                .stream()
                .filter(name -> name.endsWith(".class")
                        && !name.contains("build/classes") && !name.contains("build\\classes"))
                .forEach(fileName -> saveFile(mfs, fileName, outputDir));
    }

    private void saveFile(MemoryFileSystem mfs, String fileName, File outputDir) {
        MemoryFile memFile = (MemoryFile) mfs.getFile(fileName);
        final Path path = Paths.get(outputDir.getPath(), memFile.getPath().toPortableString());
        try {
            Files.deleteIfExists(path);
            Files.createDirectories(path);
            Files.copy(memFile.getContents(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write file: " + path, e);
        }
    }
}
