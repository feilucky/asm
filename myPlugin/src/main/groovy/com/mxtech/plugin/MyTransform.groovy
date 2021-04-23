package com.mxtech.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.mxtech.plugin.asm.ModifyUtil
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class MyTransform extends Transform {
    private static final String TRANSFORM_NAME = "MyTransform"

    @Override
    String getName() {
        return TRANSFORM_NAME// 代表该Transform对应的Task的名称
    }

    /**
     * 它是指定Transform要处理的数据类型。目前主要支持两种数据类型：
     * CLASSES:表示要处理编译后的字节码，可能是jar包也可能是目录。
     * RESOURCES:表示处理标准的java资源。
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS//
    }

    /**
     * 指定Transform作用域，常见的作用域有7种：
     * 1、PROJECT
     *    只处理当前项目
     * 2、SUB_PROJECTS
     *    只处理子项目
     * 3、PROJECT_LOCAL_DEPS
     *    只处理当前项目的本地依赖，例如jar，aar
     * 4、SUB_PROJECTS_LOACAL_DEPS
     *    只处理子项目的本地依赖，例如jar，aar
     * 5、EXTERNAL_LIBRARIES
     *    只处理外部的依赖库
     * 6、PROVIDED_ONLY
     *    只处理本地或远程以provided形式引入的依赖库
     * 7、TESTED_CODE
     *    测试代码
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否增量构建
     * @return
     */
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        if (!incremental) {
            transformInvocation.outputProvider.deleteAll()
        }
        //遍历 jar
        transformInvocation.inputs.each {
            TransformInput input ->
                System.out.println("====开始遍历jar=====")
                //遍历jar
                input.jarInputs.each {
                    JarInput jarInput ->
                        String destName = jarInput.file.name
                        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                        System.out.println("遍历jar的路径: " + destName + ", hexName: " + hexName)

                        if (destName.endsWith(".jar")) {
                            destName = destName.substring(0, destName.length() - 4)
                        }
                        File dest = transformInvocation.outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                        System.out.println("====absolutePath===" + dest.absolutePath)
                        def modifiedJar = modifyJarFile(jarInput.file, transformInvocation.context.getTemporaryDir())
                        if (modifiedJar == null) {
                            modifiedJar = jarInput.file
                        }
                        FileUtils.copyFile(modifiedJar, dest)
                }


                println("========开始遍历目录==========")
                // Transform 的 inputs 有两种类型，一种是目录，一种是 jar 包，要分开遍历
                //遍历目录
                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        File dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                        File dir = directoryInput.file
                        if (dir) {
                            println("目录中的jar路径: " + dir.absolutePath)
                            HashMap<String, File> modifyMap = new HashMap<>()
                            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                                File classFile ->
                                    File modified = modifyClassFile(dir, classFile, transformInvocation.context.getTemporaryDir())
                                    if (modified != null) {
                                        modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                                    }
                            }

                            println("source , dest : " + directoryInput.file.absolutePath + ", " + dest.absolutePath)
                            FileUtils.copyDirectory(directoryInput.file, dest)
                            modifyMap.entrySet().each {
                                Map.Entry<String, File> en ->
                                    File target = new File(dest.absolutePath + en.getKey())
                                    if (target.exists()) {
                                        target.delete()
                                    }
                                    FileUtils.copyFile(en.getValue(), target)
                                    en.getValue().delete()
                            }
                            println("===目标文件==" + dest.absolutePath)
                        }
                }
        }
    }

    private static File modifyJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return modifyJar(jarFile, tempDir, true)
        }
        return null
    }

    private static File modifyJar(File jarFile, File tempDir, boolean nameHex) {

        def file = new JarFile(jarFile)
        def hexName = ""

        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }

        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = file.getInputStream(jarEntry)
            String entryName = jarEntry.getName()
            String className
            ZipEntry zipEntry = new ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)

            byte[] modifiedClassBytes = null

            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)

            if (entryName.endsWith(".class")) {
                className = entryName.replace("/", ".").replace(".class", "")
//                System.out.println("className: " + className)
                if (isShouldModifyClass(className) && className.contains("MainActivity")) {
                    System.out.println("className: " + className)

                    modifiedClassBytes = ModifyUtil.modifyClasses(className, sourceClassBytes)
                }
            }

            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(modifiedClassBytes)
            }
            jarOutputStream.close()
            file.close()
            return outputJar


        }
    }

    /**
     * 修改目录中的字节码
     */

    private static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            if (isShouldModifyClass(className) && className.contains("MainActivity")) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = ModifyUtil.modifyClasses(className, sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace(".", '') + ".class")
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            println("Exception:  " + e.toString())
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
            }
        }
        return modified
    }


    static boolean isShouldModifyClass(String className) {
        if (className.contains('R$') || className.contains('R2$') || className.contains('R') ||
                className.contains('R2') || className.contains('BuildConfig')) {
            return false
        }
        return true
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }
}