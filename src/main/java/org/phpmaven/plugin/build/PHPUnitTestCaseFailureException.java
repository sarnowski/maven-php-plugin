package org.phpmaven.plugin.build;

public class PHPUnitTestCaseFailureException extends Exception {

	private final int completeFailures;
	private final int completeErrors;

	public PHPUnitTestCaseFailureException(int completeErrors,
			int completeFailures) {
				this.completeErrors = completeErrors;
				this.completeFailures = completeFailures;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4919747847798685627L;

	@Override
	public String getMessage() {
		
		return "PHPUnit fails with "+completeFailures+" failures and "+completeErrors+" errors.";
	}

}
