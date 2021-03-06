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
package org.n52.eventing.wv.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.n52.eventing.rest.CustomResourceDefinitions;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class WvCustomResources implements CustomResourceDefinitions {

    public static final String GROUPS_RESOURCE = "groups";

    public static final String USERS_RESOURCE = "users";

    public static final String EVENTTRIGGERS_RESOURCE = "eventTriggers";

    public static final String RULES_RESOURCE = "rules";

    public static final String NOTIFICATIONLEVELS_RESOURCE = "notificationLevels";

    public static final String EVENTTYPE_RESOURCE = "eventTypes";

    @Override
    public List<String> getCustomResources() {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, GROUPS_RESOURCE, USERS_RESOURCE, EVENTTRIGGERS_RESOURCE, RULES_RESOURCE, NOTIFICATIONLEVELS_RESOURCE, EVENTTYPE_RESOURCE);
        return result;
    }

}
