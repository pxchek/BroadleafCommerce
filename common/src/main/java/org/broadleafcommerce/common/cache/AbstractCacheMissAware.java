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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.broadleafcommerce.common.sandbox.domain.SandBox;
import org.broadleafcommerce.common.site.domain.Site;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Resource;
import javax.cache.Cache;
import javax.cache.CacheManager;


/**
 * Support for any class that wishes to utilize a query miss cache. This cache is capable of caching a query miss
 * (the query returns no results). This is beneficial since standard level 2 cache does not maintain misses.
 *
 * NOTE, special cache invalidation support must be added to address this cache if a change is made to one or more of
 * the cached missed items.
 *
 * @author Jeff Fischer
 */
public abstract class AbstractCacheMissAware<T> {
    
    @Resource(name="blStatisticsService")
    protected StatisticsService statisticsService;
    
    @Resource(name = "blCacheManager")
    protected CacheManager cacheManager;

    private Object nullObject = null;

    /**
     * Build the key representing this missed cache item. Will include sandbox and/or site information
     * if appropriate.
     *
     * @param params the appropriate params comprising a unique key for this cache item
     * @return the completed key
     */
    protected String buildKey(String... params) {
        BroadleafRequestContext context = BroadleafRequestContext.getBroadleafRequestContext();
        SandBox sandBox = null;
        if (context != null) {
            sandBox = context.getSandBox();
        }
        String key = StringUtils.join(params, '_');
        if (sandBox != null) {
            key = sandBox.getId() + "_" + key;
        }
        Site site = context.getNonPersistentSite();
        if (site != null) {
            key = key + "_" +  site.getId();
        }
        return key;
    }

    /**
     * Retrieve the missed cache item from the specified cache.
     *
     * @param key the unique key for the cache item
     * @param cacheName the name of the cache - this is the cache region name from ehcache config
     * @param <T> the type of the cache item
     * @return the cache item instance
     */
    protected T getObjectFromCache(String key, String cacheName) {
        return getCache(cacheName).get(key);
    }

    /**
     * Retrieve the underlying cache for this query miss cache. Presumably and Ehcache
     * region has been configured for this cacheName.
     *
     * @param cacheName the name of the cache - the ehcache region name
     * @return the underlying cache
     */
    protected Cache<String, T> getCache(String cacheName) {
        return cacheManager.getCache(cacheName);
    }

    /**
     * Remove a specific cache item from the underlying cache
     *
     * @param cacheName the name of the cache - the ehcache region name
     * @param params the appropriate params comprising a unique key for this cache item
     */
    protected void removeItemFromCache(String cacheName, String... params) {
        String key = buildKey(params);
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("Evicting [" + key + "] from the [" + cacheName + "] cache.");
        }
        getCache(cacheName).remove(key);
    }

    /**
     * Remove all items from the underlying cache - a complete clear
     *
     * @param cacheName the name of the cache - the ehcache region name
     */
    protected void clearCache(String cacheName) {
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("Evicting all keys from the [" + cacheName + "] cache.");
        }
        getCache(cacheName).removeAll();
    }

    /**
     * Retrieve a null representation of the cache item. This representation is the same for
     * all cache misses and is used as the object representation to store in the cache for a
     * cache miss.
     *
     * @param responseClass the class representing the type of the cache item
     * @param <T> the type of the cache item
     * @return the null representation for the cache item
     */
    protected synchronized T getNullObject(final Class<T> responseClass) {
        if (nullObject == null) {
            Class<?>[] interfaces = (Class<?>[]) ArrayUtils.add(ClassUtils.getAllInterfacesForClass(responseClass), Serializable.class);
            nullObject = Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    if (method.getName().equals("equals")) {
                        return !(objects[0] == null) && objects[0].hashCode() == 31;
                    } else if (method.getName().equals("hashCode")) {
                        return 31;
                    } else if (method.getName().equals("toString")) {
                        return "Null_" + responseClass.getSimpleName();
                    }
                    throw new IllegalAccessException("Not a real object");
                }
            });
        }
        return (T) nullObject;
    }

    /**
     * This is the main entry point for retrieving an object from this cache.
     *
     * @see org.broadleafcommerce.common.cache.StatisticsService
     * @param responseClass the class representing the type of the cache item
     * @param cacheName the name of the cache - the ehcache region name
     * @param statisticsName the name to use for cache hit statistics
     * @param retrieval the block of code to execute if a cache miss is not found in this cache
     * @param params the appropriate params comprising a unique key for this cache item
     * @param <T> the type of the cache item
     * @return The object retrieved from the executiom of the PersistentRetrieval, or null if a cache miss was found in this cache
     */
    protected T getCachedObject(Class<T> responseClass, String cacheName, String statisticsName, PersistentRetrieval<T> retrieval, String... params) {
        T nullResponse = getNullObject(responseClass);
        BroadleafRequestContext context = BroadleafRequestContext.getBroadleafRequestContext();
        String key = buildKey(params);
        T response = null;
        boolean allowL2Cache = false;
        if (context != null) {
            allowL2Cache = context.isProductionSandBox()
                || (context.getAdditionalProperties().containsKey("allowLevel2Cache")
                    && (Boolean) context.getAdditionalProperties().get("allowLevel2Cache"));
        }
        if (allowL2Cache) {
            response = getObjectFromCache(key, cacheName);
        }
        if (response == null) {
            response = retrieval.retrievePersistentObject();
            if (response == null) {
                response = nullResponse;
            }
            //only handle null, non-hits. Otherwise, let level 2 cache handle it
            if (allowL2Cache && response.equals(nullResponse)) {
                statisticsService.addCacheStat(statisticsName, false);
                getCache(cacheName).put(key, response);
                if (getLogger().isTraceEnabled()) {
                    getLogger().trace("Caching [" + key + "] as null in the [" + cacheName + "] cache.");
                }
            }
        } else {
            statisticsService.addCacheStat(statisticsName, true);
        }
        if (response.equals(nullResponse)) {
            return null;
        }
        return response;
    }
    
    /**
     * To provide more accurate logging, this abstract cache should utilize a logger from its child
     * implementation.
     * 
     * @return a {@link Log} instance from the subclass of this abstract class
     */
    protected abstract Log getLogger();
}
