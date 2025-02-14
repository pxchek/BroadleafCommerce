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
package org.broadleafcommerce.core.order.domain;

import org.broadleafcommerce.common.copy.MultiTenantCloneable;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.offer.domain.CandidateFulfillmentGroupOffer;
import org.broadleafcommerce.core.offer.domain.FulfillmentGroupAdjustment;
import org.broadleafcommerce.core.order.service.type.FulfillmentGroupStatusType;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.Phone;

import java.io.Serializable;
import java.util.List;

/**
 * This is the main entity used to hold fulfillment information about an Order. An Order can have
 * multiple FulfillmentGroups to support shipping items to multiple addresses along with fulfilling
 * items multiple ways (ship some overnight, deliver some with digital download). This constraint means
 * that a FulfillmentGroup is unique based on an Address and FulfillmentOption combination. This
 * also means that in the common case for Orders that are being delivered to a single Address and
 * a single way (shipping everything express; ie a single FulfillmentOption) then there will be
 * only 1 FulfillmentGroup for that Order.
 * 
 * @author Phillip Verheyden
 * @see {@link Order}, {@link FulfillmentOption}, {@link Address}, {@link FulfillmentGroupItem}
 */
public interface FulfillmentGroup extends Serializable, MultiTenantCloneable<FulfillmentGroup> {

    public Long getId();

    public void setId(Long id);

    public Order getOrder();

    public void setOrder(Order order);
    
    public void setSequence(Integer sequence);

    public Integer getSequence();

    public FulfillmentOption getFulfillmentOption();

    public void setFulfillmentOption(FulfillmentOption fulfillmentOption);

    public Address getAddress();

    public void setAddress(Address address);

    /**
     * @deprecated use {@link Address#getPhonePrimary()} instead.
     */
    @Deprecated
    public Phone getPhone();

    /**
     * @deprecated use {@link Address#getPhonePrimary()} instead
     * @param phone
     */
    @Deprecated
    public void setPhone(Phone phone);

    public List<FulfillmentGroupItem> getFulfillmentGroupItems();

    public void setFulfillmentGroupItems(List<FulfillmentGroupItem> fulfillmentGroupItems);

    public void addFulfillmentGroupItem(FulfillmentGroupItem fulfillmentGroupItem);

    /**
     * @deprecated Should use {@link #getFulfillmentOption()} instead
     * @see {@link FulfillmentOption}
     */
    @Deprecated
    public String getMethod();

    /**
     * @deprecated Should use {@link #setFulfillmentOption()} instead
     * @see {@link FulfillmentOption}
     */
    @Deprecated
    public void setMethod(String fulfillmentMethod);

    /**
     * Returns the retail price for this fulfillmentGroup.   The retail and sale concepts used 
     * for item pricing are not generally used with fulfillmentPricing but supported 
     * nonetheless.    Typically only a retail price would be set on a fulfillment group.
     * @return
     */
    public Money getRetailFulfillmentPrice();

    /**
     * Sets the retail price for this fulfillmentGroup.   
     * @param fulfillmentPrice
     */
    public void setRetailFulfillmentPrice(Money fulfillmentPrice);

    /**
     * Returns the sale price for this fulfillmentGroup.    
     * Typically this will be null or equal to the retailFulfillmentPrice
     * @return
     */
    public Money getSaleFulfillmentPrice();

    /**
     * Sets the sale price for this fulfillmentGroup.  Typically not used.
     * @see #setRetailFulfillmentPrice(Money)
     * @param fulfillmentPrice
     */
    public void setSaleFulfillmentPrice(Money fulfillmentPrice);

    /**
     * Gets the price to charge for this fulfillmentGroup.   Includes the effects of any adjustments such as those that 
     * might have been applied by the promotion engine (e.g. free shipping)
     * @return
     */
    public Money getFulfillmentPrice();

    /**
     * Sets the price to charge for this fulfillmentGroup.  Typically set internally by the Broadleaf pricing and
     * promotion engines.
     * @return
     */
    public void setFulfillmentPrice(Money fulfillmentPrice);

    /**
     * @deprecated - use {@link #getRetailFulfillmentPrice()} instead.   Deprecated as the price might be for other
     * fulfillment types such as PickUpAtStore fees or download fees.
     * @return
     */
    @Deprecated
    public Money getRetailShippingPrice();

    /**
     * @deprecated - use {@link #setRetailFulfillmentPrice(Money)} instead.
     * @return
     */
    @Deprecated
    public void setRetailShippingPrice(Money retailShippingPrice);

    /**
     * @deprecated - use {@link #getSaleFulfillmentPrice()} instead.
     * @return
     */
    @Deprecated
    public Money getSaleShippingPrice();

    /**
     * @deprecated - use {@link #setSaleFulfillmentPrice(Money)} instead.
     * @param saleShippingPrice
     */
    @Deprecated
    public void setSaleShippingPrice(Money saleShippingPrice);

    /**
     * @deprecated - use {@link #getFulfillmentPrice()} instead.
     * @return
     */
    @Deprecated
    public Money getShippingPrice();

    /**
     * @deprecated - use {@link #setRetailFulfillmentPrice(Money)} instead.
     * @param shippingPrice
     */
    @Deprecated
    public void setShippingPrice(Money shippingPrice);

    public String getReferenceNumber();

    public void setReferenceNumber(String referenceNumber);

    public FulfillmentType getType();

    void setType(FulfillmentType type);

    public List<CandidateFulfillmentGroupOffer> getCandidateFulfillmentGroupOffers();

    public void setCandidateFulfillmentGroupOffer(List<CandidateFulfillmentGroupOffer> candidateOffers);

    public void addCandidateFulfillmentGroupOffer(CandidateFulfillmentGroupOffer candidateOffer);

