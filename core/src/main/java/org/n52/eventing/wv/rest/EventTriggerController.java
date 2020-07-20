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
package org.n52.eventing.wv.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.n52.eventing.rest.InvalidPaginationException;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.PaginationFactory;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.ResourceCollectionWithMetadata;
import org.n52.eventing.rest.UrlSettings;
import org.n52.eventing.rest.binding.BaseController;
import org.n52.eventing.wv.coding.TrendEncoder;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Trend;
import org.n52.eventing.wv.view.TrendView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RestController
@RequestMapping(value = UrlSettings.API_V1_BASE+"/"+WvCustomResources.EVENTTRIGGERS_RESOURCE,
        produces = {"application/json"})
public class EventTriggerController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(EventTriggerController.class);

    @Autowired
    private I18nProvider i18n;

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private RequestContext context;

    @Autowired
    private PaginationFactory pageFactory;

    @RequestMapping("")
    public ResourceCollectionWithMetadata<TrendView> getTriggers() throws IOException, URISyntaxException, InvalidPaginationException {
        try (Session session = hdc.createSession()) {
            HibernateTrendDao dao = new HibernateTrendDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            TrendEncoder encoder = new TrendEncoder();
            Pagination p = pageFactory.fromQuery(context.getParameters());
            QueryResult<Trend> result = dao.retrieve(p, i18n.getLocale());
            List<TrendView> data = result.getResult().stream()
                    .map(t -> encoder.encode(t))
                    .collect(Collectors.toList());
            return new ResourceCollectionWithMetadata<>(data, new ResourceCollectionWithMetadata.Metadata(result.getTotalHits(), p));
        }
    }

}
