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
package org.broadleafcommerce.test.common.context.override.crossmodule;

import org.broadleafcommerce.common.extensibility.FrameworkXmlBeanDefinitionReader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Nick Crum ncrum
 */
@RunWith(SpringRunner.class)
public class CrossModuleConfigurationOnlyOverrideTest {

    @Configuration
    @ImportResource(value = {
            "classpath:context/crossmodule/early-applicationContext.xml",
            "classpath:context/crossmodule/late-applicationContext.xml"
    }, reader = FrameworkXmlBeanDefinitionReader.class)
    static class CrossModuleConfiguration {}

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Test
    public void testCrossModuleConfigurationBeanOverride() {
        Assert.assertEquals(BCryptPasswordEncoder.class, passwordEncoder.getClass());
    }
}
