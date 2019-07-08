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
package org.n52.eventing.wv.model;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class NotificationRule implements BaseEntity, Serializable {

    private int id;
    private boolean primaryRule;
    private Rule rule;
    private NotificationLevel level;
    private Notification notification;

    public NotificationRule() {
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public boolean isPrimaryRule() {
        return primaryRule;
    }

    public void setPrimaryRule(boolean primaryRule) {
        this.primaryRule = primaryRule;
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.rule.getId());
        hash = 97 * hash + Objects.hashCode(this.level.getId());
        hash = 97 * hash + Objects.hashCode(this.notification.getId());
        hash = 97 * hash + Objects.hashCode(this.primaryRule);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NotificationRule other = (NotificationRule) obj;
        if (!Objects.equals(this.isPrimaryRule(), other.isPrimaryRule())) {
            return false;
        }
        if (!Objects.equals(this.rule.getId(), other.rule.getId())) {
            return false;
        }
        if (!Objects.equals(this.level.getId(), other.level.getId())) {
            return false;
        }
        if (!Objects.equals(this.notification.getId(), other.notification.getId())) {
            return false;
        }
        return true;
    }




}
