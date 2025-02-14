/*-
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.server.security.domain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.extensibility.jpa.copy.DirectCopyTransform;
import org.broadleafcommerce.common.extensibility.jpa.copy.DirectCopyTransformMember;
import org.broadleafcommerce.common.extensibility.jpa.copy.DirectCopyTransformTypes;
import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.broadleafcommerce.common.presentation.AdminPresentationCollection;
import org.broadleafcommerce.common.presentation.AdminPresentationOperationTypes;
import org.broadleafcommerce.common.presentation.client.AddMethodType;
import org.broadleafcommerce.common.presentation.client.OperationType;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.common.presentation.client.VisibilityEnum;
import org.broadleafcommerce.openadmin.server.security.service.type.PermissionType;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * 
 * @author jfischer
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BLC_ADMIN_PERMISSION")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blAdminSecurity")
@AdminPresentationClass(friendlyName = "AdminPermissionImpl_baseAdminPermission")
@DirectCopyTransform({
        @DirectCopyTransformMember(templateTokens = DirectCopyTransformTypes.MULTITENANT_ADMINPERMISSION)
})
public class AdminPermissionImpl implements AdminPermission {

    private static final Log LOG = LogFactory.getLog(AdminPermissionImpl.class);
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "AdminPermissionId")
    @GenericGenerator(
        name="AdminPermissionId",
        strategy="org.broadleafcommerce.common.persistence.IdOverrideTableGenerator",
        parameters = {
            @Parameter(name="segment_value", value="AdminPermissionImpl"),
            @Parameter(name="entity_name", value="org.broadleafcommerce.openadmin.server.security.domain.AdminPermissionImpl")
        }
    )
    @Column(name = "ADMIN_PERMISSION_ID")
    @AdminPresentation(friendlyName = "AdminPermissionImpl_Admin_Permission_ID", group = "AdminPermissionImpl_Primary_Key", visibility = VisibilityEnum.HIDDEN_ALL)
    protected Long id;

    @Column(name = "NAME", nullable=false)
    @AdminPresentation(friendlyName = "AdminPermissionImpl_Name", order = 3000, group = "AdminPermissionImpl_Permission")
    protected String name;

    @Column(name = "PERMISSION_TYPE", nullable=false)
    @AdminPresentation(friendlyName = "AdminPermissionImpl_Permission_Type", order = 3000,
            group = "AdminPermissionImpl_Permission", gridOrder = 2000,
            fieldType = SupportedFieldType.BROADLEAF_ENUMERATION,
            broadleafEnumeration = "org.broadleafcommerce.openadmin.server.security.service.type.PermissionType")
    protected String type;

    @Column(name = "DESCRIPTION", nullable=false)
    @AdminPresentation(friendlyName = "AdminPermissionImpl_Description", order = 1000, group = "AdminPermissionImpl_Permission",
            prominent = true, gridOrder = 2000)
    protected String description;
    
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminRoleImpl.class)
    @JoinTable(name = "BLC_ADMIN_ROLE_PERMISSION_XREF", joinColumns = @JoinColumn(name = "ADMIN_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"), inverseJoinColumns = @JoinColumn(name = "ADMIN_ROLE_ID", referencedColumnName = "ADMIN_ROLE_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blAdminSecurityVolatile")
    @BatchSize(size = 50)
    @AdminPresentation(excluded = true)
    protected Set<AdminRole> allRoles= new HashSet<AdminRole>();

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminUserImpl.class)
    @JoinTable(name = "BLC_ADMIN_USER_PERMISSION_XREF", joinColumns = @JoinColumn(name = "ADMIN_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"), inverseJoinColumns = @JoinColumn(name = "ADMIN_USER_ID", referencedColumnName = "ADMIN_USER_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blAdminSecurityVolatile")
    @BatchSize(size = 50)
    @AdminPresentation(excluded = true)
    protected Set<AdminUser> allUsers= new HashSet<AdminUser>();

    @OneToMany(mappedBy = "adminPermission", targetEntity = AdminPermissionQualifiedEntityImpl.class, cascade = {CascadeType.ALL}, orphanRemoval=true)
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL})
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blAdminSecurity")
    @BatchSize(size = 50)
    @AdminPresentationCollection(addType = AddMethodType.LOOKUP, friendlyName = "entityPermissionsTitle",
            order = 2000)
    protected List<AdminPermissionQualifiedEntity> qualifiedEntities = new ArrayList<AdminPermissionQualifiedEntity>();

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminPermissionImpl.class)
    @JoinTable(name = "BLC_ADMIN_PERMISSION_XREF", joinColumns = @JoinColumn(name = "ADMIN_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"), inverseJoinColumns = @JoinColumn(name = "CHILD_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blAdminSecurity")
    @BatchSize(size = 50)
    @AdminPresentationCollection(addType = AddMethodType.LOOKUP, friendlyName = "childPermissionsTitle",
            order = 1000,
            manyToField = "allParentPermissions",
            operationTypes = @AdminPresentationOperationTypes(removeType = OperationType.NONDESTRUCTIVEREMOVE))
    protected List<AdminPermission> allChildPermissions = new ArrayList<AdminPermission>();
    
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminPermissionImpl.class)
    @JoinTable(name = "BLC_ADMIN_PERMISSION_XREF", joinColumns = @JoinColumn(name = "CHILD_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"), inverseJoinColumns = @JoinColumn(name = "ADMIN_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "blAdminSecurity")
    @BatchSize(size = 50)
    protected List<AdminPermission> allParentPermissions = new ArrayList<AdminPermission>();

    @Column(name = "IS_FRIENDLY")
    @AdminPresentation(friendlyName = "AdminPermissionImpl_Is_Friendly",
            order = 4000, group = "AdminPermissionImpl_Permission")
    protected Boolean isFriendly = Boolean.FALSE;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public Set<AdminRole> getAllRoles() {
        return allRoles;
    }

    @Override
    public void setAllRoles(Set<AdminRole> allRoles) {
        this.allRoles = allRoles;
    }

    @Override
    public PermissionType getType() {
        return PermissionType.getInstance(type);
    }

    @Override
    public void setType(PermissionType type) {
        if (type != null) {
            this.type = type.getType();
        }
    }

    @Override
    public List<AdminPermissionQualifiedEntity> getQualifiedEntities() {
        return qualifiedEntities;
    }

    @Override
    public void setQualifiedEntities(List<AdminPermissionQualifiedEntity> qualifiedEntities) {
        this.qualifiedEntities = qualifiedEntities;
    }

    @Override
    public Set<AdminUser> getAllUsers() {
        return allUsers;
    }

    @Override
    public void setAllUsers(Set<AdminUser> allUsers) {
        this.allUsers = allUsers;
    }

    public void checkCloneable(AdminPermission adminPermission) throws CloneNotSupportedException, SecurityException, NoSuchMethodException {
        Method cloneMethod = adminPermission.getClass().getMethod("clone", new Class[]{});
        if (cloneMethod.getDeclaringClass().getName().startsWith("org.broadleafcommerce") && !adminPermission.getClass().getName().startsWith("org.broadleafcommerce")) {
            //subclass is not implementing the clone method
            throw new CloneNotSupportedException("Custom extensions and implementations should implement clone.");
        }
    }

    @Override
    public AdminPermission clone() {
        AdminPermission clone;
        try {
            clone = (AdminPermission) Class.forName(this.getClass().getName()).newInstance();
            try {
                checkCloneable(clone);
            } catch (CloneNotSupportedException e) {
                LOG.warn("Clone implementation missing in inheritance hierarchy outside of Broadleaf: " + clone.getClass().getName(), e);
            }
            clone.setId(id);
            clone.setName(name);
            clone.setType(getType());
            clone.setDescription(description);

            //don't clone the allUsers collection, as it would cause a recursion
            //don't clone the allRoles collection, as it would cause a recursion

            if (qualifiedEntities != null) {
                for (AdminPermissionQualifiedEntity qualifiedEntity : qualifiedEntities) {
                    AdminPermissionQualifiedEntity qualifiedEntityClone = qualifiedEntity.clone();
                    qualifiedEntityClone.setAdminPermission(clone);
                    clone.getQualifiedEntities().add(qualifiedEntityClone);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return clone;
    }

    @Override
    public List<AdminPermission> getAllChildPermissions() {
        List<AdminPermission> childPermissions = new ArrayList<>();
        for (AdminPermission permission : allChildPermissions) {
            if (permission.isFriendly()) {
                childPermissions.addAll(permission.getAllChildPermissions());
            } else {
                childPermissions.add(permission);
            }
        }
        return childPermissions;
    }

    @Override
    public List<AdminPermission> getAllParentPermissions() {
        return allParentPermissions;
    }

    @Override
    public Boolean isFriendly() {
        if(isFriendly == null) {
            return false;
        }
        return isFriendly;
    }
}