    public void removeAllCandidateOffers();

    public List<FulfillmentGroupAdjustment> getFulfillmentGroupAdjustments();

    /**
     * Returns a List of FulfillmentGroupAdjustment originating from FUTURE_CREDIT Offers.
     * 
     * See {@link org.broadleafcommerce.core.offer.domain.Offer#getAdjustmentType()} for more info on future credit
     *
     * @return a List of FulfillmentGroupAdjustment
     */
    List<FulfillmentGroupAdjustment> getFutureCreditFulfillmentGroupAdjustments();

    public void setFulfillmentGroupAdjustments(List<FulfillmentGroupAdjustment> fulfillmentGroupAdjustments);

    public void removeAllAdjustments();
    
    /**
     * Gets a list of TaxDetail objects, which are taxes that apply directly to this fulfillment group. 
     * An example of a such a tax would be a shipping tax.
     * 
     * @return a list of taxes that apply to this fulfillment group
     */
    public List<TaxDetail> getTaxes();

    /**
     * Gets the list of TaxDetail objects, which are taxes that apply directly to this fulfillment group. 
     * An example of a such a tax would be a shipping tax.
     * 
     * @param taxes the list of taxes on this fulfillment group
     */
    public void setTaxes(List<TaxDetail> taxes);

    /**
     * Gets the total tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group items, fees, and taxes on this fulfillment group itself (such as a shipping tax).
     * This total is calculated in the TotalActivity stage of the pricing workflow.
     *
     * @return the total tax for the fulfillment group
     */
    public Money getTotalTax();

    /**
     * Sets the total tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group items, fees, and taxes on this fulfillment group itself (such as a shipping tax).
     * This total should only be set during the TotalActivity stage of the pricing workflow.
     *
     * @param the total tax for this fulfillment group
     */
    public void setTotalTax(Money totalTax);
    
    /**
     * Gets the total item tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group items. This total is calculated in the TotalActivity stage of the pricing workflow.
     *
     * @return the total tax for this fulfillment group
     */
    public Money getTotalItemTax();

    /**
     * Sets the total item tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group items. This total should only be set during the TotalActivity stage of the pricing workflow.
     *
     * @param the total tax for this fulfillment group
     */
    public void setTotalItemTax(Money totalItemTax);
    
    /**
     * Gets the total fee tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group fees. This total is calculated in the TotalActivity stage of the pricing workflow.
     *
     * @return the total tax for this fulfillment group
     */
    public Money getTotalFeeTax();

    /**
     * Sets the total fee tax for this fulfillment group, which is the sum of the taxes on all fulfillment 
     * group fees. This total should only be set during the TotalActivity stage of the pricing workflow.
     *
     * @param the total tax for this fulfillment group
     */
    public void setTotalFeeTax(Money totalFeeTax);
    
    /**
     * Gets the total fulfillment group tax for this fulfillment group, which is the sum of the taxes 
     * on this fulfillment group itself (such as a shipping tax) only. It does not include the taxes on 
     * items or fees in this fulfillment group. This total is calculated in the TotalActivity stage of the pricing workflow.
     *
     * @return the total tax for this fulfillment group
     */
    public Money getTotalFulfillmentGroupTax();

    /**
     * Sets the total fulfillment group tax for this fulfillment group, which is the sum of the taxes 
     * on this fulfillment group itself (such as a shipping tax) only. It does not include the taxes on 
     * items or fees in this fulfillment group. This total should only be set during the TotalActivity stage of the pricing workflow.
     *
     * @param the total tax for this fulfillment group
     */
    public void setTotalFulfillmentGroupTax(Money totalFulfillmentGroupTax);

    public String getDeliveryInstruction();

    public void setDeliveryInstruction(String deliveryInstruction);

    public PersonalMessage getPersonalMessage();

    public void setPersonalMessage(PersonalMessage personalMessage);

    public boolean isPrimary();

    public void setPrimary(boolean primary);

    public Money getMerchandiseTotal();

    public void setMerchandiseTotal(Money merchandiseTotal);

    public Money getTotal();

    public void setTotal(Money orderTotal);

    public FulfillmentGroupStatusType getStatus();

    public void setStatus(FulfillmentGroupStatusType status);
    
    public List<FulfillmentGroupFee> getFulfillmentGroupFees();

    public void setFulfillmentGroupFees(List<FulfillmentGroupFee> fulfillmentGroupFees);

    public void addFulfillmentGroupFee(FulfillmentGroupFee fulfillmentGroupFee);

    public void removeAllFulfillmentGroupFees();

    public Boolean isShippingPriceTaxable();

    public void setIsShippingPriceTaxable(Boolean isShippingPriceTaxable);

    /**
     * @deprecated Should use {@link #getFulfillmentOption()} instead
     * @see {@link FulfillmentOption}
     */
    @Deprecated
    public String getService();

    /**
     * @deprecated Should use {@link #setFulfillmentOption()} instead
     * @see {@link FulfillmentOption}
     */
    @Deprecated
    public void setService(String service);
    
    public List<DiscreteOrderItem> getDiscreteOrderItems();
    
    public Money getFulfillmentGroupAdjustmentsValue();

    /**
     * Returns the discount value of the applied future credit offers for this fulfillment group.
     * 
     * See {@link org.broadleafcommerce.core.offer.domain.Offer#getAdjustmentType()} for more info on future credit
     *
     * @return the discount value of the applied future credit offers for this fulfillment group
     */
    Money getFutureCreditFulfillmentGroupAdjustmentsValue();

    /**
     * @return whether or not to override the shipping calculation
     */
    public Boolean getShippingOverride();

    /**
     * Sets whether or not to override the shipping calculation
     * 
     * @param shippingOverride
     */
    public void setShippingOverride(Boolean shippingOverride);
    
}
