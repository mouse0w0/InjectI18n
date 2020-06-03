package com.github.mouse0w0.injecti18n;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Main {

    private static final String I18N_CLASS_FILE_NAME = "com/github/mouse0w0/injecti18n/I18n.class";
    private static final String LANG_FILE_NAME = "generated.lang";

    private static final List<IIMethodVisitor> METHOD_VISITORS = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Illegal arguments, for example: java -jar injecti18n.jar target.jar");
        }
        File inputFile = new File(args[0]).getAbsoluteFile();
        File outputFile = new File(args[0].substring(0, args[0].indexOf(".jar")) + "_transformed.jar").getAbsoluteFile();
        System.out.println("Input File: " + inputFile);
        System.out.println("Output File: " + outputFile);
        try {
            transform(inputFile, outputFile);
            System.out.println("Completed Transform!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed Transform!");
        }
    }

    public static void addMethodVisitor(IIMethodVisitor methodVisitor) {
        METHOD_VISITORS.add(methodVisitor);
    }

    private static void transform(File inputFile, File outputFile) throws IOException {
        if (!outputFile.isAbsolute()) outputFile = outputFile.getAbsoluteFile();
        if (!outputFile.exists()) {
            if (outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            outputFile.createNewFile();
        }

        try (JarFile inputJar = new JarFile(inputFile);
             JarOutputStream outputJar = new JarOutputStream(new FileOutputStream(outputFile))) {
            Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    try (InputStream input = inputJar.getInputStream(jarEntry)) {
                        ClassReader cr = new ClassReader(input);
                        ClassWriter cw = new ClassWriter(0);
                        IIClassVisitor cv = new IIClassVisitor(Opcodes.ASM5, cw);
                        cr.accept(cv, 0);
                        outputJar.putNextEntry(new JarEntry(jarEntry.getName()));
                        outputJar.write(cw.toByteArray());
                    }
                } else {
                    copy(jarEntry, inputJar, outputJar);
                }
            }

            copyI18nClass(outputJar);
            generateLangFile(outputJar);
        }
    }

    private static void generateLangFile(JarOutputStream output) throws IOException {
        output.putNextEntry(new JarEntry(LANG_FILE_NAME));
        for (IIMethodVisitor mv : METHOD_VISITORS) {
            IOUtils.write("# " + mv.getClassName() + "#" + mv.getMethodName() + "\n", output, StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : mv.getTranslationMap().entrySet()) {
                IOUtils.write(entry.getKey() + "=" + entry.getValue() + "\n", output, StandardCharsets.UTF_8);
            }
            IOUtils.write("\n", output, StandardCharsets.UTF_8);
        }
    }

    private static void copyI18nClass(JarOutputStream output) throws IOException {
        try (InputStream input = I18n.class.getResourceAsStream("I18n.class")) {
            output.putNextEntry(new JarEntry(I18N_CLASS_FILE_NAME));
            IOUtils.copy(input, output);
        }
    }

    private static void copy(JarEntry jarEntry, JarFile inputJar, JarOutputStream output) throws IOException {
        try (InputStream input = inputJar.getInputStream(jarEntry)) {
            output.putNextEntry(new JarEntry(jarEntry.getName()));
            IOUtils.copy(input, output);
        }
    }
}
