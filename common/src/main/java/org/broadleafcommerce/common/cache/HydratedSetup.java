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
package org.broadleafcommerce.common.cache;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.cache.engine.CacheFactoryException;
import org.broadleafcommerce.common.cache.engine.HydratedAnnotationManager;
import org.broadleafcommerce.common.cache.engine.HydratedCacheManager;
import org.broadleafcommerce.common.cache.engine.HydrationDescriptor;
import org.hibernate.annotations.Cache;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * 
 * @author jfischer
 *
 */
public class HydratedSetup {
    
    private static final Log LOG = LogFactory.getLog(HydratedSetup.class);
    private static Map<String, String> inheritanceHierarchyRoots = Collections.synchronizedMap(new HashMap<String, String>());
    private static HydratedCacheManager manager;

    public static void setHydratedCacheManager(HydratedCacheManager hydratedCacheManager) {
        manager = hydratedCacheManager;
    }

    private static String getInheritanceHierarchyRoot(Class<?> myEntityClass) {
        String myEntityName = myEntityClass.getName();
        if (inheritanceHierarchyRoots.containsKey(myEntityName)) {
            return inheritanceHierarchyRoots.get(myEntityName);
        }
        Class<?> currentClass = myEntityClass;
        boolean eof = false;
        while (!eof) {
            Class<?> superclass = currentClass.getSuperclass();
            if (superclass.equals(Object.class) || !superclass.isAnnotationPresent(Entity.class)) {
                eof = true;
            } else {
                currentClass = superclass;
            }
        }

        if (!currentClass.isAnnotationPresent(Cache.class)) {
            currentClass = myEntityClass;
        }

        inheritanceHierarchyRoots.put(myEntityName, currentClass.getName());
        return inheritanceHierarchyRoots.get(myEntityName);
    }

    public static void populateFromCache(Object entity) {
        populateFromCache(entity, null);
    }

    public static void populateFromCache(Object entity, String propertyName) {
        HydrationDescriptor descriptor = ((HydratedAnnotationManager) manager).getHydrationDescriptor(entity);
        if (!MapUtils.isEmpty(descriptor.getHydratedMutators())) {
            Method[] idMutators = descriptor.getIdMutators();
            String cacheRegion = descriptor.getCacheRegion();
            for (String field : descriptor.getHydratedMutators().keySet()) {
                if (StringUtils.isEmpty(propertyName) || field.equals(propertyName)) {
                    try {
                        Serializable entityId = (Serializable) idMutators[0].invoke(entity);
                        Object hydratedItem = manager.getHydratedCacheElementItem(cacheRegion, getInheritanceHierarchyRoot(entity.getClass()), entityId, field);
                        if (hydratedItem == null) {
                            Method factoryMethod = entity.getClass().getMethod(descriptor.getHydratedMutators().get(field).getFactoryMethod(), new Class[]{});
                            Object fieldVal = factoryMethod.invoke(entity);
                            manager.addHydratedCacheElementItem(cacheRegion, getInheritanceHierarchyRoot(entity.getClass()), entityId, field, fieldVal);
                            hydratedItem = fieldVal;
                        }
                        descriptor.getHydratedMutators().get(field).getMutators()[1].invoke(entity, hydratedItem);
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() != null && e.getTargetException() instanceof CacheFactoryException) {
                            LOG.warn("Unable to setup the hydrated cache for an entity. " + e.getTargetException().getMessage());
                        } else {
                            throw new RuntimeException("There was a problem while replacing a hydrated cache item - field("+field+") : entity("+entity.getClass().getName()+')', e);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("There was a problem while replacing a hydrated cache item - field("+field+") : entity("+entity.getClass().getName()+')', e);
                    }
                }
            }
        }
    }

    public static void addCacheItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName, Object elementValue) {
        manager.addHydratedCacheElementItem(cacheRegion, cacheName, elementKey, elementItemName, elementValue);
    }

    public static Object getCacheItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName) {
        return manager.getHydratedCacheElementItem(cacheRegion, cacheName, elementKey, elementItemName);
    }

    public static EntityManager retrieveBoundEntityManager() {
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        for (Map.Entry<Object, Object> entry : resources.entrySet()) {
            if (entry.getKey() instanceof EntityManagerFactory) {
                EntityManagerFactory emf = (EntityManagerFactory) entry.getKey();
                //return the entityManager from the first found
                return  ((EntityManagerHolder) entry.getValue()).getEntityManager();
            }
        }
        throw new RuntimeException("Unable to restore skus from hydrated cache. Please make sure that the OpenEntityManagerInViewFilter is configured in web.xml for the blPU persistence unit.");
    }
}
