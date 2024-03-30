package org.devil.proxy;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import javassist.expr.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yaojun
 * 2020/5/8 16:41
 */
public class FeignClientBuild {

    private final static Logger logger = LoggerFactory.getLogger(FeignClientBuild.class);


    private FeignClientBuild() {
    }

    /**
     * @param client feignClient Class
     * @return 代理class
     * @throws Exception
     */
    protected static Class<?> createClientProxy(@NonNull String client) throws Exception {

        //如果没有feignClient类需要处理

        try {
            Class<?> claz = Class.forName(client);

            /**
             * client must be annotationed  @FeignClient
             */
            Assert.isTrue(claz.isAnnotationPresent(FeignClient.class), client + " is not feign client");

            ClassPool classPool = ClassPool.getDefault();
            String newClassName = claz + "#FeignAutoProxy";


            CtClass ctClass = classPool.getOrNull(newClassName);
            if (ctClass == null) {
                ctClass = classPool.makeClass(newClassName);
            }

            if (ctClass.isFrozen()) {
                ctClass.defrost();
            }
//            ctClass.addInterface(classPool.get(client));
//            ctClass.setModifiers(Modifier.);
            
            CtClass beanCt = classPool.getCtClass(client);
            CtField field = new CtField(beanCt,"delega",ctClass);

            AnnotationsAttribute attribute = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
            attribute.addAnnotation(new javassist.bytecode.annotation.Annotation(Resource.class.getName(),ctClass.getClassFile().getConstPool()));

            field.getFieldInfo().addAttribute(attribute);

            ctClass.addField(field);

            for (CtMethod me : beanCt.getDeclaredMethods()) {
                CtMethod ctMethod = new CtMethod(me.getReturnType(),me.getName(),me.getParameterTypes(),ctClass);
                ctMethod.setBody("{return this.delega."+me.getName()+"($$);}");
                ctClass.addMethod(ctMethod);
            }

            addClassAnnotation(claz, ctClass);
            addMethodAnnotation(claz, ctClass);

//            ctClass.setSuperclass(classPool.get(beanClass.getName()));
            return ctClass.toClass();
        } catch (ClassNotFoundException | NotFoundException | CannotCompileException e) {
            if (logger.isErrorEnabled()) {
                logger.error("client {} can not find", client);
            }
            throw e;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BeanInitializationException("proxy bean instant error", e);
        }
    }

    private static void addClassAnnotation(Class feignInterface, CtClass ctClass) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        /**
         * feign client interface annotations
         */
        Annotation[] annotations = feignInterface.getAnnotations();

