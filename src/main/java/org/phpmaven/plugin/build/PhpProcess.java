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

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;


/**
 * php-validate execute the php with all php files under the source folder. 
 * All dependencies will be part of the include_path. 
 * The command line call looks like php {compileArgs} -d={generatedIncludePath} {sourceFile}
 *
 * CHECKSTYLE:OFF unknown tags
 * @requiresDependencyResolution compile
 * @goal process
 * CHECKSTYLE:ON
 * @author Tobias Sarnowski
 * @author Christian Wiedemann
 */
public final class PhpProcess extends AbstractPhpMojo {

    /**
     * A list of files which will not be validated but they will also be part of the result.
     *
     * @parameter
     */
    private String[] excludeFromValidation = new String[0];

    /**
     * If true the validation will be skipped and the source files will be moved to the target/classes
     * folder wihtout validation.
     *
     * @parameter
     */
    private boolean ignoreValidate;

    /**
     * Returns if the PHP validation should be skipped.
     *
     * @return if the validation should be skipped
     */
    private boolean isIgnoreValidate() {
        return ignoreValidate;
    }

    /**
     * Checks a file if it should be excluded from processing.
     *
     * @param file
     * @return if the file should be excluded from validation
     */
    private boolean isExcluded(File file) {
        for (String excluded : excludeFromValidation) {
            if (file.getAbsolutePath().replace("\\", "/").endsWith(excluded.replace("\\", "/"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (isIgnoreValidate()) {
            getLog().info("Validation of php sources is disabled.");
        }
        if (!isIncludeInJar()) {
            getLog().info("Not including php sources in resulting output.");
        }

        getProject().addCompileSourceRoot(getSourceDirectory().getAbsolutePath());
        try {
            if (!isIgnoreValidate()) {
                prepareCompileDependencies();
            }
            goRecursiveAndCall(getSourceDirectory());
        } catch (MultiException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (PhpException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected void executePhpFile(File file) throws MojoExecutionException {
        if (isIgnoreValidate() || isExcluded(file)) {
            return;
        }

        final String includePath = includePathParameter(new String[] {
                file.getParentFile().getAbsolutePath(),
                getDependenciesTargetDirectory().getAbsolutePath(),
                getSourceDirectory().getAbsolutePath(),
        });
        final String command = includePath + " \"" + file.getAbsolutePath() + "\"";

        try {
            getLog().debug("Validating: " + file.getAbsolutePath());
            execute(command, file);
        } catch (PhpException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected void handleProcessedFile(File file) throws MojoExecutionException {
        if (!isIncludeInJar()) {
            return;
        }

        try {
            FileHelper.copyToFolder(
                    getSourceDirectory(),
                    getTargetClassesDirectory(),
                    file,
                    isForceOverwrite());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy source file to target directory", e);
        }
    }
}
