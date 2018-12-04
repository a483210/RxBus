package com.xiuyukeji.rxbus.register

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.apache.commons.io.FileUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * 自动查找注册Index
 *
 * @author Created by jz on 2018/5/13 15:15
 */
class RegisterTransform extends Transform {

    static final String CLASS_PACKAGE_NAME = "com/xiuyukeji/rxbus/"
    static final String CLASS_INTERFACE_NAME = "com/xiuyukeji/rxbus/SubscriberIndex"
    static final String CLASS_GENERATE_NAME = "com/xiuyukeji/rxbus/SubscriberLoader"
    static final String METHOD_REGISTER_NAME = "autoRegisterIndex"
    static final String CLASS_INDEX_NAME = "com/xiuyukeji/rxbus/SubscriberIndex"
    static final String METHOD_INDEX_NAME = "addIndex"
    static final String FIELD_INDEX_NAME = "isRegisteredIndex"

    private List<String> registerList;
    private File generateFile;

    RegisterTransform() {
        registerList = new ArrayList<>()
    }

    @Override
    void transform(Context context,
                   Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental)
            throws IOException, TransformException, InterruptedException {

        def startTime = System.currentTimeMillis()

        boolean leftSlash = File.separator == '/'

        inputs.each { TransformInput input ->
            //搜索所有jar包
            input.jarInputs.each { jarInput ->
                def destName = jarInput.name
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }

                def src = jarInput.file
                def dest = outputProvider.getContentLocation(destName + "_" + hexName,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
//
                if (shouldProcessPreDexJar(src.absolutePath)) {
                    scanJar(src, dest)
                }

                FileUtils.copyFile(src, dest)
            }
            //搜索所有文件
            input.directoryInputs.each { directoryInput ->
                String root = directoryInput.file.absolutePath
                if (!root.endsWith(File.separator)) {
                    root += File.separator
                }

                directoryInput.file.eachFileRecurse { File file ->
                    def name = file.absolutePath.replace(root, '')
                    if (!leftSlash) {
                        name = name.replaceAll("\\\\", "/")
                    }

                    if (file.isFile() && shouldProcessClass(name)) {
                        scanClass(new FileInputStream(file))
                    }
                }

                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)

                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }

        //插入代码
        if (generateFile) {
            RegisterCodeGenerator.insertInitCodeTo(registerList, generateFile)
        }

        def costTime = System.currentTimeMillis() - startTime;

        println "rxbus-register time $costTime ms"
    }

    private boolean shouldProcessPreDexJar(String name) {
        return !name.contains("com.android.support") && !name.contains("/android/m2repository")
    }

    private void scanJar(File jarFile, File destFile) {
        if (!jarFile) {
            return
        }

        def file = new JarFile(jarFile)
        def enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            def jarEntry = (JarEntry) enumeration.nextElement()
            def name = jarEntry.getName()
            if (name.startsWith("android/support")) {
                break
            }

            if (shouldProcessClass(name)) {
                if (shouldGenerateClass(name)) {
                    generateFile = destFile
                } else {
                    scanClass(file.getInputStream(jarEntry))
                }
            }
        }
        file.close()
    }

    private boolean shouldProcessClass(String name) {
        if (!shouldClass(name)) {
            return false
        }
        if (!name.startsWith(CLASS_PACKAGE_NAME)) {
            return false
        }
        return true
    }

    private boolean shouldGenerateClass(String name) {
        if (!shouldClass(name)) {
            return false
        }
        if (!name.startsWith(CLASS_GENERATE_NAME)) {
            return false
        }
        return true
    }

    private boolean shouldClass(String name) {
        if (RegisterUtils.isEmpty(name)) {
            return false
        }
        if (!name.endsWith(".class")) {
            return false
        }
        return true;
    }

    private void scanClass(InputStream inputStream) {
        def classReader = new ClassReader(inputStream)
        def classWriter = new ClassWriter(classReader, 0)
        def scanClassVisitor = new ScanClassVisitor(Opcodes.ASM5, classWriter)
        classReader.accept(scanClassVisitor, ClassReader.EXPAND_FRAMES)

        inputStream.close()
    }

    private class ScanClassVisitor extends ClassVisitor {

        ScanClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        boolean is(int access, int flag) {
            return (access & flag) == flag
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)

            if (is(access, Opcodes.ACC_ABSTRACT)) {//抽象类
                return
            }
            if (is(access, Opcodes.ACC_INTERFACE)) {//接口类
                return
            }
            if (!is(access, Opcodes.ACC_PUBLIC)) {//非公共类
                return
            }

            interfaces.each { itName ->
                if (itName.equals(CLASS_INTERFACE_NAME)) {
                    registerList.add(name)
                }
            }
        }
    }

    @Override
    String getName() {
        return "rxbus-register"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}
