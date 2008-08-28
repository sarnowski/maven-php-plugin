package de.keytec.maven.php;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;



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
		
		String commandString = phpExe +  getCompilerArgs()+" -d include_path=\";"
				+ file.getParentFile().getAbsolutePath() + ";"
				+ baseDir.getAbsolutePath() + Statics.phpinc + ";" + baseDir
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
		String relative = PathUtils.toRelative(new File(baseDir.toString()
				+ sourceDirectory), file.toString());
		
		File targetFile = new File(baseDir.toString()
				+ Statics.targetClassesFolder + "/" + relative);
		
		if (targetFile.getParentFile().getName().equalsIgnoreCase("cvs")) {
			return;
		}
		targetFile.getParentFile().mkdirs();
		getLog().debug("copy from:" + file + " to: " + targetFile.toString());
		try {
			FileUtils.copyFileToDirectory(file, targetFile.getParentFile());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}