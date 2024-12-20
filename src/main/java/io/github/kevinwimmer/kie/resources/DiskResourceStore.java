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

package io.github.kevinwimmer.kie.resources;

import static org.drools.util.IoUtils.readBytesFromInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.drools.util.PortablePath;
import org.gradle.api.UncheckedIOException;
import org.kie.memorycompiler.resources.ResourceStore;

public class DiskResourceStore implements ResourceStore {

    private final File root;

    public DiskResourceStore(File root) {
        this.root = root;
    }

    @Override
    public void write(PortablePath resourcePath, byte[] resourceData) {
        write(resourcePath, resourceData, false);
    }

    @Override
    public void write(PortablePath resourcePath, byte[] resourceData, boolean createFolder) {
        File file = new File(getFilePath(resourcePath.asString()));
        if (createFolder) {
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(resourceData);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] read(PortablePath resourcePath) {
        try (FileInputStream fis = new FileInputStream(getFilePath(resourcePath.asString()))) {
            return readBytesFromInputStream(fis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void remove(PortablePath resourcePath) {
        try {
            Files.deleteIfExists(new File(getFilePath(resourcePath.asString())).toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getFilePath(String resourceName) {
        return root.getAbsolutePath() + File.separator + resourceName;
    }
}
