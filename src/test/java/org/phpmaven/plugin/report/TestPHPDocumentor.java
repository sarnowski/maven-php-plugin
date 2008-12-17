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
		mavenProject.setFile(new File("C:/de.keytec.dev/cw/y/workbenches/maventest/org.phpmaven.multimaster/org.phpmaven.php4.sample/pom.xml"));
		doc.setProject(mavenProject);
		doc.phpExe="php.exe";
		doc.sourceDirectory="src/main/php";
		doc.phpDocConfigFile=new File(mavenProject.getBasedir()+"/src/site/phpdoc/phpdoc.config");
		doc.generatedPhpDocConfigFile=new File(mavenProject.getBasedir()+"/target/site/phpdoc/phpdoc.ini");
		doc.outputApiDocDirectory=new File(mavenProject.getBasedir()+"target/site/apidocs/phpdoc");
		//doc.executeReport(new Locale("de"));
	}
}
