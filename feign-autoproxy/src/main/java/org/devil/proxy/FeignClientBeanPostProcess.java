package org.devil.proxy;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class FeignClientBeanPostProcess extends InstantiationAwareBeanPostProcessorAdapter{

    private final Logger logger = LoggerFactory.getLogger(FeignClientBeanPostProcess.class);

    private final String[] clients;

    public FeignClientBeanPostProcess(String[] clients) {
        this.clients = clients;
    }

    @Override
    public Object postProcessBeforeInstantiation(@NonNull Class<?> beanClass,@NonNull String beanName) throws BeansException {

        //如果没有feignClient类需要处理
        if (clients == null || clients.length == 0) {
            return null;
        }

        for (String client : clients) {
            try {
                Class<?> claz = Class.forName(client);

                /**
                 * 不需要执行
                 */
                if (!claz.isAssignableFrom(beanClass) ){
                    continue;
                }

                /**
                 * client must be annotationed  @FeignClient
                 */
                Assert.isTrue(claz.isAnnotationPresent(FeignClient.class), client + " is not feign client");

                ClassPool classPool = ClassPool.getDefault();
                String newClassName = beanClass.getName()+"#FeignAutoProxy";


                CtClass ctClass = classPool.getOrNull(newClassName);
                if (ctClass == null){
                    ctClass = classPool.getAndRename(beanClass.getName(),newClassName);
                }

                if (ctClass.isFrozen()){
                    ctClass.defrost();
                }

                addClassAnnotation(claz, ctClass);
                addMethodAnnotation(claz,ctClass);

                ctClass.setSuperclass(classPool.get(beanClass.getName()));

                Class<?> targetClass = ctClass.toClass();
                Constructor<?> constructor = targetClass.getDeclaredConstructor();
                return constructor.newInstance();
            } catch (ClassNotFoundException | NotFoundException | CannotCompileException | NoSuchMethodException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("client {} can not find", client);
                }
                throw new CannotLoadBeanClassException("proxy bean error",beanName,beanClass.getName(),new LinkageError(e.getMessage(),e));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw  new BeanInitializationException("proxy bean instant error",e);
            }
        }
        return null;
    }

    private void addClassAnnotation(Class feignInterface, CtClass ctClass) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        /**
         * feign client interface annotations
         */
        Annotation[] annotations = feignInterface.getAnnotations();

        /**
         * proxy bean annotations
         */
        AnnotationsAttribute attribute = (AnnotationsAttribute)classFile.getAttribute(AnnotationsAttribute.visibleTag);

        if (attribute == null){
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
                    if (logger.isDebugEnabled()){
                        logger.debug("client {} add class annotation {}",ctClass.getSimpleName(),proxyAnnotation.toString());
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
                if (attribute.getAnnotation(annotation.annotationType().getName()) != null){
                    continue;
                }
                javassist.bytecode.annotation.Annotation proxyAnnotation = AnnotationUtil.createAnnotation(annotation, classFile.getConstPool());
                if (logger.isDebugEnabled()){
                    logger.debug("client {} add class annotation {}",ctClass.getSimpleName(),proxyAnnotation.toString());
                }
                attribute.addAnnotation(proxyAnnotation);
            }
        }
        /**
         * auto add RestController annotation
         */
        if (attribute.getAnnotation(RestController.class.getName())== null){
            javassist.bytecode.annotation.Annotation proxyAnnotation = new javassist.bytecode.annotation.Annotation(RestController.class.getName(), classFile.getConstPool());
            attribute.addAnnotation(proxyAnnotation);
            if (logger.isDebugEnabled()){
                logger.debug("client {} add class annotation {}",ctClass.getSimpleName(),proxyAnnotation.toString());
            }
        }
        classFile.addAttribute(attribute);
    }

    private void addMethodAnnotation(Class superClass, CtClass ctClass) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        Method[] methods = superClass.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            CtClass[] params = new CtClass[method.getParameterTypes().length];
            Class<?>[] classes = method.getParameterTypes();

            for (int i = 0; i < classes.length; i++) {
                params[i] = ClassPool.getDefault().get(classes[i].getName());
            }

            CtMethod ctMethod = ctClass.getDeclaredMethod(name,params);

            MethodInfo methodInfo = ctMethod.getMethodInfo();
            AnnotationsAttribute attribute = (AnnotationsAttribute)methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
            if (attribute == null){
                attribute = new AnnotationsAttribute(methodInfo.getConstPool(),AnnotationsAttribute.visibleTag);
            }

            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (attribute.getAnnotation(annotation.annotationType().getName()) != null){
                    continue;
                }
                javassist.bytecode.annotation.Annotation proxyAnnotation = AnnotationUtil.createAnnotation(annotation,methodInfo.getConstPool());
                if (logger.isDebugEnabled()){
                    logger.debug("client {} add method annotation {}", ctClass.getSimpleName(), proxyAnnotation.toString());
                }
                attribute.addAnnotation(proxyAnnotation);
            }
            methodInfo.addAttribute(attribute);

            Annotation[][] paramsAnnotation = method.getParameterAnnotations();

            ParameterAnnotationsAttribute parameterAnnotationsAttribute = (ParameterAnnotationsAttribute)methodInfo.getAttribute(ParameterAnnotationsAttribute.visibleTag);
            /**
             * if implement has no parameter annotation
             */
            if (parameterAnnotationsAttribute == null){
                parameterAnnotationsAttribute = new ParameterAnnotationsAttribute(methodInfo.getConstPool(),ParameterAnnotationsAttribute.visibleTag);
            }
            javassist.bytecode.annotation.Annotation[][]  annotations = parameterAnnotationsAttribute.getAnnotations();
            List<javassist.bytecode.annotation.Annotation[]> targetAnnotations = new ArrayList<>();
            for (int i = 0; i < paramsAnnotation.length; i++) {
                targetAnnotations.add(mergeAnnotations(paramsAnnotation[i],annotations.length>i?annotations[i]:new javassist.bytecode.annotation.Annotation[0],methodInfo.getConstPool()));
            }
            parameterAnnotationsAttribute.setAnnotations(targetAnnotations.toArray(new javassist.bytecode.annotation.Annotation[0][]));
            if (logger.isDebugEnabled()){
                logger.debug("client {} method {} add parameter annotation {}", ctClass.getSimpleName(),method.getName(), parameterAnnotationsAttribute.toString());
            }
            methodInfo.addAttribute(parameterAnnotationsAttribute);
        }
    }

    /**
     * 合并父类注解和子类注解
     */
    private javassist.bytecode.annotation.Annotation[] mergeAnnotations(Annotation[] javaAnnotations,javassist.bytecode.annotation.Annotation[] mergeAnnotation,ConstPool constPool) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        List<javassist.bytecode.annotation.Annotation> annotations = new ArrayList<>();
        for (Annotation javaAnnotation : javaAnnotations) {
            for (javassist.bytecode.annotation.Annotation assistAnnotation : mergeAnnotation) {
                if (assistAnnotation.getTypeName().equals(javaAnnotation.annotationType().getName())){
                    continue;
                }
            }
            javassist.bytecode.annotation.Annotation assistAnnotation = AnnotationUtil.createAnnotation(javaAnnotation,constPool);
            annotations.add(assistAnnotation);
        }
        annotations.addAll(Arrays.asList(mergeAnnotation));
        return annotations.toArray(new javassist.bytecode.annotation.Annotation[0]);
    }
}

