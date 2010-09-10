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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to give fast access to the PHP executable and the basic configuration.
 *
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public abstract class AbstractPhpMojo extends AbstractMojo implements DirectoryWalkListener {
    /**
     * Parameter to let PHP print out its version.
     */
    public static final String PHP_FLAG_VERSION = "-v";

    /**
     * Parameter to specify the include paths for PHP.
     */
    public static final String PHP_FLAG_INCLUDES = "-d include_path";

    /**
     * This list describes all keywords which will be printed out by PHP
     * if an error occurs.
     */
    private static final String[] ERROR_IDENTIFIERS = new String[]{
        "Fatal error",
        "Error",
        "Parse error"
    };

    /**
     * This list describes all keywords which will be printed out by PHP
     * if a warrning occurs.
     */
    private static final String[] WARNING_IDENTIFIERS = new String[]{
        "Warning",
        "Notice"
    };


    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The project's base directory.
     *
     * @parameter expression="${project.basedir}"
     * @required
     * @readonly
     */
    private File baseDir;

    /**
     * Path to the php executable.
     *
     * @parameter
     */
    private String phpExecutable = "php";

    /**
     * Files and directories to exclude.
     *
     * @parameter
     */
    private String[] excludes = new String[0];

    /**
     * Files and directories to include.
     *
     * @parameter
     */
    private String[] includes = new String[0];

    /**
     * PHP arguments. Use php -h to get a list of all php compile arguments.
     *
     * @parameter
     */
    private String additionalPhpParameters;

    /**
     * Project compile-time classpath.
     *
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    private List<String> compileClasspathElements;

    /**
     * Project test classpath elements.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List<String> testClasspathElements;

    /**
     * The php source folder.
     *
     * @parameter
     */
    private String sourceDirectory = "src/main/php";

    /**
     * The php test source folder.
     *
     * @parameter
     */
    private String testSourceDirectory = "src/test/php";

    /**
     * Where the output should be stored for jar inclusion.
     *
     * @parameter
     */
    private String targetClassesDirectory = "target/classes";

    /**
     * Where the test output should be stored for jar inclusion.
     *
     * @parameter
     */
    private String targetTestClassesDirectory = "target/test-classes";

    /**
     * Where the php dependency files will be written to.
     *
     * @parameter
     */
    private String dependenciesTargetDirectory = "target/php-deps";

    /**
     * Where the php test dependency files will be written to.
     *
     * @parameter
     */
    private String testDependenciesTargetDirectory = "target/php-test-deps";

    /**
     * How php files will be identified after the last point.
     *
     * @parameter
     */
    private String phpFileEnding = "php";

    /**
     * If the source files should be included in the resulting jar.
     *
     * @parameter
     */
    private boolean includeInJar = true;

    /**
     * If true, php maven will allways overwrite existing php files in the classes folder
     * even if the files in the target folder are newer or at the same date.
     *
     * @parameter
     */
    private boolean forceOverwrite;

    /**
     * If true, errors triggered because of missing includes will be ignored.
     *
     * @parameter
     */
    private boolean ignoreIncludeErrors;

    /**
     * If the output of the php scripts will be written to the console.
     *
     * @parameter
     */
    private boolean logPhpOutput;

    /**
     * The used PHP version (cached after initial call of {@link #getPhpVersion()}.
     */
    private PhpVersion phpVersion;

    /**
     * collects all exceptions during the file walk.
     */
    private List<Exception> collectedExceptions = Lists.newArrayList();


    /**
     * Callback for executing a file.
     *
     * @param file the PHP file to execute
     * @throws MojoExecutionException if something goes wrong during the execution
     */
    protected abstract void executePhpFile(File file) throws MojoExecutionException;

    /**
     * Callback for file processing.
     *
     * @param file the PHP file to process
     * @throws MojoExecutionException if something goes wrong during the execution
     */
    protected abstract void handleProcessedFile(File file) throws MojoExecutionException;

    /**
     * Represents the maven project.
     *
     * @return the current maven project.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * The project's base directory.
     *
     * @return the project's basedir
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Path to the PHP executable.
     *
     * @return path of the php executable
     */
    public String getPhpExecutable() {
        return phpExecutable;
    }

    /**
     * Array of paths to exclude.
     *
     * @return paths and files to exclude
     */
    public String[] getExcludes() {
        return excludes;
    }

    /**
     * Array of paths to include.
     *
     * @return paths and files to include
     */
    public String[] getIncludes() {
        return includes;
    }

    /**
     * Parameters which should be added to the generated PHP parameters.
     *
     * @return additional arguments for php execution
     */
    public String getAdditionalPhpParameters() {
        return additionalPhpParameters;
    }

    /**
     * List of dependencies of the compile scope.
     *
     * @return elements used in compile scope
     */
    public List<String> getCompileClasspathElements() {
        return compileClasspathElements;
    }

    /**
     * List of dependencies of the test scope.
     *
     * @return elements used in test scope
     */
    public List<String> getTestClasspathElements() {
        return testClasspathElements;
    }

    /**
     * Where the PHP source files can be found.
     *
     * @return where the php sources can be found
     */
    public File getSourceDirectory() {
        return new File(getBaseDir(), sourceDirectory);
    }

    /**
     * The configured path to the source directory.
     *
     * @see #getSourceDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Where the PHP test sources can be found.
     *
     * @return where the php test sources can be found
     */
    public File getTestSourceDirectory() {
        return new File(getBaseDir(), testSourceDirectory);
    }

    /**
     * The configured source directory.
     *
     * @see #getTestSourceDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainTestSourceDirectory() {
        return testSourceDirectory;
    }

    /**
     * Where the dependencies should be unpacked to.
     *
     * @return where to store the dependency files
     */
    public File getDependenciesTargetDirectory() {
        return new File(getBaseDir(), dependenciesTargetDirectory);
    }

    /**
     * The configured dependency target directory.
     *
     * @see #getDependenciesTargetDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainDependenciesTargetDirectory() {
        return dependenciesTargetDirectory;
    }

    /**
     * Where the test dependencies should be unpacked to.
     *
     * @return where to store the test dependency files
     */
    public File getTestDependenciesTargetDirectory() {
        return new File(getBaseDir(), testDependenciesTargetDirectory);
    }

    /**                                      e
     * The configured test dependency target directory.
     *
     * @see #getTargetTestClassesDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainTestDependenciesTargetDirectory() {
        return testDependenciesTargetDirectory;
    }

    /**
     * Where the sources should get copied to.
     *
     * @return where the jar inclusion directory is
     */
    public File getTargetClassesDirectory() {
        return new File(getBaseDir(), targetClassesDirectory);
    }

    /**
     * The configured target directory.
     *
     * @see #getTargetClassesDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainTargetClassesDirectory() {
        return targetClassesDirectory;
    }

    /**
     * The target directory where to copy the test sources to.
     *
     * @return where the test-jar inclusion directory is
     */
    public File getTargetTestClassesDirectory() {
        return new File(getBaseDir(), targetTestClassesDirectory);
    }

    /**
     * The configured target directory.
     *
     * @see #getPlainTargetTestClassesDirectory()
     * @return the configured probably relative directory
     */
    public String getPlainTargetTestClassesDirectory() {
        return targetTestClassesDirectory;
    }

    /**
     * Wich file ending to use for identifying php files.
     *
     * @return the file ending (except the dot)
     */
    public String getPhpFileEnding() {
        return phpFileEnding;
    }

    /**
     * If the sources should be included in the resulting jar file. If not,
     * the sourcess won't be copied to the target directory.
     *
     * @return if the php sources should be included in the resulting jar
     */
    public boolean isIncludeInJar() {
        return includeInJar;
    }

    /**
     * Do not care about file timestamps and copy every time.
     *
     * @return forces target files to be overwritten
     */
    public boolean isForceOverwrite() {
        return forceOverwrite;
    }

    /**
     * Returns if include errors should be ignored.
     *
     * @return if include errors should be ignored
     */
    public boolean isIgnoreIncludeErrors() {
        return ignoreIncludeErrors;
    }

    /**
     * Returns if output from the PHP executable should be logged.
     *
     * @return if php output will be printed to the log
     */
    public boolean isLogPhpOutput() {
        return logPhpOutput;
    }

    /**
     * Nessecary for the DirectoryWalker, do not use.
     *
     * @param message message to log
     * @deprecated use getLog() instead
     */
    @Override
    @Deprecated
    public void debug(String message) {
        getLog().debug(message);
    }

    /**
     * Checks if a line (string) contains a PHP error message.
     *
     * @param line output line
     * @return if the line contains php error messages
     */
    private boolean isError(String line) {
        final String trimmedLine = line.trim();
        for (String errorIdentifier : ERROR_IDENTIFIERS) {
            if (trimmedLine.startsWith(errorIdentifier + ":")
                || trimmedLine.startsWith("<b>" + errorIdentifier + "</b>:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a line (string) contains a PHP warning message.
     *
     * @param line output line
     * @return if the line contains php warning messages
     */
    private boolean isWarning(String line) {
        final String trimmedLine = line.trim();
        for (String warningIdentifier : WARNING_IDENTIFIERS) {
            if (trimmedLine.startsWith(warningIdentifier + ":")
                || trimmedLine.startsWith("<b>" + warningIdentifier + "</b>:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes PHP with the given arguments.
     *
     * @param arguments string of arguments for PHP
     * @param stdout handler for stdout lines
     * @param stderr handler for stderr lines
     * @return the return code of PHP
     * @throws PhpException if the executions fails
     */
    public int execute(String arguments, StreamConsumer stdout, StreamConsumer stderr) throws PhpException {
        Preconditions.checkNotNull(arguments, "Arguments");
        Preconditions.checkNotNull(stdout, "stdout");
        Preconditions.checkNotNull(stderr, "stderr");

        final String command;
        if (getAdditionalPhpParameters() != null) {
            command = phpExecutable + " " + getAdditionalPhpParameters() + " " + arguments;
        } else {
            command = phpExecutable + " " + arguments;
        }

        final Commandline commandLine = new Commandline(command);

        try {
            getLog().debug("Executing " + commandLine);
            return CommandLineUtils.executeCommandLine(commandLine, stdout, stderr);
        } catch (CommandLineException e) {
            throw new PhpCoreException(e);
        }
    }

    /**
     * Executes PHP with the given arguments and throws an IllegalStateException if the
     * execution fails.
     *
     * @param arguments string of arguments for PHP
     * @param file a hint which file will be processed
     * @param stdout handler for stdout lines
     * @return the returncode of PHP
     * @throws PhpException if the execution failed
     */
    public int execute(String arguments, File file, final StreamConsumer stdout) throws PhpException {
        final StringBuilder stderr = new StringBuilder();

        final AtomicBoolean throwError = new AtomicBoolean(false);
        final AtomicBoolean throwWarning = new AtomicBoolean(false);

        final int returnCode = execute(
            arguments,
            new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    if (logPhpOutput) {
                        getLog().info("php.out: " + line);
                    } else {
                        getLog().debug("php.out: " + line);
                    }

                    stdout.consumeLine(line);

                    final boolean error = isError(line);
                    final boolean warning = isWarning(line);
                    if (error || warning) {
                        if (isIgnoreIncludeErrors()
                            && !line.contains("require_once")
                            && !line.contains("include_once")
                            ) {
                            stderr.append(line);
                            stderr.append("\n");
                        } else if (!isIgnoreIncludeErrors()) {
                            stderr.append(line);
                            stderr.append("\n");
                        }
                        if (error) throwError.set(true);
                        if (warning) throwWarning.set(true);
                    }
                }
            },
            new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    stderr.append(line);
                    stderr.append("\n");
                    throwError.set(true);
                }
            }
        );
        final String error = stderr.toString();
        if (returnCode == 0 && !throwError.get() && !throwWarning.get()) {
            return returnCode;
        } else {
            String message = "Failed to execute PHP with arguments '" + arguments + "' [Return: " + returnCode + "]";
            if (error.length() > 0) {
                message = message + ":\n" + error;
            }

            if (throwWarning.get()) {
                throw new PhpWarningException(file, message);
            } else if (throwError.get()) {
                throw new PhpErrorException(file, message);
            } else {
                throw new PhpCoreException(message);
            }
        }
    }

    /**
     * Executes PHP with the given arguments and returns its output.
     *
     * @param arguments string of arguments for PHP
     * @param file a hint which file will be processed
     * @return the output string
     * @throws PhpException if the execution failed
     */
    public String execute(String arguments, File file) throws PhpException {
        final StringBuilder stdout = new StringBuilder();
        try {
            execute(arguments, file, new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    stdout.append(line);
                    stdout.append("\n");
                }
            });
        } catch (PhpException e) {
            e.appendOutput(stdout.toString());
            throw e;
        }
        return stdout.toString();
    }

    /**
     * Generates a string which can be used as a parameter for the PHP
     * executable defining the include paths to use.
     *
     * @param paths a list of paths
     * @return the complete parameter for PHP
     */
    public String includePathParameter(String[] paths) {
        final StringBuilder includePath = new StringBuilder();
        includePath.append(PHP_FLAG_INCLUDES);
        includePath.append("=\"");
        for (String path : paths) {
            includePath.append(File.pathSeparator);
            includePath.append(path);
        }
        includePath.append("\"");
        return includePath.toString();
    }

    /**
     * Retrieves the used PHP version.
     *
     * @return the PHP version
     * @throws PhpException is the php version is not resolvable or supported
     */
    public final PhpVersion getPhpVersion() throws PhpException {

        // already found out?
        if (phpVersion != null) {
            return phpVersion;
        }

        // execute PHP
        execute(PHP_FLAG_VERSION,
            (File) null,
            new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    if (phpVersion == null && line.startsWith("PHP")) {
                        final String version = line.substring(4, 5);
                        if ("6".equals(version)) {
                            phpVersion = PhpVersion.PHP6;
                            getLog().warn("PHP6 is not supported yet!");
                        } else if ("5".equals(version)) {
                            phpVersion = PhpVersion.PHP5;
                        } else if ("4".equals(version)) {
                            phpVersion = PhpVersion.PHP4;
                            getLog().warn("PHP4 will not be supported anymore!");
                        } else {
                            phpVersion = PhpVersion.UNKNOWN;
                            getLog().error("Cannot find out PHP version: " + line);
                        }
                    }
                }
            }
        );

        getLog().debug("PHP version: " + phpVersion.name());
        return phpVersion;
    }

    /**
     * Unzips all compile dependency sources.
     *
     * @throws IOException if something goes wrong while prepareing the dependencies
     * @throws PhpException php exceptions can fly everywhere..
     */
    protected void prepareCompileDependencies() throws IOException, PhpException {
        FileHelper.unzipElements(getDependenciesTargetDirectory(), getCompileClasspathElements());
    }

    /**
     * Unzips all test dependency sources.
     *
     * @throws IOException if something goes wrong while prepareing the dependencies
     * @throws PhpException php exceptions can fly everywhere..
     */
    protected void prepareTestDependencies() throws IOException, PhpException {
        FileHelper.unzipElements(getTestDependenciesTargetDirectory(), getTestClasspathElements());
    }

    /**
     * Forced to implement by the {@link org.codehaus.plexus.util.DirectoryWalkListener}.
     *
     * {@inheritDoc}
     */
    @Override
    public void directoryWalkFinished() {
        /* ignore */
    }

    /**
     * Forced to implement by the {@link org.codehaus.plexus.util.DirectoryWalkListener}.
     *
     * {@inheritDoc}
     */
    @Override
    public void directoryWalkStarting(File basedir) {
        /* ignore */
    }

    /**
     * Will be triggered for every file in the directory.
     *
     * {@inheritDoc}
     */
    @Override
    public void directoryWalkStep(int percentage, File file) {
        try {
            if (file.isFile() && file.getName().endsWith("." + getPhpFileEnding()))
                executePhpFile(file);
            if (file.isFile())
                handleProcessedFile(file);
        /*CHECKSTYLE:OFF*/
        } catch (Exception e) {
        /*CHECKSTYLE:ON*/
            getLog().debug(e);
            collectedExceptions.add(e);
        }
    }

    /**
     * Triggers the walk process.
     *
     * @param parentFolder the folder to start in
     * @throws MultiException every catched exception collected during the walk
     */
    protected final void goRecursiveAndCall(File parentFolder) throws MultiException {
        if (!parentFolder.isDirectory()) {
            getLog().error("Source directory (" + parentFolder.getAbsolutePath() + ")");
            return;
        }

        final DirectoryWalker walker = new DirectoryWalker();

        walker.setBaseDir(parentFolder);
        walker.addDirectoryWalkListener(this);
        walker.addSCMExcludes();

        for (String exclude : excludes) {
            walker.addExclude(exclude);
        }
        for (String include : includes) {
            walker.addInclude(include);
        }

        // new list
        collectedExceptions = Lists.newArrayList();

        // do the action
        walker.scan();

        if (collectedExceptions.size() != 0) {
            throw new MultiException(collectedExceptions);
        }
    }
}
