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
package org.n52.eventing.wv.rest;

import java.util.List;
import java.util.Optional;
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
import org.n52.eventing.rest.binding.exception.ResourceNotFoundException;
import org.n52.eventing.rest.binding.exception.concrete.ResourceWithIdNotFoundException;
import org.n52.eventing.wv.coding.NotificationLevelEncoder;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationLevelDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.NotificationLevel;
import org.n52.eventing.wv.view.NotificationLevelView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RestController
@RequestMapping(value = UrlSettings.API_V1_BASE + "/" + WvCustomResources.NOTIFICATIONLEVELS_RESOURCE,
        produces = {"application/json"})
public class NotificationLevelController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private RequestContext context;

    @Autowired
    private PaginationFactory pageFactory;

    @RequestMapping("")
    public ResourceCollectionWithMetadata<NotificationLevelView> get() throws InvalidPaginationException {
        try (Session session = hdc.createSession()) {
            HibernateNotificationLevelDao dao = new HibernateNotificationLevelDao(session);
            NotificationLevelEncoder encoder = new NotificationLevelEncoder();
            Pagination p = pageFactory.fromQuery(context.getParameters());
            QueryResult<NotificationLevel> result = dao.retrieve(p);
            List<NotificationLevelView> data = result.getResult().stream()
                    .map(nl -> encoder.encode(nl))
                    .collect(Collectors.toList());
            return new ResourceCollectionWithMetadata<>(data, new ResourceCollectionWithMetadata.Metadata(result.getTotalHits(), p));
        }
    }

    @RequestMapping("{item}")
    public NotificationLevelView get(@PathVariable String item) throws ResourceNotFoundException, InvalidPaginationException {
        Integer intId = Integer.valueOf(item, 10);
        try (Session session = hdc.createSession()) {
            HibernateNotificationLevelDao dao = new HibernateNotificationLevelDao(session);
            NotificationLevelEncoder encoder = new NotificationLevelEncoder();
            Pagination p = pageFactory.fromQuery(context.getParameters());
            Optional<NotificationLevelView> result = dao.retrieve(p).getResult().stream()
                    .filter(nl -> nl.getId() == intId)
                    .map(et -> encoder.encode(et))
                    .findFirst();

            if (result.isPresent()) {
                return result.get();
            }
        }

        throw new ResourceWithIdNotFoundException(item);
    }
}
