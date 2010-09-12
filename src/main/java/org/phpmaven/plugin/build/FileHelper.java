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

import com.google.common.base.Preconditions;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

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
    public static void copyToFolder(File sourceDirectory, File targetDirectory, File sourceFile, boolean forceOverwrite)
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
        Preconditions.checkArgument(
            !targetDirectory.exists() || targetDirectory.isDirectory(),
            "Destination Directory");

        targetDirectory.mkdirs();
        if (!targetDirectory.exists()) {
            throw new IllegalStateException("Could not create target directory " + targetDirectory.getAbsolutePath());
        }

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
     * @param jarFile the jar file
     * @param destDir the destination directory
     * @throws IOException if something goes wrong
     */
    public static void unjar(File jarFile, File destDir) throws IOException {
        Preconditions.checkNotNull(jarFile, "JarFile");

        final JarFile jar = new JarFile(jarFile);

        final Enumeration<JarEntry> items = jar.entries();
        while (items.hasMoreElements()) {
            final JarEntry entry = items.nextElement();
            unpackJarEntry(entry, jar.getInputStream(entry), destDir);
        }
    }

    /**
     * Unpacks a jar URI.
     *
     * @param jarUri the jar uri
     * @param destDir the destination directory
     * @throws IOException if something goes wrong
     */
    public static void unjar(URI jarUri, File destDir) throws IOException {
        Preconditions.checkNotNull(jarUri, "JarFile");

        unjar(jarUri.toURL().openStream(), destDir);
    }

    /**
     * Unpacks a jar stream.
     *
     * @param inputStream the jar stream
     * @param destDir the destination directory
     * @throws IOException if something goes wrong
     */
    public static void unjar(InputStream inputStream, File destDir) throws IOException {
        Preconditions.checkNotNull(inputStream, "InputStream");

        final JarInputStream jarInputStream = new JarInputStream(inputStream);
        while (true) {
            final JarEntry entry = jarInputStream.getNextJarEntry();
            if (entry == null) {
                break;
            }
            unpackJarEntry(entry, jarInputStream, destDir);
        }
    }

    /**
     * Unpacks a single jar entry.
     *
     * @param jarEntry the jar entry
     * @param jarEntryInputStream the source stream of the entry
     * @param destDir the destination directory
     * @throws IOException if something goes wrong
     */
    public static void unpackJarEntry(JarEntry jarEntry, InputStream jarEntryInputStream, File destDir)
        throws IOException {

        Preconditions.checkNotNull(jarEntry, "JarEntry");
        Preconditions.checkNotNull(jarEntryInputStream, "JarEntryInputStream");
        Preconditions.checkNotNull(destDir, "Destination Directory");
        Preconditions.checkArgument(!destDir.exists() || destDir.isDirectory(), "Destination Directory");

        // final name
        final File destFile = new File(destDir, jarEntry.getName());

        // already there
        if (destFile.exists()) {
            return;
        }

        // just a directory to create
        if (jarEntry.isDirectory()) {
            destFile.mkdir();
            return;
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(destFile);
            IOUtil.copy(jarEntryInputStream, out);
        } finally {
            if (out != null) out.close();
        }
    }
}
