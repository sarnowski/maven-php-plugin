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
 *
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public abstract class AbstractPhpMojo extends AbstractMojo implements DirectoryWalkListener {

    /**
     * Can be defined via -DphpLogOutput=true
     */
    public final static String LOG_PHP_OUTPUT = "phpLogOutput";

    /**
     * How to get PHP versions output
     */
    public final static String PHP_FLAG_VERSION = "-v";

    /**
     * List of PHP error keywords
     */
    private final static String[] ERROR_IDENTIFIERS = new String[]{
            "Fatal error",
            "Error",
            "Parse error"
    };

    /**
     * List of PHP warning keywords
     */
    private final static String[] WARNING_IDENTIFIERS = new String[]{
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
     * Files and directories to exclude
     *
     * @parameter
     */
    private String[] excludes = new String[0];

    /**
     * Files and directories to include
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
     * Where the output should be stored for jar inclusion
     *
     * @parameter
     */
    private String targetClassesDirectory = "target/classes";

    /**
     * Where the test output should be stored for jar inclusion
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
     * if true php maven will allways overwrite existing php files
     * in the classes folder even if the files in the target folder are newer or at the same date
     *
     * @parameter
     */
    private boolean forceOverwrite = false;

    /**
     * If true, errors triggered because of missing includes will be ignored.
     *
     * @parameter
     */
    private boolean ignoreIncludeErrors = false;

    /**
     * If the output of the php scripts will be written to the console
     */
    private boolean logPhpOutput = false;

    /**
     * The used PHP version (cached after initial call of {@link #getPhpVersion()}
     */
    private PhpVersion phpVersion;

    /**
     * collects all exceptions during the file walk
     */
    private List<Exception> collectedExceptions = Lists.newArrayList();


    public AbstractPhpMojo() {
        if (System.getProperty(LOG_PHP_OUTPUT) != null) {
            logPhpOutput = "true".equals(System.getProperty(LOG_PHP_OUTPUT));
        }
    }

    /**
     * Callback for executing a file.
     *
     * @param file the PHP file to execute
     * @throws MojoExecutionException
     */
    abstract protected void executePhpFile(File file) throws MojoExecutionException;

    /**
     * Callback for file processing.
     *
     * @param file the PHP file to process
     * @throws MojoExecutionException
     */
    abstract protected void handleProcessedFile(File file) throws MojoExecutionException;

    /**
     *
     * @return the current maven project.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     *
     * @return the project's basedir
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     *
     * @return path of the php executable
     */
    public String getPhpExecutable() {
        return phpExecutable;
    }

    /**
     *
     * @return paths and files to exclude
     */
    public String[] getExcludes() {
        return excludes;
    }

    /**
     *
     * @return paths and files to include
     */
    public String[] getIncludes() {
        return includes;
    }

    /**
     *
     * @return additional arguments for php execution
     */
    public String getAdditionalPhpParameters() {
        return additionalPhpParameters;
    }

    /**
     *
     * @return elements used in compile scope
     */
    public List<String> getCompileClasspathElements() {
        return compileClasspathElements;
    }

    /**
     *
     * @return elements used in test scope
     */
    public List<String> getTestClasspathElements() {
        return testClasspathElements;
    }

    /**
     *
     * @return where the php sources can be found
     */
    public File getSourceDirectory() {
        return new File(getBaseDir(), sourceDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainSourceDirectory() {
        return sourceDirectory;
    }

    /**
     *
     * @return where the php test sources can be found
     */
    public File getTestSourceDirectory() {
        return new File(getBaseDir(), testSourceDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainTestSourceDirectory() {
        return testSourceDirectory;
    }

    /**
     *
     * @return where to store the dependency files
     */
    public File getDependenciesTargetDirectory() {
        return new File(getBaseDir(), dependenciesTargetDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainDependenciesTargetDirectory() {
        return dependenciesTargetDirectory;
    }

    /**
     *
     * @return where to store the test dependency files
     */
    public File getTestDependenciesTargetDirectory() {
        return new File(getBaseDir(), testDependenciesTargetDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainTestDependenciesTargetDirectory() {
        return testDependenciesTargetDirectory;
    }

    /**
     *
     * @return where the jar inclusion directory is
     */
    public File getTargetClassesDirectory() {
        return new File(getBaseDir(), targetClassesDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainTargetClassesDirectory() {
        return targetClassesDirectory;
    }

    /**
     *
     * @return where the test-har inclusion directory is
     */
    public File getTargetTestClassesDirectory() {
        return new File(getBaseDir(), targetTestClassesDirectory);
    }

    /**
     *
     * @return the configured probably relative directory
     */
    public String getPlainTargetTestClassesDirectory() {
        return targetTestClassesDirectory;
    }

    public String getPhpFileEnding() {
        return phpFileEnding;
    }

    /**
     *
     * @return if the php sources should be included in the resulting jar
     */
    public boolean isIncludeInJar() {
        return includeInJar;
    }

    /**
     *
     * @return forces target files to be overwritten
     */
    public boolean isForceOverwrite() {
        return forceOverwrite;
    }

    /**
     *
     * @return if include errors should be ignored
     */
    public boolean isIgnoreIncludeErrors() {
        return ignoreIncludeErrors;
    }

    /**
     *
     * @return if php output will be printed to the log
     */
    public boolean isLogPhpOutput() {
        return logPhpOutput;
    }

    /**
     * nessecary for the DirectoryWalker
     *
     * @param message message to log
     */
    @Override
    public void debug(String message) {
        getLog().debug(message);
    }

    /**
     *
     * @param line output line
     * @return if the line contains php error messages
     */
    private boolean isError(String line) {
        line = line.trim();
        for (String errorIdentifier: ERROR_IDENTIFIERS) {
            if (line.startsWith(errorIdentifier + ":")
                    || line.startsWith("<b>" + errorIdentifier + "</b>:")) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param line output line
     * @return if the line contains php warning messages
     */
    private boolean isWarning(String line) {
        line = line.trim();
        for (String warningIdentifier: WARNING_IDENTIFIERS) {
            if (line.startsWith(warningIdentifier + ":")
                    || line.startsWith("<b>" + warningIdentifier + "</b>:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes PHP with the given arguments
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
        String error = stderr.toString();
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

    public String includePathParameter(String[] paths) {
        StringBuilder includePath = new StringBuilder();
        includePath.append("-d include_path=\"");
        for (String path: paths) {
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
                (File)null,
                new StreamConsumer() {
                    @Override
                    public void consumeLine(String line) {
                        if (phpVersion == null && line.startsWith("PHP")) {
                            String version = line.substring(4, 5);
                            if (version.equals("5") || version.equals("6")) {
                                phpVersion = PhpVersion.PHP5;
                            } else if (version.equals("4")) {
                                phpVersion = PhpVersion.PHP4;
                            } else {
                                getLog().error("Unsupported PHP version: " + version + " [" + line + "]");
                            }
                        }
                    }
                }
        );
        if (phpVersion == null) {
            throw new PhpCoreException("Cannot resolve PHP version");
        }

        getLog().debug("PHP version: " + phpVersion.name());
        return phpVersion;
    }

    /**
     * Unzips all compile dependency sources.
     *
     * @throws IOException
     * @throws PhpException
     */
    protected void prepareCompileDependencies() throws IOException, PhpException {
        FileHelper.unzipElements(getDependenciesTargetDirectory(), getCompileClasspathElements());
    }

    /**
     * Unzips all test dependency sources.
     *
     * @throws IOException
     * @throws PhpException
     */
    protected void prepareTestDependencies() throws IOException, PhpException {
        FileHelper.unzipElements(getTestDependenciesTargetDirectory(), getTestClasspathElements());
    }

    /**
     * Event hook
     */
    @Override
    public void directoryWalkFinished() {
        /* ignore */
    }

    /**
     * Event hook
     *
     * @param basedir
     */
    @Override
    public void directoryWalkStarting(File basedir) {
        /* ignore */
    }

    /**
     * Event hook
     *
     * @param percentage
     * @param file
     */
    @Override
    public void directoryWalkStep(int percentage, File file) {
        try {
            if (file.isFile() && file.getName().endsWith("." + getPhpFileEnding()))
                executePhpFile(file);
            if (file.isFile())
                handleProcessedFile(file);
        } catch (Exception e) {
            getLog().debug(e);
            collectedExceptions.add(e);
        }
    }

    /**
     * Triggers the walk process
     *
     * @param parentFolder the folder to start in
     * @throws MultiException
     */
    protected final void goRecursiveAndCall(File parentFolder) throws MultiException {
        if (!parentFolder.isDirectory()) {
            getLog().error("Source directory (" + parentFolder.getAbsolutePath() + ")");
            return;
        }

        DirectoryWalker walker = new DirectoryWalker();

        walker.setBaseDir(parentFolder);
        walker.addDirectoryWalkListener(this);
        walker.addSCMExcludes();

        for (String exlude: excludes) {
            walker.addExclude(exlude);
        }
        for (String include: includes) {
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
