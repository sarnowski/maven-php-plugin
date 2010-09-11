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

import com.google.common.collect.Lists;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * PHPUnit executes <a href="http://www.phpunit.de/">phpunit</a> TestCases and
 * generate SourceFire Reports.
 *
 * CHECKSTYLE:OFF unknown tags
 * @requiresDependencyResolution test
 * @goal test
 * CHECKSTYLE:ON
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public final class PhpTest extends AbstractPhpMojo {

    /**
     * Possible system property to override test process.
     * Should be reformed to maven configuration parameter
     */
    public static final String TEST_FILE = "testFile";

    /**
     * Where the test results should be stored.
     *
     * Default: target/surefire-reports
     *
     * @parameter
     */
    private String resultFolder = "target/surefire-reports";

    /**
     * If the parameter is set only this testFile will run.
     *
     * Default: -unset-
     *
     * @parameter
     */
    private String testFile;

    /**
     * Which postfix will be used to find test-cases. The default ist "Test" and
     * all php files, ending with Test will be treated as test case files.
     * E.g. Logic1Test.php will be used.
     *
     * Default: Test
     *
     * @parameter
     */
    private String testPostfix = "Test";

    /**
     * Collection of test results.
     */
    private List<SurefireResult> surefireResults = Lists.newArrayList();

    public PhpTest() {
        super();
        if (System.getProperty(TEST_FILE) != null) {
            testFile = System.getProperty(TEST_FILE);
        }
    }

    public File getResultFolder() {
        return new File(getBaseDir(), resultFolder);
    }

    public String getPlainResultFolder() {
        return resultFolder;
    }

    public String getTestPostfix() {
        return testPostfix;
    }

    /**
     * Prepares the test paths.
     *
     * @throws IOException if something on copying the files goes wrong
     * @throws PhpException can fly anytime..
     */
    @Override
    protected void prepareTestDependencies() throws IOException, PhpException {
        super.prepareTestDependencies();

        if (getPhpVersion() == PhpVersion.PHP5) {
            final File mavenTestFile = new File(getTestDependenciesTargetDirectory() + "/PHPUnit/TextUI/Maven.php");
            if (!mavenTestFile.exists()) {
                final URL mavenUrl = getClass().getResource("Maven.php");
                FileUtils.copyURLToFile(mavenUrl, mavenTestFile);
            }
        }
    }

    /**
     * php:test execution startpoint.
     *
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getProject().addTestCompileSourceRoot(getTestSourceDirectory().getAbsolutePath());

            final File testSourceFolder = getTestSourceDirectory();
            if (!testSourceFolder.isDirectory()) {
                getLog().info("No test cases found; skipping.");
                return;
            }

            if (!isIncludeInJar()) {
                getLog().info("Not including php sources in resulting output.");
            }

            final File folder = getResultFolder();
            folder.mkdirs();

            prepareTestDependencies();
            getLog().info("Surefire report directory: " + folder.getAbsolutePath());
            System.out.println("\n-------------------------------------------------------");
            System.out.println("T E S T S");
            System.out.println("-------------------------------------------------------");

            goRecursiveAndCall(testSourceFolder);

            System.out.println();
            System.out.println("Results :");
            System.out.println();

            int completeTests = 0;
            int completeFailures = 0;
            int completeErrors = 0;

            for (SurefireResult surefireResult : surefireResults) {
                completeTests += surefireResult.getTests();
                completeFailures += surefireResult.getFailure();
                completeErrors += surefireResult.getErrors();
            }

            System.out.println("Tests run: " + completeTests
                + ", Failures: " + completeFailures
                + ", Errors: " + completeErrors + "\n");

            if (completeErrors != 0 || completeFailures != 0) {
                throw new UnitTestCaseFailureException(completeErrors, completeFailures);
            }

        } catch (MultiException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (PhpException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (UnitTestCaseFailureException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Tests a single PHP file.
     *
     * @param file the PHP file to test
     * @throws MojoExecutionException if something goes wrong.
     */
    protected void handlePhpFile(File file) throws MojoExecutionException {
        // by definition, handlePhpFile will only be called for .php files.
        final String ending = "." + getPhpFileEnding();

        if (!isTestFile(file)) {
            return;
        }

        // replace file ending with .xml
        String name = file.getName();
        name = name.substring(0, name.length() - ending.length()) + ".xml";
        final File targetFile = new File(getResultFolder(), name);

        // create report directory
        targetFile.getParentFile().mkdirs();

        try {
            final String command = createCommandLine(file, targetFile);
            String output = "-no output-";
            try {
                output = execute(command, file);
            } catch (PhpException e) {
                writeFailure(file, targetFile, e.getAppendedOutput());
            }

            if (targetFile.exists()) {
                parseResultingXML(targetFile);
            } else {
                throw new PhpErrorException(file, output);
            }
        } catch (PhpException e) {
            try {
                writeFailure(file, targetFile, e.getMessage());
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (IOException ioe) {
                throw new MojoExecutionException(ioe.getMessage(), ioe);
            }
        } catch (SAXException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean isTestFile(File file) {
        // check if the test file matches the path
        if (testFile != null && !(File.separatorChar + file.getAbsolutePath()).endsWith(testFile)) {
            return false;
        }

        // only files ending with "Test" are treated as testcase files
        if (!file.getName().toLowerCase().endsWith(getTestPostfix().toLowerCase() + "." + getPhpFileEnding())) {
            return false;
        }

        return true;
    }

    private String createCommandLine(File file, File targetFile) throws PhpException {
        String command = includePathParameter(new String[]{
            getSourceDirectory().getAbsolutePath(),
            getTestSourceDirectory().getAbsolutePath(),
            getDependenciesTargetDirectory().getAbsolutePath(),
            getTestDependenciesTargetDirectory().getAbsolutePath(),
            file.getParentFile().getAbsolutePath()
        });

        if (getPhpVersion() == PhpVersion.PHP5) {
            command +=
                " \"" + getTestDependenciesTargetDirectory().getAbsolutePath() + "/PHPUnit/TextUI/Maven.php\""
                    + " \"" + file.getAbsolutePath() + "\""
                    + " \"" + targetFile.getAbsolutePath() + "\"";
        } else if (getPhpVersion() == PhpVersion.PHP4) {
            command +=
                " \"" + getTestDependenciesTargetDirectory().getAbsolutePath() + "/XMLWriter.php\""
                    + " \"" + file.getAbsolutePath() + "\""
                    + " \"" + targetFile.getAbsolutePath() + "\"";
        }
        return command;
    }

    /**
     * Parses the XML output.
     *
     * @param file
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void parseResultingXML(File file) throws SAXException, IOException, ParserConfigurationException {
        final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = fact.newDocumentBuilder();

        final Document doc = builder.parse(file);
        final NodeList elementsByTagName = doc.getElementsByTagName("testsuite");
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            final Element e = (Element) elementsByTagName.item(i);

            final SurefireResult surefireResult = new SurefireResult(
                e.getAttribute("name"),
                Integer.parseInt(e.getAttribute("tests")),
                Integer.parseInt(e.getAttribute("failures")),
                Integer.parseInt(e.getAttribute("errors")), e.getAttribute("time")
            );
            System.out.println(surefireResult.toString());
            System.out.println();
            surefireResults.add(surefireResult);
        }
    }

    /**
     * Write message to report file.
     *
     * @param testCase
     * @param targetReportFilePath
     * @param output
     * @throws IOException
     */
    private void writeFailure(File testCase, File targetReportFilePath, String output) throws IOException {
        String logFile = targetReportFilePath.getAbsolutePath();
        logFile = logFile.substring(0, logFile.length() - ".xml".length()) + ".txt";

        getLog().error("Testcase: " + testCase.getName() + " fails.");
        getLog().error("See log: " + logFile);
        final FileWriter fstream = new FileWriter(logFile);
        final BufferedWriter out = new BufferedWriter(fstream);
        try {
            out.write(output);
        } finally {
            out.close();
        }
    }

    @Override
    protected void handleProcessedFile(File file) throws MojoExecutionException {
        if (!isIncludeInJar()) {
            return;
        }

        try {
            FileHelper.copyToFolder(
                getTestSourceDirectory(),
                getTargetTestClassesDirectory(),
                file,
                isForceOverwrite());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy test sources to target directory", e);
        }
    }

    /**
     * Represents a surefire result parsed from its xml output.
     */
    class SurefireResult {
        private final String name;
        private final int tests;
        private final int failure;
        private final int errors;
        private final String time;

        public SurefireResult(String name, int tests, int failure, int errors, String time) {
            super();
            this.name = name;
            this.tests = tests;
            this.failure = failure;
            this.errors = errors;
            this.time = time;
        }

        @Override
        public String toString() {
            return "Running " + name + "\n"
                + "Tests run: " + tests
                + ", Failures: " + failure
                + ", Errors: " + errors
                + ", Time elapsed: " + time;
        }

        public int getTests() {
            return tests;
        }

        public int getFailure() {
            return failure;
        }

        public int getErrors() {
            return errors;
        }
    }
}
