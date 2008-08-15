package de.keytec.maven.php;

import junit.framework.Assert;
import junit.framework.TestCase;

public class PhpUnitCompileTest extends TestCase {
	public void testGetPhpVersion() throws Exception{
		PhpUnitCompile compile = new PhpUnitCompile();
		compile.phpExe="php.exe";
		PHPVersion phpVersion = compile.getPhpVersion();
		Assert.assertEquals(PHPVersion.PHP5, phpVersion);
	}
}
