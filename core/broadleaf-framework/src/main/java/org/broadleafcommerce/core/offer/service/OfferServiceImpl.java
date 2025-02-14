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
package org.broadleafcommerce.core.offer.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.persistence.EntityDuplicateModifier;
import org.broadleafcommerce.common.persistence.EntityDuplicator;
import org.broadleafcommerce.common.sandbox.SandBoxHelper;
import org.broadleafcommerce.common.util.StreamCapableTransactionalOperationAdapter;
import org.broadleafcommerce.common.util.StreamingTransactionCapableUtil;
import org.broadleafcommerce.core.offer.dao.CustomerOfferDao;
import org.broadleafcommerce.core.offer.dao.OfferCodeDao;
import org.broadleafcommerce.core.offer.dao.OfferDao;
import org.broadleafcommerce.core.offer.domain.Adjustment;
import org.broadleafcommerce.core.offer.domain.CustomerOffer;
import org.broadleafcommerce.core.offer.domain.Offer;
import org.broadleafcommerce.core.offer.domain.OfferCode;
import org.broadleafcommerce.core.offer.domain.OfferImpl;
import org.broadleafcommerce.core.offer.domain.OrderItemPriceDetailAdjustment;
import org.broadleafcommerce.core.offer.service.discount.domain.PromotableCandidateFulfillmentGroupOffer;
import org.broadleafcommerce.core.offer.service.discount.domain.PromotableCandidateItemOffer;
import org.broadleafcommerce.core.offer.service.discount.domain.PromotableCandidateOrderOffer;
import org.broadleafcommerce.core.offer.service.discount.domain.PromotableItemFactory;
import org.broadleafcommerce.core.offer.service.discount.domain.PromotableOrder;
import org.broadleafcommerce.core.offer.service.processor.FulfillmentGroupOfferProcessor;
import org.broadleafcommerce.core.offer.service.processor.ItemOfferProcessor;
import org.broadleafcommerce.core.offer.service.processor.OrderOfferProcessor;
import org.broadleafcommerce.core.offer.service.type.CustomerMaxUsesStrategyType;
import org.broadleafcommerce.core.offer.service.type.OfferType;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemPriceDetail;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * The Class OfferServiceImpl.
 */
@Service("blOfferService")
public class OfferServiceImpl implements OfferService {
    
    private static final Log LOG = LogFactory.getLog(OfferServiceImpl.class);

    // should be called outside of Offer service after Offer service is executed
    @Resource(name="blCustomerOfferDao")
    protected CustomerOfferDao customerOfferDao;

    @Resource(name="blOfferCodeDao")
    protected OfferCodeDao offerCodeDao;
    
    @Resource(name="blOfferAuditService")
    protected OfferAuditService offerAuditService;

    @Resource(name="blOfferDao")
    protected OfferDao offerDao;
    
    @Resource(name="blOrderOfferProcessor")
    protected OrderOfferProcessor orderOfferProcessor;
    
    @Resource(name="blItemOfferProcessor")
    protected ItemOfferProcessor itemOfferProcessor;
    
    @Resource(name="blFulfillmentGroupOfferProcessor")
    protected FulfillmentGroupOfferProcessor fulfillmentGroupOfferProcessor;
    
    @Resource(name="blPromotableItemFactory")
    protected PromotableItemFactory promotableItemFactory;

