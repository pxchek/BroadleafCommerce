/*-
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2025 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.common.security.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.security.web.access.channel.ChannelDecisionManagerImpl;
import org.springframework.security.web.access.channel.ChannelProcessor;
import org.springframework.security.web.access.channel.InsecureChannelProcessor;
import org.springframework.security.web.access.channel.SecureChannelProcessor;

import java.lang.reflect.Field;
import java.util.List;

import javax.servlet.ServletRequest;

/**
 * <p>This class is designed to work in both a load-balanced and non load-balanced environment by replacing the existing
 * default Spring channel processors which do not work in a load balanced environment. Configuration should be done
 * as follows in your applicationContext-security:</p>
 * 
 * <b>Deploying to a load balanced environment with SSL termination at the load balancer as well as an environment
 * with SSL termination at Tomcat/Apache:</b>
 * 
 * <pre>
 * {@code
 *   <bean class="org.broadleafcommerce.common.security.channel.ProtoChannelBeanPostProcessor">
 *       <property name="channelProcessorOverrides">
 *           <list>
 *              <bean class="org.broadleafcommerce.common.security.channel.ProtoInsecureChannelProcessor" />
 *              <bean class="org.broadleafcommerce.common.security.channel.ProtoSecureChannelProcessor" />
 *          </list>
 *      </property>
 *  </bean>
 *  }
 * </pre>
 * 
 * <p>That said, this solution only overrides the Spring Security directives but does not make any attempts to override
 * any invocations to {@link ServletRequest#isSecure}. If your application server supports it, we recommend instead using
 * that approach which will encapsulate any functionality encapsulated within the Proto processors. For more information
 * on configuring your specific servlet container, see
 * <a href="https://github.com/BroadleafCommerce/BroadleafCommerce/issues/424">this issue report</a>
 * </p>
 * 
 * @author Jeff Fischer
 * @author Phillip Verheyden (phillipuniverse)
 * @see {@link ProtoSecureChannelProcessor}
 * @see {@link ProtoInsecureChannelProcessor}
 * @see {@link SecureChannelProcessor}
 * @see {@link InsecureChannelProcessor}
 */
public class ProtoChannelBeanPostProcessor implements BeanPostProcessor, Ordered {

    Log LOG = LogFactory.getLog(ProtoChannelBeanPostProcessor.class);
    
    protected List<ChannelProcessor> channelProcessorOverrides;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ChannelDecisionManagerImpl) {
            try {
                ChannelDecisionManagerImpl manager = (ChannelDecisionManagerImpl) bean;
                Field channelProcessors = manager.getClass().getDeclaredField("channelProcessors");
                channelProcessors.setAccessible(true);
                List<ChannelProcessor> list = (List<ChannelProcessor>) channelProcessors.get(manager);
                list.clear();
                manager.setChannelProcessors(channelProcessorOverrides);
                LOG.info("Replacing the standard Spring Security channel processors with custom processors that look for a " +
                		"'X-Forwarded-Proto' request header. This allows Spring Security to sit behind a load balancer with SSL termination.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return 9999;
    }

    /**
     * @return the channelProcessors
     */
    public List<ChannelProcessor> getChannelProcessorOverrides() {
        return channelProcessorOverrides;
    }
    
    /**
     * @param channelProcessors the channelProcessors to set
     */
    public void setChannelProcessorOverrides(List<ChannelProcessor> channelProcessorOverrides) {
        this.channelProcessorOverrides = channelProcessorOverrides;
    }
    
}
