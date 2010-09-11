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

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * A maven 2.0 plugin for generating phpdocumentor documentations.This plugin is
 * used in the <code>site</code> phase.
 *
 * @goal phpdocumentor
 * @phase site
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public class PhpDocumentor extends AbstractApiDocReport {

    /**
     * Path to phpDoc. If nothing is configured phpdoc is expected in the path.
     *
     * @parameter
     */
    private String phpDocFilePath = "phpdoc";

    /**
     * Path to the php executable.
     *
     * @parameter
     */
    private String phpExe = "php";

    /**
     * The phpdoc configuraton file. The default is ${project.basedir}/phpdoc.config
     *
     * @parameter expression="${project.basedir}/phpdoc.config";
     * @required
     */
    private File phpDocConfigFile;

    /**
     * The generated phpDoc file.
     *
     * @parameter expression="${project.build.directory}/site/phpdoc/phpdoc.ini";
     * @required
     * @readonly
     */
    private File generatedPhpDocConfigFile;

    private void writeReport() {
        if (getSink() != null)  {
            getSink().rawText(
                "<a href=\"phpdocumentor/HTMLframesConverter/default/index.html\" target=\"_blank\">" +
                    "Show documention<br>" +
                    "<iframe src=\"phpdocumentor/HTMLframesConverter/default/index.html\"" +
                    "frameborder=0 style=\"border=0px;width:100%;height:400px\">");
        }
    }
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        try {
            System.out.println(phpDocConfigFile.getAbsolutePath());
            if (phpDocConfigFile.isFile()) {
                final Properties properties = new Properties();

                properties.load(new FileInputStream(phpDocConfigFile));
                properties.put("directory", getProject().getBasedir() + "/"
                    + getSourceDirectory());
                properties.put("target", getApiDocOutputDirectory().
                    getAbsoluteFile().getPath() + "/" + getFolderName());

                writePropFile(properties, generatedPhpDocConfigFile, "[Parse Data]");
                final String path = System.getProperty("java.library.path");
                getLog().debug("PATH: " + path);
                final String[] paths = path.split(File.pathSeparator);
                File phpDocFile = null;
                if ("phpdoc".equals(phpDocFilePath)) {
                    for (int i = 0; i < paths.length; i++) {
                        final File file = new File(paths[i], "phpdoc");
                        if (file.isFile()) {
                            phpDocFile = file;
                            break;
                        }
                    }
                } else {
                    phpDocFile = new File(phpDocFilePath);
                }
                if (phpDocFile == null || !phpDocFile.isFile()) {
                    throw new PhpDocumentorNotFoundException();
                }
                final String executing = phpExe
                    + " phpdoc -c \"" + generatedPhpDocConfigFile.getAbsolutePath() + "\"";
                getLog().debug("Executing PHPDocumentor: " + executing);
                final Commandline commandLine = new Commandline(executing);
                commandLine.setWorkingDirectory(phpDocFile.getParent());

                final int executeCommandLine = CommandLineUtils.executeCommandLine(
                    commandLine, new StreamConsumer() {

                        public void consumeLine(String line) {
                            getLog().debug("system.out: " + line);
                        }

                    }, new StreamConsumer() {
                        public void consumeLine(String line) {

                            getLog().debug("system.err: " + line);
                        }

                    });
                if (executeCommandLine == 1) {
                    throw new PhpDocumentorExecuteException(phpDocFile.getParent());
                }

            }
        /*CHECKSTYLE:OFF*/
        } catch (Exception e) {
        /*CHECKSTYLE:ON*/
            throw new MavenReportException(e.getMessage(), e);
        }
        writeReport();
    }

    /**
     * The name to use localized by a locale.
     *
     * @param locale the locale to localize
     * @return the name
     */
    public String getName(Locale locale) {
        return "PHPDocumentor";
    }

    /**
     * Returns the description text, dependent of the locale.
     *
     * @param locale the locale to localize
     * @return the text
     */
    public String getDescription(Locale locale) {
        return "PHPDocumentor generated documentation";
    }

    public String getOutputName() {
        return "apidocs/phpdocumentor";
    }
    @Override
    protected String getFolderName() {
        return "phpdocumentor";
    }

}