        /**
         * proxy bean annotations
         */
        AnnotationsAttribute attribute = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.visibleTag);

        if (attribute == null) {
            attribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
        }

        for (Annotation annotation : annotations) {
            /**
             * auto add RequestMapping annotation if necessary
             */
            if (annotation instanceof FeignClient) {
                FeignClient feignClient = (FeignClient) annotation;
                String path = feignClient.path();
                /**
                 * add requestMapping annotation if necessary
                 */
                if (StringUtils.hasText(path) && attribute.getAnnotation(RequestMapping.class.getName()) == null) {
                    javassist.bytecode.annotation.Annotation proxyAnnotation = new javassist.bytecode.annotation.Annotation(RequestMapping.class.getName(), classFile.getConstPool());
                    ArrayMemberValue memberValue = new ArrayMemberValue(constPool);
                    memberValue.setValue(new MemberValue[]{new StringMemberValue(path, constPool)});
                    proxyAnnotation.addMemberValue("path", memberValue);
                    if (logger.isDebugEnabled()) {
                        logger.debug("client {} add class annotation {}", ctClass.getSimpleName(), proxyAnnotation.toString());
                    }
                    /**
                     * add requestMapping annotation to proxybean
                     */
                    attribute.addAnnotation(proxyAnnotation);
                }
            } else {
                /**
                 * if proxy bean has this annotation,its unnecessary to add feign client interface's annotation
                 */
                if (attribute.getAnnotation(annotation.annotationType().getName()) != null) {
                    continue;
                }
                javassist.bytecode.annotation.Annotation proxyAnnotation = AnnotationUtil.createAnnotation(annotation, classFile.getConstPool());
                if (logger.isDebugEnabled()) {
                    logger.debug("client {} add class annotation {}", ctClass.getSimpleName(), proxyAnnotation.toString());
                }
                attribute.addAnnotation(proxyAnnotation);
            }
        }
        /**
         * auto add RestController annotation
         */
        if (attribute.getAnnotation(RestController.class.getName()) == null) {
            javassist.bytecode.annotation.Annotation proxyAnnotation = new javassist.bytecode.annotation.Annotation(RestController.class.getName(), classFile.getConstPool());
            attribute.addAnnotation(proxyAnnotation);
            if (logger.isDebugEnabled()) {
                logger.debug("client {} add class annotation {}", ctClass.getSimpleName(), proxyAnnotation.toString());
            }
        }
        classFile.addAttribute(attribute);
    }

    private static void addMethodAnnotation(Class superClass, CtClass ctClass) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        Method[] methods = superClass.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            CtClass[] params = new CtClass[method.getParameterTypes().length];
            Class<?>[] classes = method.getParameterTypes();

            for (int i = 0; i < classes.length; i++) {
                params[i] = ClassPool.getDefault().get(classes[i].getName());
            }

            CtMethod ctMethod = ctClass.getDeclaredMethod(name, params);

            MethodInfo methodInfo = ctMethod.getMethodInfo();
            AnnotationsAttribute attribute = (AnnotationsAttribute) methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
            if (attribute == null) {
                attribute = new AnnotationsAttribute(methodInfo.getConstPool(), AnnotationsAttribute.visibleTag);
            }

            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (attribute.getAnnotation(annotation.annotationType().getName()) != null) {
                    continue;
                }
                javassist.bytecode.annotation.Annotation proxyAnnotation = AnnotationUtil.createAnnotation(annotation, methodInfo.getConstPool());
                if (logger.isDebugEnabled()) {
                    logger.debug("client {} add method annotation {}", ctClass.getSimpleName(), proxyAnnotation.toString());
                }
                attribute.addAnnotation(proxyAnnotation);
            }
            methodInfo.addAttribute(attribute);

            Annotation[][] paramsAnnotation = method.getParameterAnnotations();

            ParameterAnnotationsAttribute parameterAnnotationsAttribute = (ParameterAnnotationsAttribute) methodInfo.getAttribute(ParameterAnnotationsAttribute.visibleTag);
            /**
             * if implement has no parameter annotation
             */
            if (parameterAnnotationsAttribute == null) {
                parameterAnnotationsAttribute = new ParameterAnnotationsAttribute(methodInfo.getConstPool(), ParameterAnnotationsAttribute.visibleTag);
            }
            javassist.bytecode.annotation.Annotation[][] annotations = parameterAnnotationsAttribute.getAnnotations();
            List<javassist.bytecode.annotation.Annotation[]> targetAnnotations = new ArrayList<>();
            for (int i = 0; i < paramsAnnotation.length; i++) {
                targetAnnotations.add(mergeAnnotations(paramsAnnotation[i], annotations.length > i ? annotations[i] : new javassist.bytecode.annotation.Annotation[0], methodInfo.getConstPool()));
            }
            parameterAnnotationsAttribute.setAnnotations(targetAnnotations.toArray(new javassist.bytecode.annotation.Annotation[0][]));
            if (logger.isDebugEnabled()) {
                logger.debug("client {} method {} add parameter annotation {}", ctClass.getSimpleName(), method.getName(), parameterAnnotationsAttribute.toString());
            }
            methodInfo.addAttribute(parameterAnnotationsAttribute);
        }
    }

    /**
     * 合并父类注解和子类注解
     */
    private static javassist.bytecode.annotation.Annotation[] mergeAnnotations(Annotation[] javaAnnotations, javassist.bytecode.annotation.Annotation[] mergeAnnotation, ConstPool constPool) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        List<javassist.bytecode.annotation.Annotation> annotations = new ArrayList<>();
        for (Annotation javaAnnotation : javaAnnotations) {
            for (javassist.bytecode.annotation.Annotation assistAnnotation : mergeAnnotation) {
                if (assistAnnotation.getTypeName().equals(javaAnnotation.annotationType().getName())) {
                    continue;
                }
            }
            javassist.bytecode.annotation.Annotation assistAnnotation = AnnotationUtil.createAnnotation(javaAnnotation, constPool);
            annotations.add(assistAnnotation);
        }
        annotations.addAll(Arrays.asList(mergeAnnotation));
        return annotations.toArray(new javassist.bytecode.annotation.Annotation[0]);
    }

}

