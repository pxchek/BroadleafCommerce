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
package org.broadleafcommerce.common.site.service.type;

import org.broadleafcommerce.common.BroadleafEnumerationType;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An extendible enumeration of order status types.
 * 
 * <ul>
 *  <li><b>DOMAIN</b> - Resolve a site based on the whole domain (e.g. request.getServerName())</li>
 *  <li><b>DOMAIN_PREFIX</b> - Resolve a site based on the first word in the domain.</li>
 * </ul>
 * 
 * For example, if the URL you wanted to resolve was
 * http://mysite.mycompany.com.
 * 
 * The {@link #getSiteIdentifierValue()} should be set as follows: 
 *    
 * to use DOMAIN resolution, set to "mysite.mycompany.com"
 * to use DOMAIN_PREFIX resolution, set to "mysite"
 * 
 * @author jfischer
 */
public class SiteResolutionType implements Serializable, BroadleafEnumerationType {

    private static final long serialVersionUID = 1L;

    private static final Map<String, SiteResolutionType> TYPES = new LinkedHashMap<String, SiteResolutionType>();


    public static final SiteResolutionType DOMAIN = new SiteResolutionType("DOMAIN", "Domain");
    public static final SiteResolutionType DOMAIN_PREFIX = new SiteResolutionType("DOMAIN_PREFIX", "Domain Prefix");

    public static SiteResolutionType getInstance(final String type) {
        return TYPES.get(type);
    }

    private String type;
    private String friendlyType;

    public SiteResolutionType() {
        //do nothing
    }

    public SiteResolutionType(final String type, final String friendlyType) {
        this.friendlyType = friendlyType;
        setType(type);
    }

    public String getType() {
        return type;
    }

    public String getFriendlyType() {
        return friendlyType;
    }

    private void setType(final String type) {
        this.type = type;
        if (!TYPES.containsKey(type)) {
            TYPES.put(type, this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!getClass().isAssignableFrom(obj.getClass()))
            return false;
        SiteResolutionType other = (SiteResolutionType) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
