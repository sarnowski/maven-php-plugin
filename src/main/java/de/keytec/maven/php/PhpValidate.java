package de.keytec.maven.php;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;



/**
 * php-validate execute the php with all php files under the source folder. 
 * All dependencies will be part of the include_path. 
 * The comand line call looks like php {compileArgs} -d={generatedIncludePath} {sourceFile} 
 * 
 * @requiresDependencyResolution
 * @goal php-validate
 */
public class PhpValidate extends AbstractPhpCompile {

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
		
		String commandString = phpExe +  getCompilerArgs()+" -d include_path=\"" + File.pathSeparator
				+ file.getParentFile().getAbsolutePath() + File.pathSeparator
				+ baseDir.getAbsolutePath() + Statics.phpinc + File.pathSeparator + baseDir
				+ sourceDirectory + "\" \"" + file.getAbsolutePath() + "\"";
		try {
			if (ignoreValidate == false && isExcluded(file)==false) {
				phpCompile(commandString, file);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public void execute() throws MojoExecutionException {
		try {
			if (!ignoreValidate)
				prepareCompileDependencies();
			File file = new File(baseDir.getAbsolutePath() + sourceDirectory);
			goRecursiveAndCall(file);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	protected boolean getIgnoreIncludeErrors() { 
		return ignoreIncludeErrors;
	}

	@Override
	protected void handleProcesedFile(File file) throws MojoExecutionException {
		copyToTargetFolder(sourceDirectory,file,Statics.targetClassesFolder);
	}
}