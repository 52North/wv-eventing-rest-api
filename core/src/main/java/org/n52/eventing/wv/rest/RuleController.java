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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
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
import org.n52.eventing.wv.coding.Context;
import org.n52.eventing.wv.coding.RuleEncoder;
import org.n52.eventing.wv.dao.hibernate.HibernateRuleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.view.RuleView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RestController
@RequestMapping(value = UrlSettings.API_V1_BASE+"/"+WvCustomResources.RULES_RESOURCE,
        produces = {"application/json"})
public class RuleController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(RuleController.class);

    @Autowired
    private I18nProvider i18n;

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private RequestContext context;

    @Autowired
    private PaginationFactory pageFactory;

    @RequestMapping("")
    public ResourceCollectionWithMetadata<RuleView> getRules() throws IOException, URISyntaxException, InvalidPaginationException {
        Map<String, String[]> query = context.getParameters();
        Pagination p = pageFactory.fromQuery(query);

        boolean expanded = false;
        if (query.containsKey("expanded")) {
            expanded = Boolean.parseBoolean(query.get("expanded")[0]);
        }

        try (Session session = hdc.createSession()) {
            HibernateRuleDao dao = new HibernateRuleDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

            RuleEncoder encoder = new RuleEncoder(new Context(context.getBaseApiUrl(), expanded, true), i18n);
            QueryResult<Rule> result = dao.retrieve(p, i18n.getLocale());

            QueryResult<RuleView> qr = new QueryResult<>(result.getResult().stream()
                    .map(t -> encoder.encode(t))
                    .collect(Collectors.toList()), result.getTotalHits());

            return new ResourceCollectionWithMetadata<>(qr, p);
        }
    }

    @RequestMapping("/{item}")
    public RuleView getSingleRule(@PathVariable("item") String id) throws IOException, URISyntaxException, ResourceNotFoundException {
        try (Session session = hdc.createSession()) {
            HibernateRuleDao dao = new HibernateRuleDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

            RuleEncoder encoder = new RuleEncoder(new Context(context.getBaseApiUrl(), true, true), i18n);

            Optional<Rule> result = dao.retrieveById(Integer.parseInt(id), i18n.getLocale());
            if (result.isPresent()) {
                return encoder.encode(result.get());
            }

            throw new ResourceWithIdNotFoundException(id);
        }
    }

}
