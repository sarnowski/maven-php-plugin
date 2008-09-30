package org.phpmaven.plugin.build;

import java.util.ArrayList;

public class MuilplePhpCompileException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ArrayList<Exception> exceptions;
	public MuilplePhpCompileException(ArrayList<Exception> exceptions){
		this.exceptions = exceptions;
		
	}
	@Override
	public String getMessage() {
		StringBuffer message = new StringBuffer(); 
		for (int i = 0; i < exceptions.size(); i++) {
			message.append(exceptions.get(i).getMessage()+" \n");
		}
		return message.toString();
	}


}
