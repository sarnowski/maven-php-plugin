package org.phpmaven.plugin.build;

public class ExecutionError extends Exception {
	
	private String message;

	public ExecutionError(String message) {
		this.message = message;
	}
	public ExecutionError() {
		this.message = "Execution of the script failed. ";
	}

	public String getMessage() {
		return message;
	}

	private static final long serialVersionUID = 1L;

}
