package de.keytec.maven.php;

public class PhpExecutionError extends Exception {
	
	public String getMessage() {
		return "Execution of php failed. Maybe the php.exe is not in path";
	}

	private static final long serialVersionUID = 1L;

}
