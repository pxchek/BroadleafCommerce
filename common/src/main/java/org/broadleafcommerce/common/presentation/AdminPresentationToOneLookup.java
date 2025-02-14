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
package org.broadleafcommerce.common.presentation;

import org.broadleafcommerce.common.presentation.client.LookupType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jeff Fischer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface AdminPresentationToOneLookup {

    /**
     * <p>Optional - only required if the display property is other than "name"</p>
     *
     * <p>Specify the property on a lookup class that should be used as the value to display to the user in
     * a form in the admin tool UI</p>
     *
     * @return the property on the lookup class containing the displayable value
     */
    String lookupDisplayProperty() default "";

    /**
     * <p>Optional - only required if you need to specially handle crud operations for this
     * specific collection on the server</p>
     *
     * <p>Custom string values that will be passed to the server during Read and Inspect operations on the
     * entity lookup. This allows for the creation of a custom persistence handler to handle both
     * inspect and fetch phase operations. Presumably, one could use this to
     * somehow filter the list of records shown when the user interacts with the lookup widget in the
     * admin UI.</p>
     *
     * @return the custom string array to pass to the server during CRUD operations
     */
    String[] customCriteria() default {};

    /**
     * <p>Optional - only required if you want to make the field ignore caching</p>
     *
     * <p>Explicitly specify whether or not this field will use server-side
     * caching during inspection</p>
     *
     * @return whether or not this field uses caching
     */
    boolean useServerSideInspectionCache() default true;
    
    /**
     * <p>Optional - only required if you want to configure the lookup
     * to be driven by a prepopulated dropdown instead of the standard
     * lookup type, which is modal based.</p>
     *
     * <p>Define whether or not the lookup type for this field should be
     * handled through a modal or through a dropdown</p>
     *
     * @return the item is looked up via a modal or dropdown
     */
    LookupType lookupType() default LookupType.STANDARD;
    
    
    /**
     * <p>Optional - by setting this value to true, the admin will identify
     * the properties that are inside the target of this to-one field. </p>
     * 
     * <p>Typically, this is done if you want to expose a certain field as an
     * AdminPresentationToOneLookup but also allow filtering on a property that
     * resides inside of the target of this lookup.</p>
     * 
     * @return whether or not to force population of the child properties
     */
    boolean forcePopulateChildProperties() default false;
    
    /**
     * <p>Optional - by setting this value to true, the admin will utilize a typeahead
     * based lookup to search for the associated entity. When the value is set to
     * false, the admin will utilize a model that can be used to
     * search to the server for the associated entity.</p>
     * 
     * @return whether or not to enable the typeahead lookup feature for this field
     */
    boolean enableTypeaheadLookup() default false;

}
