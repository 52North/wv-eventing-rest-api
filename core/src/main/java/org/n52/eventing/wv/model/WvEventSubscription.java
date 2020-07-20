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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class WvEventSubscription implements BaseEntity {

    private WvEvent event;
    private SortedSet<Integer> subscriptions = new TreeSet<>();

    public WvEventSubscription(WvEvent event) {
        setEvent(event);
    }

    public WvEventSubscription(WvEvent event, Integer subscription) {
        setEvent(event);
        addSubscription(subscription);
    }

    public WvEventSubscription(WvEvent event, Set<WvSubscription> subscriptions) {
        this(event);
        subscriptions.stream().filter(Objects::nonNull).forEach(s -> addSubscription(s.getId()));
    }

    public WvEvent getEvent() {
        return event;
    }

    private void setEvent(WvEvent event) {
        this.event = event;
    }

    public Set<Integer> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscription(Collection<Integer> subscriptions) {
        this.subscriptions.clear();
        if (subscriptions != null) {
            this.subscriptions.addAll(subscriptions);
        }
    }

    public void addSubscriptions(Collection<Integer> subscriptions) {
        if (subscriptions != null) {
            this.subscriptions.addAll(subscriptions);
        }
    }

    public void addSubscription(Integer subscription) {
        if (subscription != null) {
            this.subscriptions.add(subscription);
        }
    }

    @Override
    public int getId() {
        return getEvent().getId();
    }

    @Override
    public void setId(int id) {
        getEvent().setId(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WvEventSubscription) {
            return getId() == ((WvEventSubscription) obj).getId();

        }
        return false;
    }


}
