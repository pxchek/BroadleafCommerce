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
package org.broadleafcommerce.core.order.service.manipulation;

import org.broadleafcommerce.core.order.domain.BundleOrderItem;

import java.util.ArrayList;
import java.util.List;

public class BundleOrderItemSplitContainer {
    
    protected BundleOrderItem key;
    protected List<BundleOrderItem> splitItems = new ArrayList<BundleOrderItem>();
    
    public BundleOrderItem getKey() {
        return key;
    }
    
    public void setKey(BundleOrderItem key) {
        this.key = key;
    }
    
    public List<BundleOrderItem> getSplitItems() {
        return splitItems;
    }
    
    public void setSplitItems(List<BundleOrderItem> splitItems) {
        this.splitItems = splitItems;
    }

}
