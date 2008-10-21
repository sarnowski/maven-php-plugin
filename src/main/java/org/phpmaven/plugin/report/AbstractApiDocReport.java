package org.phpmaven.plugin.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;

/**
 * Abstract base class for api docs.
 * 
 * @author cw
 * 
 */
public abstract class AbstractApiDocReport extends AbstractMavenReport {
	protected abstract String getFolderName();
	/**
	 * The output directory of doxygen generated documentation.
	 * 
	 * @parameter expression="${project.build.directory}/site/apidocs"
	 * @required
	 */
	protected File outputApiDocDirectory;

	/**
	 * The source code directory where doxygen start generating documentation.
	 * 
	 * @parameter expression="/src/main/php";
	 * @required
	 */
	protected String sourceDirectory = "/src/main/php";

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

	protected File getApiDocOutputDirectory() {
		return outputApiDocDirectory;
	}

	protected String getSourceDirectory() {
		return sourceDirectory;
	}

	protected void writePropFile(Properties properties,
			File generatedPhpDocConfigFile, String preFileContent)
			throws IOException {
		String lineSeparator = System.getProperty("line.separator");
		generatedPhpDocConfigFile.getParentFile().mkdirs();
		FileWriter fileWriter = new FileWriter(generatedPhpDocConfigFile);
		Set<Object> keySet = properties.keySet();
		if (preFileContent != null)
			fileWriter.append(preFileContent + lineSeparator);

		for (Iterator<Object> iterator = keySet.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String value = properties.getProperty(key);
			fileWriter.append(key + "=" + value + lineSeparator);
		}
		fileWriter.close();
	}

	/**
	 * @param siteRenderer
	 *            The siteRenderer to set.
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
