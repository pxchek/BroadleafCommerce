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
package org.broadleafcommerce.core.offer.service.discount.domain;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.offer.domain.OrderItemPriceDetailAdjustment;
import org.broadleafcommerce.core.offer.service.discount.PromotionQualifier;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemContainer;
import org.broadleafcommerce.core.order.domain.OrderItemPriceDetail;
import org.broadleafcommerce.core.order.domain.OrderItemQualifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PromotableOrderItemImpl implements PromotableOrderItem {

    private static final Log LOG = LogFactory.getLog(PromotableOrderItem.class);

    private static final long serialVersionUID = 1L;
    
    protected PromotableOrder promotableOrder;
    protected OrderItem orderItem;
    protected PromotableItemFactory itemFactory;
    protected List<PromotableOrderItemPriceDetail> itemPriceDetails = new ArrayList<PromotableOrderItemPriceDetail>();
    protected boolean includeAdjustments;
    protected Map<String, Object> extraDataMap = new HashMap<String, Object>();

    public PromotableOrderItemImpl(OrderItem orderItem, PromotableOrder promotableOrder, PromotableItemFactory itemFactory,
            boolean includeAdjustments) {
        this.orderItem = orderItem;
        this.promotableOrder = promotableOrder;
        this.itemFactory = itemFactory;
        this.includeAdjustments = includeAdjustments;
        initializePriceDetails();
    }

    @Override
    public void resetPriceDetails() {
        itemPriceDetails.clear();
        initializePriceDetails();
    }

    protected void initializePriceDetails() {
        if (includeAdjustments) {
            for (OrderItemPriceDetail detail : orderItem.getOrderItemPriceDetails()) {
                PromotableOrderItemPriceDetail poid =
                        itemFactory.createPromotableOrderItemPriceDetail(this, detail.getQuantity());
                itemPriceDetails.add(poid);
                for (OrderItemPriceDetailAdjustment adjustment : detail.getOrderItemPriceDetailAdjustments()) {
                    PromotableOrderItemPriceDetailAdjustment poidAdj =
                            new PromotableOrderItemPriceDetailAdjustmentImpl(adjustment, poid);
                    poid.addCandidateItemPriceDetailAdjustment(poidAdj);
                }
                poid.chooseSaleOrRetailAdjustments();

                List<OrderItemQualifier> oiqs = poid.getPromotableOrderItem().getOrderItem().getOrderItemQualifiers();
                if (CollectionUtils.isNotEmpty(oiqs)) {
                    for (OrderItemQualifier oiq : oiqs) {
                        PromotionQualifier pq = new PromotionQualifier();
                        pq.setPromotion(oiq.getOffer());
                        pq.setQuantity(oiq.getQuantity().intValue());
                        pq.setFinalizedQuantity(oiq.getQuantity().intValue());
                        pq.setPrice(oiq.getOrderItem().getPriceBeforeAdjustments(oiq.getOffer().getApplyDiscountToSalePrice()));
                        poid.getPromotionQualifiers().add(pq);
                    }
                }
            }
        } else {
            PromotableOrderItemPriceDetail poid =
                    itemFactory.createPromotableOrderItemPriceDetail(this, orderItem.getQuantity());
            itemPriceDetails.add(poid);
        }
    }
    
    /**
     * Adds the item to the rule variables map.
     * @param ruleVars
     */
    public void updateRuleVariables(Map<String, Object> ruleVars) {
        ruleVars.put("orderItem", orderItem);
        ruleVars.put("discreteOrderItem", orderItem);
        ruleVars.put("bundleOrderItem", orderItem);
    }

    @Override
    public boolean isDiscountingAllowed() {
        return orderItem.isDiscountingAllowed();
    }
    
    @Override
    public boolean isOrderItemContainer() {
        return orderItem instanceof OrderItemContainer;
    }

    @Override
    public OrderItemContainer getOrderItemContainer() {
        if (orderItem instanceof OrderItemContainer) {
            return (OrderItemContainer) orderItem;
        }
        return null;
    }

    public List<PromotableOrderItemPriceDetail> getPromotableOrderItemPriceDetails() {
        return itemPriceDetails;
    }

    public Money getSalePriceBeforeAdjustments() {
        return orderItem.getSalePrice();
    }

    public Money getRetailPriceBeforeAdjustments() {
        return orderItem.getRetailPrice();
    }

    public Money getPriceBeforeAdjustments(boolean applyToSalePrice) {
        if (applyToSalePrice && getSalePriceBeforeAdjustments() != null) {
            return getSalePriceBeforeAdjustments();
        }
        return getRetailPriceBeforeAdjustments();
    }

    public Money getCurrentBasePrice() {
        if (orderItem.getIsOnSale()) {
            return orderItem.getSalePrice();
        } else {
            return orderItem.getRetailPrice();
        }
    }

    public int getQuantity() {
        return orderItem.getQuantity();
    }

    @Override
    public boolean isOnSale() {
        return orderItem.getIsOnSale();
    }

    @Override
    public BroadleafCurrency getCurrency() {
        return orderItem.getOrder().getCurrency();
    }

    @Override
    public void removeAllItemAdjustments() {
        Iterator<PromotableOrderItemPriceDetail> detailIterator = itemPriceDetails.iterator();

        boolean first = true;
        while (detailIterator.hasNext()) {
            PromotableOrderItemPriceDetail detail = detailIterator.next();
            if (first) {
                detail.setQuantity(getQuantity());
                detail.getPromotionDiscounts().clear();
                detail.getPromotionQualifiers().clear();
                detail.removeAllAdjustments();
                first = false;
            } else {
                // Get rid of all other details
                detailIterator.remove();
            }
        }
    }

    protected void mergeDetails(PromotableOrderItemPriceDetail firstDetail, PromotableOrderItemPriceDetail secondDetail) {
        int firstQty = firstDetail.getQuantity();
        int secondQty = secondDetail.getQuantity();
        
        if (LOG.isDebugEnabled()) {
            LOG.trace("Merging priceDetails with quantities " + firstQty + " and " + secondQty);
        }

        firstDetail.setQuantity(firstQty + secondQty);
    }

    @Override
    public void mergeLikeDetails() {
        if (itemPriceDetails.size() > 1) {
            Iterator<PromotableOrderItemPriceDetail> detailIterator = itemPriceDetails.iterator();
            Map<String, PromotableOrderItemPriceDetail> detailMap = new HashMap<String, PromotableOrderItemPriceDetail>();

            while (detailIterator.hasNext()) {
                PromotableOrderItemPriceDetail currentDetail = detailIterator.next();
                String detailKey = currentDetail.buildDetailKey();
                if (detailMap.containsKey(detailKey)) {
                    PromotableOrderItemPriceDetail firstDetail = detailMap.get(detailKey);
                    mergeDetails(firstDetail, currentDetail);
                    detailIterator.remove();
                } else {
                    detailMap.put(detailKey, currentDetail);
                }
            }
        }
    }

    @Override
    public Long getOrderItemId() {
        return orderItem.getId();
    }

    @Override
    public Money calculateTotalWithAdjustments() {
        Money returnTotal = new Money(getCurrency());
        for (PromotableOrderItemPriceDetail detail : itemPriceDetails) {
            returnTotal = returnTotal.add(detail.getFinalizedTotalWithAdjustments());
        }
        return returnTotal;
    }

    @Override
    public Money calculateTotalWithoutAdjustments() {
        return getCurrentBasePrice().multiply(orderItem.getQuantity());
    }

    @Override
    public Money calculateTotalAdjustmentValue() {
        Money returnTotal = new Money(getCurrency());
        for (PromotableOrderItemPriceDetail detail : itemPriceDetails) {
            returnTotal = returnTotal.add(detail.calculateTotalAdjustmentValue());
        }
        return returnTotal;
    }

    public PromotableOrderItemPriceDetail createNewDetail(int quantity) {
        if (includeAdjustments) {
            throw new RuntimeException("Trying to createNewDetail when adjustments have already been included.");
        }
        return itemFactory.createPromotableOrderItemPriceDetail(this, quantity);
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    @Override
    public Map<String, Object> getExtraDataMap() {
        return extraDataMap;
    }
}
