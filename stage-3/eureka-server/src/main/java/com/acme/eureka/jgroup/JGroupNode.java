package com.acme.eureka.jgroup;

import com.acme.eureka.EurekaReplicator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.wrappers.CodecWrapper;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;
import org.jgroups.*;
import org.jgroups.conf.ClassConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class JGroupNode implements Receiver, EurekaReplicator,
        InitializingBean, DisposableBean, ResourceLoaderAware, BeanFactoryAware, ApplicationListener<ApplicationStartedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(JGroupNode.class);

    private final String name;
    private final String configPath;
    private final ObjectMapper mapper = new ObjectMapper();
    private JChannel channel;
    private ResourceLoader resourceLoader;
    private BeanFactory beanFactory;

    private EurekaServerContext eurekaServerContext;


    private CodecWrapper codecWrapper;

    private PeerAwareInstanceRegistryImpl registry;


    public static final String config = "classpath:config/jgroup/jgroup-conf.xml";

    public JGroupNode(String name) {
        this(name, config);
    }

    public JGroupNode(String name, String configPath) {
        this.name = name;
        this.configPath = configPath;
    }

    private void initCodecWrapper(EurekaServerContext eurekaServerContext) {
        ServerCodecs serverCodecs = eurekaServerContext.getServerCodecs();
        this.codecWrapper = serverCodecs.getFullJsonCodec();
        logger.info("The CodecWrapper has been initialized");
    }

    private void initPeerAwareInstanceRegistry(EurekaServerContext eurekaServerContext) {
        this.registry = (PeerAwareInstanceRegistryImpl) eurekaServerContext.getRegistry();
        logger.info("The PeerAwareInstanceRegistry has been initialized");
    }

    @Override
    public void receive(Message msg) {
        ActionHeader actionHeader = (ActionHeader) msg.getHeader(ActionHeader.MAGIC_ID);
        byte action = actionHeader.getAction();
        String body = msg.getObject();

        InstanceInfo instanceInfo = null;
        try {
            instanceInfo = this.codecWrapper.decode(body, InstanceInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (action == EurekaReplicator.ADD) {
            logger.info("add instance [app:{}, id:{}]", instanceInfo.getAppName(), instanceInfo.getInstanceId());
            this.registry.register(instanceInfo, true);
        }
        if (action == EurekaReplicator.HEART_BEAT) {
            logger.info("renew instance [app:{}, id:{}]", instanceInfo.getAppName(), instanceInfo.getInstanceId());
            this.registry.renew(instanceInfo.getAppName(), instanceInfo.getInstanceId(), true);
        }
        if (action == EurekaReplicator.DELETE) {
            logger.info("delete instance [app:{}, id:{}]", instanceInfo.getAppName(), instanceInfo.getInstanceId());
            this.registry.cancel(instanceInfo.getAppName(), instanceInfo.getInstanceId(), true);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (this.channel != null)
            this.channel.close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //init eureka
        this.eurekaServerContext = this.beanFactory.getBean(EurekaServerContext.class);

        initPeerAwareInstanceRegistry(this.eurekaServerContext);
        initCodecWrapper(this.eurekaServerContext);

        //init jgroup
        ClassConfigurator.addIfAbsent(ActionHeader.MAGIC_ID, ActionHeader.class);
        Resource resource = this.resourceLoader.getResource(this.configPath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("config path : " + this.configPath + " not found");
        }

        channel = new JChannel(resource.getInputStream());
        channel.setReceiver(this);
        channel.connect(this.name);
        channel.setDiscardOwnMessages(true);

    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        final Applications applications = this.registry.getApplications();
        Set<InstanceInfo> instanceInfos = new HashSet<>();
        List<Application> registeredApplications = applications.getRegisteredApplications();
        for (Application application : registeredApplications) {
            instanceInfos.addAll(application.getInstances());
        }
        StreamUtils.copy(mapper.writeValueAsBytes(instanceInfos), output);
    }

    @Override
    public void setState(InputStream input) throws Exception {
        logger.info("try sync state");
        JavaType instanceSetType = mapper.getTypeFactory().constructCollectionType(HashSet.class, InstanceInfo.class);
        Set<InstanceInfo> instanceInfos = mapper.readValue(input, instanceSetType);
        for (InstanceInfo info : instanceInfos) {
            this.registry.register(info, true);
        }
        logger.info("state sync finished!");
    }

    @Override
    public void replicate(InstanceInfo instance, byte action) throws IOException {
        String text = this.codecWrapper.encode(instance);
        Message message = new ObjectMessage(null, text);
        message.putHeader(ActionHeader.MAGIC_ID, new ActionHeader(action));
        try {
            this.channel.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            this.channel.getState(null, 30000);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
