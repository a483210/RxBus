package com.xiuyukeji.rxbus;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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
        ClassName subscriberInfoType = ClassName.get(path, "SubscriberInfo");
        ClassName subscriberMethodInfoType = ClassName.get(path, "SubscriberMethodInfo");
        String variableName = "subscriberClass";
        String infoName = "info";
        String mapName = "subscriberIndex";

        String readIndexName = "readIndex";
        String putIndexName = "putIndex";

        ClassName threadModeName = ClassName.get(path, "ThreadMode");
        ClassName onCallListenerName = ClassName.get(OnCallListener.class);
        ClassName overrideName = ClassName.get(Override.class);
        ClassName objectName = ClassName.get(Object.class);

        ParameterizedTypeName classType = ParameterizedTypeName.get(ClassName.get(Class.class),
                WildcardTypeName.subtypeOf(Object.class));

        ParameterizedTypeName mapType = ParameterizedTypeName
                .get(ClassName.get(HashMap.class), classType, subscriberInfoType);

        FieldSpec mapField = FieldSpec.builder(mapType, mapName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();

        MethodSpec putIndexMethod = MethodSpec.methodBuilder(putIndexName)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(subscriberInfoType, infoName)
                .addStatement("$N.put($N.subscriberType,$N)", mapName, infoName, infoName)
                .build();

        MethodSpec getIndexMethod = MethodSpec.methodBuilder("getIndex")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(subscriberInfoType)
                .addParameter(classType, variableName)
                .addStatement("$T $N = $N.get($N)", subscriberInfoType, infoName, mapName, variableName)
                .beginControlFlow("if($N == null)", infoName)
                .addStatement("$N = $N($N)", infoName, readIndexName, variableName)
                .beginControlFlow("if($N != null)", infoName)
                .addStatement("$N($N)", putIndexName, infoName)
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $N", infoName)
                .build();

        MethodSpec.Builder readIndexMethodBuilder = MethodSpec.methodBuilder(readIndexName)
                .addJavadoc("通过该方法懒加载数据")
                .addModifiers(Modifier.PRIVATE)
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
                readIndexMethodBuilder.beginControlFlow("if($N == $T.class)", variableName, index.classTypeName);
            } else {
                readIndexMethodBuilder.nextControlFlow("else if($N == $T.class)", variableName, index.classTypeName);
            }

            readIndexMethodBuilder.addCode("return new $T($T.class, new $T[]{\n",
                    subscriberInfoType,
                    index.classTypeName,
                    subscriberMethodInfoType);

            index.processorInfos.sort(new TagComparator());
            int count = index.processorInfos.size();
            readIndexMethodBuilder.addCode("$>");
            for (int j = 0; j < count; j++) {
                ProcessorInfo info = index.processorInfos.get(j);
                readIndexMethodBuilder.addCode("new $T($L, $L, $T.$L, $T.class, new $T() {\n",
                        subscriberMethodInfoType,
                        info.tag,
                        info.sticky,
                        threadModeName,
                        info.mode,
                        info.parameterTypeName,
                        onCallListenerName);
                readIndexMethodBuilder.addCode("$>@$T\n", overrideName);
                readIndexMethodBuilder.addCode("public void onCall($T subscriber, $T value) {\n",
                        objectName, objectName);
                readIndexMethodBuilder.addStatement("$>(($T) subscriber).$L((($T) value))",
                        index.classTypeName, info.methodName, info.parameterTypeName);
                readIndexMethodBuilder.addCode("$<}\n");
                readIndexMethodBuilder.addCode("$<})$L\n", j == count - 1 ? "" : ",");
            }
            readIndexMethodBuilder.addStatement("$<})");
            i++;
        }
        readIndexMethodBuilder.endControlFlow();

        MethodSpec readIndexMethod = readIndexMethodBuilder.addStatement("return null").build();

        TypeSpec typeSpec = TypeSpec.classBuilder("SubscriberInfoIndexImpl")
                .addJavadoc("rxBus 生成代码 请不要修改！")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ClassName.get(path, "SubscriberInfoIndex"))
                .addField(mapField)
                .addMethod(getIndexMethod)
                .addMethod(putIndexMethod)
                .addMethod(readIndexMethod)
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