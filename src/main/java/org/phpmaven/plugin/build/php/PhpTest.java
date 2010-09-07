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

package org.phpmaven.plugin.build.php;

import com.google.common.collect.Lists;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.phpmaven.plugin.build.FileHelper;
import org.phpmaven.plugin.build.UnitTestCaseFailureException;
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
 * @requiresDependencyResolution test
 * @goal phpunit
 */
public class PhpTest extends AbstractPhpExecutor {
    private List<SurefireResult> surefireResults = Lists.newArrayList();

    /**
     * Project test classpath elements.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List<String> testClasspathElements;

    /**
     * If the parameter is set only this testFile will run.
     * NB: do not specify the file path. Only specify the file name.
     *
     * @parameter expression=""
     */
    private String testFile="";
    /**
     * The test source Folder.
     *
     * @parameter expression="/src/test/php"
     * @required
     */
    private String testDirectory;

    /**
     * where to keep the test results?
     */
    File resultFolder;


    /**
     * prepares the paths
     *
     * @throws IOException
     * @throws PhpException
     */
    protected final void prepareTestDependencies() throws IOException, PhpException {
        FileHelper.prepareDependencies(baseDir.toString() + Statics.phpinc, testClasspathElements);

        if (getPhpVersion() == PhpVersion.PHP5) {
            File mavenTestFile = new File(new Statics(baseDir).getPhpInc() + "/PHPUnit/TextUI/Maven.php");
            if (!mavenTestFile.exists()) {
                URL mavenUrl = getClass().getResource("Maven.php");
                FileUtils.copyURLToFile(mavenUrl, mavenTestFile);
            }
        }
    }

    /**
     * php:phpunit
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            project.addTestCompileSourceRoot(testDirectory);
            testFile = System.getProperty("testFile") != null ? System.getProperty("testFile") : "";

            resultFolder = new File(baseDir.getAbsoluteFile() + "/target/surefire-reports");

            resultFolder.mkdirs();
            if (!(new File(baseDir.getAbsoluteFile() + "/" + testDirectory)).isDirectory()) {
                getLog().info("No test cases found");
                return;
            }

            prepareTestDependencies();
            getLog().info("Surefire report directory: " + resultFolder.getAbsolutePath());
            System.out.println("\n-------------------------------------------------------");
            System.out.println("T E S T S");
            System.out.println("-------------------------------------------------------");

            goRecursiveAndCall(new File(baseDir.getAbsoluteFile() + testDirectory));

            System.out.println();
            System.out.println("Results :");
            System.out.println();

            int completeTests = 0;
            int completeFailures = 0;
            int completeErrors = 0;

            for (int i = 0; i < surefireResults.size(); i++) {
                completeTests += surefireResults.get(i).getTests();
                completeFailures += surefireResults.get(i).getFailure();
                completeErrors += surefireResults.get(i).getErrors();
            }

            System.out.println("Tests run: " + completeTests
                    + ", Failures: " + completeFailures
                    + ", Errors: " + completeErrors + "\n");

            if (completeErrors!=0 || completeFailures!=0) {
                throw new UnitTestCaseFailureException(completeErrors, completeFailures);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public PhpTest() {

    }

    private void parseResultingXML(File file) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = fact.newDocumentBuilder();

        Document doc = builder.parse(file);
        NodeList elementsByTagName = doc.getElementsByTagName("testsuite");
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            Element e = ((Element) elementsByTagName.item(i));

            SurefireResult surefireResult = new SurefireResult(e
                    .getAttribute("name"), Integer.parseInt(e
                    .getAttribute("tests")), Integer.parseInt(e
                    .getAttribute("failures")), Integer.parseInt(e
                    .getAttribute("errors")), e.getAttribute("time")

            );
            System.out.println(surefireResult.toString());
            System.out.println();
            surefireResults.add(surefireResult);
        }
    }

    protected void executePhpFile(File file) throws MojoExecutionException {
        String targetFile = resultFolder.getAbsolutePath() + "/" + file.getName().replace(".php", ".xml");

        if (!"".equals(testFile) && !file.getName().equals(testFile)) {
            return;
        }

        if (!file.getName().toLowerCase().endsWith("test.php")) {
            return;
        }

        try {
            try {
                String command = getCompilerArgs()
                        + " -d include_path=\""+File.pathSeparator
                        + baseDir.getAbsolutePath() + sourceDirectory + File.pathSeparator
                        + baseDir+"/"+testDirectory+ File.pathSeparator
                        + new Statics(baseDir).getPhpInc() + File.pathSeparator
                        + file.getParentFile().getAbsolutePath() + File.pathSeparator
                        + new Statics(baseDir).getTargetTestClassesFolder()+ File.pathSeparator
                        + new Statics(baseDir).getTargetClassesFolder() + File.pathSeparator
                        + "\" \"" + new Statics(baseDir).getPhpInc();

                if (getPhpVersion() == PhpVersion.PHP5) {
                    command += "/PHPUnit/TextUI/Maven.php\" \""
                            + file.getAbsolutePath() + "\" \"" + targetFile
                            + "\"";
                } else if (getPhpVersion() == PhpVersion.PHP4) {
                    command += "/XMLWriter.php\" \"" + file.getAbsolutePath()
                            + "\" \"" + targetFile + "\"";
                }

                String output = "no output";
                try {
                    output = execute(command, file);
                } catch (PhpException e) {
                    writeFailure(file, targetFile, e.getAppendedOutput());
                }

                // wtf?
                File targetFileObj = new File(targetFile);
                if (targetFileObj.exists()) {
                    parseResultingXML(targetFileObj);
                } else {
                    writeFailure(file, targetFile, output);
                    throw new PhpErrorException(file, output);
                }


            } catch (PhpExecutionException pex) {
                writeFailure(file, targetFile, pex.getMessage());
                throw pex;
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    /**
     * write message to report file
     *
     * @param testCase
     * @param targetReportFilePath
     * @param output
     * @throws IOException
     */
    private void writeFailure(File testCase, String targetReportFilePath, String output) throws IOException {
        String logFile = targetReportFilePath.replace(".xml", ".txt");
        getLog().error("Testcase: " + testCase.getName() + " fails.");
        getLog().error("See log: " + logFile);
        FileWriter fstream = new FileWriter(logFile);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(output);
        out.close();
    }

    @Override
    protected void handleProcesedFile(File file) throws MojoExecutionException {
        FileHelper.copyToTargetFolder(baseDir,testDirectory,file,Statics.targetTestClassesFolder,forceOverwrite);
    }

    class SurefireResult {
        String name;
        int tests = 0;
        int failure = 0;
        int errors = 0;
        String time;

        public String toString() {
            return "Running " + name + "\n"
                    + "Tests run: " + tests
                    + ", Failures: " + failure
                    + ", Errors: " + errors
                    + ", Time elapsed: " + time;
        }

        public SurefireResult(String name, int tests, int failure, int errors, String time) {
            super();
            this.name = name;
            this.tests = tests;
            this.failure = failure;
            this.errors = errors;
            this.time = time;
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
