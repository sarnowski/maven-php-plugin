package org.phpmaven.plugin.build;

import java.io.File;

import org.phpmaven.plugin.build.script.ScriptTestRunnerMojo;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ScriptTestRunnerTest extends TestCase {
	public void testReplace() {
		ScriptTestRunnerMojo mojo= new ScriptTestRunnerMojo();
		String command = mojo.replaceCommandArgs("hansi *{testFolder}", "testFolder", "/home/cw");
		System.out.println(command);
		System.out.println(File.pathSeparator);
		Assert.assertEquals("hansi /home/cw", command);
	}
}
