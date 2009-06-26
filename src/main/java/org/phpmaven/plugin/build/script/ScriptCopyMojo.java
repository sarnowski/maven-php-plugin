package org.phpmaven.plugin.build.script;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.phpmaven.plugin.build.FileHelper;


/**
 * @requiresDependencyResolution
 * @goal scriptcopy
 */


public class ScriptCopyMojo extends AbstractMojo {
	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	
	protected File baseDir;
	/**
	 * Project classpath.
	 * 
	 * @parameter expression="${project.compileClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List<String> classpathElements;
	
	/**
	 * The script source folder.
	 * 
	 * @parameter expression="/src/main/script"
	 * @required
	 */
	protected String sourceDirectory;
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			FileHelper.prepareDependencies(baseDir.toString()
					+"/target/classes", classpathElements);
			
			DirectoryWalker walker = new DirectoryWalker();
			walker.setBaseDir(new File(baseDir.toString()+sourceDirectory));
			walker.addDirectoryWalkListener(new DirectoryWalkListener(){

				public void debug(String arg0) {
					
				}

				public void directoryWalkFinished() {
					
				}

				public void directoryWalkStarting(File arg0) {
					
				}

				public void directoryWalkStep(int arg0, File arg1) {
					try {
						FileHelper.copyToTargetFolder(baseDir, sourceDirectory, arg1, "target/classes");
					} catch (MojoExecutionException e) {
						// TODO Auto-generated catch block
						getLog().error(e);
					}
				}
				
			});
			walker.addSCMExcludes();
			walker.scan();

			
			
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(),e);
		}
	}

}
