/*-
 * #%L
 * BroadleafCommerce Framework
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
package org.broadleafcommerce.core.order.service.type;

import org.broadleafcommerce.common.BroadleafEnumerationType;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An extendible enumeration of order item types.
 * 
 * @author jfischer
 */
public class OrderItemType implements Serializable, BroadleafEnumerationType {

    private static final long serialVersionUID = 1L;

    private static final Map<String, OrderItemType> TYPES = new LinkedHashMap<String, OrderItemType>();

    public static final OrderItemType BASIC  = new OrderItemType("org.broadleafcommerce.core.order.domain.OrderItem", "Basic Order Item");
    public static final OrderItemType DISCRETE  = new OrderItemType("org.broadleafcommerce.core.order.domain.DiscreteOrderItem", "Discrete Order Item");
    public static final OrderItemType EXTERNALLY_PRICED  = new OrderItemType("org.broadleafcommerce.core.order.domain.DynamicPriceDiscreteOrderItem", "Externally Priced Discrete Order Item");
    public static final OrderItemType BUNDLE = new OrderItemType("org.broadleafcommerce.core.order.domain.BundleOrderItem", "Bundle Order Item");
    public static final OrderItemType GIFTWRAP = new OrderItemType("org.broadleafcommerce.core.order.domain.GiftWrapOrderItem", "Gift Wrap Order Item");

    public static OrderItemType getInstance(final String type) {
        return TYPES.get(type);
    }

    private String type;
    private String friendlyType;

    public OrderItemType() {
        //do nothing
    }

    public OrderItemType(final String type, final String friendlyType) {
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
        OrderItemType other = (OrderItemType) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }
}
