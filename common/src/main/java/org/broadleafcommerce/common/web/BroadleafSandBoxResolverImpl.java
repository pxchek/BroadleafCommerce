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
package org.broadleafcommerce.common.web;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.crossapp.service.CrossAppAuthService;
import org.broadleafcommerce.common.sandbox.dao.SandBoxDao;
import org.broadleafcommerce.common.sandbox.domain.SandBox;
import org.broadleafcommerce.common.sandbox.domain.SandBoxType;
import org.broadleafcommerce.common.site.domain.Site;
import org.broadleafcommerce.common.time.FixedTimeSource;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.util.BLCRequestUtils;
import org.broadleafcommerce.common.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Responsible for determining the SandBox to use for the current request. 
 * SandBox's are used to store a user's changes to products, content-items, etc. 
 * until they are ready to be pushed to production.  
 * 
 * If a request is being served with a SandBox parameter, it indicates that the user
 * wants to see the site as if their changes were applied.
 *
 * @author bpolster
 */
@Component("blSandBoxResolver")
public class BroadleafSandBoxResolverImpl implements BroadleafSandBoxResolver  {
    private final Log LOG = LogFactory.getLog(BroadleafSandBoxResolverImpl.class);
    
    /**
     * Property used to disable sandbox mode.   Some implementations will want to
     * turn off sandboxes in production.
     */
    protected Boolean sandBoxPreviewEnabled = true;
    
    // Request Parameters and Attributes for Sandbox Mode properties - mostly values to manage dates.
    private static String SANDBOX_DATE_TIME_VAR = "blSandboxDateTime";
    private static final FastDateFormat CONTENT_DATE_FORMATTER = FastDateFormat.getInstance("yyyyMMddHHmm");
    private static final FastDateFormat CONTENT_DATE_DISPLAY_FORMATTER = FastDateFormat.getInstance("MM/dd/yyyy");
    private static final FastDateFormat CONTENT_DATE_DISPLAY_HOURS_FORMATTER = FastDateFormat.getInstance("h");
    private static final FastDateFormat CONTENT_DATE_DISPLAY_MINUTES_FORMATTER = FastDateFormat.getInstance("mm");
    private static final FastDateFormat CONTENT_DATE_PARSE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy hh:mm aa");
    private static String SANDBOX_DATE_TIME_RIBBON_OVERRIDE_PARAM = "blSandboxDateTimeRibbonOverride";
    private static final String SANDBOX_DISPLAY_DATE_TIME_DATE_PARAM = "blSandboxDisplayDateTimeDate";
    private static final String SANDBOX_DISPLAY_DATE_TIME_HOURS_PARAM = "blSandboxDisplayDateTimeHours";
    private static final String SANDBOX_DISPLAY_DATE_TIME_MINUTES_PARAM = "blSandboxDisplayDateTimeMinutes";
    private static final String SANDBOX_DISPLAY_DATE_TIME_AMPM_PARAM = "blSandboxDisplayDateTimeAMPM";

    /**
     * Request attribute to store the current sandbox
     */
    public static String SANDBOX_VAR = "blSandbox";
    public static final String CLIENT_TIMEZONE = "clientTimezone";

    @Resource(name = "blSandBoxDao")
    private SandBoxDao sandBoxDao;

    @Autowired(required = false)
    @Qualifier("blCrossAppAuthService")
    protected CrossAppAuthService crossAppAuthService;
    
    /**
     * Determines the current sandbox based on other parameters on the request such as
     * the blSandBoxId parameters.    
     * 
     * If the {@link #getSandBoxPreviewEnabled()}, then this method will not return a user
     * SandBox. 
     * 
     */
    @Override
    public SandBox resolveSandBox(HttpServletRequest request, Site site) {
        return resolveSandBox(new ServletWebRequest(request), site);
    }
    
