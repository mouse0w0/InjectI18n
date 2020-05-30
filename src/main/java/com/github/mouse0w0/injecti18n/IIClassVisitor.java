package com.github.mouse0w0.injecti18n;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class IIClassVisitor extends ClassVisitor {

    private String className;

    public IIClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name.replace("/", ".");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new IIMethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions), className, name, descriptor);
    }
}
