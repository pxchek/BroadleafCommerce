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
package org.broadleafcommerce.common.web.util;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Code is largely copied from StackOverflow post made by David Rabinowitz with contributions
 * by others in the same thread.   Overrides all status setting methods and retains the status.
 * <br><br>
 *
 * http://stackoverflow.com/questions/1302072/how-can-i-get-the-http-status-code-out-of-a-servletresponse-in-a-servletfilter<br><br>
 *
 * This won't be needed with Servlet 3.0.<br><br>
 *
 * Addeded by bpolster.
 */
public class StatusExposingServletResponse extends HttpServletResponseWrapper {

    private int httpStatus=200;

    public StatusExposingServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void sendError(int sc) throws IOException {
        httpStatus = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        httpStatus = sc;
        super.sendError(sc, msg);
    }

    @Override
    public void setStatus(int sc) {
        httpStatus = sc;
        super.setStatus(sc);
    }

    @Override
    public void reset() {
        super.reset();
        this.httpStatus = SC_OK;
    }

    @Override
    public void setStatus(int status, String string) {
        super.setStatus(status, string);
        this.httpStatus = status;
    }

    public int getStatus() {
        return httpStatus;
    }

}
