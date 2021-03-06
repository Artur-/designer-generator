package org.vaadin.artur.designer.generator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

public class ElementFileGenerator extends DesignFileGenerator {

    public ElementFileGenerator(Document document) {
        super(document);
    }

    public void generate(String designJavaPkg, String designJavaClass,
            File outputJavaFileBaseFolder) throws IOException {

        // @ServerClass("com.vaadin.template.orders.ui.view.orders.OrderEditViewDesign")
        // public class OrderEditViewDesignElement extends VerticalLayoutElement
        // {
        String elementJavaClass = designJavaClass + "Element";
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(designJavaPkg).setName(elementJavaClass);
        javaClass.setSuperType(
                getElementClass(tagToClass(getRootDesignElement())));
        javaClass.addAnnotation("com.vaadin.testbench.elementsbase.ServerClass")
                .setStringValue(designJavaPkg + "." + designJavaClass);
        javaClass.addAnnotation("com.vaadin.annotations.AutoGenerated");

        Elements idElements = Selector.select("[id]", getBody());
        Iterator<Element> i = idElements.iterator();
        while (i.hasNext()) {
            // public HorizontalLayoutElement getReportHeader() {
            // return $(HorizontalLayoutElement.class).id("reportHeader");
            // }

            Element idElement = i.next();
            String type = tagToClass(idElement);
            String propertyName = idElement.attr("id");
            String elementType = getElementClass(type);
            javaClass.addMethod().setName("get" + Util.capitalize(propertyName))
                    .setBody("return $(" + elementType + ".class).id(\""
                            + propertyName + "\");")
                    .setPublic().setReturnType(elementType);
        }
        javaClass.getJavaDoc().setFullText("!! DO NOT EDIT THIS FILE !!\n"
                + "\n"
                + "This class is generated by Vaadin Designer and will be overwritten.\n");

        writeFile(outputJavaFileBaseFolder, javaClass, true);

        // Generate implementation file, mainly to avoid compilation errors when
        // using nested designs
        if (elementJavaClass.endsWith("DesignElement")) {
            String designImplementationJavaClass = designJavaClass
                    .replaceFirst("Design$", "");
            String elementImplementationJavaClass = elementJavaClass
                    .replaceFirst("DesignElement$", "Element");
            final JavaClassSource implJavaClass = Roaster
                    .create(JavaClassSource.class);
            implJavaClass.setPackage(designJavaPkg)
                    .setName(elementImplementationJavaClass);
            javaClass
                    .addAnnotation(
                            "com.vaadin.testbench.elementsbase.ServerClass")
                    .setStringValue(designJavaPkg + "."
                            + designImplementationJavaClass);

            implJavaClass.setSuperType(elementJavaClass);

            writeFile(outputJavaFileBaseFolder, implJavaClass, false);
        }
    }

    private String getElementClass(String componentClass) {
        componentClass = componentClass.replace("com.vaadin.ui.",
                "com.vaadin.testbench.elements.");
        return componentClass + "Element";
    }
}
