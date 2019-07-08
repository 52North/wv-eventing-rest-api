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

import com.fasterxml.jackson.annotation.JsonView;
import org.n52.eventing.rest.model.views.Views;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class SeriesView {

    private int id;
    private String href;
    private CategoryView category;
    private PhenomenonView phenomenon;
    private ProcedureView procedure;
    private FeatureOfInterestView featureOfInterest;
    private UnitView unit;

    @JsonView(Views.SubscriptionExpanded.class)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CategoryView getCategory() {
        return category;
    }

    public void setCategory(CategoryView category) {
        this.category = category;
    }

    public PhenomenonView getPhenomenon() {
        return phenomenon;
    }

    public void setPhenomenon(PhenomenonView phenomenon) {
        this.phenomenon = phenomenon;
    }

    public ProcedureView getProcedure() {
        return procedure;
    }

    public void setProcedure(ProcedureView procedure) {
        this.procedure = procedure;
    }

    public FeatureOfInterestView getFeatureOfInterest() {
        return featureOfInterest;
    }

    public void setFeatureOfInterest(FeatureOfInterestView featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    public UnitView getUnit() {
        return unit;
    }

    public void setUnit(UnitView unit) {
        this.unit = unit;
    }

    @JsonView(Views.SubscriptionExpanded.class)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

}
