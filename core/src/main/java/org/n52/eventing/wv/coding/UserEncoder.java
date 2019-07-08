/*
 * Copyright (C) 2016 - 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.eventing.wv.coding;

import java.util.stream.Collectors;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.rest.WvCustomResources;
import org.n52.eventing.wv.view.UserView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class UserEncoder extends ModelEncoder<WvUser, UserView> {

    public UserEncoder() {
        super();
    }

    public UserEncoder(Context context) {
        super(context);
    }

    @Override
    public UserView encode(WvUser model) throws IllegalStateException {
        if (model == null) {
            throw new IllegalStateException("source cannot be null");
        }

        UserView result = new UserView();

        result.setId(model.getId());

        if (getContext() != null && getContext().getBaseUrl() != null) {
            result.setHref(String.format("%s/%s/%s",
                getContext().getBaseUrl(),
                    WvCustomResources.USERS_RESOURCE,
                model.getId()));
        }
//        else {
            result.setScreenName(model.getName());
            result.setFirstName(model.getFirstName());
            result.setLastName(model.getLastName());
            result.setEmail(model.getEmail());
            if (model.isAdmin()) {
                result.setAdmin(model.isAdmin());
            }

            Context refContext = new Context(getContext() != null ? getContext().getBaseUrl() : null, false, true);

            GroupEncoder encoder = new GroupEncoder(refContext);
            result.setGroups(model.getGroups().stream()
                    .map(g -> encoder.encode(g))
                    .collect(Collectors.toSet())
            );
//        }

        return result;
    }

}
