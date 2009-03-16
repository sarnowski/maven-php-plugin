package org.phpmaven.plugin.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

public abstract class AbstractPhpCompile extends AbstractMojo implements
		DirectoryWalkListener {

	private ArrayList<Exception> compilerExceptions = new ArrayList<Exception>();
	private final static ArrayList ERRORIDENTIFIERS = new ArrayList();
	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	protected File baseDir;
	private StringBuffer currentBuffer;

	protected StringBuffer getCurrentCommandLineOutput() {
		return currentBuffer;
	}
	/**
	 * @parameter
	 */
	public String[] excludes=new String[0];
	/**
	 * @parameter
	 */
	public String[] includes=new String[0];

	protected String flushPHPOutput = System.getProperty("flushPHPOutput")!=null?System.getProperty("flushPHPOutput"):"false";
	/**
	 * PHP Compile args. Use php -h to get a list of all php compile arguments.
	 * 
	 * @parameter
	 */
	private String compileArgs;
	/**
	 * Project classpath.
	 * 
	 * @parameter expression="${project.compileClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List<String> classpathElements;

	/**
	 * The php source folder.
	 * 
	 * @parameter expression="/src/main/php"
	 * @required
	 */
	protected String sourceDirectory;

	/**
	 * Path to the php executable.
	 * 
	 * @parameter expression="php"
	 * @required
	 */
	protected String phpExe;
	private PHPVersion phpVersion;

	public AbstractPhpCompile() {
		ERRORIDENTIFIERS.add("Error");
		ERRORIDENTIFIERS.add("Parse error");
		ERRORIDENTIFIERS.add("Warning");
		ERRORIDENTIFIERS.add("Fatal error");
		ERRORIDENTIFIERS.add("Notice");
	}

	private boolean isError(String line) {
		line = line.trim();
		for (int i = 0; i < ERRORIDENTIFIERS.size(); i++) {
			if (line.startsWith((String) ERRORIDENTIFIERS.get(i) + ":")
					|| line.startsWith("<b>" + (String) ERRORIDENTIFIERS.get(i)
							+ "</b>:")) {
				return true;
			}
		}
		return false;
	}

	protected String getCompilerArgs() {
		if (compileArgs == null) {
			compileArgs = "";
		} else {
			compileArgs = !compileArgs.startsWith(" ") ? compileArgs = " "
					+ compileArgs : compileArgs;
		}
		return compileArgs;
	}

	protected boolean getIgnoreIncludeErrors() {
		return false;
	}

	abstract protected void executePhpFile(File file)
			throws MojoExecutionException;

	abstract protected void handleProcesedFile(File file)
			throws MojoExecutionException;

	class InputConsumer implements StreamConsumer {
		private final StringBuffer bufferOutBuffer;

		public InputConsumer(StringBuffer bufferOutBuffer) {
			this.bufferOutBuffer = bufferOutBuffer;

		}

		private PHPVersion phpVersion;

		public final PHPVersion getPhpVersion() {
			return phpVersion;
		}

		public void consumeLine(String line) {
			if (bufferOutBuffer.length() == 0 && line.startsWith("PHP")) {
				String version = line.substring(4, 5);
				if (version.equals("5") || version.equals("6")) {
					phpVersion = PHPVersion.PHP5;
				} else if (version.equals("4")) {
					phpVersion = PHPVersion.PHP4;
				}
			}
		}
	}

	protected final void copyToTargetFolder(String sourceDirectory,
			File sourceFile, String targetClassFolder)
			throws MojoExecutionException {
		String relative = PathUtils.toRelative(new File(baseDir.toString()
				+ sourceDirectory), sourceFile.toString());

		getLog().debug("Relative :" + relative);
		File targetFile = new File(baseDir.toString() + targetClassFolder + "/"
				+ relative);

		if (targetFile.getParentFile().getName().equalsIgnoreCase("cvs")) {
			return;
		}
		getLog().debug(
				"copy from:" + sourceFile + " to: " + targetFile.toString());
		try {
			FileUtils.copyFileIfModified(sourceFile, targetFile);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public final PHPVersion getPhpVersion() throws CommandLineException,
			PhpExecutionError {
		if (phpVersion != null) {
			return phpVersion;
		}
		String commandString = phpExe + " -v";
		getLog().debug("Try to execute command: " + commandString);
		final StringBuffer bufferErrBuffer = new StringBuffer();
		final StringBuffer bufferOutBuffer = new StringBuffer();
		Commandline commandLine = new Commandline(commandString);
		InputConsumer inputConsumer = new InputConsumer(bufferOutBuffer);

		int executeCommandLine = CommandLineUtils.executeCommandLine(
				commandLine, inputConsumer, new StreamConsumer() {
					public void consumeLine(String line) {

						getLog().debug("system.err: " + line);
						bufferErrBuffer.append(line);
					}

				});
		String errString = bufferErrBuffer.toString();
		PHPVersion phpVersion = inputConsumer.getPhpVersion();
		getLog().debug("PHPVersion: " + phpVersion.toString());
		if (executeCommandLine != 0 || errString.length() != 0) {
			getLog().error(
					"Error while execution php -v\n" + errString.toString()
							+ "\n comandLine:" + executeCommandLine + "\n"
							+ bufferOutBuffer.toString());
			throw new PhpExecutionError();
		}
		this.phpVersion = phpVersion;
		return phpVersion;
	}

	private void unjar(File jarFile, File destDir) throws IOException {
		java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		java.util.Enumeration<java.util.jar.JarEntry> items = jar.entries();
		
		while (items.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) items
					.nextElement();
			
			java.io.File f = new java.io.File(destDir + java.io.File.separator
					+ file.getName());
			if (f.exists() ) {
				continue;
			}
			if (file.isDirectory()) { // if its a directory, create it
				f.mkdir();
				continue;
			}
			
			java.io.InputStream is = jar.getInputStream(file); // get the input
			// stream
			java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
			IOUtil.copy(is, fos);
//			while (is.available() > 0) { // write contents of 'is' to 'fos'
//				fos.write(is.read());
//			}
			fos.close();
			is.close();
		}

	}

	protected final void prepareDependencies(List<String> elements)
			throws IOException {

		File targetFile = new File(baseDir.toString() + Statics.phpinc);
		targetFile.mkdirs();
		for (int i = 0; i < elements.size(); i++) {
			File sourceFile = new File((String) elements.get(i));
			if (sourceFile.isFile()) {
				getLog().debug("unjar dependency: " + sourceFile);
				unjar(sourceFile, targetFile);
			}
		}
	}

	protected final void prepareCompileDependencies() throws IOException {
		prepareDependencies(classpathElements);
	}

	protected final void phpCompile(String commandString, final File file)
			throws PhpCompileException, CommandLineException, PhpExecutionError {
		getLog().debug(
				"Try to execute command (" + getPhpVersion() + "): "
						+ commandString);
		final StringBuffer bufferErrBuffer = new StringBuffer();
		final StringBuffer bufferOutBuffer = new StringBuffer();
		Commandline commandLine = new Commandline(commandString);

		final boolean ignoreIncludeErrors = getIgnoreIncludeErrors();

		int executeCommandLine = CommandLineUtils.executeCommandLine(
				commandLine, new StreamConsumer() {
					public void consumeLine(String line) {
						
						if (flushPHPOutput.equals("true")) {
							getLog().info("php.out: " + line);
						}else { 
							getLog().debug("php.out: " + line);
						}
						if (isError(line) == true) {
							if (ignoreIncludeErrors == false
									|| (ignoreIncludeErrors == true
											&& !line.contains("require_once") && !line
											.contains("include_once"))) {
								bufferErrBuffer.append(line);
							}

						}
						bufferOutBuffer.append(line);
					}
				}, new StreamConsumer() {
					public void consumeLine(String line) {
						getLog().debug("php.err: " + line);
						bufferErrBuffer.append(line);
					}

				});
		currentBuffer = bufferOutBuffer;
		String errString = bufferErrBuffer.toString();
		if (!"".equals(errString)) {
			throw new PhpCompileException(commandString,
					PhpCompileException.ERROR, file, errString);
		}
		if (executeCommandLine == 1) {
			throw new PhpExecutionError();
		}
	}

	public void debug(String message) {
		getLog().debug(message);

	}

	public void directoryWalkFinished() {
		getLog().debug("--- Compiling has finished.");

	}

	public void directoryWalkStarting(File basedir) {
		getLog().debug(
				"--- Start compiling source folder: "
						+ basedir.getAbsoluteFile());

	}

	public void directoryWalkStep(int percentage, File file) {
		getLog().debug("percentage: " + percentage);
		try {
			if (file.isFile()) {
				if (file.getName().endsWith(".php"))
					executePhpFile(file);
				handleProcesedFile(file);
			}
		} catch (Exception e) {
			getLog().debug(e);
			compilerExceptions.add(e);
		}
	}

	protected final void goRecursiveAndCall(File parentFolder)
			throws MuilplePhpCompileException {
		if (!parentFolder.isDirectory()) {
			getLog()
					.error(
							"Source directory ("
									+ parentFolder.getAbsolutePath() + ")");
			return;
		}
		DirectoryWalker walker = new DirectoryWalker();
		walker.setBaseDir(parentFolder);
		walker.addDirectoryWalkListener(this);
		walker.addSCMExcludes();
		for(int i =0; excludes!=null && i<excludes.length;i++ ) { 
			walker.addExclude(excludes[i]);	
		}
		for(int i =0; includes!=null && i<includes.length;i++ ) { 
			walker.addInclude(excludes[i]);	
		}
		walker.scan();
		if (compilerExceptions.size() != 0) {
			throw new MuilplePhpCompileException(compilerExceptions);
		}
		/*
		 * File[] listFiles = parentFolder.listFiles(); for (int i = 0; i <
		 * listFiles.length; i++) { if (listFiles[i].isDirectory()) {
		 * goRecursiveAndCall(listFiles[i]); } else if (listFiles[i].isFile() &&
		 * listFiles[i].getName().endsWith(".php")) {
		 * executePhpFile(listFiles[i]); } if (listFiles[i].isFile()) {
		 * handleProcesedFile(listFiles[i]); } }
		 */
	}
}
