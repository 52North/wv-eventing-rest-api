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
package org.n52.eventing.wv.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.PaginationFactory;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.UrlSettings;
import org.n52.eventing.rest.binding.exception.BadRequestException;
import org.n52.eventing.rest.binding.exception.InternalServerException;
import org.n52.eventing.rest.eventlog.EventLogStore;
import org.n52.eventing.rest.model.EventHolder;
import org.n52.eventing.rest.model.Subscription;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.coding.Context;
import org.n52.eventing.wv.coding.EventEncoder;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.hibernate.HibernateEventDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvEventSubscription;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.view.EventView;
import org.n52.eventing.wv.view.IdHref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class EventLogServiceImpl extends BaseService implements EventLogStore {

    private static final Logger LOG = LoggerFactory.getLogger(EventLogServiceImpl.class);
    private static final String USER = "user";
    private static final String EVENT = "event";
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String GROUPS = "groups";

    @Autowired
    private I18nProvider i18n;

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private SubscriptionsServiceImpl subscriptionService;

    @Autowired
    private EventTypesStore store;

    @Autowired
    private PaginationFactory pageFactory;

    @Autowired
    private AccessRights accessRights;

    @Override
    public void addEvent(Subscription sub, EventHolder eh, int maximumCapacity) {
        LOG.debug("NoOp addEvent()");
    }

    @Override
    public QueryResult<EventHolder> getAllEvents() {
        return getAllEvents(null);
    }

    @Override
    public QueryResult<EventHolder> getAllEvents(Pagination pagination) {
        RequestContext context = RequestContext.retrieveFromThreadLocal();

        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn("Not user found: {}", ex.getMessage());
            LOG.debug(ex.getMessage(), ex);
            return new QueryResult<>(Collections.emptyList(), 0);
        }

        try (Session session = hdc.createSession()) {
            HibernateEventDao dao = new HibernateEventDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());

            Map<String, String[]> filter;
            try {
                LOG.trace("Creating filter for event retrieval");
                filter = createFilter(context);
            } catch (NotAuthenticatedException ex) {
                LOG.warn("Not logged in", ex.getMessage());
                LOG.debug(ex.getMessage(), ex);
                return new QueryResult(Collections.emptyList(), 0);
            }

            if (filter.containsKey(SUBSCRIPTIONS) && filter.get(SUBSCRIPTIONS).length > 0
                    && filter.containsKey(GROUPS) && filter.get(GROUPS).length > 0) {
                throw new BadRequestException("Parameters 'subsrcitpions' and 'groups' cannot be used in combination");
            }

            String[] subs = filter.get(SUBSCRIPTIONS);
            String[] groups = filter.get(GROUPS);

            // if no subs are defined in the query, use all subscriptions of the user
            try {
                LOG.trace("resolving relevant subscriptions");
                HibernateSubscriptionDao subDao = new HibernateSubscriptionDao(session);
                List<WvSubscription> candidates = subDao.retrieveByUserNoPaging(user).getResult();

                if (subs != null && subs.length > 0 && !accessRights.isInAdminGroup(user)) {
                    Set<String> candidateSet = candidates.stream()
                            .map(s -> Integer.toString(s.getId(), 10))
                            .distinct().collect(Collectors.toSet());
                    String[] validSubs = Arrays.asList(subs).stream().filter(s -> {
                        return candidateSet.contains(s);
                    }).distinct().toArray(String[]::new);
                    if (validSubs != null && validSubs.length > 0) {
                        filter.put(SUBSCRIPTIONS, validSubs);
                    } else {
                        return new QueryResult(Collections.emptyList(), 0);
                    }
                }
            } catch (DatabaseException ex) {
                LOG.warn(ex.getMessage(), ex);
                throw new InternalServerException("Could not resolve users subscription", ex);
            }
            // check groups
            if (groups != null && groups.length > 0 && !accessRights.isInAdminGroup(user)) {
                Set<String> candidateGroups = user.getGroups().stream().map(g -> Integer.toString(g.getId(), 10))
                        .collect(Collectors.toSet());
                String[] validGroups = Arrays.asList(groups).stream().filter(g -> {
                    return candidateGroups.contains(g);
                }).distinct().toArray(String[]::new);
                if (validGroups != null && validGroups.length > 0) {
                    filter.put(GROUPS, validGroups);
                } else {
                    return new QueryResult(Collections.emptyList(), 0);
                }
            }

            // retrieve only the latest for each subscription
            if (filter.containsKey(getLatestKey(filter)) && Boolean.parseBoolean(filter.get(getLatestKey(filter))[0])) {
                LOG.trace("Retrieving events with 'latest' method");
                if (filter.containsKey("timespan")) {
                    throw new BadRequestException("Parameters 'latest' and 'timespan' cannot be used in combination");
                }
//                QueryResult<WvEventSubscription> result = dao.retrieveWithFilter(filter, pagination);
                QueryResult<WvEvent> result = dao.retrieveWithFilter(filter, pagination);
                return new QueryResult<>(result.getResult().stream().map((WvEvent e) -> wrapEventBrief(e, context))
                        .collect(Collectors.toList()), result.getTotalHits());


            } // retrieve events from the specified subscriptions
            else {
                LOG.trace("Retrieving events with 'subscriptions' method");
                if (filter.isEmpty()) {
                    QueryResult<WvEvent> result = dao.retrieve(pagination);
                    return new QueryResult<>(result.getResult().stream()
                            .map((WvEvent e) -> {
                                return wrapEventBrief(e, context);
                            })
                            .collect(Collectors.toList()), result.getTotalHits());
                } else {
                    QueryResult<WvEvent> result =  dao.retrieveWithFilter(filter, pagination);
                    return new QueryResult<>(result.getResult().stream()
                            .map((WvEvent e) -> {
                                return wrapEventBrief(e, context);
                            })
                            .collect(Collectors.toList()), result.getTotalHits());
                }
            }
        }
    }

    @Override
    public QueryResult<EventHolder> getEventsForSubscription(Subscription subscription, Pagination pagination) {
        RequestContext context = RequestContext.retrieveFromThreadLocal();
        try (Session session = hdc.createSession()) {
            int idInt = super.parseId(subscription.getId());
            HibernateEventDao dao = new HibernateEventDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            QueryResult<WvEvent> result = dao.retrieveForSubscription(idInt);
            return new QueryResult<>(result.getResult().stream()
                    .map((WvEvent e) -> wrapEventBrief(e, context))
                    .collect(Collectors.toList()), result.getTotalHits());
        } catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
        }

        return new QueryResult<>(Collections.emptyList(), 0);
    }

    @Override
    public QueryResult<EventHolder> getEventsForSubscription(Subscription subscription) {
        return getEventsForSubscription(subscription, null);
    }

    @Override
    public Optional<EventHolder> getSingleEvent(String eventId, RequestContext context) {
        try (Session session = hdc.createSession()) {
            int idInt = super.parseId(eventId);
            HibernateEventDao dao = new HibernateEventDao(session);
            dao.setDefaultLanguage(i18n.getDefaultLocale().getLanguage());
            Map<String, String[]> filter = new HashMap<>();
            filter.put(EVENT,  new String[]{eventId});
            try {
                WvUser user = super.resolveUser(accessRights);
                if (!user.isAdmin()) {
                    filter.put(USER, new String[] { Integer.toString(user.getId()) });
                    filter.put(GROUPS, user.getGroups().stream().map(g -> Integer.toString(g.getId())).distinct()
                            .toArray(String[]::new));
                }
            } catch (NotAuthenticatedException ex) {
                LOG.warn("Not logged in", ex.getMessage());
                LOG.debug(ex.getMessage(), ex);
                return Optional.empty();
            }
            QueryResult<WvEvent> queryResult = dao.retrieveWithFilter(filter, null);

//            Optional<WvEvent> result = dao.retrieveById(idInt);
            if (queryResult.getResult() != null) {
                Optional<WvEvent> result = queryResult.getResult().stream().findFirst();
                if (result.isPresent()) {
//                    WvEvent eventSub = result.get();
//                    Hibernate.initialize(eventSub.getEvent().getRule());
                    return Optional.of(wrapEventBrief(result.get(), context, true));
                }
            }

            return Optional.empty();
        } catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
        }

        return Optional.empty();
    }

