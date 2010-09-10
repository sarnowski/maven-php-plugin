/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.phpmaven.plugin.build;

import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Static utilities for file handling.
 *
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public final class FileHelper {

    private FileHelper() {
        // we only have static methods
    }

    /**
     * Copies over a file from the sourceDirectory to the targetDirectory perserving its relative subdirectories.
     *
     * @param sourceDirectory where the main source directory is
     * @param targetDirectory where the target directory is
     * @param sourceFile which file to copy to the target directory
     * @param forceOverwrite if timestamps should be ignored
     * @throws IOException if something goes wrong while copying
     */
    public static void copyToFolder(
        File sourceDirectory,
        File targetDirectory,
        File sourceFile,
        boolean forceOverwrite)
        throws IOException {

        final String relativeFile = PathUtils.toRelative(
            sourceDirectory.getAbsoluteFile(),
            sourceFile.getAbsolutePath()
        );
        final File targetFile = new File(targetDirectory, relativeFile);

        if (forceOverwrite) {
            FileUtils.copyFile(sourceFile, targetFile);
        } else {
            FileUtils.copyFileIfModified(sourceFile, targetFile);
        }
    }

    /**
     * Unzips all files to the given directory (using jar).
     *
     * @param targetDirectory where to unpack the files to
     * @param elements list of files to unpack
     * @throws IOException if something goes wrong while copying
     */
    public static void unzipElements(File targetDirectory, List<String> elements) throws IOException {
        targetDirectory.mkdirs();
        for (String element : elements) {
            final File sourceFile = new File(element);
            if (sourceFile.isFile()) {
                unjar(sourceFile, targetDirectory);
            }
        }
    }

    /**
     * Unpacks a jar file.
     *
     * @param jarFile
     * @param destDir
     * @throws IOException
     */
    private static void unjar(File jarFile, File destDir) throws IOException {
        final JarFile jar = new JarFile(jarFile);

        final Enumeration<JarEntry> items = jar.entries();
        while (items.hasMoreElements()) {
            final JarEntry entry = items.nextElement();
            final File destFile = new File(destDir, entry.getName());

            if (destFile.exists()) {
                continue;
            }
            if (entry.isDirectory()) {
                destFile.mkdir();
                continue;
            }

            InputStream in = null;
            OutputStream out = null;
            try {
                in = jar.getInputStream(entry);
                out = new FileOutputStream(destFile);
                IOUtil.copy(in, out);
            } finally {
                if (out != null) out.close();
                if (in != null) in.close();
            }
        }
    }
}
