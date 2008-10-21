package org.phpmaven.plugin.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
 * @author Christian Wiedemann
 * @goal phpdocumentor
 * @phase site
 */

public class PHPDocumentor extends AbstractApiDocReport {
	/**
	 * Path to phpDoc. If nothing is configured phpdoc is expected in the path.
	 * 
	 * @parameter expression="phpdoc"
	 * @required
	 */ 
	public String phpDocFilePath = "phpdoc";
	/**
	 * Path to the php executable.
	 * 
	 * @parameter expression="php"
	 * @required
	 */
	protected String phpExe;

	/**
	 * The phpdoc configuraton file. By default it is searched under
	 * "src/site/phpdoc"
	 * 
	 * @parameter expression="${basedir}/src/site/phpdoc/phpdoc.config";
	 * @required
	 */
	protected File phpDocConfigFile;
	
	/**
	 * The generated phpDoc file.
	 * 
	 * @parameter expression="${project.build.directory}/site/phpdoc/phpdoc.ini";
	 * @required
	 * @readonly
	 */
	protected File generatedPhpDocConfigFile;
	private void writeReport() {
		if (getSink()!=null)  { 
		getSink()
				.rawText(
						"<a href=\"phpdocumentor/HTMLframesConverter/default/index.html\" target=\"_blank\">Show documention<br><iframe src=\"phpdocumentor/HTMLframesConverter/default/index.html\" frameborder=0 style=\"border=0px;width:100%;height:400px\">");
		}
	}
	@Override
	protected void executeReport(Locale locale) throws MavenReportException {
		try {
			if (phpDocConfigFile.isFile()) {
				Properties properties = new Properties();
				
				properties.load(new FileInputStream(phpDocConfigFile));
				properties.put("directory", getProject().getBasedir() + "/"
						+ getSourceDirectory());
				properties.put("target", getApiDocOutputDirectory()
						.getAbsoluteFile().getPath()+"/"+getFolderName());
				
				writePropFile(properties, generatedPhpDocConfigFile,"[Parse Data]");
				String path = System.getProperty("java.library.path");
				getLog().debug("PATH: "+ path);
				String[] paths = path.split(File.pathSeparator);
				File phpDocFile = null;
				if (phpDocFilePath.equals("phpdoc")) {
					for (int i = 0; i < paths.length; i++) {
						String cpath = paths[i] + "/phpdoc";
						File file = new File(cpath);
						if (file.isFile()) {
							phpDocFile = file;
							break;
						}
					}
				} else {
					phpDocFile = new File(getProject().getBasedir() + "/"
							+ phpDocFile);
				}
				if (phpDocFile == null || !phpDocFile.isFile()) {
					throw new PHPDocumentorNotFoundException();
				} 
				String executing = phpExe
				+ " phpdoc -c \"" + generatedPhpDocConfigFile.getAbsolutePath()+"\"";
				Commandline commandLine = new Commandline(executing
						);
				commandLine.setWorkingDirectory(phpDocFile.getParent());

				int executeCommandLine = CommandLineUtils.executeCommandLine(
						commandLine, new StreamConsumer() {

							public void consumeLine(String line) {
								getLog().debug("system.out: " + line);
							}

						}, new StreamConsumer() {
							public void consumeLine(String line) {

								getLog().debug("system.err: " + line);
							}

						});
				if (executeCommandLine==1) { 
					throw new PHPDocumentorExecuteException(phpDocFile.getParent());
				}
				
			}
		} catch (Exception e) {
			throw new MavenReportException(e.getMessage(), e);
		}
		writeReport();
	}

	public String getDescription(Locale locale) {
		return "PHPDocumentor generated documentation";
	}

	public String getName(Locale locale) {
		return "PHPDocumentor";
	}

	public String getOutputName() {
		return "apidocs/phpdocumentor";
	}
	@Override
	protected String getFolderName() {
		// TODO Auto-generated method stub
		return "phpdocumentor";
	}

}
