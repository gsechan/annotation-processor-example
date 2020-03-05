package com.gabesechansoftware.liblistenerannotation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "com.gabesechansoftware.liblistenerannotation.Listener"
})
public class ListenerAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment roundEnv) {
        // Itearate over all @Listener annotated elements
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Listener.class)) {
            //Validate the element
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE) {
                error(annotatedElement, "Only classes and interfaces can be @Listener",
                        Listener.class.getSimpleName());
                return true;
            }
            try {
                writeListenerHolderInterface((TypeElement)annotatedElement);
            }
            catch(IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        return true;
    }

    private void writeListenerHolderInterface(TypeElement annotatedElement) throws IOException {
        ClassName listenerClass =  ClassName.get(annotatedElement);

        FieldSpec fieldSpec = FieldSpec.builder(ParameterizedTypeName.get( ClassName.get( List.class ),
                TypeName.get( annotatedElement.asType() ) ),
                "listeners",
                Modifier.PRIVATE, Modifier.FINAL).initializer("new $T<>()", LinkedList.class)
                .build();

        MethodSpec addListenerSpec = MethodSpec.methodBuilder("add"+listenerClass.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(listenerClass, "listener")
                .beginControlFlow("synchronized(listeners)")
                .addStatement("listeners.add(listener)")
                .endControlFlow()
                .build();

        MethodSpec removeListenerSpec = MethodSpec.methodBuilder("remove"+listenerClass.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(listenerClass, "listener")
                .beginControlFlow("synchronized(listeners)")
                .addStatement("listeners.remove(listener)")
                .endControlFlow()
                .build();

        List<MethodSpec> callMethods = getMethodsFromListenerClass(annotatedElement, listenerClass);

        TypeSpec.Builder listenerHolderBuilder = TypeSpec.classBuilder("Has"+listenerClass.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(addListenerSpec)
                .addMethod(removeListenerSpec)
                .addField(fieldSpec);

        for(MethodSpec spec : callMethods) {
            listenerHolderBuilder.addMethod(spec);
        }

        TypeSpec listenerHolder = listenerHolderBuilder.build();

        JavaFile javaFile = JavaFile.builder(listenerClass.packageName(), listenerHolder).skipJavaLangImports(false)
                .build();

        javaFile.writeTo(filer);
    }

    private List<MethodSpec> getMethodsFromListenerClass(TypeElement annotatedElement, ClassName listenerClass) {
        List<MethodSpec> retVal = new LinkedList<>();
        List<ExecutableElement> elements = ElementFilter.methodsIn(annotatedElement.getEnclosedElements());
        for(ExecutableElement element : elements) {
            List<? extends VariableElement> parameters = element.getParameters();
            MethodSpec.Builder spec = MethodSpec.methodBuilder("call"+listenerClass.simpleName()+element.getSimpleName());

            StringBuilder params = new StringBuilder();
            if(parameters.size() > 0) {
                for(VariableElement parameter : parameters) {
                    spec.addParameter(ClassName.get(parameter.asType()), parameter.getSimpleName().toString());
                    params = params.append(parameter.getSimpleName().toString());
                    params.append(",");
                }
                params.deleteCharAt(params.length()-1);
            }

            spec.addModifiers(Modifier.PROTECTED)
            .beginControlFlow("synchronized(listeners)")
            .beginControlFlow("for($T listener : listeners)", listenerClass)
            .addStatement("listener.$L($L)",element.getSimpleName(), params.toString())
            .endControlFlow()
            .endControlFlow();

            retVal.add(spec.build());
        }
        return retVal;

    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}