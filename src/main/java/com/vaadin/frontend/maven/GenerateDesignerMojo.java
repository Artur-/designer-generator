package com.vaadin.frontend.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Selector;
import org.vaadin.artur.designer.generator.CompanionFileGenerator;
import org.vaadin.artur.designer.generator.ElementFileGenerator;

/**
 * Generate designer files
 *
 */
@Mojo(name = "generate-designer", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateDesignerMojo extends AbstractMojo {

    @Parameter(property = "project", defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject mavenProject;

    /**
     * The folder to generate the Java Design files in.
     * <p>
     * Default is {@literal src/main/designer-java}
     */
    @Parameter(property = "vaadin.designer.java.dir", defaultValue = "${basedir}/src/main/designer-java")
    protected File designJavaFolder;

    /**
     * The folder to generate the Java Element test files in.
     * <p>
     * Default is {@literal src/test/designer-java}
     */
    @Parameter(property = "vaadin.designer.elements.java.dir", defaultValue = "${basedir}/src/test/designer-java")
    protected File designJavaElementsFolder;

    public static class DesignFileFinder extends SimpleFileVisitor<Path> {

        private Map<Path, Document> designFiles = new HashMap<>();

        public Map<Path, Document> getDesignFiles() {
            return designFiles;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
            File file = path.toFile();
            if (file.getName().endsWith(".html")) {
                Document doc = Jsoup.parse(file, "utf-8");
                if (!Selector.select("[name='design-properties']", doc.head())
                        .isEmpty()) {
                    designFiles.put(path, doc);
                }
            }
            return FileVisitResult.CONTINUE;
        }
    };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Resource resource : mavenProject.getResources()) {
            DesignFileFinder finder = new DesignFileFinder();
            String root = resource.getDirectory();
            File rootFile = new File(root);
            Path rootPath = rootFile.toPath();
            try {
                Files.walkFileTree(rootPath, finder);
            } catch (IOException e1) {
                getLog().error("Error finding design files in " + rootPath, e1);
            }

            for (Entry<Path, Document> designFile : finder.getDesignFiles()
                    .entrySet()) {
                try {

                    Path designRelativeToRoot = rootPath
                            .relativize(designFile.getKey().toFile().toPath());

                    Path packagePath = designRelativeToRoot.getParent();
                    String javaPackage = packagePath.toString().replace("/",
                            ".");
                    String javaClass = designRelativeToRoot.getFileName()
                            .toString().replace(".html", "");
                    boolean generateImplementationJava = javaClass
                            .endsWith("Design");
                    new CompanionFileGenerator(designFile.getValue()).generate(
                            javaPackage, javaClass, generateImplementationJava,
                            designJavaFolder);
                    new ElementFileGenerator(designFile.getValue()).generate(
                            javaPackage, javaClass, designJavaElementsFolder);
                } catch (IOException e) {
                    getLog().error("Unable to create design file for "
                            + designFile.getKey(), e);
                }
            }
        }

        getLog().debug("Adding compile source root: " + designJavaFolder);
        mavenProject.addCompileSourceRoot(designJavaFolder.getAbsolutePath());

    }

    protected void writeFile(File folder, JavaClassSource javaClass)
            throws IOException {
        String pkgName = javaClass.getPackage();
        String className = javaClass.getName();
        String outFile = pkgName.replace(".", File.separator);
        outFile += File.separator;
        outFile += className;
        outFile += ".java";

        File outputFile = new File(folder, outFile);
        File parent = outputFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            IOUtils.write(javaClass.toString(), out, StandardCharsets.UTF_8);
        }
    }
}