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

import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class WvEvent implements BaseEntity {

    private int id;
    private Rule rule;
    private Series series;
    private Notification notification;
    private WvUser user;
    private Group group;
    private WvEventType eventType;
    private WvEventMessage eventMessage;
    private Date timestamp;
    private Date created;
    private Double value;
    private Date previousTimestamp;
    private Double previousValue;
    private Set<WvEventMail> logMails;


    public WvEvent() {
    }

    public WvEvent(Rule rule, Date timestamp, Double value, Date previousTimestamp, Double previousValue) {
        this.rule = rule;
        this.timestamp = timestamp;
        this.value = value;
        this.previousTimestamp = previousTimestamp;
        this.previousValue = previousValue;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getPreviousTimestamp() {
        return previousTimestamp;
    }

    public void setPreviousTimestamp(Date previousTimestamp) {
        this.previousTimestamp = previousTimestamp;
    }

    public Double getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(Double previousValue) {
        this.previousValue = previousValue;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public WvUser getUser() {
        return user;
    }

    public void setUser(WvUser user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public WvEventType getEventType() {
        return eventType;
    }

    public void setEventType(WvEventType eventType) {
        this.eventType = eventType;
    }

    public WvEventMessage getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(WvEventMessage eventMessage) {
        this.eventMessage = eventMessage;
    }

    public Set<WvEventMail> getLogMails() {
        return logMails;
    }

    public void setLogMails(Set<WvEventMail> logMail) {
        this.logMails = logMail;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof WvEvent) {
            if (((WvEvent) obj).id != this.id) {
                return false;
            }

            Series otherSeries = ((WvEvent) obj).getSeries();
            if (otherSeries == null) {
                if (this.series != null) {
                    return false;
                }
            } else {
                return this.series != null && otherSeries.getId() == this.series.getId();
            }

        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + this.id;

        if (this.series != null) {
            hash = 23 * hash + Objects.hashCode(this.series.getId());
        }

        return hash;
    }


}
