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
import java.io.IOException;
import java.util.List;

/**
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public final class FileHelper {

    /**
     * Copies over a file from the sourceDirectory to the targetDirectory perserving its relative subdirectories.
     *
     * @param sourceDirectory
     * @param targetDirectory
     * @param sourceFile
     * @param forceOverwrite
     * @throws IOException
     */
    public static void copyToFolder(
            File sourceDirectory,
            File targetDirectory,
            File sourceFile,
            boolean forceOverwrite)
            throws IOException {

        String relativeFile = PathUtils.toRelative(sourceDirectory.getAbsoluteFile(), sourceFile.getAbsolutePath());
        File targetFile = new File(targetDirectory, relativeFile);

        if (forceOverwrite) {
            FileUtils.copyFile(sourceFile, targetFile);
        } else {
            FileUtils.copyFileIfModified(sourceFile, targetFile);
        }
    }

    /**
     * Unzips all files to the given directory (using jar)
     *
     * @param targetDirectory
     * @param elements
     * @throws IOException
     */
    public static void unzipElements(File targetDirectory, List<String> elements) throws IOException {
        targetDirectory.mkdirs();
        for (String element: elements) {
            File sourceFile = new File(element);
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
        java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
        java.util.Enumeration<java.util.jar.JarEntry> items = jar.entries();

        while (items.hasMoreElements()) {
            java.util.jar.JarEntry file = (java.util.jar.JarEntry) items
                    .nextElement();

            java.io.File f = new java.io.File(destDir + java.io.File.separator
                    + file.getName());
            if (f.exists() ) {
                continue;
            }
            if (file.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }

            java.io.InputStream is = jar.getInputStream(file); // get the input
            // stream
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            IOUtil.copy(is, fos);
//			while (is.available() > 0) { // write contents of 'is' to 'fos'
//				fos.write(is.read());
//			}
            fos.close();
            is.close();
        }
    }
}
