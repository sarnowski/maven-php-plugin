package org.phpmaven.plugin.report;

public class PHPDocumentorExecuteException extends Exception {

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