    @Override
    public SandBox resolveSandBox(WebRequest request, Site site) {
        Long previousSandBoxId = null;
        if (BLCRequestUtils.isOKtoUseSession(request)) {
            previousSandBoxId = (Long) request.getAttribute(SANDBOX_ID_VAR, WebRequest.SCOPE_SESSION);
            // set a web request to scope session
            if (request.getParameter(BroadleafIncludeMyChangesResolver.INCLUDE_MY_CHANGES_VAR) != null) {
                request.setAttribute(BroadleafIncludeMyChangesResolver.INCLUDE_MY_CHANGES_VAR, Boolean.parseBoolean(request.getParameter("blIncludeMyChanges")), WebRequest.SCOPE_SESSION);
            }
        }
        SandBox currentSandbox = null;
        if (!sandBoxPreviewEnabled) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sandbox preview disabled. Setting sandbox to production");
            }
            request.setAttribute(SANDBOX_VAR, currentSandbox, WebRequest.SCOPE_REQUEST);
        } else if (crossAppAuthService != null && !crossAppAuthService.isAuthedFromAdmin()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sandbox preview attempted without authentication");
            }
            request.setAttribute(SANDBOX_VAR, currentSandbox, WebRequest.SCOPE_REQUEST);
        } else if (crossAppAuthService != null && crossAppAuthService.hasCsrPermission()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sandbox preview attempted in CSR mode");
            }
            request.setAttribute(SANDBOX_VAR, currentSandbox, WebRequest.SCOPE_REQUEST);
        } else {
            Long sandboxId = null;
            // Clear the sandBox - second parameter is to support legacy implementations.
            if ((request.getParameter("blClearSandBox") == null) && (request.getParameter("blSandboxDateTimeRibbonProduction") == null)) {
                sandboxId = lookupSandboxId(request);
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing sandbox from session.");
                }
                if (BLCRequestUtils.isOKtoUseSession(request)) {
                    request.removeAttribute(SANDBOX_DATE_TIME_VAR, WebRequest.SCOPE_SESSION);
                    request.removeAttribute(SANDBOX_ID_VAR, WebRequest.SCOPE_SESSION);
                    request.removeAttribute(BroadleafIncludeMyChangesResolver.INCLUDE_MY_CHANGES_VAR, WebRequest.SCOPE_SESSION);
                }
                SystemTime.resetLocalTimeSource();
            }
            if (sandboxId != null) {
                if (previousSandBoxId != null && !previousSandBoxId.equals(sandboxId)) {
                    request.setAttribute(BroadleafRequestProcessor.REPROCESS_PARAM_NAME, true, WebRequest.SCOPE_REQUEST);
                }

                currentSandbox = sandBoxDao.retrieve(sandboxId);
                request.setAttribute(SANDBOX_VAR, currentSandbox, WebRequest.SCOPE_REQUEST);
                if (currentSandbox != null && !SandBoxType.PRODUCTION.equals(currentSandbox.getSandBoxType())) {
                    setContentTime(request);
                }
            }

