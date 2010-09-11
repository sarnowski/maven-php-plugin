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

package org.phpmaven.plugin.report;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Abstract base class for api docs.
 *
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public abstract class AbstractApiDocReport extends AbstractMavenReport {

    /**
     * The output directory of doxygen generated documentation.
     *
     * @parameter expression="${project.build.directory}/site/apidocs"
     * @required
     */
    private File outputApiDocDirectory;

    /**
     * The source code directory where doxygen start generating documentation.
     *
     * @parameter expression="/src/main/php";
     * @required
     */
    private String sourceDirectory = "/src/main/php";

    /**
     * <i>Maven Internal</i>: The Doxia Site Renderer.
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * <i>Maven Internal</i>: The Project descriptor.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Where to store the output.
     *
     * @return the folder
     */
    protected abstract String getFolderName();

    protected File getApiDocOutputDirectory() {
        return outputApiDocDirectory;
    }

    protected String getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Creates a property file.
     *
     * @param properties the properties to use
     * @param generatedPhpDocConfigFile the resulting file
     * @param preFileContent templates
     * @throws IOException if something goes wrong while writing
     */
    protected void writePropFile(Properties properties,
                                 File generatedPhpDocConfigFile, String preFileContent)
        throws IOException {
        final String lineSeparator = System.getProperty("line.separator");
        generatedPhpDocConfigFile.getParentFile().mkdirs();
        final FileWriter fileWriter = new FileWriter(generatedPhpDocConfigFile);
        final Set<Object> keySet = properties.keySet();
        if (preFileContent != null)
            fileWriter.append(preFileContent + lineSeparator);

        final Iterator<Object> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            final String key = (String) iterator.next();
            final String value = properties.getProperty(key);
            fileWriter.append(key + "=" + value + lineSeparator);
        }
        fileWriter.close();
    }

    /**
     * Sets the renderer for site generation.
     *
     * @param siteRenderer the siteRenderer to set.
     */
    public void setSiteRenderer(Renderer siteRenderer) {
        this.siteRenderer = siteRenderer;
    }

    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    protected MavenProject getProject() {
        return project;
    }

    protected String getOutputDirectory() {
        return getApiDocOutputDirectory().getAbsolutePath();
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

}
