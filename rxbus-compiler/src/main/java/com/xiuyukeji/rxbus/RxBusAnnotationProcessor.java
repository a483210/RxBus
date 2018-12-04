package com.xiuyukeji.rxbus;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 扫描工程，生成Bus序列
 *
 * @author Created by jz on 2016/12/8 18:18
 */
@AutoService(Processor.class)
public class RxBusAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        String path = "com.xiuyukeji.rxbus";

        String fileName = "SubscriberIndexImpl";

        String suffix = processingEnv.getOptions().get("rxBusSuffix");
        if (suffix != null) {
            fileName = String.format("%s$%s", fileName, suffix);
        }

        ClassName subscriberInfoType = ClassName.get(path, "SubscriberInfo");
        ClassName subscriberMethodInfoType = ClassName.get(path, "SubscriberMethodInfo");
        String variableName = "subscriberClass";

        String readIndexName = "readInfo";

        ClassName threadModeName = ClassName.get(path, "ThreadMode");
        ClassName onCallListenerName = ClassName.get(OnCallListener.class);
        ClassName overrideName = ClassName.get(Override.class);
        ClassName objectName = ClassName.get(Object.class);

        ParameterizedTypeName classType = ParameterizedTypeName.get(ClassName.get(Class.class),
                WildcardTypeName.subtypeOf(Object.class));

        MethodSpec.Builder readInfoMethodBuilder = MethodSpec.methodBuilder(readIndexName)
                .addJavadoc("通过该方法懒加载数据")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(classType, variableName)
                .returns(subscriberInfoType);

        ProcessorInfoIndex infoIndex = null;
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Subscribe.class);
        if (elements == null || elements.isEmpty()) {
            return false;
        }

        Elements elementsUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();
        Filer filerUtils = processingEnv.getFiler();

        HashMap<ClassName, ProcessorInfoIndex> map = new HashMap<>();

        for (Element element : elements) {
            Element parentElement = element.getEnclosingElement();
            if (parentElement.getKind() != ElementKind.CLASS) {
                logError(parentElement, "subscriber parent isn't class.");
                return false;
            }
            if (!parentElement.getModifiers().contains(Modifier.PUBLIC)) {
                logError(parentElement, "subscriber parent class must be public.");
                return false;
            }

            if (!element.getModifiers().contains(Modifier.PUBLIC)) {
                logError(element, "subscriber method must be public.");
                return false;
            }
            if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                logError(element, "subscriber method must not be abstract.");
                return false;
            }
            if (element.getModifiers().contains(Modifier.STATIC)) {
                logError(element, "subscriber method must not be static.");
                return false;
            }

            if (!(element instanceof ExecutableElement)) {
                logError(element, "reflection fail reason unknown.");
                return false;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            List<? extends VariableElement> variableElements = executableElement.getParameters();
            if (variableElements == null || variableElements.size() != 1) {
                logError(element, "subscriber method must have exactly 1 parameter.");
                return false;
            }

            ClassName classTypeName = ClassName.get((TypeElement) parentElement);
            if (infoIndex == null || classTypeName.compareTo(infoIndex.classTypeName) != 0) {
                infoIndex = map.computeIfAbsent(classTypeName, ProcessorInfoIndex::new);
            }
            Subscribe subscribe = element.getAnnotation(Subscribe.class);

            VariableElement parameterElement = variableElements.get(0);
            TypeMirror parameterType = parameterElement.asType();

            if (parameterType.getKind().isPrimitive()) {
                logError(element, "subscriber method parameter class must not be primitive.");
                return false;
            }

            Element parameterClassElement = typeUtils.asElement(parameterType);
            String parameterName = parameterClassElement.toString();

            ClassName parameterTypeClassName;

            if (parameterName.equals("T")) {
                TypeMirror typeMirror = ProcessorUtil.getParamTypeMirror(parameterElement);
                parameterTypeClassName = ClassName.bestGuess(typeMirror.toString());
            } else {
                if (!parameterClassElement.getModifiers().contains(Modifier.PUBLIC)) {
                    logError(parameterClassElement, "subscriber method parameter class must be public.");
                    return false;
                }

                parameterTypeClassName = ClassName.bestGuess(parameterName);
            }

            infoIndex.processorInfos.add(new ProcessorInfo(
                    subscribe.tag(),
                    subscribe.sticky(),
                    subscribe.mode(),
                    parameterTypeClassName,
                    element.getSimpleName().toString()));
        }
        int i = 0;
        for (ProcessorInfoIndex index : map.values()) {
            if (i == 0) {
                readInfoMethodBuilder.beginControlFlow("if($N == $T.class)", variableName, index.classTypeName);
            } else {
                readInfoMethodBuilder.nextControlFlow("else if($N == $T.class)", variableName, index.classTypeName);
            }

            readInfoMethodBuilder.addCode("return new $T($T.class, new $T[]{\n",
                    subscriberInfoType,
                    index.classTypeName,
                    subscriberMethodInfoType);

            int count = index.processorInfos.size();
            readInfoMethodBuilder.addCode("$>");
            for (int j = 0; j < count; j++) {
                ProcessorInfo info = index.processorInfos.get(j);
                readInfoMethodBuilder.addCode("new $T($S, $L, $T.$L, $T.class, new $T() {\n",
                        subscriberMethodInfoType,
                        info.tag,
                        info.sticky,
                        threadModeName,
                        info.mode,
                        info.parameterTypeName,
                        onCallListenerName);
                readInfoMethodBuilder.addCode("$>@$T\n", overrideName);
                readInfoMethodBuilder.addCode("public void onCall($T subscriber, $T value) {\n",
                        objectName, objectName);
                readInfoMethodBuilder.addStatement("$>(($T) subscriber).$L((($T) value))",
                        index.classTypeName, info.methodName, info.parameterTypeName);
                readInfoMethodBuilder.addCode("$<}\n");
                readInfoMethodBuilder.addCode("$<})$L\n", j == count - 1 ? "" : ",");
            }
            readInfoMethodBuilder.addStatement("$<})");
            i++;
        }
        readInfoMethodBuilder.endControlFlow();

        MethodSpec readInfoMethod = readInfoMethodBuilder.addStatement("return null").build();

        TypeSpec typeSpec = TypeSpec.classBuilder(fileName)
                .addJavadoc("rxBus 自动生成代码 请不要修改，")
                .addJavadoc("如果出现类重复错误请设置[rxBusSuffix]！")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get(path, "SubscriberIndex"))
                .addMethod(readInfoMethod)
                .build();

        JavaFile javaFile = JavaFile.builder(path, typeSpec).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            // ignored
        }
        return true;
    }

    private void logError(Element element, String str) {
        log(Diagnostic.Kind.ERROR, element, str);
    }

    private void logNote(String str) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, str);
    }

    private void log(Diagnostic.Kind kind, Element element, String str) {
        processingEnv.getMessager().printMessage(kind, str, element);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedTypes = new LinkedHashSet<>();
        supportedTypes.add(com.xiuyukeji.rxbus.Subscribe.class.getCanonicalName());
        return supportedTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}