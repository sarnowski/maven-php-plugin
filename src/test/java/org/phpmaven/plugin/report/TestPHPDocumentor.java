package org.phpmaven.plugin.report;

import java.io.File;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

public class TestPHPDocumentor extends TestCase{
	public void testPHPDocumentor() throws Exception {

		PHPDocumentor doc = new PHPDocumentor();
		Model model = new Model();
		MavenProject mavenProject = new MavenProject(model);
		
		File absoluteFile = new File("/home/cw/workspace/phpmaven/multimaster/org.phpmaven.php5.sample/pom.xml").getAbsoluteFile();
		mavenProject.setFile(absoluteFile);
		doc.setProject(mavenProject);
		doc.phpDocFilePath="/home/cw/PhpDocumentor-1.4.3/phpdoc";
		doc.phpExe="php";
		doc.sourceDirectory="src/main/php";
		
		doc.phpDocConfigFile=new File(mavenProject.getBasedir()+"/src/site/phpdoc/phpdoc.config");
		doc.generatedPhpDocConfigFile=new File(mavenProject.getBasedir()+"/target/site/phpdoc/phpdoc.ini");
		doc.outputApiDocDirectory=new File(mavenProject.getBasedir()+"target/site/apidocs/phpdoc");
		doc.executeReport(new Locale("de"));
	}
}
