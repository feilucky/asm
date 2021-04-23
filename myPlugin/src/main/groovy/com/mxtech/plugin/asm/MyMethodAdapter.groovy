package com.mxtech.plugin.asm

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

public class MyMethodAdapter extends AdviceAdapter implements Opcodes {

    private static final String TAG = "MyMethodAdapter"

    /**
     * Creates a new {@link AdviceAdapter}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     * @param mv
     *            the method visitor to which this adapter delegates calls.
     * @param access
     *            the method's access flags (see {@link Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type Type}).
     */
    public MyMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc)
        println(TAG + ",MyMethodAdapter() .name.desc: " + name + ", " + desc)
    }

    @Override
    void visitCode() {
        super.visitCode()
        println(TAG + "===visitCode===")
    }

    @Override
    void visitLabel(Label label) {
        super.visitLabel(label)
        println(TAG + "===visitLabel===" + label.info)

    }

    @Override
    void visitInsn(int opcode) {
        super.visitInsn(opcode)
        println(TAG + "===visitInsn===")

    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        println(TAG + "===onMethodEnter===")
        visitMethodInsn(INVOKESTATIC, "com/mxtech/demo/plugin/AopInteceptor", "start", "()V", false)
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode)
        println(TAG + "===onMethodExit===")
        visitMethodInsn(INVOKESTATIC, "com/mxtech/demo/plugin/AopInteceptor", "end", "()V", false)

    }
}