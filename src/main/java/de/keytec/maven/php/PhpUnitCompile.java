package de.keytec.maven.php;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * PHPUnit executes <a href="http://www.phpunit.de/">phpunit</a> TestCases and
 * generate SourceFire Reports.
 * 
 * @requiresDependencyResolution test
 * @goal phpunit
 */
public class PhpUnitCompile extends AbstractPhpCompile {

	private ArrayList<SurefireResult> surefireResults = new ArrayList<SurefireResult>();
	/**
	 * Project test classpath elements.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List testClasspathElements;

	File resultFolder;
	/**
	 * The test source Folder.
	 * 
	 * @parameter expression="/src/test/php"
	 * @required
	 */
	private String testDirectory;

	protected final void prepareTestDependencies() throws IOException {
		prepareDependencies(testClasspathElements);
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {

			resultFolder = new File(baseDir.getAbsoluteFile()
					+ "/target/surefire-reports");

			resultFolder.mkdirs();
			if (new File(baseDir.getAbsoluteFile() + "/" + testDirectory)
					.isDirectory() == false) {
				getLog().info("No test cases found");
				return;
			}
			prepareTestDependencies();
			getLog().info(
					"Surefire report directory: "
							+ resultFolder.getAbsolutePath());
			System.out
					.println("\n-------------------------------------------------------");
			System.out.println("T E S T S");
			System.out
					.println("-------------------------------------------------------");
			goRecursiveAndCall(new File(baseDir.getAbsoluteFile()
					+ testDirectory));
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
			System.out.println("Tests run: " + completeTests + ", Failures: "
					+ completeFailures + ", Errors: " + completeErrors + "\n");
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}

	public PhpUnitCompile() {

	}

	/*
	 * private void compileReports() throws Exception { File[] listFiles =
	 * resultFolder.listFiles(); for (int i = 0; i < listFiles.length; i++) { if
	 * (!listFiles[i].isFile()) { continue; } File xmlFile = listFiles[i]; //
	 * JAXP liest Daten über die Source-Schnittstelle Source xmlSource = new
	 * StreamSource(xmlFile); Source xsltSource = new
	 * StreamSource(getClass().getResourceAsStream("sunfire.xslt")); // das
	 * Factory-Pattern unterstützt verschiedene XSLT-Prozessoren
	 * TransformerFactory transFact = TransformerFactory.newInstance();
	 * Transformer trans = transFact.newTransformer(xsltSource); File surfire =
	 * new File(baseDir .getAbsoluteFile() + "/target/surefire-reports/" +
	 * xmlFile.getName()); surfire.getParentFile().mkdirs(); FileWriter
	 * fileWriter = new FileWriter(surfire); trans.transform(xmlSource, new
	 * StreamResult(fileWriter)); } }
	 */

	class SurefireResult {
		String name;
		int tests = 0;
		int failure = 0;
		int errors = 0;
		String time;

		public String toString() {
			return "Running " + name + "\n" + "Tests run: " + tests
					+ ", Failures: " + failure + ", Errors: " + errors
					+ ", Time elapsed: " + time;
		}

		public SurefireResult(String name, int tests, int failure, int errors,
				String time) {
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

	private void parseResultingXML(File file) throws SAXException, IOException,
			ParserConfigurationException {
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
			surefireResults.add(surefireResult);
		}
	}

	protected void executePhpFile(File file) throws MojoExecutionException {
		String targetFile = resultFolder.getAbsolutePath() + "/"
				+ file.getName().replace(".php", ".xml");
		try {
			try {
				String command = phpExe + getCompilerArgs()
						+ " -d include_path=\";"
						+ file.getParentFile().getAbsolutePath() + ";"
						+ baseDir.getAbsoluteFile() + testDirectory + ";"
						+ new Statics(baseDir).getTargetClassesFolder() + ";"
						+ new Statics(baseDir).getPhpInc() + ";"
						+ baseDir.getAbsolutePath() + sourceDirectory + ";"

						+ "\" \"" + new Statics(baseDir).getPhpInc();

				if (getPhpVersion() == PHPVersion.PHP5) {
					command += "/PHPUnit/TextUI/Maven.php\" \""
							+ file.getAbsolutePath() + "\" \"" + targetFile
							+ "\"";
				} else if (getPhpVersion() == PHPVersion.PHP4) {
					command += "/XMLWriter.php\" \"" + file.getAbsolutePath()
							+ "\" \"" + targetFile + "\"";
				}
				phpCompile(command, file);
				File targetFileObj = new File(targetFile);
				if (targetFileObj.exists()) {
					parseResultingXML(targetFileObj);
				} else {
					writeFailure(file, targetFile);
					throw new PhpCompileException(command, 0, file,
							getCurrentCommandLineOutput().toString());
				}

			} catch (PhpExecutionError ex) {
				writeFailure(file, targetFile);
				throw ex;
			} catch (PhpCompileException pex) {
				writeFailure(file, targetFile);
				throw pex;
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}

	private void writeFailure(File testCase, String targetReportFilePath)
			throws IOException {
		String logFile = targetReportFilePath.replace(".xml", ".txt");
		getLog().error("Testcase: " + testCase.getName() + " fails.");
		getLog().error("See log: " + logFile);
		FileWriter fstream = new FileWriter(logFile);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(getCurrentCommandLineOutput().toString());
		out.close();

	}

	@Override
	protected void handleProcesedFile(File file) throws MojoExecutionException {
	}

}
// [DEBUG] Try to execute command (PHP4):
// C:\Programme\xampp_164\php\php4\php.exe -d
// include_path=";C:\de.keytec.dev\cw\y\workbenches\maventest\de.keytec.maven.php4sample//src/test/php;C:\de.keytec.dev\cw\y\workbenches\maventest\de.keytec.maven.php4sample/target/classes;/src/main/php;"
// -f
// "C:\de.keytec.dev\cw\y\workbenches\maventest\de.keytec.maven.php4sample/target/phpinc/XMLWriter.php"
// C:\de.keytec.dev\cw\y\workbenches\maventest\de.keytec.maven.php4sample\src\test\php\hellotest.php"
// "C:\de.keytec.dev\cw\y\workbenches\maventest\de.keytec.maven.php4sample\target\surefire-reports/hellotest.xml"

// 

// 