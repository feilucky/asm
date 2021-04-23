package com.mxtech.plugin.asm


import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MyClassVisitor extends ClassVisitor {
    private ClassVisitor classVisitor

    MyClassVisitor(final ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor)
        this.classVisitor = classVisitor
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = null
        println("===visitMethod.name===" + name + ": " + desc + ": " + signature)
        if (name.equals("onCreate")) {
            methodVisitor = new MyMethodAdapter(Opcodes.ASM4, cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc)
        } else {
            methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        }

        return methodVisitor
    }


    @Override
    void visitEnd() {
        super.visitEnd()
    }
}
