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

import org.apache.maven.plugin.MojoExecutionException;
import org.phpmaven.plugin.build.FileHelper;

import java.io.File;



/**
 * php-validate execute the php with all php files under the source folder. 
 * All dependencies will be part of the include_path. 
 * The comand line call looks like php {compileArgs} -d={generatedIncludePath} {sourceFile} 
 * 
 * @requiresDependencyResolution
 * @goal php-validate
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public class PhpValidate extends AbstractPhpMojo {
	
	/**
	 * If true require_once or include_once errors will be ignored Default is
	 * false.
	 * 
	 * @parameter expression=false
	 * @required
	 */
	private boolean ignoreIncludeErrors;
	/**
	 * A list of files which will not be validated but they will also be part of the result.
	 * @parameter
	 */
	private String[] excludeFromValidation;
	/**
	 * If true the validation will be skiped and the source files will be moved to the target/classes folder wihtouth validation.
	 * false.
	 * 
	 * @parameter expression=false
	 * @required
	 */
	private boolean ignoreValidate;

	protected boolean getIgnoreValidate() {
		return ignoreValidate;
	}
	private boolean isExcluded(File file) {
		
		for (int i=0; excludeFromValidation!=null && i<excludeFromValidation.length;i++) { 
			
			if (file.getAbsolutePath().replace("\\", "/").endsWith(excludeFromValidation[i].replace("\\", "/"))) { 
				return true;
			}
		}
		return false;
	}
	protected void executePhpFile(File file) throws MojoExecutionException {
		
		String commandString = getCompilerArgs()+" -d include_path=\"" + File.pathSeparator
				+ file.getParentFile().getAbsolutePath() + File.pathSeparator
				+ baseDir.getAbsolutePath() + Statics.phpinc + File.pathSeparator + baseDir
				+ sourceDirectory + "\" \"" + file.getAbsolutePath() + "\"";
		try {
			if (ignoreValidate == false && isExcluded(file)==false) {
				execute(commandString, file);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public void execute() throws MojoExecutionException {
		project.addCompileSourceRoot(sourceDirectory);
		try {
			if (!ignoreValidate)
				prepareCompileDependencies();
			File file = new File(baseDir.getAbsolutePath() + sourceDirectory);
			goRecursiveAndCall(file);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	protected boolean isIgnoreIncludeErrors() {
		return ignoreIncludeErrors;
	}

	@Override
	protected void handleProcesedFile(File file) throws MojoExecutionException {
		FileHelper.copyToTargetFolder(baseDir,sourceDirectory,file,Statics.targetClassesFolder,forceOverwrite);
	}
}