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
import org.phpmaven.plugin.build.FileHelper;
import org.phpmaven.plugin.build.MultipleCompileException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public abstract class AbstractPhpExecutor extends AbstractMojo implements DirectoryWalkListener {

    /**
     * Can be defined via -DflushPHPOutput=true
     */
    public final static String LOG_PHP_OUTPUT = "flushPHPOutput";

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
    protected MavenProject project;

    /**
     * if true php maven will allways overwrite existing php files
     * in the classes folder even if the files in the target folder are newer or at the same date
     *
     * @parameter expression="false" required="true"
     */
    protected boolean forceOverwrite = false;

    /**
     * @parameter expression="${project.basedir}" required="true"
     * @readonly
     */
    protected File baseDir;

    /**
     * Files and directories to exclude
     *
     * @parameter
     */
    public String[] excludes = new String[0];

    /**
     * Files and directories to include
     *
     * @parameter
     */
    public String[] includes = new String[0];

    /**
     * PHP arguments. Use php -h to get a list of all php compile arguments.
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


    /**
     * collects all exceptions during the file walk
     */
    private ArrayList<Exception> collectedExceptions = Lists.newArrayList();

    /**
     * If the output of the php scripts will be written to the console
     */
    protected boolean logPhpOutput;

    /**
     * The used PHP version
     */
    private PhpVersion phpVersion;


    public AbstractPhpExecutor() {
        if (System.getProperty(LOG_PHP_OUTPUT) == null) {
            logPhpOutput = false;
        } else {
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
    abstract protected void handleProcesedFile(File file) throws MojoExecutionException;

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
     *
     * @return configured additional parameters for validation
     */
    protected String getCompilerArgs() {
        if (compileArgs == null) {
            compileArgs = "";
        } else if (!compileArgs.startsWith(" ")) {
            compileArgs = " " + compileArgs;
        }
        return compileArgs;
    }

    /**
     *
     * @return will always return false
     */
    protected boolean isIgnoreIncludeErrors() {
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

        final Commandline commandLine = new Commandline(phpExe + " " + arguments);

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
            if (throwError.get()) {
                throw new PhpErrorException(file, message);
            } else if (throwWarning.get()) {
                throw new PhpWarningException(file, message);
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
     *
     * @throws IOException
     */
    protected final void prepareCompileDependencies() throws IOException {
        FileHelper.prepareDependencies(baseDir.toString() + Statics.phpinc, classpathElements);
    }

    /**
     * Event hook
     */
    public void directoryWalkFinished() {
        getLog().debug("--- All files processed.");
    }

    /**
     * Event hook
     *
     * @param basedir
     */
    public void directoryWalkStarting(File basedir) {
        getLog().debug("--- Starting folder walking in: " + basedir.getAbsoluteFile());
    }

    /**
     * Event hook
     *
     * @param percentage
     * @param file
     */
    public void directoryWalkStep(int percentage, File file) {
        getLog().debug("percentage: " + percentage);
        try {
            if (file.isFile() && file.getName().endsWith(".php"))
                executePhpFile(file);
            if (file.isFile())
                handleProcesedFile(file);
        } catch (Exception e) {
            getLog().debug(e);
            collectedExceptions.add(e);
        }
    }

    /**
     * Triggers the walk process
     *
     * @param parentFolder the folder to start in
     * @throws MultipleCompileException
     */
    protected final void goRecursiveAndCall(File parentFolder) throws MultipleCompileException {
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
            throw new MultipleCompileException(collectedExceptions);
        }
    }
}
