package org.phpmaven.plugin.build.script;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.phpmaven.plugin.build.ExecutionError;
import org.phpmaven.plugin.build.FileHelper;
import org.phpmaven.plugin.build.MultipleCompileException;

/**
 * 
 * 
 * 
 * @requiresDependencyResolution
 * @goal scripttestrunner
 */
public class ScriptTestRunnerMojo extends AbstractMojo {
	
	
	private ArrayList<Exception> errors = new ArrayList<Exception>();
	
	/**
	 * Environment Variables.
	 * 
	 * @parameter
	 */
	private Properties environmentVariables;
	/**
	 * The script source folder.
	 * 
	 * @parameter expression="/src/main/script"
	 * @required
	 */
	protected String sourceDirectory;
	/**
	 * The test script source Folder.
	 * 
	 * @parameter expression="/src/test/script"
	 * @required
	 */
	private String testDirectory;
	/**
	 * The command will be executed at startup
	 * 
	 * @parameter expression=""
	 */
	private String startupCommand;
	/**
	 * The command will be executed on each file in the testfolder
	 * 
	 * @parameter
	 * @required
	 */
	private String command;
	/**
	 * Project classpath.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List<String> classpathElements;

	
	/**
	 * Error identifier.
	 * @parameter 
	 */
	private List<String> errorIdentifiers;
	
	/**
	 * Includes.
	 * @parameter 
	 */
	private List<String> testIncludes;
	/**
	 * Excludes.
	 * @parameter 
	 */
	private List<String> testExcludes;
	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	
	protected File baseDir;
	private String includeDirectory;


	public String replaceCommandArgs(String command,String arg,String value) { 
		String maskedArg = "\\*\\{"+arg+"\\}";
		
		command = command.replaceAll(maskedArg, value);
		return command;
	}
	private String doDefaultReplaces(String command) {
		String baseDirR = baseDir.getPath().replace("\\", "/");
		command = replaceCommandArgs(command, "includeDirectory", baseDirR+includeDirectory);
		command = replaceCommandArgs(command, "sourceDirectory", baseDirR+sourceDirectory);
		command = replaceCommandArgs(command, "testDirectory", baseDirR+testDirectory);
		command = replaceCommandArgs(command, "file.pathSeparator", File.pathSeparator);
		command = replaceCommandArgs(command, "file.separator", File.separator);
		
		
		return command;
	}
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			
			
			includeDirectory = baseDir.getPath()
			+ "/target/include";

			FileHelper.prepareDependencies(includeDirectory, classpathElements);
			
			if (startupCommand!=null && !"".equals(startupCommand)) { 
				startupCommand = doDefaultReplaces(startupCommand);
				getLog().info("=====================================================");
				getLog().info("RUNNING STARTUP COMMAND");
				getLog().info("=====================================================");
				executeCommand(startupCommand);
			}
			DirectoryWalker walker = new DirectoryWalker();
			walker.setBaseDir(new File(baseDir.getPath()+testDirectory));
			if (testExcludes!=null)  {
				walker.setExcludes(testExcludes);	
			}
			if (testIncludes!=null)  {
				walker.setIncludes(testIncludes);	
			}
			
			walker.addDirectoryWalkListener(new DirectoryWalkListener(){

				public void debug(String arg0) {
					
				}

				public void directoryWalkFinished() {
					
				}

				public void directoryWalkStarting(File arg0) {
					
				}

				public void directoryWalkStep(int arg0, File arg1) {
					String execCommand = doDefaultReplaces(command);
					String filePath =arg1.getPath();
					filePath = filePath.replace("\\", "/");
					execCommand = replaceCommandArgs(execCommand, "file", filePath);
					getLog().debug("file: " + arg1.getPath()); 
					executeCommand(execCommand);
					
				}
				
			});
			walker.addSCMExcludes();
			
			getLog().info("=====================================================");
			getLog().info("START TEST PHASE");
			getLog().info("=====================================================");
			walker.scan();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (errors.size()!=0) { 
			MultipleCompileException e = new MultipleCompileException(errors);
			throw new MojoFailureException(e,"Failure",e.getMessage());
		}
	}
	
	private void handleError(String line) {
		for (int i = 0; errorIdentifiers!=null && i < errorIdentifiers.size(); i++) {
			if (line!=null && line.trim().startsWith(errorIdentifiers.get(i))) { 
				ExecutionError executionError = new ExecutionError("Error identifier matches: "+errorIdentifiers.get(i)+ ". Line: " +line.trim());
				errors.add(executionError);
			}
		}
		
	}
	private void executeCommand(String command) { 
		getLog().info("-----------------------------------------------------");
		getLog().info("script: " + command);
		
		try {
			
			Commandline c = new Commandline(command);
			if (environmentVariables!=null) {
				Set<Entry<Object, Object>> entrySet = environmentVariables.entrySet();
				for (Iterator iterator = entrySet.iterator(); iterator
						.hasNext();) {
					Entry<Object, Object> entry = (Entry<Object, Object>) iterator
							.next();
					String value  =this.doDefaultReplaces((String)entry.getValue());
					c.addEnvironment((String)entry.getKey(), value );
					getLog().debug("Adding to env: " + (String)entry.getKey() + " : "+ value );
				}
				
			}
			CommandLineUtils.executeCommandLine(
					c, new StreamConsumer() {
						public void consumeLine(String line) {
							getLog().info("script.out: " + line);
							handleError(line);
						}
					}, new StreamConsumer() {
						public void consumeLine(String line) {
							
							getLog().info("script.out: " + line);
							handleError(line);
						}
					});
			getLog().info("-----------------------------------------------------");
			getLog().info("");
		} catch (CommandLineException e) {
			errors.add(e);
			getLog().error(e);
		}
	
	}
}
