package de.keytec.maven.php;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * A maven 2.0 plugin for generating doxygen documentations.
 * This plugin is used in the <code>site</code> phase.
 * @author Christian Wiedemann
 * @goal doxygen
 * @phase site
 */

public class DoxygenReport extends AbstractMavenReport {
	/**
	 * Path to the doxygen.exe. 
	 * If nothing is configured doxygen is expected in the path. 
	 * 
	 * @parameter expression="doxygen"
	 * @required
	 */
	private String doxyGenExe;

	/**
	 * The output directory of doxygen generated documentation.
	 * 
	 * @parameter expression="${project.build.directory}/site/apidocs"
	 * @required
	 */
	private File outputDoxygenDirectory;

	/**
	 * The source code directory where doxygen start generating documentation.
	 * 
	 * @parameter expression="/src/main/php";
	 * @required
	 */
	private String sourceDirectory;

	/**
	 * The doxygen configuraton file. 
	 * By default it is searched under "src/site/doxygen"
	 * 
	 * @parameter expression="${basedir}/src/site/doxygen/doxygen.config";
	 * @required
	 */
	private File doxygenConfigFile;

	/**
	 * The doxygen file.
	 * 
	 * @parameter expression="${project.build.directory}/site/doxygen/doxygen.config";
	 * @required
	 * @readonly
	 */
	private File generatedDoxygenConfigFile;
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

	@Override
	protected void executeReport(Locale locale) throws MavenReportException {
		try {
			if (doxygenConfigFile.isFile()) {
				Properties properties = new Properties();
				properties.load(new FileReader(doxygenConfigFile));
				properties.put("INPUT",  project.getBasedir()+"/"+ sourceDirectory);
				properties.put("OUTPUT_DIRECTORY", outputDoxygenDirectory
						.getAbsoluteFile().getPath());
				properties.put("PROJECT_NAME", project.getGroupId() + ":"
						+ project.getArtifactId());
				properties.put("PROJECT_NUMBER", project.getVersion());

				generatedDoxygenConfigFile.getParentFile().mkdirs();
				FileWriter fileWriter = new FileWriter(
						generatedDoxygenConfigFile);
				Set<Object> keySet = properties.keySet();
				for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					String value = properties.getProperty(key);
					fileWriter.append(key + "=" + value + "\n");
				}
				fileWriter.close();
				// properties.store(new FileWriter(generatedDoxygenConfigFile),
				// "");
				String execute = doxyGenExe+" \""
						+ generatedDoxygenConfigFile.getAbsolutePath() + "\"";
				getLog().debug("Doxygen execute: " + execute);
				Commandline commandLine = new Commandline(execute);

				CommandLineUtils.executeCommandLine(commandLine,
						new StreamConsumer() {
							@Override
							public void consumeLine(String line) {
								getLog().debug(line);
							}
						}, new StreamConsumer() {
							@Override
							public void consumeLine(String line) {
								getLog().debug(line);
							}
						});
				writeReport();
			} else {
				getLog().warn("No doxygen configuration file found. (search for: "+doxygenConfigFile.getAbsolutePath()+")");
			}
		} catch (Exception e) {
			throw new MavenReportException(e.getMessage(), e);
		}

	}

	private void writeReport() {

		getSink()
				.rawText(
						"<a href=\"html/index.html\" target=\"_blank\">Show documention<br><iframe src=\"html/index.html\" frameborder=0 style=\"border=0px;width:100%;height:400px\">");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
	 */
	protected String getOutputDirectory() {
		return outputDoxygenDirectory.getAbsolutePath();
	}

	@Override
	protected MavenProject getProject() {
		return project;
	}

	@Override
	protected Renderer getSiteRenderer() {
		return siteRenderer;
	}

	@Override
	public String getDescription(Locale locale) {
		return "Doxygen generated phpdoc";
	}

	@Override
	public String getName(Locale locale) {
		return "Doxygen";
	}

	@Override
	public String getOutputName() {
		// TODO Auto-generated method stub
		return "apidocs/doxygen";
	}

	/**
	 * @param siteRenderer
	 *            The siteRenderer to set.
	 */
	public void setSiteRenderer(Renderer siteRenderer) {
		this.siteRenderer = siteRenderer;
	}

}
