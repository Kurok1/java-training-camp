package indi.kurok1.curator.config;

import indi.kurok1.curator.config.annotation.ZookeeperConfigSource;
import indi.kurok1.curator.config.annotation.ZookeeperConfigSources;
import indi.kurok1.curator.config.source.MapConfigUtils;
import indi.kurok1.curator.config.source.ZookeeperConfigUtils;
import indi.kurok1.curator.config.source.ZookeeperMapPropertySource;
import indi.kurok1.curator.config.source.ZookeeperTextPropertySource;
import indi.kurok1.curator.config.stream.StreamConfigAccumulator;
import indi.kurok1.curator.config.watch.StreamRefreshTask;
import indi.kurok1.curator.config.watch.ZookeeperConfigWatcher;
import indi.kurok1.curator.config.watch.ZookeeperStreamConfigRefresher;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.Pathable;
import org.apache.curator.retry.RetryForever;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ZookeeperPropertySourcePostProcessor implements
        BeanDefinitionRegistryPostProcessor, BeanFactoryPostProcessor, EnvironmentAware, ApplicationEventPublisherAware {

    private ConfigurableEnvironment environment;
    private ApplicationEventPublisher publisher;

    public static final String BEAN_NAME = "zookeeperPropertySourcePostProcessor";

    private final String ZOOKEEPER_CONFIG_SOURCE_NAME = ZookeeperConfigSource.class.getName();
    private final String ZOOKEEPER_CONFIG_SOURCES_NAME = ZookeeperConfigSources.class.getName();

    private List<ZookeeperConfigSourceMetaData> metaDataList = new ArrayList<>();
    private List<String> interestedWatchPath = new CopyOnWriteArrayList<>();

    private final ZookeeperStreamConfigRefresher refresher = new ZookeeperStreamConfigRefresher();
    private final RestTemplate restTemplate = new RestTemplate();

    private boolean containsZookeeperConfigSource(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.hasAnnotation(ZOOKEEPER_CONFIG_SOURCE_NAME) || metadata.hasAnnotation(ZOOKEEPER_CONFIG_SOURCES_NAME);
    }

    private List<ZookeeperConfigSourceMetaData> retrieveZookeeperConfigSources(AnnotatedBeanDefinition beanDefinition) {
        List<ZookeeperConfigSourceMetaData> list = new ArrayList<>();
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        Set<String> annotations = metadata.getAnnotationTypes();

        for (String annotation : annotations) {
            if (ZOOKEEPER_CONFIG_SOURCE_NAME.equals(annotation)) {//@ZookeeperConfigSource
                Map<String, Object> attributes = metadata.getAnnotationAttributes(annotation);
                list.add(ZookeeperConfigSourceMetaData.byMap(attributes));
            } else if (ZOOKEEPER_CONFIG_SOURCES_NAME.equals(annotation)) {//@ZookeeperConfigSources
                Map<String, Object>[] attributeValues = (Map<String, Object>[]) metadata.getAnnotationAttributes(annotation).get("value");
                for (Map<String, Object> attributeValue : attributeValues)
                    list.add(ZookeeperConfigSourceMetaData.byMap(attributeValue));

            }
        }

        return list;
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        //获取所有beanNames
        String[] beanNames = beanDefinitionRegistry.getBeanDefinitionNames();

        //过滤bean definition,初始化配置元数据
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
                if (containsZookeeperConfigSource(annotatedBeanDefinition)) {
                    metaDataList.addAll(retrieveZookeeperConfigSources(annotatedBeanDefinition));
                }
            }
        }
    }

    protected final ZookeeperConfigWatcher getWatcher(BeanFactory beanFactory) {
        return beanFactory.getBeanProvider(ZookeeperConfigWatcher.class).getIfUnique();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        //初始化配置
        CuratorClientConfig config = configurableListableBeanFactory.getBean(ZookeeperConfigBeanRegistrar.CURATOR_CLIENT_CONFIG_BEAN_NAME, CuratorClientConfig.class);
        //1.设置客户端
        CuratorFramework client = initializeCuratorAndStart(config);
        //批量读取数据
        //去重
        metaDataList = metaDataList.stream().distinct().collect(Collectors.toList());

        //stream相关配置
        List<StreamRefreshTask> refreshTasks = new ArrayList<>();

        for (ZookeeperConfigSourceMetaData metaData : metaDataList) {

            if (metaData.getConfigType() == ConfigType.EVENT_STREAM) {
                //stream数据的处理,stream类型，在zookeeper中配置的是配置服务url，获取即可
                final String path = getPath(config, metaData);
                Class<?> accumulatorClass = metaData.getStreamConfigAccumulatorClass();
                StreamConfigAccumulator accumulator = null;
                try {
                    accumulator = newStreamConfigAccumulator(path, (Class<StreamConfigAccumulator>) accumulatorClass);
                    String configUrl = new String(client.getData().forPath(path), StandardCharsets.UTF_8);
                    StreamRefreshTask task = new StreamRefreshTask(this.restTemplate, configUrl,
                            accumulator, this.publisher, this.environment, path);
                    refreshTasks.add(task);
                } catch (Exception e) {
                    throw new ApplicationContextException(e.getMessage());
                }
                continue;
            }

            try {
                final String path = getPath(config, metaData);
                ZookeeperConfigUtils.retrieveConfigAndSave(environment, client, path, metaData.getConfigType());
                if (metaData.isWatchChange()) {
                    //监控配置变更
                    this.interestedWatchPath.add(path);
                }

            } catch (Exception e) {
                throw new ApplicationContextException(e.getMessage());
            }
        }

        if (refreshTasks.size() > 0) {
            this.refresher.initializeExecutors(refreshTasks.size());
            refreshTasks.forEach(this.refresher::addRefreshTask);
        }

        if (this.interestedWatchPath.size() > 0) {
            ZookeeperConfigWatcher configWatcher = getWatcher(configurableListableBeanFactory);
            Pathable<Void> voidPathable = client.watchers().add().usingWatcher(configWatcher);
            configWatcher.setClient(client);
            for (String watchPath : this.interestedWatchPath) {
                try {
                    voidPathable.forPath(watchPath);
                } catch (Exception e) {
                    throw new ApplicationContextException(e.getMessage());
                }
            }

        }
    }

    private StreamConfigAccumulator newStreamConfigAccumulator(String path, Class<StreamConfigAccumulator> accumulatorClass) throws Exception {
        Constructor<StreamConfigAccumulator> constructor = accumulatorClass.getConstructor(String.class);
        return constructor.newInstance(path);
    }

    private static String startWith(String text, String prefix) {
        if (ObjectUtils.isEmpty(text))
            return "";

        if (!text.startsWith(prefix))
            return prefix + text;

        return text;
    }

    private String resolveConfig(ConfigurableEnvironment environment, CuratorClientConfig clientConfig, ZookeeperConfigSourceMetaData metaData, CuratorFramework client) throws Exception {
        //1.构建path /globalPath + /nodeParentPath + /name
        String path = getPath(clientConfig, metaData);

        ZookeeperConfigUtils.retrieveConfigAndSave(environment, client, path, metaData.getConfigType());
        return path;
    }

    private static String getPath(CuratorClientConfig clientConfig, ZookeeperConfigSourceMetaData metaData) {
        String globalPath = startWith(clientConfig.getGlobalPath(), "/");
        String nodeParentPath = startWith(metaData.getNodePath(), "/");

        String path = globalPath + nodeParentPath;

        path = path.replace("//", "/");
        return path;
    }


    private CuratorFramework initializeCuratorAndStart(CuratorClientConfig config) {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectString(config.getConnectString())
                .retryPolicy(new RetryForever(10000))
                .namespace(config.getNamespace())
                .sessionTimeoutMs(config.getSessionTimeoutMs())
                .connectionTimeoutMs(config.getConnectionTimeoutMs())
                .build();

        framework.start();

        return framework;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
