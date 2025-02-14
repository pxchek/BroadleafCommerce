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
package org.broadleafcommerce.core.payment.domain.secure;

import org.broadleafcommerce.common.encryption.EncryptionModule;
import org.broadleafcommerce.core.payment.service.SecureOrderPaymentService;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 
 * @author jfischer
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BLC_BANK_ACCOUNT_PAYMENT")
public class BankAccountPaymentImpl implements BankAccountPayment {

    private static final long serialVersionUID = 1L;

    /**
     * Rather than constructing directly, use {@link SecureOrderPaymentService#create(org.broadleafcommerce.core.payment.service.type.PaymentType)}
     * so that the appropriate {@link EncryptionModule} can be hooked up to this entity
     */
    protected BankAccountPaymentImpl() {
        //do not allow direct instantiation -- must at least be package private for bytecode instrumentation
        //this complies with JPA specification requirements for entity construction
    }

    @Transient
    protected EncryptionModule encryptionModule;

    @Id
    @GeneratedValue(generator = "BankPaymentId")
    @GenericGenerator(
            name="BankPaymentId",
            strategy="org.broadleafcommerce.common.persistence.IdOverrideTableGenerator",
            parameters = {
                @Parameter(name="segment_value", value="BankAccountPaymentImpl"),
                @Parameter(name="entity_name", value="org.broadleafcommerce.core.payment.domain.BankAccountPaymentInfoImpl")
            }
        )
    @Column(name = "PAYMENT_ID")
    protected Long id;

    @Column(name = "REFERENCE_NUMBER", nullable=false)
    @Index(name="BANKACCOUNT_INDEX", columnNames={"REFERENCE_NUMBER"})
    protected String referenceNumber;

    @Column(name = "ACCOUNT_NUMBER", nullable=false)
    protected String accountNumber;

    @Column(name = "ROUTING_NUMBER", nullable=false)
    protected String routingNumber;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getReferenceNumber() {
        return referenceNumber;
    }

    @Override
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    @Override
    public String getAccountNumber() {
        return encryptionModule.decrypt(accountNumber);
    }

    @Override
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = encryptionModule.encrypt(accountNumber);
    }

    @Override
    public String getRoutingNumber() {
        return encryptionModule.decrypt(routingNumber);
    }

    @Override
    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = encryptionModule.encrypt(routingNumber);
    }

    @Override
    public EncryptionModule getEncryptionModule() {
        return encryptionModule;
    }

    @Override
    public void setEncryptionModule(EncryptionModule encryptionModule) {
        this.encryptionModule = encryptionModule;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accountNumber == null) ? 0 : accountNumber.hashCode());
        result = prime * result + ((referenceNumber == null) ? 0 : referenceNumber.hashCode());
        result = prime * result + ((routingNumber == null) ? 0 : routingNumber.hashCode());
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
        BankAccountPaymentImpl other = (BankAccountPaymentImpl) obj;

        if (id != null && other.id != null) {
            return id.equals(other.id);
        }

        if (accountNumber == null) {
            if (other.accountNumber != null)
                return false;
        } else if (!accountNumber.equals(other.accountNumber))
            return false;
        if (referenceNumber == null) {
            if (other.referenceNumber != null)
                return false;
        } else if (!referenceNumber.equals(other.referenceNumber))
            return false;
        if (routingNumber == null) {
            if (other.routingNumber != null)
                return false;
        } else if (!routingNumber.equals(other.routingNumber))
            return false;
        return true;
    }

}
