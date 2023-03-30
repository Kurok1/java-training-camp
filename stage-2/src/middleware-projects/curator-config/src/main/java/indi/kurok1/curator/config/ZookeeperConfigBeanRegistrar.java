package indi.kurok1.curator.config;

import indi.kurok1.curator.config.annotation.EnableZookeeperConfig;
import indi.kurok1.curator.config.watch.ZookeeperConfigWatcher;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 3.8.0
 */
public class ZookeeperConfigBeanRegistrar implements ImportBeanDefinitionRegistrar{

    public static final String CURATOR_CLIENT_CONFIG_BEAN_NAME = "curatorClientConfig";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //Curator配置
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableZookeeperConfig.class.getName());
        registerCuratorClientConfig(annotationAttributes, registry);
        //配置加载注册
        registerZookeeperConfigSourcePostProcessor(registry);

        //配置监听
        registerZookeeperConfigWatcher(registry);

    }

    private void registerZookeeperConfigWatcher(BeanDefinitionRegistry registry) {
        registerBeanDefinition(registry, ZookeeperConfigWatcher.WATCHER_BEAN_NAME, ZookeeperConfigWatcher.class);
    }

    private void registerCuratorClientConfig(final Map<String, Object> annotationAttributes, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(CuratorClientConfig.class, () -> generateCuratorConfig(annotationAttributes));
        // ROLE_INFRASTRUCTURE
        beanDefinitionBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(CURATOR_CLIENT_CONFIG_BEAN_NAME,
                beanDefinitionBuilder.getBeanDefinition());
    }

    private void registerZookeeperConfigSourcePostProcessor(BeanDefinitionRegistry registry) {
        registerBeanDefinition(registry, ZookeeperPropertySourcePostProcessor.BEAN_NAME, ZookeeperPropertySourcePostProcessor.class);
    }

    private void registerBeanDefinition(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass) {
        if (registry.containsBeanDefinition(beanName))
            return;
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                .rootBeanDefinition(beanClass);
        // ROLE_INFRASTRUCTURE
        beanDefinitionBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(beanName,
                beanDefinitionBuilder.getBeanDefinition());
    }


    private CuratorClientConfig generateCuratorConfig(Map<String, Object> annotationAttributes) {
        CuratorClientConfig config = new CuratorClientConfig();
        config.setGlobalPath((String)annotationAttributes.get("globalPath"));
        config.setConnectString((String)annotationAttributes.get("connectString"));
        config.setNamespace((String)annotationAttributes.get("namespace"));
        config.setConnectionTimeoutMs((Integer) annotationAttributes.get("connectionTimeoutMs"));
        config.setSessionTimeoutMs((Integer) annotationAttributes.get("sessionTimeoutMs"));

        return config;
    }
}
