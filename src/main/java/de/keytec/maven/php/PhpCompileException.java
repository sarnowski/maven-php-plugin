package de.keytec.maven.php;

import java.io.File;


public class PhpCompileException extends Exception {


	private static final long serialVersionUID = 1L;
	private final File phpFile;
	private final String phpErrorMessage;
	public final static int ERROR = 0;
	public final static int WARNING = 1;
	private final int errorType;
	private final String commandString;

	public PhpCompileException(String commandString,int errorType, File phpFile,
			String phpErrorMessage) {
		this.commandString = commandString;
		this.errorType = errorType;
		this.phpFile = phpFile;
		this.phpErrorMessage = phpErrorMessage;
	}

	public String getMessage() {
		return "Type: " + errorType + " " + phpErrorMessage + " in "
				+ phpFile.toString() +" ("+commandString+")+\n";
	}
}
