package org.phpmaven.plugin.report;

public class PHPDocumentorExecuteException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String parent;

	public PHPDocumentorExecuteException(String parent) {
		this.parent = parent;
	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return "Error while executing phpdoc. ("+parent+")";
	}

}
