package org.phpmaven.plugin.build;

public class UnitTestCaseFailureException extends Exception {

	private final int completeFailures;
	private final int completeErrors;

	public UnitTestCaseFailureException(int completeErrors,
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
		
		return "Unit Test fails with "+completeFailures+" failures and "+completeErrors+" errors.";
	}

}
