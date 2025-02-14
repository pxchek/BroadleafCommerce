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
package org.broadleafcommerce.common.config.service.type;

import org.broadleafcommerce.common.BroadleafEnumerationType;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class ModuleConfigurationType implements BroadleafEnumerationType, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Map<String, ModuleConfigurationType> TYPES = new LinkedHashMap<String, ModuleConfigurationType>();

    public static final ModuleConfigurationType FULFILLMENT_PRICING = new ModuleConfigurationType("FULFILLMENT_PRICING", "Fulfillment Pricing Module");
    public static final ModuleConfigurationType TAX_CALCULATION = new ModuleConfigurationType("TAX_CALCULATION", "Tax Calculation Module");
    public static final ModuleConfigurationType ADDRESS_VERIFICATION = new ModuleConfigurationType("ADDRESS_VERIFICATION", "Address Verification Module");
    public static final ModuleConfigurationType PAYMENT_PROCESSOR = new ModuleConfigurationType("PAYMENT_PROCESSOR", "Payment Processor Module");
    public static final ModuleConfigurationType CDN_PROVIDER = new ModuleConfigurationType("CDN_PROVIDER", "Content Delivery Network Module");
    public static final ModuleConfigurationType SITE_MAP = new ModuleConfigurationType("SITE_MAP", "Site Map Generator");

    public static ModuleConfigurationType getInstance(final String type) {
        return TYPES.get(type);
    }

    private String type;
    private String friendlyType;

    public ModuleConfigurationType() {
        //do nothing
    }

    public ModuleConfigurationType(final String type, final String friendlyType) {
        this.friendlyType = friendlyType;
        setType(type);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
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
        ModuleConfigurationType other = (ModuleConfigurationType) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