    @Resource(name = "blOfferServiceExtensionManager")
    protected OfferServiceExtensionManager extensionManager;

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name = "blSandBoxHelper")
    protected SandBoxHelper sandBoxHelper;

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blStreamingTransactionCapableUtil")
    protected StreamingTransactionCapableUtil transUtil;

    @Resource(name="blEntityDuplicator")
    protected EntityDuplicator duplicator;

    /**
     * @deprecated Add {@link EntityDuplicateModifier}s to {@code blEntityDuplicationHelpers}
     */
    @Deprecated
    protected EntityDuplicateModifier<Offer> offerDuplicateModifier;

    @Override
    public List<Offer> findAllOffers() {
        return offerDao.readAllOffers();
    }

    @Override
    @Transactional("blTransactionManager")
    public Offer save(Offer offer) {
        return offerDao.save(offer);
    }

    @Override
    @Transactional("blTransactionManager")
    public OfferCode saveOfferCode(OfferCode offerCode) {
        offerCode.setOffer(offerDao.save(offerCode.getOffer()));
        return offerCodeDao.save(offerCode);
    }

    /**
     * Creates a list of offers that applies to this order.  All offers that are assigned to the customer,
     * entered during checkout, or has a delivery type of automatic are added to the list.  The same offer
     * cannot appear more than once in the list.
     *
     * @param code
     * @return a List of offers that may apply to this order
     */
    @Override
    public Offer lookupOfferByCode(String code) {
        Offer offer = null;
        OfferCode offerCode = offerCodeDao.readOfferCodeByCode(code);
        if (offerCode != null) {
            offer = offerCode.getOffer();
        }
        return offer;
    }
    
    @Override
    public OfferCode lookupOfferCodeByCode(String code){
        return offerCodeDao.readOfferCodeByCode(code);
    }

    @Override
    public List<Offer> lookupAllOffersByCode(String code) {
        List<Offer> offers = new ArrayList<Offer>();
        List<OfferCode> offerCodes = offerCodeDao.readAllOfferCodesByCode(code);
        for (OfferCode offerCode : offerCodes) {
            if (offerCode != null) {
                offers.add(offerCode.getOffer());
            }
        }
        return offers;
    }

    @Override
    public List<OfferCode> lookupAllOfferCodesByCode(String code){
        return offerCodeDao.readAllOfferCodesByCode(code);
    }

    /**
     * Creates a list of offers that applies to this order.  All offers that are assigned to the customer,
     * entered during checkout, or has a delivery type of automatic are added to the list.  The same offer
     * cannot appear more than once in the list.
     *
     * @param order
     * @return a List of offers that may apply to this order
     */
    @Override
    public List<Offer> buildOfferListForOrder(Order order) {
        List<Offer> offers = new ArrayList<Offer>();
        List<CustomerOffer> customerOffers = lookupOfferCustomerByCustomer(order.getCustomer());
        for (CustomerOffer customerOffer : customerOffers) {
            if (!offers.contains(customerOffer.getOffer())) {
                offers.add(customerOffer.getOffer());
            }
        }
        List<OfferCode> orderOfferCodes = refreshOfferCodesIfApplicable(order);
        orderOfferCodes = removeOutOfDateOfferCodes(orderOfferCodes);
        for (OfferCode orderOfferCode : orderOfferCodes) {
            if (!offers.contains(orderOfferCode.getOffer())) {
                offers.add(orderOfferCode.getOffer());
            }
            extensionManager.getProxy().addAdditionalOffersForCode(offers, orderOfferCode);
        }
        List<Offer> globalOffers = lookupAutomaticDeliveryOffers();
        for (Offer globalOffer : globalOffers) {
            if (!offers.contains(globalOffer) && verifyMaxCustomerUsageThreshold(order, globalOffer)) {
                offers.add(globalOffer);
            }
        }
        
        if (extensionManager != null) {
            extensionManager.applyAdditionalFilters(offers, order);
        }
        
        return offers;
    }

    @Override
    public List<OfferCode> buildOfferCodeListForCustomer(Order order) {
        Customer customer = order.getCustomer();
        ArrayList<OfferCode> offerCodes = new ArrayList<>();
        
        if (extensionManager != null) {
            extensionManager.buildOfferCodeListForCustomer(customer, offerCodes);
        }
        if (!offerCodes.isEmpty()) {
            Iterator<OfferCode> itr = offerCodes.iterator();
            while (itr.hasNext()) {
                OfferCode offerCode = itr.next();
                if (!offerCode.isActive() || !verifyMaxCustomerUsageThreshold(order, offerCode)) {
                    itr.remove();
                }
            }
        }
        return offerCodes;
    }
    
    @Deprecated
    @Override
    public List<OfferCode> buildOfferCodeListForCustomer(Customer customer) {
        ArrayList<OfferCode> offerCodes = new ArrayList<OfferCode>();
        if (extensionManager != null) {
            extensionManager.buildOfferCodeListForCustomer(customer, offerCodes);
        }
        if (!offerCodes.isEmpty()) {
            Iterator<OfferCode> itr = offerCodes.iterator();
            while (itr.hasNext()) {
                OfferCode offerCode = itr.next();
                if (!offerCode.isActive() || !verifyMaxCustomerUsageThreshold(customer, offerCode)) {
                    itr.remove();
                }
            }
        }
        return offerCodes;
    }

    /**
     * Private method used to retrieve all offers assigned to this customer.  These offers are
     * programmatically assigned to the customer.
     *
     * @param customer
     * @return a List of offers assigned to the customer
     */
    protected List<CustomerOffer> lookupOfferCustomerByCustomer(Customer customer) {
        List<CustomerOffer> offerCustomers = customerOfferDao.readCustomerOffersByCustomer(customer);
        return offerCustomers;
    }

    /**
     * Private method used to retrieve all offers with automaticallyAdded set to true
     *
     * @return a List of automatic delivery offers
     */
    protected List<Offer> lookupAutomaticDeliveryOffers() {
        List<Offer> globalOffers = offerDao.readOffersByAutomaticDeliveryType();
        return globalOffers;
    }

    /**
     * Removes all out of date offerCodes based on the offerCode and its offer's start and end
     * date.  If an offerCode has a later start date, that offerCode will be removed.
     * OfferCodes without a start date will still be processed. If the offerCode
     * has a end date that has already passed, that offerCode will be removed.  OfferCodes
     * without a end date will be processed.  The start and end dates on the offer will
     * still need to be evaluated.
     *
     * @param offerCodes
     * @return a List of non-expired offers
     */
    protected List<OfferCode> removeOutOfDateOfferCodes(List<OfferCode> offerCodes){
        List<OfferCode> offerCodesToRemove = new ArrayList<OfferCode>();
        for (OfferCode offerCode : offerCodes) {
            if (!offerCode.isActive()){
                offerCodesToRemove.add(offerCode);
            }
        }
        // remove all offers in the offersToRemove list from original offers list
        for (OfferCode offerCode : offerCodesToRemove) {
            offerCodes.remove(offerCode);
        }
        return offerCodes;
    }

    /**
     * For enterprise installations, this will refresh any OfferCodes found to be out-of-date with
     * current sandbox status.
     *
     * @param order the order to check
     * @return the refreshed list of OfferCodes
     */
    protected List<OfferCode> refreshOfferCodesIfApplicable(final Order order) {
        final List<OfferCode> orderOfferCodes = order.getAddedOfferCodes();

        transUtil.runTransactionalOperation(new StreamCapableTransactionalOperationAdapter() {
            @Override
            public void execute() {
                for (OfferCode offerCode : orderOfferCodes) {
                    if (offerCode.getOffer() != null) {
                        Long sandBoxVersionId = sandBoxHelper.getSandBoxVersionId(OfferImpl.class, offerCode.getOffer().getId());
                        if (sandBoxVersionId != null && !Objects.equals(sandBoxVersionId, offerCode.getOffer().getId())) {
                            em.refresh(offerCode);
                            //trigger loading of offer. Somehow sometimes if you have offer & offer code in global(template) site
                            //and have overridden both in test site, sometimes offer code will from the test site has reference
                            //to the offer from global, after triggering of fetch it will replace with correct version
                            OfferImpl offer = em.find(OfferImpl.class, sandBoxVersionId);
                            offerCode.setOffer(offer);
                        }
                    }
                }
            }

            @Override
            public boolean shouldRetryOnTransactionLockAcquisitionFailure() {
                return true;
            }
        }, RuntimeException.class);

        return orderOfferCodes;
    }

    /*
     *
     * Offers Logic:
     * 1) Remove all existing offers in the Order (order, item, and fulfillment)
     * 2) Check and remove offers
     *    a) Remove out of date offers
     *    b) Remove offers that do not apply to this customer
     * 3) Loop through offers
     *    a) Verifies type of offer (order, order item, fulfillment)
     *    b) Verifies if offer can be applies
     *    c) Assign offer to type (order, order item, or fulfillment)
     * 4) Sorts the order and item offers list by priority and then discount
     * 5) Identify the best offers to apply to order item and create adjustments for each item offer
     * 6) Compare order item adjustment price to sales price, and remove adjustments if sale price is better
     * 7) Identify the best offers to apply to the order and create adjustments for each order offer
     * 8) If item contains non-combinable offers remove either the item or order adjustments based on discount value
     * 9) Set final order item prices and reapply order offers
     *
     * Assumptions:
     * 1) % off all items will be created as an item offer with no expression
     * 2) $ off order will be created as an order offer
     * 3) Order offers applies to the best price for each item (not just retail price)
     * 4) Fulfillment offers apply to best price for each item (not just retail price)
     * 5) Stackable only applies to the same offer type (i.e. a not stackable order offer can be used with item offers)
     * 6) Fulfillment offers cannot be not combinable
     * 7) Order offers cannot be FIXED_PRICE
     * 8) FIXED_PRICE offers cannot be stackable
     * 9) Non-combinable offers only apply to the order and order items, fulfillment group offers will always apply
     *
     */
    @Override
    @Transactional("blTransactionManager")
    public Order applyAndSaveOffersToOrder(List<Offer> offers, Order order) throws PricingException {
        /*
        TODO rather than a threadlocal, we should update the "shouldPrice" boolean on the service API to
        use a richer object to describe the parameters of the pricing call. This object would include
        the pricing boolean, but would also include a list of activities to include or exclude in the
        call - see http://jira.broadleafcommerce.org/browse/BLC-664
         */
        OfferContext offerContext = OfferContext.getOfferContext();
        if (offerContext == null || offerContext.executePromotionCalculation) {
            PromotableOrder promotableOrder = promotableItemFactory.createPromotableOrder(order, false);
            List<Offer> filteredOffers = orderOfferProcessor.filterOffers(offers, order.getCustomer());
            if ((filteredOffers == null) || (filteredOffers.isEmpty())) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("No offers applicable to this order.");
                }
            } else {
                List<PromotableCandidateOrderOffer> qualifiedOrderOffers = new ArrayList<PromotableCandidateOrderOffer>();
                List<PromotableCandidateItemOffer> qualifiedItemOffers = new ArrayList<PromotableCandidateItemOffer>();

                itemOfferProcessor.filterOffers(promotableOrder, filteredOffers, qualifiedOrderOffers, qualifiedItemOffers);

                if (! (qualifiedItemOffers.isEmpty() && qualifiedOrderOffers.isEmpty())) {                
                    // At this point, we should have a PromotableOrder that contains PromotableItems each of which
                    // has a list of candidatePromotions that might be applied.

                    // We also have a list of orderOffers that might apply and a list of itemOffers that might apply.
                    itemOfferProcessor.applyAndCompareOrderAndItemOffers(promotableOrder, qualifiedOrderOffers, qualifiedItemOffers);
                }
            }
            orderOfferProcessor.synchronizeAdjustmentsAndPrices(promotableOrder);
            
            verifyAdjustments(order, true);
            
            order.setSubTotal(order.calculateSubTotal());
            order.finalizeItemPrices();
            
            order = orderService.save(order, false);
            
            boolean madeChange = verifyAdjustments(order, false);
            if (madeChange) {
                order = orderService.save(order, false);
            }
        }

        return order;
    }
    
    protected boolean verifyAdjustments(Order order, boolean beforeSave) {
        boolean madeChange = false;
        
        if (order.getOrderItems() == null) {
            return madeChange;
        }

        for (OrderItem oi : order.getOrderItems()) {
            if (oi.getOrderItemPriceDetails() == null) {
                continue;
            }

            for (OrderItemPriceDetail pd : oi.getOrderItemPriceDetails()) {
                if (pd.getOrderItemPriceDetailAdjustments() == null) {
                    continue;
                }

                Map<Long, OrderItemPriceDetailAdjustment> adjs = new HashMap<Long, OrderItemPriceDetailAdjustment>();
                List<OrderItemPriceDetailAdjustment> adjustmentsToRemove = new ArrayList<OrderItemPriceDetailAdjustment>();
                for (OrderItemPriceDetailAdjustment adj : pd.getOrderItemPriceDetailAdjustments()) {
                    if (adjs.containsKey(adj.getOffer().getId())) {
                        adjustmentsToRemove.add(adj);
                        if (LOG.isDebugEnabled()) {
                            StringBuilder sb = new StringBuilder("Detected collisions ")
                                .append(beforeSave ? "before saving" : "after saving")
                                .append(" with ids ")
                                .append(adjs.get(adj.getOffer().getId()).getId())
                                .append(" and ")
                                .append(adj.getId());
                            LOG.debug(sb.toString());
                        }
                    } else {
                        adjs.put(adj.getOffer().getId(), adj);
                    }
                }
                
                for (OrderItemPriceDetailAdjustment adj : adjustmentsToRemove) {
                    pd.getOrderItemPriceDetailAdjustments().remove(adj);
                    madeChange = true;
                }
            }
        }

        return madeChange;
     }

    @Override
    @Transactional("blTransactionManager")
    @Deprecated
    public void applyOffersToOrder(List<Offer> offers, Order order) throws PricingException {
        applyAndSaveOffersToOrder(offers, order);
    }

    @Override
    @Transactional("blTransactionManager")
    @Deprecated
    public void applyFulfillmentGroupOffersToOrder(List<Offer> offers, Order order) throws PricingException {
        applyAndSaveFulfillmentGroupOffersToOrder(offers, order);
    }

    @Override
    @Transactional("blTransactionManager")
    public Order applyAndSaveFulfillmentGroupOffersToOrder(List<Offer> offers, Order order) throws PricingException {
        OfferContext offerContext = OfferContext.getOfferContext();
        if (offerContext == null || offerContext.executePromotionCalculation) {
            PromotableOrder promotableOrder =
                    promotableItemFactory.createPromotableOrder(order, true);
            List<Offer> possibleFGOffers = new ArrayList<Offer>();
            for (Offer offer : offers) {
                if (offer.getType().getType().equals(OfferType.FULFILLMENT_GROUP.getType())) {
                    possibleFGOffers.add(offer);
                }
            }
            List<Offer> filteredOffers = orderOfferProcessor.filterOffers(possibleFGOffers, order.getCustomer());
            List<PromotableCandidateFulfillmentGroupOffer> qualifiedFGOffers = new ArrayList<PromotableCandidateFulfillmentGroupOffer>();
            for (Offer offer : filteredOffers) {
                fulfillmentGroupOfferProcessor.filterFulfillmentGroupLevelOffer(promotableOrder, qualifiedFGOffers, offer);
            }
            if (!qualifiedFGOffers.isEmpty()) {
                fulfillmentGroupOfferProcessor.applyAllFulfillmentGroupOffers(qualifiedFGOffers, promotableOrder);
                fulfillmentGroupOfferProcessor.calculateFulfillmentGroupTotal(promotableOrder);
                orderOfferProcessor.synchronizeAdjustmentsAndPrices(promotableOrder);
                order.setSubTotal(order.calculateSubTotal());
                order.finalizeItemPrices();
            }

            return orderService.save(order, false);
        }
        return order;
    }

    @Override
    public boolean verifyMaxCustomerUsageThreshold(Order order, Offer offer) {
        if (offer.isLimitedUsePerCustomer()) {
            CustomerMaxUsesStrategyType strategy = offer.getMaxUsesStrategyType();
            boolean checkUsingCustomer = (strategy == null || strategy.equals(CustomerMaxUsesStrategyType.CUSTOMER));
            Long currentUses;
            if (checkUsingCustomer) {
                currentUses = offerAuditService.countUsesByCustomer(order, order.getCustomer().getId(), offer.getId(), offer.getMinimumDaysPerUsage());
            } else {
                currentUses = offerAuditService.countUsesByAccount(order, order.getBroadleafAccountId(), offer.getId(), offer.getMinimumDaysPerUsage());
            }
            
            if (currentUses >= offer.getMaxUsesPerCustomer()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Deprecated
    @Override
    public boolean verifyMaxCustomerUsageThreshold(Customer customer, Offer offer) {
        if (offer.isLimitedUsePerCustomer()) {                
            Long currentUses = offerAuditService.countUsesByCustomer(customer.getId(), offer.getId());
            
            if (currentUses >= offer.getMaxUsesPerCustomer()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean verifyMaxCustomerUsageThreshold(Order order, OfferCode code) {
        boolean underCodeMaxUses = true;
        
        if (code.isLimitedUse()) {
            Long currentCodeUses = offerAuditService.countOfferCodeUses(order, code.getId());
            underCodeMaxUses = currentCodeUses < code.getMaxUses();
        }
        
        return underCodeMaxUses && verifyMaxCustomerUsageThreshold(order, code.getOffer());
    }

    @Deprecated
    @Override
    public boolean verifyMaxCustomerUsageThreshold(Customer customer, OfferCode code) {
        boolean underCodeMaxUses = true;
        if (code.isLimitedUse()) {
            Long currentCodeUses = offerAuditService.countOfferCodeUses(code.getId());
            underCodeMaxUses = currentCodeUses < code.getMaxUses();
        }
        return underCodeMaxUses && verifyMaxCustomerUsageThreshold(customer, code.getOffer());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<Offer> getUniqueOffersFromOrder(Order order) {
        HashSet<Offer> result = new HashSet<Offer>();
        
        Transformer adjustmentToOfferTransformer = new Transformer() {
            
            @Override
            public Object transform(Object input) {
                return ((Adjustment)input).getOffer();
            }
        };
        
        result.addAll(CollectionUtils.collect(order.getOrderAdjustments(), adjustmentToOfferTransformer));

        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                result.addAll(CollectionUtils.collect(item.getOrderItemAdjustments(), adjustmentToOfferTransformer));
                
                //record usage for price details on the item as well
                if (item.getOrderItemPriceDetails() != null) {
                    for (OrderItemPriceDetail detail : item.getOrderItemPriceDetails()) {
                        result.addAll(CollectionUtils.collect(detail.getOrderItemPriceDetailAdjustments(), adjustmentToOfferTransformer));
                    }
                }
            }
        }

        if (order.getFulfillmentGroups() != null) {
            for (FulfillmentGroup fg : order.getFulfillmentGroups()) {
                result.addAll(CollectionUtils.collect(fg.getFulfillmentGroupAdjustments(), adjustmentToOfferTransformer));
            }
        }
        return result;
    }
    
    @Override
    public Map<Offer, OfferCode> getOffersRetrievedFromCodes(Order order)  {
        return getOffersRetrievedFromCodes(order.getAddedOfferCodes(), getUniqueOffersFromOrder(order));
    }
    
    @Override
    public Map<Offer, OfferCode> getOffersRetrievedFromCodes(List<OfferCode> codes, Set<Offer> appliedOffers) {
        HashMap<Offer, OfferCode> offerToCodeMapping = new HashMap<Offer, OfferCode>();
        for (OfferCode code : codes) {
            if (appliedOffers.contains(code.getOffer())) {
                offerToCodeMapping.put(code.getOffer(), code);
            }

            List<Offer> additionalOffersToBeApplied = new ArrayList<Offer>();

            extensionManager.getProxy().addAdditionalOffersForCode(additionalOffersToBeApplied, code);
            for (Offer additionalOfferToBeApplied : additionalOffersToBeApplied) {
                offerToCodeMapping.put(additionalOfferToBeApplied, code);
            }
        }
        return offerToCodeMapping;
    }
    
    @Override
    @Transactional("blTransactionManager")
    public Boolean deleteOfferCode(OfferCode code) {
        if (offerCodeDao.offerCodeIsUsed(code)) {
            return false;
        }

        offerCodeDao.delete(code);
        return true;
    }

    @Transactional("blTransactionManager")
    @Override
    public Offer duplicate(Long originalOfferId) {
        return duplicator.copy(OfferImpl.class, originalOfferId);
    }

    @Override
    public CustomerOfferDao getCustomerOfferDao() {
        return customerOfferDao;
    }

    @Override
    public void setCustomerOfferDao(CustomerOfferDao customerOfferDao) {
        this.customerOfferDao = customerOfferDao;
    }

    @Override
    public OfferCodeDao getOfferCodeDao() {
        return offerCodeDao;
    }

    @Override
    public void setOfferCodeDao(OfferCodeDao offerCodeDao) {
        this.offerCodeDao = offerCodeDao;
    }

    @Override
    public OfferDao getOfferDao() {
        return offerDao;
    }

    @Override
    public void setOfferDao(OfferDao offerDao) {
        this.offerDao = offerDao;
    }

    @Override
    public OrderOfferProcessor getOrderOfferProcessor() {
        return orderOfferProcessor;
    }

    @Override
    public void setOrderOfferProcessor(OrderOfferProcessor orderOfferProcessor) {
        this.orderOfferProcessor = orderOfferProcessor;
    }

    @Override
    public ItemOfferProcessor getItemOfferProcessor() {
        return itemOfferProcessor;
    }

    @Override
    public void setItemOfferProcessor(ItemOfferProcessor itemOfferProcessor) {
        this.itemOfferProcessor = itemOfferProcessor;
    }

    @Override
    public FulfillmentGroupOfferProcessor getFulfillmentGroupOfferProcessor() {
        return fulfillmentGroupOfferProcessor;
    }

    @Override
    public void setFulfillmentGroupOfferProcessor(FulfillmentGroupOfferProcessor fulfillmentGroupOfferProcessor) {
        this.fulfillmentGroupOfferProcessor = fulfillmentGroupOfferProcessor;
    }

    @Override
    public PromotableItemFactory getPromotableItemFactory() {
        return promotableItemFactory;
    }

    @Override
    public void setPromotableItemFactory(PromotableItemFactory promotableItemFactory) {
        this.promotableItemFactory = promotableItemFactory;
    }

    @Override
    public OfferCode findOfferCodeById(Long id) {
        return offerCodeDao.readOfferCodeById(id);
    }

    @Override
    public List<OfferCode> findOfferCodesByIds(Collection<Long> ids) {
        return offerCodeDao.readOfferCodesByIds(ids);
    }

    @Override
    public OrderService getOrderService() {
        return orderService;
    }

    @Override
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Offer findOfferById(Long offerId) {
        return offerDao.readOfferById(offerId);
    }
}
