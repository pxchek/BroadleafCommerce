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
package org.broadleafcommerce.core.util.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table(name = "BLC_CODE_TYPES")
@Inheritance(strategy=InheritanceType.JOINED)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
@Deprecated
public class CodeTypeImpl implements CodeType {

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "CodeTypeId", strategy = GenerationType.TABLE)
    @GenericGenerator(
        name="CodeTypeId",
        strategy="org.broadleafcommerce.common.persistence.IdOverrideTableGenerator",
        parameters = {
            @Parameter(name="segment_value", value="CodeTypeImpl"),
            @Parameter(name="entity_name", value="org.broadleafcommerce.core.util.domain.CodeTypeImpl")
        }
    )
    @Column(name = "CODE_ID")
    protected Long id;

    @Column(name = "CODE_TYPE", nullable=false)
    protected String codeType;

    @Column(name = "CODE_KEY", nullable=false)
    protected String key;

    @Column(name = "CODE_DESC")
    protected String description;

    @Column(name = "MODIFIABLE")
    protected Character modifiable;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getCodeType() {
        return codeType;
    }

    @Override
    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Boolean isModifiable() {
        if(modifiable == null)
            return null;
        return modifiable == 'Y' ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean getModifiable() {
        return isModifiable();
    }

    @Override
    public void setModifiable(Boolean modifiable) {
        if(modifiable == null) {
            this.modifiable = null;
        } else {
            this.modifiable = modifiable ? 'Y' : 'N';
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((codeType == null) ? 0 : codeType.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result
                + ((modifiable == null) ? 0 : modifiable.hashCode());
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
        CodeTypeImpl other = (CodeTypeImpl) obj;
        if (codeType == null) {
            if (other.codeType != null)
                return false;
        } else if (!codeType.equals(other.codeType))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (modifiable == null) {
            if (other.modifiable != null)
                return false;
        } else if (!modifiable.equals(other.modifiable))
            return false;
        return true;
    }
    
    
}
