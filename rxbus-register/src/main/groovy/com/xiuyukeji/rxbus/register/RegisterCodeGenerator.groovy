package com.xiuyukeji.rxbus.register

import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.Opcodes.*

/**
 * 注册Index代码生成
 *
 * @author Created by jz on 2018/5/13 16:54
 */
class RegisterCodeGenerator {

    static void insertInitCodeTo(List<String> names, File generateFile) {
        if (names == null) {
            return
        }

        def generator = new RegisterCodeGenerator(names)
        if (generateFile.getName().endsWith('.jar')) {
            generator.insertInitCodeIntoJarFile(generateFile)
        }
    }

    private List<String> names;

    RegisterCodeGenerator(List<String> names) {
        this.names = names
    }

    private File insertInitCodeIntoJarFile(File jarFile) {
        if (!jarFile) {
            return jarFile
        }

        def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")
        if (optJar.exists()) {
            optJar.delete()
        }

        def file = new JarFile(jarFile)
        def enumeration = file.entries()
        def jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))

        while (enumeration.hasMoreElements()) {
            def jarEntry = (JarEntry) enumeration.nextElement()
            def entryName = jarEntry.getName()
            def zipEntry = new ZipEntry(entryName)
            def inputStream = file.getInputStream(jarEntry)
            jarOutputStream.putNextEntry(zipEntry)
            if (entryName.startsWith(RegisterTransform.CLASS_GENERATE_NAME)) {
                def bytes = referHackWhenInit(inputStream)
                jarOutputStream.write(bytes)
            } else {
                jarOutputStream.write(IOUtils.toByteArray(inputStream))
            }
            inputStream.close()
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        file.close()

        if (jarFile.exists()) {
            jarFile.delete()
        }
        optJar.renameTo(jarFile)

        return jarFile
    }

    private byte[] referHackWhenInit(InputStream inputStream) {
        def classReader = new ClassReader(inputStream)
        def classWriter = new ClassWriter(classReader, 0)
        def registerClassVisitor = new RegisterClassVisitor(ASM5, classWriter)
        classReader.accept(registerClassVisitor, ClassReader.EXPAND_FRAMES)

        return classWriter.toByteArray()
    }

    private class RegisterClassVisitor extends ClassVisitor {

        RegisterClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc,
                                  String signature, String[] exceptions) {
            def methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
            if (name.equals(RegisterTransform.METHOD_REGISTER_NAME)) {
                methodVisitor = new RegisterAdviceAdapter(ASM5, methodVisitor, access, name, desc)
            }
            return methodVisitor
        }
    }

    private class RegisterAdviceAdapter extends AdviceAdapter {

        private def firstReturn = true

        RegisterAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc)
        }

        @Override
        void onMethodExit(int opcode) {
            if (firstReturn) {
                firstReturn = false
                return
            }

            mv.visitVarInsn(ALOAD, 0)
            mv.visitInsn(ICONST_1)
            mv.visitFieldInsn(PUTFIELD,
                    RegisterTransform.CLASS_GENERATE_NAME,
                    RegisterTransform.FIELD_INDEX_NAME, "Z")

            names.each { name ->
                mv.visitVarInsn(ALOAD, 0)
                mv.visitTypeInsn(NEW, name)
                mv.visitInsn(DUP)
                mv.visitMethodInsn(INVOKESPECIAL, name,
                        "<init>", "()V", false)
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        RegisterTransform.CLASS_GENERATE_NAME,
                        RegisterTransform.METHOD_INDEX_NAME,
                        "(L$RegisterTransform.CLASS_INDEX_NAME;)V", false)
            }
        }
    }
}