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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.n52.eventing.wv.JsonConfigured;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class EventTypesStore extends JsonConfigured implements InitializingBean {

    private final static String CONFIG_FILE = "/wv/event-types.json";
    private final static String CONFIG_DEFAULT_FILE = "/wv/event-types-default.json";
    private final String configFile;
    private final Map<Integer, String> typeMap = new HashMap<>();

    public EventTypesStore() {
        this(CONFIG_FILE);
    }

    public EventTypesStore(String configFileResource) {
        this.configFile = configFileResource;
    }

    @Override
    protected String getDefaultConfigFileName() {
        return CONFIG_DEFAULT_FILE;
    }

    public ArrayNode getEventTypes() {
        return (ArrayNode) this.getConfig().get("eventTypes");
    }

    public Map<Integer, String> getTypeMap() {
        return typeMap;
    }

    public Optional<String> getLabelForId(int id) {
        if (this.typeMap.containsKey(id)) {
            return Optional.of(this.typeMap.get(id));
        }

        return Optional.empty();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init(this.configFile);
        ArrayNode types = getEventTypes();
        types.forEach(t -> {
            if (t instanceof ObjectNode) {
                this.typeMap.put(Integer.valueOf(t.path("id").asText(), 10), t.path("label").asText());
            }
        });
    }

}
