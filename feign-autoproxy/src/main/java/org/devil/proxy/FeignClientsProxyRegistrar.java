package org.devil.proxy;

import org.devil.proxy.annotation.EnableAutoProxyFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author yaojun
 * 2020/5/8 14:47
 */
public class FeignClientsProxyRegistrar implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {

    public final static String FEIGN_PROXY_ENABLE = "feign.proxy.enable";

    private final Logger logger = LoggerFactory.getLogger(FeignClientsProxyRegistrar.class);

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,@NonNull BeanDefinitionRegistry registry) {
        registerProxy(importingClassMetadata, registry);
    }

    private void registerProxy(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry){
        Map<String,Object> attribute = importingClassMetadata.getAnnotationAttributes(EnableAutoProxyFeign.class.getName(),true);
        boolean environmentEnable = Optional.ofNullable(environment.getProperty(FEIGN_PROXY_ENABLE,Boolean.class)).orElseGet(() -> true);

        //必须同时满足
        boolean isEnable = (Boolean) attribute.getOrDefault("enable",true) && environmentEnable;
        if (!isEnable){
            return;
        }

        String[] clients = (String[])attribute.get("clients");

        String[] proxyClients;
        if (clients == null || clients.length == 0){
            Set<String> basepackages = getBasePackage(importingClassMetadata);
            if (logger.isDebugEnabled()){
                logger.debug("find base packages {}",basepackages);
            }
            proxyClients = searchClients(basepackages);
        }else {
            proxyClients = clients;
        }

        if (proxyClients.length > 0) {
            for (String proxyClient : proxyClients) {
                registerClient(proxyClient,registry);
            }
        }

    }

    protected void registerClient(String client,BeanDefinitionRegistry registry){
        try {
            Class<?> target = FeignClientBuild.createClientProxy(client);
            if (target != null) {
//                String feignClientName = Class.forName(client).getAnnotation(FeignClient.class).qualifier();
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(target);
                beanDefinitionBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                beanDefinitionBuilder.setLazyInit(true);

                BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
//                if (StringUtils.isEmpty(feignClientName)){
                String feignClientName = new AnnotationBeanNameGenerator().generateBeanName(beanDefinition,registry);
//                }
                registry.registerBeanDefinition(feignClientName,beanDefinition);
            }
        }catch (BeansException e){
            if (logger.isErrorEnabled()){
                logger.error("can not getBean,client:{}",client);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()){
                logger.error("proxy client {} error",client,e);
            }
        }

    }

    protected Set<String> getBasePackage(AnnotationMetadata metadata){
        Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableAutoProxyFeign.class.getName(),true);
        Set<String> basePackages = new HashSet<>();

        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (String clazz : (String[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(metadata.getClassName()));
        }

        return basePackages;
    }

    private String[] searchClients(Set<String> basePackages){
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));

        Set<String> clients = new HashSet<>();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                if (beanDefinition instanceof AnnotatedBeanDefinition){
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition)beanDefinition;
                    AnnotationMetadata annotationMetadata = annotatedBeanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");
                    clients.add(beanDefinition.getBeanClassName());
                }
            }
        }
        if (logger.isDebugEnabled()){
            logger.debug("find client to proxy {}",clients);
        }
        return clients.toArray(new String[0]);
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }
}
