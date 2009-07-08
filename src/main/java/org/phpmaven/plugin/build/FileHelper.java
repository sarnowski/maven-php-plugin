package org.phpmaven.plugin.build;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

public class FileHelper {
	private static void unjar(File jarFile, File destDir) throws IOException {
		java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		java.util.Enumeration<java.util.jar.JarEntry> items = jar.entries();
		
		while (items.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) items
					.nextElement();
			
			java.io.File f = new java.io.File(destDir + java.io.File.separator
					+ file.getName());
			if (f.exists() ) {
				continue;
			}
			if (file.isDirectory()) { // if its a directory, create it
				f.mkdir();
				continue;
			}
			
			java.io.InputStream is = jar.getInputStream(file); // get the input
			// stream
			java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
			IOUtil.copy(is, fos);
//			while (is.available() > 0) { // write contents of 'is' to 'fos'
//				fos.write(is.read());
//			}
			fos.close();
			is.close();
		}

	}

	public final static void prepareDependencies(String targetFolder,List<String> elements)
			throws IOException {

		File targetFile = new File(targetFolder);
		targetFile.mkdirs();
		for (int i = 0; i < elements.size(); i++) {
			File sourceFile = new File((String) elements.get(i));
			if (sourceFile.isFile()) {
				unjar(sourceFile, targetFile);
			}
		}
	}
	public static final void copyToTargetFolder(File baseDir,String sourceDirectory,
			File sourceFile, String targetClassFolder, boolean forceOverwrite)
			throws MojoExecutionException {
		String relative = PathUtils.toRelative(new File(baseDir.toString()
				+ sourceDirectory), sourceFile.toString());

		
		File targetFile = new File(baseDir.toString() + targetClassFolder + "/"
				+ relative);

		
		try {
			if (forceOverwrite) { 
				FileUtils.copyFile(sourceFile, targetFile);
			}else { 
				FileUtils.copyFileIfModified(sourceFile, targetFile);
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