//            if (currentSandbox == null && site != null) {
//                currentSandbox = site.getProductionSandbox();
//            }
        }

        if (LOG.isTraceEnabled()) {
            if (currentSandbox != null) {
                LOG.trace("Serving request using sandbox: " + currentSandbox);
            } else {
                LOG.trace("Serving request without a sandbox.");
            }
        }

        Date currentSystemDateTime = SystemTime.asDate(true);
        Calendar sandboxDateTimeCalendar = Calendar.getInstance();
        sandboxDateTimeCalendar.setTime(currentSystemDateTime);
        request.setAttribute(SANDBOX_DISPLAY_DATE_TIME_DATE_PARAM, CONTENT_DATE_DISPLAY_FORMATTER.format(currentSystemDateTime), WebRequest.SCOPE_REQUEST);
        request.setAttribute(SANDBOX_DISPLAY_DATE_TIME_HOURS_PARAM, CONTENT_DATE_DISPLAY_HOURS_FORMATTER.format(currentSystemDateTime), WebRequest.SCOPE_REQUEST);
        request.setAttribute(SANDBOX_DISPLAY_DATE_TIME_MINUTES_PARAM, CONTENT_DATE_DISPLAY_MINUTES_FORMATTER.format(currentSystemDateTime), WebRequest.SCOPE_REQUEST);
        request.setAttribute(SANDBOX_DISPLAY_DATE_TIME_AMPM_PARAM, sandboxDateTimeCalendar.get(Calendar.AM_PM), WebRequest.SCOPE_REQUEST);
        return currentSandbox;
    }

    /**
     * If another filter has already set the language as a request attribute, that will be honored.
     * Otherwise, the request parameter is checked followed by the session attribute.
     *
     * @param request
     * @return
     */
    private Long lookupSandboxId(WebRequest request) {
        String sandboxIdStr = request.getParameter(SANDBOX_ID_VAR);
        Long sandboxId = null;

        if (sandboxIdStr != null) {
            try {
                sandboxId = Long.valueOf(sandboxIdStr);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("SandboxId found on request " + sandboxId);
                }
            } catch (NumberFormatException nfe) {
                LOG.warn("blcSandboxId parameter could not be converted into a Long", nfe);
            }
        }

        if (BLCRequestUtils.isOKtoUseSession(request)) {
            if (sandboxId == null) {
                // check the session            
                sandboxId = (Long) request.getAttribute(SANDBOX_ID_VAR, WebRequest.SCOPE_SESSION);

                if (LOG.isTraceEnabled()) {
                    if (sandboxId != null) {
                        LOG.trace("SandboxId found in session " + sandboxId);
                    }
                }
            } else {
                request.setAttribute(SANDBOX_ID_VAR, sandboxId, WebRequest.SCOPE_SESSION);
            }
        }
        return sandboxId;
    }

    /**
     * Allows a user in SandBox mode to override the current time and date being used by the system.
     * 
     * @param request
     */
    private void setContentTime(WebRequest request) {
        String sandboxDateTimeParam = request.getParameter(SANDBOX_DATE_TIME_VAR);
        if (!sandBoxPreviewEnabled) {
            sandboxDateTimeParam = null;
        }
        Date overrideTime = null;

        try {
            if (request.getParameter(SANDBOX_DATE_TIME_RIBBON_OVERRIDE_PARAM) != null) {
                overrideTime = readDateFromRequest(request);
            } else if (sandboxDateTimeParam != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Setting date/time using " + StringUtil.sanitize(sandboxDateTimeParam));
                }
                TimeZone clientTimeZone = TimeZone.getTimeZone(sandboxDateTimeParam.substring(12));

                FastDateFormat parser = FastDateFormat.getInstance("yyyyMMddHHmm", clientTimeZone);

                overrideTime = parser.parse(sandboxDateTimeParam);

                BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();

                if(BLCRequestUtils.isOKtoUseSession(brc.getWebRequest())) {
                    HttpSession session = brc.getRequest().getSession();

                    session.setAttribute(CLIENT_TIMEZONE, clientTimeZone);
                }
            }
        } catch (ParseException e) {
            LOG.debug(e);
        }

        if (BLCRequestUtils.isOKtoUseSession(request)) {
            if (overrideTime == null) {
                overrideTime = (Date) request.getAttribute(SANDBOX_DATE_TIME_VAR, WebRequest.SCOPE_SESSION);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Setting date-time for sandbox mode to " + overrideTime + " for sandboxDateTimeParam = " + StringUtil.sanitize(sandboxDateTimeParam));
                }
                request.setAttribute(SANDBOX_DATE_TIME_VAR, overrideTime, WebRequest.SCOPE_SESSION);
            }
        }

        if (overrideTime != null) {
            FixedTimeSource ft = new FixedTimeSource(overrideTime.getTime());
            SystemTime.setLocalTimeSource(ft);
        } else {
            SystemTime.resetLocalTimeSource();
        }
    }

    private Date readDateFromRequest(WebRequest request) throws ParseException {
        String date = request.getParameter(SANDBOX_DISPLAY_DATE_TIME_DATE_PARAM);
        String minutes = request.getParameter(SANDBOX_DISPLAY_DATE_TIME_MINUTES_PARAM);
        String hours = request.getParameter(SANDBOX_DISPLAY_DATE_TIME_HOURS_PARAM);
        String ampm = request.getParameter(SANDBOX_DISPLAY_DATE_TIME_AMPM_PARAM);

        if (StringUtils.isEmpty(minutes)) {
            minutes = Integer.toString(SystemTime.asCalendar().get(Calendar.MINUTE));
        }

        if (StringUtils.isEmpty(hours)) {
            hours = Integer.toString(SystemTime.asCalendar().get(Calendar.HOUR_OF_DAY));
        }

        String dateString = date + " " + hours + ":" + minutes + " " + ampm;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting date/time using " + StringUtil.sanitize(dateString));
        }

        Date parsedDate = CONTENT_DATE_PARSE_FORMAT.parse(dateString);
        return parsedDate;
    }
    

    /**
     * Sets whether or not the site can be viewed in preview mode.  
     * @return
     */
    public Boolean getSandBoxPreviewEnabled() {
        return sandBoxPreviewEnabled;
    }

    public void setSandBoxPreviewEnabled(Boolean sandBoxPreviewEnabled) {
        this.sandBoxPreviewEnabled = sandBoxPreviewEnabled;
    }
 
}
