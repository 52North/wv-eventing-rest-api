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
import java.util.Date;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class WvEventMail implements Serializable{

    private static final long serialVersionUID = 4167380449528755149L;
    private int id;
    private String mailAddress;
    private Date mailSentOn;
    private WvSubscription subscription;
    private String content;
    private WvUser user;
    private NotificationLevel notificationLevel;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMailAddress() {
        return mailAddress;
    }

    public void setMailAddress(String mailAddress) {
        this.mailAddress = mailAddress;
    }

    public Date getMailSentOn() {
        return mailSentOn;
    }

    public void setMailSentOn(Date mailSentOn) {
        this.mailSentOn = mailSentOn;
    }

    public WvSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(WvSubscription subscription) {
        this.subscription = subscription;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public WvUser getUser() {
        return user;
    }

    public void setUser(WvUser user) {
        this.user = user;
    }

    public NotificationLevel getNotificationLevel() {
        return notificationLevel;
    }

    public void setNotificationLevel(NotificationLevel notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

}
