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
package org.broadleafcommerce.core.workflow.state;

import org.broadleafcommerce.core.workflow.Activity;
import org.broadleafcommerce.core.workflow.ProcessContext;

import java.util.Map;

/**
 * Manages activity state for the current thread during workflow execution. Provides facility
 * for registering state and RollbackHandler instances, as well as initiating rollbacks of
 * previously registered state.
 *
 * @author Jeff Fischer
 */
public interface ActivityStateManager<T extends ProcessContext<?>> {

    /**
     * Register a RollbackHandler instance and some arbitrary state items with the
     * StateManager. In the event of a rollbackAllState() call, the StateManager will
     * execute all registered RollbackHandler instances. Note, Broadleaf does not try to wrap
     * the RollbackHandler execution in a database transaction. Therefore, if the RollbackHandler
     * implementation requires a database transaction (i.e. it's updating the database), then
     * the implementer must supply it. The easiest way to achieve this is to register the RollbackHandler
     * as a Spring bean and either use <aop> declaration in the app context xml, or use @Transactional
     * annotations in the implementation itself. Then, inject the RollbackHandler into your activity and
     * call registerState when appropriate.
     *
     * @param rollbackHandler A RollbackHandler instance that should be executed by the StateManager
     * @param stateItems Configuration items for the RollbackHandler (can be null)
     */
    public void registerState(RollbackHandler<T> rollbackHandler, Map<String, Object> stateItems);

    /**
     * Register a RollbackHandler instance and some arbitrary state items with the
     * StateManager. In the event of a rollbackAllState() call, the StateManager will
     * execute all registered RollbackHandler instances. Note, Broadleaf does not try to wrap
     * the RollbackHandler execution in a database transaction. Therefore, if the RollbackHandler
     * implementation requires a database transaction (i.e. it's updating the database), then
     * the implementer must supply it. The easiest way to achieve this is to register the RollbackHandler
     * as a Spring bean and either use <aop> declaration in the app context xml, or use @Transactional
     * annotations in the implementation itself. Then, inject the RollbackHandler into your activity and
     * call registerState when appropriate.
     *
     * @param activity the current activity associated with the RollbackHandler (can be null)
     * @param processContext the current ProcessContext associated with the activity (can be null)
     * @param rollbackHandler A RollbackHandler instance that should be executed by the StateManager
     * @param stateItems Configuration items for the RollbackHandler (can be null)
     */
    public void registerState(Activity<T> activity, T processContext, RollbackHandler<T> rollbackHandler, Map<String, Object> stateItems);

    /**
     * Register a RollbackHandler instance and some arbitrary state items with the
     * StateManager. Can be used in conjunction with rollbackRegionState() to limit the scope of a rollback.
     * Note, Broadleaf does not try to wrap the RollbackHandler execution in a database transaction. Therefore,
     * if the RollbackHandler implementation requires a database transaction (i.e. it's updating the database), then
     * the implementer must supply it. The easiest way to achieve this is to register the RollbackHandler
     * as a Spring bean and either use <aop> declaration in the app context xml, or use @Transactional
     * annotations in the implementation itself. Then, inject the RollbackHandler into your activity and
     * call registerState when appropriate.
     *
     * @param activity the current activity associated with the RollbackHandler (can be null)
     * @param processContext the current ProcessContext associated with the activity (can be null)
     * @param region Label this rollback handler with a particular name.
     * @param rollbackHandler A RollbackHandler instance that should be executed by the StateManager
     * @param stateItems Configuration items for the RollbackHandler (can be null)
     */
    public void registerState(Activity<T> activity, T processContext, String region, RollbackHandler<T> rollbackHandler, Map<String, Object> stateItems);

    /**
     * Cause the StateManager to call all registered RollbackHandlers
     *
     * @throws RollbackFailureException if the rollback fails for some reason
     */
    public void rollbackAllState() throws RollbackFailureException;

    /**
     * Cause the StateManager to call all registered RollbackHandlers in the specified region.
     *
     * @throws RollbackFailureException if the rollback fails for some reason
     */
    public void rollbackRegionState(String region) throws RollbackFailureException;

    /**
     * Remove all previously registered RollbackHandlers for the current workflow
     */
    public void clearAllState();

    /**
     * Remove all previously registered Rollbackhandlers for the current workflow labelled with the specified region
     *
     * @param region The region to which the scope of removal is limited
     */
    public void clearRegionState(String region);
}
