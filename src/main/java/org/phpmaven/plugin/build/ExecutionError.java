package org.phpmaven.plugin.build;

public class ExecutionError extends Exception {
	
	public String getMessage() {
		return "Execution of the script failed. ";
	}

	private static final long serialVersionUID = 1L;

}
