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
        writeString(getBuildFile(),
                "plugins {\r\n" +
                "  id 'io.github.kevin-wimmer.kjar' version '7.74.1.Final'\r\n" +
                "}\r\n" +
                "group = 'io.github.kevinwimmer.test'\r\n" +
                "version = '1.0.0'\r\n" +
                "repositories {\r\n" +
                "  mavenCentral()\r\n" +
                "}\r\n" +
                "dependencies {\r\n" +
                "  implementation 'org.drools:drools-core:7.74.1.Final'\r\n" +
                "  implementation 'com.google.guava:guava:31.1-jre'\r\n" +
                "}");
        writeString(getDrlFile(),
                "package io.github.kevinwimmer.test;\r\n" +
                "import java.net.InetAddress;\r\n" +
                "import java.time.DayOfWeek;\r\n" +
                "import java.time.LocalDate;\r\n" +
                "import com.google.common.net.InetAddresses;\r\n" +
                "rule \"Is it Friday?\"\r\n" +
                "when\r\n" +
                "  LocalDate(dayOfWeek == DayOfWeek.FRIDAY)\r\n" +
                "then\r\n" +
                "  System.out.println(\"Today is Friday!\");\r\n" +
                "end\r\n" +
                "rule \"What's My IP Address?\"\r\n" +
                "when\r\n" +
                "  $inetAddress : InetAddress()\r\n" +
                "then\r\n" +
                "  System.out.println(\"My IP address is: \" + InetAddresses.toAddrString($inetAddress));\r\n" +
                "end\r\n");
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
