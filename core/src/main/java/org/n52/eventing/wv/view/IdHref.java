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
package org.n52.eventing.wv.view;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */

@JsonPropertyOrder({"id", "href", "label"})
public class IdHref implements Comparable<IdHref> {

    private String id;
    private String href;
    private String label;

    public IdHref(String id, String href) {
        this.id = id;
        this.href = href;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.BaseView.class)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.BaseView.class)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonView(org.n52.eventing.rest.model.views.Views.BaseView.class)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IdHref ? getId().equals(((IdHref) obj).getId()) : false;
    }

    @Override
    public int compareTo(IdHref o) {
        return getId().compareTo(o.getId());
    }

}
