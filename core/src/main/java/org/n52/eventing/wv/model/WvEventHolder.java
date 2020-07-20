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
package org.n52.eventing.wv.model;

import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.n52.eventing.rest.model.EventHolder;
import org.n52.eventing.rest.model.impl.SubscriptionImpl;
import org.n52.subverse.delivery.Streamable;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class WvEventHolder implements EventHolder {

    private final String id;
    private final DateTime timestamp;
    private final SubscriptionImpl subscription;
    private final String label;
    private Map<String, String> publication;
    private Map<String, String> notification;
    private DateTime created;
    private String href;
    private String content;

    public WvEventHolder(String id, DateTime time, SubscriptionImpl subscription, String label) {
        this.id = id;
        this.timestamp = time;
        this.subscription = subscription;
        this.label = label;
    }

    public Map<String, String> getPublication() {
        return publication;
    }

    public void setPublication(Map<String, String> publication) {
        this.publication = publication;
    }

    public Map<String, String> getNotification() {
        return notification;
    }

    public void setNotification(Map<String, String> notification) {
        this.notification = notification;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setHref(String h) {
        this.href = h;
    }

    @Override
    public String getHref() {
        return this.href;
    }

    @Override
    public void setContent(String c) {
        this.content = c;
    }

    @Override
    public String getContent() {
        return this.content;
    }

    @Override
    public Optional<Streamable> streamableObject() {
        return Optional.empty();
    }

}
