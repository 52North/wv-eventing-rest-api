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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.model.Subscription;
import org.n52.eventing.rest.model.impl.SubscriptionImpl;
import org.n52.eventing.rest.subscriptions.SubscriptionsService;
import org.n52.eventing.rest.subscriptions.UnknownSubscriptionException;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.coding.Context;
import org.n52.eventing.wv.coding.SubscriptionEncoder;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.SubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.UserSecurityService;
import org.n52.eventing.wv.view.SubscriptionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class SubscriptionsServiceImpl extends BaseService implements SubscriptionsService {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsServiceImpl.class);

    @Autowired
    private I18nProvider i18n;

    @Autowired
    HibernateDatabaseConnection hibernateConnection;

    @Autowired
    UserSecurityService userSecurityService;

    @Autowired
    AccessRights accessRights;

    private final EmailDeliveryProviderImpl deliveryProvider;

    public SubscriptionsServiceImpl() {
        this.deliveryProvider = new EmailDeliveryProviderImpl();
    }

    @Override
    public boolean hasSubscription(String id) {
        int idInt = super.parseId(id);

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            return false;
        }

        try (Session session = hibernateConnection.createSession()) {
            SubscriptionDao dao = new HibernateSubscriptionDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            Optional<WvSubscription> sub = dao.retrieveById(idInt);
            return sub.isPresent() && accessRights.canSeeSubscription(user, sub.get());
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public QueryResult<Subscription> getSubscriptions(Pagination p) {
        RequestContext context = RequestContext.retrieveFromThreadLocal();

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            return new QueryResult<>(Collections.emptyList(), 0);
        }

        Map<String, String[]> params = RequestContext.retrieveFromThreadLocal().getParameters();
        AtomicBoolean expanded = new AtomicBoolean(false);
        if (params.containsKey("expanded")) {
            expanded.set(Boolean.parseBoolean(params.get("expanded")[0]));
        }

        Map<String, String[]> filter = createFilter(context, user);

        try (Session session = hibernateConnection.createSession()) {
            SubscriptionDao dao = new HibernateSubscriptionDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

            QueryResult<WvSubscription> subs = dao.retrieveWithFilter(filter, user, p);
            return new QueryResult<>(subs.getResult().stream()
                    .filter(s -> accessRights.canSeeSubscription(user, s))
                    .map((WvSubscription t) -> {
                return expanded.get() ? wrapSubscription(t, false) : wrapSubscriptionBrief(t);
            }).collect(Collectors.toList()), subs.getTotalHits());
        }
        catch (DatabaseException | NumberFormatException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }

        return new QueryResult<>(Collections.emptyList(), 0);
    }


    private Map<String, String[]> createFilter(RequestContext context, WvUser user) {
        Map<String, String[]> params = context.getParameters();
        Map<String, String[]> filter = new HashMap<>();
        if (params != null && !params.isEmpty()) {

            String[] publications = params.get("publications");
            if (publications != null && publications.length > 0) {
                filter.put("publications", publications[0].split(","));
            }

            String[] groups = params.get("groups");
            if (groups != null && groups.length > 0) {
                filter.put("groups", groups[0].split(","));
            }

            String[] notifications = params.containsKey("notifications") ? params.get("notifications") : params.get("templates");
            if (notifications != null && notifications.length > 0) {
                filter.put("notifications", notifications[0].split(","));
            }
        }

        return filter;
    }


    @Override
    public Subscription getSubscription(String id) throws UnknownSubscriptionException {
        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new UnknownSubscriptionException("Could not find subscription with id: "+id);
        }

        Map<String, String[]> params = RequestContext.retrieveFromThreadLocal().getParameters();
        AtomicBoolean expanded = new AtomicBoolean(false);
        if (params.containsKey("expanded")) {
            expanded.set(Boolean.parseBoolean(params.get("expanded")[0]));
        }

        try (Session session = hibernateConnection.createSession()) {
            SubscriptionDao dao = new HibernateSubscriptionDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            Optional<WvSubscription> sub = dao.retrieveById(Integer.parseInt(id));
            if (!sub.isPresent() || !accessRights.canSeeSubscription(user, sub.get())) {
                throw new UnknownSubscriptionException("Could not find subscription with id: "+id);
            }
            return wrapSubscription(sub.get(), expanded.get());
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            LOG.debug(e.getMessage(), e);
        }

        throw new UnknownSubscriptionException("Could not find subscription with id: "+id);
    }

    @Override
    public void addSubscription(String subId, Subscription subscription) {
        LOG.debug("No operation required, everything already done by "+FilterLogicImpl.class.getSimpleName());
    }

    @Override
    public SubscriptionImpl updateEndOfLife(String id, DateTime eol) throws UnknownSubscriptionException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public SubscriptionImpl updateStatus(String id, boolean enabled) throws UnknownSubscriptionException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void remove(String id) throws UnknownSubscriptionException {
        int idInt = super.parseId(id);

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new UnknownSubscriptionException("Could not find subscription with id: "+id);
        }

        try (Session session = hibernateConnection.createSession()) {
            SubscriptionDao dao = new HibernateSubscriptionDao(session);
            Optional<WvSubscription> sub = dao.retrieveById(idInt);

            if (!sub.isPresent() || !accessRights.canManageSubscription(user, sub.get())) {
                throw new UnknownSubscriptionException("Could not find subscription with id: "+id);
            }

            dao.remove(sub.get());
        } catch (DatabaseException ex) {
            throw new RuntimeException("Could not remove subscription", ex);
        }
    }

    private Subscription wrapSubscriptionBrief(WvSubscription sub) {
        Subscription result = wrapSubscription(sub, false);
//        result.setLabel(null);
        return result;
    }

    private Subscription wrapSubscription(WvSubscription sub, boolean fullyExpanded) {
        String baseUrl = RequestContext.retrieveFromThreadLocal().getBaseApiUrl();
        SubscriptionView details = new SubscriptionEncoder(new Context(baseUrl, true, !fullyExpanded), i18n).encode(sub);

        return details;
    }

}
