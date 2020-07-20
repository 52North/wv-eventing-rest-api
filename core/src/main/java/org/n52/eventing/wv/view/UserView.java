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
package org.n52.eventing.wv.view;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import org.n52.eventing.rest.model.views.Views;
import org.n52.eventing.rest.users.User;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@JsonPropertyOrder({"id", "href", "admin", "screenName", "firstName", "lastName", "email", "groups"})
public class UserView implements User {

    private String id;
    private String screenName;
    private String firstName;
    private String lastName;
    private String email;
    private Set<GroupView> groups;
    private Boolean admin;
    private String href;

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        setId(Integer.toString(id, 10));
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, org.n52.eventing.wv.view.Views.UserOverview.class, Views.TemplateOverview.class})
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public Set<GroupView> getGroups() {
        return groups;
    }

    public void setGroups(Set<GroupView> groups) {
        this.groups = groups;
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    @JsonView(value = {Views.SubscriptionExpanded.class, org.n52.eventing.wv.view.Views.UserOverview.class, Views.TemplateOverview.class})
    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isAdmin() {
        return this.admin;
    }

}
