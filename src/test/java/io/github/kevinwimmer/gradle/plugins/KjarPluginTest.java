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

package io.github.kevinwimmer.gradle.plugins;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KjarPluginTest {

    @TempDir
    File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    private File getDrlFile() {
        return new File(projectDir, "src/main/resources/io/github/kevinwimmer/rules/test.drl");
    }

    @Test
    void testBuild() throws IOException {
        createProjectFiles();

        // execute the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.forwardStdOutput(new PrintWriter(System.out));
        runner.withPluginClasspath();
        runner.withArguments("build", "-info");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        // Verify the result
        assertTrue(result.getOutput().contains("KieModule successfully built!"));

        File artifact = new File(projectDir, "/build/libs/kjar-plugin-test-1.0.0.jar");
        assertTrue(artifact.exists());

        try (JarFile kjar = new JarFile(artifact)) {
            assertKjarEntryExists("io/github/kevinwimmer/rules/test.drl", kjar);
            assertKjarEntryExists("META-INF/defaultKieBase/kbase.cache", kjar);
            assertKjarEntryExists("META-INF/maven/io.github.kevinwimmer.test/kjar-plugin-test/pom.properties", kjar);
            assertKjarEntryExists("META-INF/kmodule.info", kjar);
            assertKjarEntryExists("META-INF/kmodule.xml", kjar);
        }
    }

    private void createProjectFiles() throws IOException {
        writeString(getSettingsFile(), "rootProject.name = 'kjar-plugin-test'");
        writeString(getBuildFile(), """
                plugins {
                  id 'io.github.kevin-wimmer.kjar' version '%1$s-SNAPSHOT'
                }
                group = 'io.github.kevinwimmer.test'
                version = '1.0.0'
                repositories {
                  mavenCentral()
                }
                dependencies {
                  implementation 'org.drools:drools-core:%1$s'
                  implementation 'com.google.guava:guava:31.1-jre'
                }""".formatted("8.44.2.Final"));
        writeString(getDrlFile(), """
                package io.github.kevinwimmer.rules;
                import java.net.InetAddress;
                import java.time.DayOfWeek;
                import java.time.LocalDate;
                import com.google.common.net.InetAddresses;
                rule "Is it Friday?"
                when
                  LocalDate(dayOfWeek == DayOfWeek.FRIDAY)
                then
                  System.out.println("Today is Friday!");
                end
                rule "What's My IP Address?"
                when
                  $inetAddress : InetAddress()
                then
                  System.out.println("My IP address is: " + InetAddresses.toAddrString($inetAddress));
                end""");
    }

    private void writeString(File file, String string) throws IOException {
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }

    private void assertKjarEntryExists(String entry, JarFile kjar) {
        assertNotNull(kjar.getEntry(entry), "File \"" + entry + "\" not found");
    }
}
