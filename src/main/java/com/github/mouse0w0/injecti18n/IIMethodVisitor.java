package com.github.mouse0w0.injecti18n;

import org.objectweb.asm.MethodVisitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class IIMethodVisitor extends MethodVisitor {
    private static final String TRANSLATE_METHOD_OWNER = "com/github/mouse0w0/injecti18n/I18n";
    private static final String TRANSLATE_METHOD_NAME = "translate";
    private static final String TRANSLATE_METHOD_DESC = "(Ljava/lang/String;)Ljava/lang/String;";

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[!-/:-@\\[-`{-~\\s]*");
    private static final Pattern LINE_SYMBOL_PATTERN = Pattern.compile("[\n\r\f]");

    private final String className;
    private final String methodName;
    private final String uniqueId;

    private final Map<String, String> translationMap = new LinkedHashMap<>();

    public IIMethodVisitor(int i, MethodVisitor methodVisitor, String className, String methodName, String methodDesc) {
        super(i, methodVisitor);
        this.className = className;
        this.methodName = methodName;
        this.uniqueId = digits(className.hashCode() ^ methodName.hashCode() ^ methodDesc.hashCode(), 8);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Map<String, String> getTranslationMap() {
        return translationMap;
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (!(value instanceof String)) {
            super.visitLdcInsn(value);
            return;
        }

        String str = (String) value;
        if (SYMBOL_PATTERN.matcher(str).matches() || LINE_SYMBOL_PATTERN.matcher(str).find()) {
            super.visitLdcInsn(value);
            return;
        }

        String translationKey = className + "#" + methodName + "#" + uniqueId + "#" + digits(str.hashCode(), 8) + "#" + translationMap.size();
        translationMap.put(translationKey, str);
        super.visitLdcInsn(translationKey);
        super.visitMethodInsn(INVOKESTATIC,
                TRANSLATE_METHOD_OWNER, TRANSLATE_METHOD_NAME, TRANSLATE_METHOD_DESC, false);
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (!translationMap.isEmpty()) {
            Main.addMethodVisitor(this);
        }
    }
}