//    private EventHolder wrapEventBrief(WvEventSubscription e, RequestContext context) {
//        EventView data = (EventView) wrapEventBrief(e.getEvent(), context);
//        if (e.getSubscriptions() != null) {
//            for (Integer sub : e.getSubscriptions()) {
//                data.addSubscription(
//                        new IdHref(Integer.toString(sub, 10), String.format("%s/%s/%s",
//                                context.getBaseApiUrl(), UrlSettings.SUBSCRIPTIONS_RESOURCE, sub)));
//            }
//        }
//        return data;
//    }

    private EventHolder wrapEventBrief(WvEvent e, RequestContext context) {
        return wrapEventBrief(e, context, false);
    }

    private EventHolder wrapEventBrief(WvEvent e, RequestContext context, boolean expanded) {
        EventEncoder encoder = new EventEncoder(new Context(context != null ? context.getBaseApiUrl() : null, expanded), store, i18n);
        EventView data = encoder.encode(e);
        return data;
    }

    private Map<String, String[]> createFilter(RequestContext context) throws NotAuthenticatedException {
        Map<String, String[]> params = context.getParameters();
        Map<String, String[]> filter = new HashMap<>();
        filter.put(USER, new String[]{Integer.toString(super.resolveUser(accessRights).getId())});
        if (params != null && !params.isEmpty()) {

            // filter on publications is currently not required
//            String[] publications = params.get("publication");
//            if (publications != null && publications.length > 0) {
//                filter.put("publication", publications[0].split(","));
//            }
            String[] timespans = params.get("timespan");
            if (timespans != null && timespans.length > 0) {
                // only use the first one
                filter.put("timespan", new String[]{timespans[0]});
            }

            String[] subscriptions = params.get(SUBSCRIPTIONS);
            if (subscriptions != null && subscriptions.length > 0) {
                filter.put(SUBSCRIPTIONS, subscriptions[0].split(","));
            }

            String[] groups = params.get(GROUPS);
            if (groups != null && groups.length > 0) {
                filter.put(GROUPS, groups[0].split(","));
            }
            String latestKey =  getLatestKey(params);
            String[] latest = params.get(latestKey);
            boolean latestBool = false;
            if (latest != null && latest.length > 0) {
                latestBool = Boolean.parseBoolean(latest[0]);
            }
            filter.put(latestKey, new String[]{Boolean.toString(latestBool)});
        }

        return filter;
    }

    private String getLatestKey(Map<String, String[]> map) {
        return map.get("latest") != null ? "latest" : map.get("latestView") != null ? "latestView" : null;
    }

}
