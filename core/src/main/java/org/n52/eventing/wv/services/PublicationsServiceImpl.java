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
package org.n52.eventing.wv.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.n52.eventing.rest.Configuration;
import org.n52.eventing.rest.InvalidPaginationException;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.PaginationFactory;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.factory.TemplatesDaoFactory;
import org.n52.eventing.rest.model.Publication;
import org.n52.eventing.rest.publications.PublicationsService;
import org.n52.eventing.rest.publications.UnknownPublicationsException;
import org.n52.eventing.rest.model.TemplateDefinition;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.SeriesDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSeriesDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Series;
import org.n52.eventing.wv.model.WvPublication;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class PublicationsServiceImpl extends BaseService implements PublicationsService, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(PublicationsServiceImpl.class);

    @Autowired
    private I18nProvider i18n;

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private AccessRights accessRights;

    @Autowired
    private TemplatesDaoFactory templatesDaoFactory;

    @Autowired
    private Configuration config;

    @Autowired
    private PaginationFactory pageFactory;

    private String seriesHref;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.seriesHref = config.getParameter("timeSeriesApiBaseUrl").orElse("http://localhost/series-api/");
    }

    @Override
    public boolean hasPublication(String id) {
        int idInt = super.parseId(id);

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            return false;
        }

        if (!accessRights.canSeeSeries(user, idInt)) {
            LOG.debug("User {} is not allowed to see the series {}", user.getId(), idInt);
            return false;
        }

        Session session = hdc.createSession();
        SeriesDao dao = new HibernateSeriesDao(session);
        dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

        try {
            Optional<Series> pub = dao.retrieveById(idInt);
            return pub.isPresent();
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
        }
        finally {
            session.close();
        }
        return false;
    }

    @Override
    public QueryResult<Publication> getPublications(Map<String, String[]> filter, Pagination page) throws InvalidPaginationException {
        RequestContext context = RequestContext.retrieveFromThreadLocal();

        if (filter == null || filter.isEmpty() || !filter.containsKey("feature")) {
            return getPublications(page);
        }

        return internalGet((Session session, Pagination p) -> {
            SeriesDao dao = new HibernateSeriesDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            String[] val = filter.get("feature");
            if (val == null || val.length == 0) {
                throw new DatabaseException("Filter 'feature' cannot be empty");
            }
            return dao.retrieveByFeature(val[0], p);
        }, context, page);
    }

    @Override
    public QueryResult<Publication> getPublications(Pagination page) throws InvalidPaginationException {
        RequestContext context = RequestContext.retrieveFromThreadLocal();

        return internalGet((Session session, Pagination p) -> {
            SeriesDao dao = new HibernateSeriesDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            return dao.retrieve(p);
        }, context, page);
    }

    private QueryResult<Publication> internalGet(DaoSupplier<Series> retriever, RequestContext context, Pagination page) throws InvalidPaginationException {
        Pagination p;
        if (page != null) {
            p = page;
        }
        else {
            p = pageFactory.fromQuery(context.getParameters());
        }

        try (Session session = hdc.createSession()) {
            WvUser user = super.resolveUser(accessRights);
            QueryResult<Series> pubs = retriever.getFromDao(session, p);

            List<InvalidPaginationException> exceptions = new ArrayList<>();

            List<Publication> result = pubs.getResult().stream()
                    .filter(s -> accessRights.canSeeSeries(user, s.getId()))
                    .map((Series s) -> {
                        try {
                            return wrapSeriesBrief(s, context);
                        } catch (InvalidPaginationException ex) {
                            LOG.warn(ex.getMessage(), ex);
                            exceptions.add(ex);
                        }
                        return null;
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            if (!exceptions.isEmpty()) {
                throw exceptions.get(0);
            }

            return new QueryResult<>(result, pubs.getTotalHits());
        }
        catch (NotAuthenticatedException | DatabaseException e) {
            LOG.warn(e.getMessage());
        }

        return new QueryResult<>(Collections.emptyList(), 0);
    }

    @Override
    public Publication getPublication(String id) throws UnknownPublicationsException {
        RequestContext context = RequestContext.retrieveFromThreadLocal();

        int idInt = super.parseId(id);

        Session session = hdc.createSession();
        SeriesDao dao = new HibernateSeriesDao(session);
        dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

        try {
            WvUser user = super.resolveUser(accessRights);
            Optional<Series> series = dao.retrieveById(idInt);
            if (series.isPresent() && accessRights.canSeeSeries(user, series.get().getId())) {
                return wrapSeries(series.get(), context);
            }
        }
        catch (NotAuthenticatedException | NumberFormatException | InvalidPaginationException e) {
            LOG.warn("Exception on retrieving publication: "+e.getMessage());
            LOG.debug(e.getMessage(), e);
        }
        finally {
            session.close();
        }

        throw new UnknownPublicationsException("The publication is unknown: "+id);
    }

    private Publication wrapSeriesBrief(Series s, RequestContext context) throws InvalidPaginationException {
        String label = Series.generateSeriesLabel(s);

        WvPublication pub = new WvPublication(Integer.toString(s.getId()), label, null);
        pub.setSeriesHref(String.format(this.seriesHref, s.getId()));

        injectTemplates(pub, context);

        return pub;
    }

    private Publication wrapSeries(Series s, RequestContext context) throws InvalidPaginationException {
        String label = Series.generateSeriesLabel(s);

        WvPublication pub = new WvPublication(Integer.toString(s.getId()), label, null);
        Map<String, Object> props = new HashMap<>();
        props.put("feature", s.getFeature().getIdentifier());
        props.put("phenomenon", s.getPhenomenon().getPhenomenonId());
        props.put("category", s.getCategory().getCategoryId());
        props.put("procedure", s.getProcedure().getProcedureId());
        props.put("unit", s.getUnit().getCode());
        pub.setDetails(props);

        pub.setSeriesHref(String.format(this.seriesHref, s.getId()));

        injectTemplates(pub, context);

        return pub;
    }

    private void injectTemplates(WvPublication pub, RequestContext context) throws InvalidPaginationException {
        Map<String, String[]> params = context.getParameters();
        if (params.containsKey("expanded") && Boolean.parseBoolean(params.get("expanded")[0])) {

            //TODO: currently, the pagination from the request is applied here as well,
            // this may result in only 100 templates being returned, or different, depending on the request
            List<TemplateDefinition> notifications = templatesDaoFactory.newDao(false)
                    .getTemplates(Collections.singletonMap("publications", new String[] {pub.getId()}), pageFactory.create(0, 1000)).getResult();
            pub.setNotifications(notifications);
        }
    }

}
