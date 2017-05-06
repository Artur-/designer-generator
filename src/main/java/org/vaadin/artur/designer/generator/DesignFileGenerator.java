package org.vaadin.artur.designer.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import elemental.json.Json;
import elemental.json.JsonObject;

public abstract class DesignFileGenerator {

    private Document design;
    private JsonObject designProperties;
    private Map<String, String> packageMapping;

    public DesignFileGenerator(Document design) {
        this.design = design;
    }

    protected Map<String, String> getPackageMapping() {
        if (packageMapping == null) {

            packageMapping = new HashMap<>();

            Elements mappings = Selector.select("[name=package-mapping]",
                    design.head());
            Iterator<Element> i = mappings.iterator();
            while (i.hasNext()) {
                Element mapping = i.next();
                String[] parts = mapping.attr("content").split(":", 2);
                packageMapping.put(parts[0], parts[1]);
            }

        }
        return packageMapping;
    }

    protected String getDesignPropertyString(String key) {
        JsonObject prop = getDesignProperties();
        if (prop.hasKey(key)) {
            return getDesignProperties().getString(key);
        } else {
            return null;
        }
    }

    protected Boolean getDesignPropertyBoolean(String key) {
        JsonObject prop = getDesignProperties();
        if (prop.hasKey(key)) {
            return getDesignProperties().getBoolean(key);
        } else {
            return null;
        }
    }

    private JsonObject getDesignProperties() {
        if (designProperties == null) {
            Elements properties = Selector.select("[name=design-properties]",
                    design.head());
            if (properties.size() != 0) {
                String designPropertiesContent = properties.get(0)
                        .attr("content");
                designProperties = Json.parse(designPropertiesContent);
            } else {
                designProperties = Json.createObject();
            }
        }

        return designProperties;
    }

    protected String tagToClass(Element element) {
        return tagToClass(element.tagName());
    }

    protected String tagToClass(String tag) {
        String dashClassName;
        String pkgName;
        if (tag.startsWith("vaadin-")) {
            pkgName = "com.vaadin.ui";
            dashClassName = tag.substring("vaadin-".length());
        } else if (tag.startsWith("v-")) {
            pkgName = "com.vaadin.ui";
            dashClassName = tag.substring("v-".length());
        } else {
            int dashIndex = tag.indexOf("-");
            String prefix = tag.substring(0, dashIndex);
            pkgName = getPackageMapping().get(prefix);
            dashClassName = tag.substring(dashIndex + 1);
        }
        String className = Util
                .capitalize(Util.dashSeparatedToCamelCase(dashClassName));

        return pkgName + "." + className;
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

        orderImports(javaClass);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            IOUtils.write(javaClass.toString(), out, StandardCharsets.UTF_8);
        }
    }

    private void orderImports(JavaClassSource javaClass) {
        // Implemented manually because of
        // https://issues.jboss.org/browse/ROASTER-56
        List<Import> imports = new ArrayList<>(javaClass.getImports());
        for (Import imp : imports) {
            javaClass.removeImport(imp);
        }
        Collections.sort(imports, (i1, i2) -> {
            return i1.getQualifiedName().compareTo(i2.getQualifiedName());
        });

        for (Import imp : imports) {
            javaClass.addImport(imp);
        }

    }

    protected Element getBody() {
        return design.body();
    }

    protected Element getRootDesignElement() {
        return getBody().child(0);
    }

}
