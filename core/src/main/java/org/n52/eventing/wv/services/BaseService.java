/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.eventing.wv.services;

import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.UserSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public abstract class BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(BaseService.class);


    private UserSecurityService userSecurityService;

    @Autowired
    public final void setUserSecurityService(UserSecurityService userSecurityService) {
        this.userSecurityService = userSecurityService;
    }

    protected int parseId(String id) {
        try {
            return Integer.parseInt(id);
        }
        catch (NumberFormatException e) {
            LOG.warn("NumberFormatException: {}", e.getMessage());
            throw new NumberFormatException("Invalid ID provided. IDs must be integers");
        }
    }


    protected WvUser resolveUser(AccessRights accessRights) throws NotAuthenticatedException {
        WvUser user = userSecurityService.resolveCurrentWvUser();

        if (user == null) {
            throw new NotAuthenticatedException("Could not resolve the user from user services");
        }

        return accessRights.enhanceUser(user);
    }

}
