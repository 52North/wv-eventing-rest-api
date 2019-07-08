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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.n52.eventing.rest.InvalidPaginationException;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.PaginationFactory;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.model.TemplateDefinition;
import org.n52.eventing.rest.templates.TemplatesDao;
import org.n52.eventing.rest.templates.UnknownTemplateException;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.coding.Context;
import org.n52.eventing.wv.coding.NotificationEncoder;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.NotificationDao;
import org.n52.eventing.wv.dao.RuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationLevelDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateRuleDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSeriesDao;
import org.n52.eventing.wv.dao.hibernate.HibernateTrendDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationLevel;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.Rule;
import org.n52.eventing.wv.model.Series;
import org.n52.eventing.wv.model.Trend;
import org.n52.eventing.wv.model.WvTemplateDefinition;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.UserSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class TemplatesServiceImpl extends BaseService implements TemplatesDao {

    private static final Logger LOG = LoggerFactory.getLogger(TemplatesServiceImpl.class);

    private final I18nProvider i18n;
    private final HibernateDatabaseConnection hdc;
    private final RequestContext context;
    private final boolean expanded;
    private final AccessRights accessRights;
    private PaginationFactory pageFactory;

    public TemplatesServiceImpl(I18nProvider i18n, HibernateDatabaseConnection hdc, RequestContext context,
            boolean expanded, AccessRights accessRights, UserSecurityService userSecurityService, PaginationFactory pageFactory) {
        this.i18n = i18n;
        this.hdc = hdc;
        this.context = context;
        this.expanded = expanded;
        this.accessRights = accessRights;
        this.pageFactory = pageFactory;
        setUserSecurityService(userSecurityService);
    }

    @Override
    public String createTemplate(TemplateDefinition td) {
        if (!(td instanceof WvTemplateDefinition)) {
            throw new IllegalStateException("Unsupported template type: "+ td);
        }

        WvTemplateDefinition def = (WvTemplateDefinition) td;
        if (def.getDefinition() == null || !(def.getDefinition().getContent() instanceof Map<?, ?>)) {
            throw new IllegalStateException("Provided definition does not match the required schema");
        }

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new IllegalStateException("User is not authenticated");
        }

        if (!accessRights.canCreateNotifications(user)) {
            throw new IllegalStateException("User is not allowed to create notifications");
        }

        try (Session session = hdc.createSession()) {
            Integer seriesId = extractParameter((Map<?, ?>) def.getDefinition().getContent(), "publication");

            Optional<Series> seriesOpt = new HibernateSeriesDao(session).retrieveById(seriesId);
            if (!seriesOpt.isPresent()) {
                throw new IllegalArgumentException("Series is not known: "+seriesId);
            }
            Series series = seriesOpt.get();

            // create the resulting notification
            // notification
            Notification notification = new Notification();
            notification.setSeries(series);

            List<Map<?, ?>> rules = extractParameter(def, "rules");

            RuleDao ruleDao = new HibernateRuleDao(session);
            HibernateTrendDao trendDao = new HibernateTrendDao(session);
            HibernateNotificationLevelDao notificationLevelDao = new HibernateNotificationLevelDao(session);

            // one transaction for all persistency
            Transaction trans = session.beginTransaction();

            // the resulting notifcation rule objects
            Set<NotificationRule> notificationRules = new HashSet<>(rules.size());
            for (Map<?, ?> rule : rules) {
                Rule r = new Rule();

                Number threshold = extractParameter(rule, "threshold");
                Integer trendCode = extractParameter(rule, "trend");
                Integer level = extractParameter(rule, "level", 0);
                Boolean primaryRule = extractParameter(rule, "primaryRule", false);
                r.setThreshold(threshold.doubleValue());

                r.setSeries(series);

                Optional<Trend> t = trendDao.retrieveById(trendCode);
                if (!t.isPresent()) {
                    throw new IllegalArgumentException("Trend is not supported: "+trendCode);
                }
                r.setTrendCode(t.get());

                // check if the same rule is already present in the database
                if (ruleDao.hasEntity(r)) {
                    throw new IllegalArgumentException(String.format(i18n.getString("rule.alreadyPresent"),
                            Series.generateSeriesLabel(r.getSeries()),
                            r.getTrendCode().getLabel(),
                            r.getThreshold()
                            ));
                }

                ruleDao.store(r, true);

                // relate to notification
                NotificationRule notificationRule = new NotificationRule();
                notificationRule.setRule(r);
                notificationRule.setPrimaryRule(primaryRule);
                Optional<NotificationLevel> levelOpt = notificationLevelDao.retrieveById(level);

                if (!levelOpt.isPresent()) {
                    throw new IllegalArgumentException("NotificationLevel not available: "+level);
                }

                notificationRule.setLevel(levelOpt.get());
                notificationRule.setNotification(notification);
                notificationRules.add(notificationRule);
            }

            notification.setNotificationRules(notificationRules);

            new HibernateNotificationDao(session).store(notification, true);

            HibernateNotificationRuleDao nrd = new HibernateNotificationRuleDao(session);
            for (NotificationRule nr : notificationRules) {
                nrd.store(nr, true);
            }

            // commit all remaining changes
            trans.commit();

            LOG.info("Template successfuly created: {}", notification.getId());

            return Integer.toString(notification.getId());
        }
        catch (DatabaseException e) {
            LOG.warn(e.getMessage());
            throw new RuntimeException("Error on storing notification", e);
        }
    }

    @Override
    public boolean hasTemplate(String id) {
        Session session = hdc.createSession();

        NotificationDao dao = new HibernateNotificationDao(session);
        dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
        try {
            Optional<Notification> templ = dao.retrieveById(Integer.parseInt(id));
            return templ.isPresent();
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
    public TemplateDefinition getTemplate(String id) throws UnknownTemplateException {
        Session session = hdc.createSession();

        NotificationDao dao = new HibernateNotificationDao(session);
        HibernateNotificationLevelDao levelDao = new HibernateNotificationLevelDao(session);
        dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
        try {
            Optional<Notification> templ = dao.retrieveById(Integer.parseInt(id));
            if (templ.isPresent()) {
                RequestContext requestContext = RequestContext.retrieveFromThreadLocal();
                Context ctx = new Context(requestContext != null ? requestContext.getBaseApiUrl() : null, false);
                NotificationEncoder encoder = new NotificationEncoder(ctx, i18n, levelDao);
                return encoder.encode(templ.get());
            }
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }
        finally {
            session.close();
        }

        throw new UnknownTemplateException("Template not availabled: "+id);
    }

    @Override
    public QueryResult<TemplateDefinition> getTemplates() throws InvalidPaginationException {
        return internalGet((Session s, Pagination p) -> {
            NotificationDao dao = new HibernateNotificationDao(s);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            return dao.retrieve(p);
        }, null);
    }


    @Override
    public QueryResult<TemplateDefinition> getTemplates(Map<String, String[]>  filter) throws InvalidPaginationException {
        return getTemplates(filter, null);
    }

    @Override
    public QueryResult<TemplateDefinition> getTemplates(Map<String, String[]>  filter, Pagination page) throws InvalidPaginationException {
        if (filter == null || filter.isEmpty() || !filter.containsKey("publications")) {
            return getTemplates();
        }
        return internalGet((Session s, Pagination p) -> {
            NotificationDao dao = new HibernateNotificationDao(s);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            String[] val = filter.get("publications");
            if (val == null || val.length == 0) {
                throw new DatabaseException("Filter 'publications' cannot be empty");
            }
            return dao.retrieveBySeries(p, val[0].split(","));
        }, page);
    }

    private QueryResult<TemplateDefinition> internalGet(DaoSupplier<Notification> supplier, Pagination page) throws InvalidPaginationException {
        if (page == null) {
            page = pageFactory.fromQuery(context.getParameters());
        }

        try (Session session = hdc.createSession()) {
            QueryResult<Notification> templ = supplier.getFromDao(session, page);
            HibernateNotificationLevelDao levelDao = new HibernateNotificationLevelDao(session);
            RequestContext requestContext = RequestContext.retrieveFromThreadLocal();

            Context ctx = new Context(requestContext != null ? requestContext.getBaseApiUrl() : null, false);
            NotificationEncoder encoder = new NotificationEncoder(ctx, i18n, levelDao);

            return new QueryResult<>(templ.getResult().stream()
                    .map((Notification r) -> encoder.encode(r))
                    .collect(Collectors.toList()), templ.getTotalHits());
        }
        catch (DatabaseException | NumberFormatException | NotAuthenticatedException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }

        return new QueryResult<>(Collections.emptyList(), 0);
    }

    private <T> T extractParameter(WvTemplateDefinition def, String param) {
        if (def.getDefinition() != null && def.getDefinition().getContent() instanceof Map<?, ?>) {
            Map<?, ?> content = (Map<?, ?>) def.getDefinition().getContent();
            if (content.containsKey(param)) {
                return (T) content.get(param);
            }
        }

        throw new IllegalArgumentException("Required parameter not provided: "+param);
    }

    private <T> T extractParameter(Map<?, ?> def, String param) {
        return extractParameter(def, param, null);
    }

    private <T> T extractParameter(Map<?, ?> def, String param, T defaultValue) {
        if (def != null && def instanceof Map<?, ?>) {
            if (def.containsKey(param)) {
                return (T) def.get(param);
            }
        }

        if (defaultValue != null) {
            return defaultValue;
        }

        throw new IllegalArgumentException("Required parameter not provided: "+param);
    }

}
