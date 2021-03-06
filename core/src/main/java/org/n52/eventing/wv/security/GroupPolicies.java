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
package org.n52.eventing.wv.security;

import org.n52.eventing.wv.JsonConfigured;
import java.util.Set;
import java.util.stream.Collectors;
import org.n52.eventing.wv.model.Group;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class GroupPolicies extends JsonConfigured {

    private final static String CONFIG_FILE = "/wv/group-policies.json";
    private final static String CONFIG_DEFAULT_FILE = "/wv/group-policies-default.json";

    public GroupPolicies() {
        this(CONFIG_FILE);
    }

    public GroupPolicies(String configFileResource) {
        init(configFileResource);
    }

    @Override
    protected String getDefaultConfigFileName() {
        return CONFIG_DEFAULT_FILE;
    }

    public Set<String> getAdminGroupNames() {
        return readStringArray("adminGroupNames");
    };

    public Set<String> getEditorGroupNames() {
        return readStringArray("editorGroupNames");
    };

    public Set<Integer> getRestrictedSeriesIds() {
        return readIntegerArray("restrictedSeriesIds");
    };

    public String getAdminSuffix() {
        return readStringProperty("adminSuffix").orElse("_admin");
    }

    public String getGroupPrefix() {
        return readStringProperty("groupPrefix").orElse("sensorweb-");
    }

    public Set<Group> filterGroups(Set<Group> groups) {
        return groups.stream()
                .filter(g -> isSensorWebGroup(g))
                .collect(Collectors.toSet());
    }

    public boolean isSensorWebGroup(Group g) {
        return g.getName().startsWith(this.getGroupPrefix());
    }

}
