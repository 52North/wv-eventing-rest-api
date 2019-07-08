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
package org.n52.eventing.wv.dao.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;
import org.joda.time.DateTime;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.EventDao;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.NotificationRule;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvEventSubscription;
import org.n52.eventing.wv.model.WvNotificationEvent;
import org.n52.eventing.wv.model.WvSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateEventDao
        extends
        BaseHibernateDao<WvEvent>
        implements
        EventDao {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateEventDao.class.getName());
    private final SubscriptionMaxTimeTransformer subscriptionMaxTimeTransformer = new SubscriptionMaxTimeTransformer();
    private static final String PARAM_SUB_ID = "subId";
    private static final String PARAM_SUB_IDS = "subIds";
    private static final String PARAM_NOTI_ID = "notiId";
    private static final String PARAM_NOTI_IDS = "notiIds";
    private static final String PARAM_SERIES_ID = "seriesId";
    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_GROUP_ID = "groupId";
    private static final String PARAM_EVENT_ID = "eventId";
    private static final String PARAM_EVENT_IDS = "eventIds";
    private static final String PARAM_CREATED = "createdTime";
    private static final String PARAM_TIMESTAMP = "timestampTime";
    private static final String PARAM_EVENT_FLAG = "flag";

    public HibernateEventDao(Session session) {
        super(session);
    }

    @Override
    public QueryResult<WvEvent> retrieveForSubscription(int... idInt) {
        return retrieveForSubscription(null, idInt);
    }

    @Override
    public QueryResult<WvEvent> retrieveForSubscription(Pagination pagination, int... idInt) {
        List<Integer> idList = new ArrayList<>(idInt.length);
        for (int i = 0; i < idInt.length; i++) {
            idList.add(idInt[i]);
        }

        return internalRetrieve(pagination, idList, null, null, null, null, null, null);
    }

    private QueryResult<WvEvent> internalRetrieve(Pagination pagination, Collection<Integer> subscriptionIds,
            Collection<Integer> seriesIds, Long userId, Collection<Integer> groupIds, Integer eventId, DateTime start, DateTime end) {
        LOG.trace("running internalRetrieve()");
        long startTimeTotal = System.currentTimeMillis();

        StringBuilder whereBuilder = new StringBuilder();
        List<Integer> notificationIds = null;
        if (eventId != null) {
            whereBuilder.append("ev.id in (:").append(PARAM_EVENT_ID).append(") ");
        } else {
            notificationIds = queryNotificationIds(subscriptionIds, seriesIds, userId, groupIds, eventId != null);

            if (notificationIds == null || notificationIds.isEmpty()) {
                return new QueryResult<>(Collections.emptyList(), 0);
            }
            // time filter
            String timeFilter;
            if (start != null && end != null) {
                timeFilter = " ev.created >= :start AND ev.created <= :end ";
            } else if (start == null && end != null) {
                timeFilter = "  ev.created <= :end ";
            } else if (start != null && end == null) {
                timeFilter = " ev.created >= :start ";
            } else {
                timeFilter = "";
            }

            whereBuilder.append(timeFilter)
                    .append(timeFilter.isEmpty() ? "" : " AND ");
            // we assume that every entry in event_log has an associated
            // notification
            whereBuilder.append("noev.notification.id in (:").append(PARAM_NOTI_IDS).append(") ");
        }

        String hql = getBaseSelect().append(whereBuilder.toString()).append(" order by ev asc").toString();

        LOG.trace("Creating HQL query: {}", hql);
        Session session = getSession();
        Query<WvEvent> query = session.createQuery(hql);

        setQueryParameter(query, eventId, notificationIds, start, end);

        int totalHits = 0;
        if (pagination != null) {
            long startTimeCount = System.currentTimeMillis();
            StringBuilder countSelect = getBaseCountSelect().append(whereBuilder.toString());
            Query<Long> countQuery = session.createQuery(countSelect.toString());
            setQueryParameter(countQuery, eventId, notificationIds, start, end);
            totalHits = ((Long) countQuery.getSingleResult()).intValue();
            LOG.trace("Querying latest data count from event table takes {} ms!", (System.currentTimeMillis() - startTimeCount));
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }

        LOG.trace("Querying data from event table");
        long startTime = System.currentTimeMillis();
        List<WvEvent> data = query.list();
        if (totalHits == 0) {
            totalHits = data.size();
        }
        LOG.trace("Querying data from event table takes {} ms!", (System.currentTimeMillis() - startTime));
        LOG.trace("Total querying of data from event table takes {} ms!", (System.currentTimeMillis() - startTimeTotal));
        return new QueryResult<>(data, totalHits);

    }

    private QueryResult<WvEvent> internalRetrieveLatest(Pagination pagination,
            List<Integer> subscriptionIds, List<Integer> seriesIds, Long userId, List<Integer> groupIds, DateTime start, DateTime end) {
        LOG.trace("running internalRetrieveLatest()");
        long startTimeTotal = System.currentTimeMillis();
//        QueryResult<SubscriptionMaxTime> resultSubscriptionMaxTimes =
//                querySubscriptionMaxTimes(subscriptionIds, seriesIds, userId, groupIds, start, end);
//        List<SubscriptionMaxTime> subscriptionMaxTimes = resultSubscriptionMaxTimes.getResult();

        List<Integer> eventIds = getEventIds(subscriptionIds, seriesIds, userId, groupIds, false);

        if (eventIds == null || eventIds.isEmpty()) {
            return new QueryResult<>(Collections.emptyList(), 0);
        }

        StringBuilder builder = new StringBuilder().append("SELECT DISTINCT ev")
                .append(" FROM ")
                .append(WvEvent.class.getSimpleName()).append(" as ev ")
                .append(" WHERE ")
                .append(" ev.id IN (:").append(PARAM_EVENT_IDS).append(")");


//        StringBuilder whereBuilder = new StringBuilder();
//        whereBuilder.append(" ( sub.id IN (:").append(PARAM_SUB_IDS)
//        .append(") OR subs.id IN (:").append(PARAM_SUB_IDS).append(")) ");
//
//        for (int i = 0; i < subscriptionMaxTimes.size(); i++) {
//            if (i != 0) {
//                whereBuilder.append(" OR ");
//            } else {
//                whereBuilder.append(" AND ");
//            }
//
//            whereBuilder.append(" (( sub.id = :").append(PARAM_SUB_ID + i)
//            .append(" OR subs.id = :").append(PARAM_SUB_ID + i).append(") ");
//            whereBuilder.append(" AND ev.created = :").append(PARAM_CREATED + i);
//            whereBuilder.append(" AND (ev.timestamp = :").append(PARAM_TIMESTAMP + i)
//                    .append(" OR ev.timestamp IS NULL) ) ");
//        }

//        String hql = getBaseSelect().append(whereBuilder.toString()).append(" order by ev asc").toString();
        String hql = builder.toString();

        LOG.trace("Creating HQL query: {}", hql);
        Query<WvEvent> query = getSession().createQuery(hql);
        query.setParameterList(PARAM_EVENT_IDS, eventIds);
//        Set<Integer> subIds = getSubscriptionIds(subscriptionMaxTimes);

//        query.setResultTransformer(new EventSubscriptionsTransformer(subIds));
//        setLatestQueryParameter(query, subIds, subscriptionMaxTimes);
        int totalHits = 0;
        if (pagination != null) {
            long startTimeCount = System.currentTimeMillis();
            StringBuilder countBuilder = new StringBuilder().append("SELECT count(DISTINCT ev.id)")
                    .append(" FROM ")
                    .append(WvEvent.class.getSimpleName()).append(" as ev ")
                    .append(" WHERE ")
                    .append(" ev.id IN (:").append(PARAM_EVENT_IDS).append(")");
            Query<Long> countQuery = getSession().createQuery(countBuilder.toString());
            countQuery.setParameterList(PARAM_EVENT_IDS, eventIds);
            totalHits = ((Long) countQuery.getSingleResult()).intValue();
            LOG.trace("Querying latest data count from event table takes {} ms!", (System.currentTimeMillis() - startTimeCount));
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }
        long startTime = System.currentTimeMillis();
        List<WvEvent> data = query.list();
        if (totalHits == 0) {
            totalHits = data.size();
        }

//        List<WvEventSubscription> data = (List<WvEventSubscription>)query.list();
        LOG.trace("Querying latest data from event table takes {} ms!", (System.currentTimeMillis() - startTime));
        LOG.trace("Total querying of latest data from event table takes {} ms!", (System.currentTimeMillis() - startTimeTotal));
        return new QueryResult<>(data, totalHits);
    }

    private List<Integer> getEventIds(List<Integer> subscriptionIds, List<Integer> seriesIds, Long userId,
            List<Integer> groupIds, boolean b) {
        List<Integer> notificationIds = queryNotificationIds(subscriptionIds, seriesIds, userId, groupIds, false);
        if (notificationIds == null || notificationIds.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder builder = new StringBuilder().append("SELECT DISTINCT noev.notification.id, max(noev.event.id) ")
                .append(" FROM ")
                .append(WvNotificationEvent.class.getSimpleName()).append(" as noev ")
                .append(" WHERE ")
                .append(" noev.notification.id in (:").append(PARAM_NOTI_IDS).append(") ");

        String hql = builder.append("group by noev.notification.id").append(" order by noev.notification.id asc").toString();

        LOG.trace("Creating HQL query: {}", hql);
        Session session = getSession();
        Query<Integer> query = session.createQuery(hql);
        query.setResultTransformer(new LatestEventTransformer());

        LOG.trace("Setting {} query parameter: {}", PARAM_NOTI_IDS, notificationIds);
        query.setParameterList(PARAM_NOTI_IDS, notificationIds);


        return query.list();
    }

    private Set<Integer> getSubscriptionIds(List<SubscriptionMaxTime> subscriptionMaxTimes) {
        return subscriptionMaxTimes.stream().map(s -> s.getSubscription()).collect(Collectors.toSet());
    }

    private List<Integer> queryNotificationIds(Collection<Integer> subscriptionIds, Collection<Integer> seriesIds, Long userId,
            Collection<Integer> groupIds, boolean userAndGroup) {
        LOG.trace("running queryNotificationIds()");
        long startTime = System.currentTimeMillis();
        boolean hasSubscriptionIds = subscriptionIds != null && !subscriptionIds.isEmpty();
        boolean hasSeriesIds = seriesIds != null && !seriesIds.isEmpty();
        boolean hasGroupIds = groupIds != null && !groupIds.isEmpty();
        boolean hasUserId = userId != null;
        StringBuilder builder = new StringBuilder();
        builder.append(
                "SELECT DISTINCT n.pkid FROM ")
                .append(" {h-schema}notification as n, ")
                .append(" {h-schema}subscription as s, ")
                .append(" {h-schema}series as ser ")
                .append(" WHERE s.notification_pkid = n.pkid ")
                .append(" AND n.series_pkid = ser.pkid ");
        if (userAndGroup) {
            builder.append(" AND (s.userid in (:").append(PARAM_USER_ID).append(") ");
            if (hasGroupIds) {
                builder.append(" OR s.usergroupid in (:").append(PARAM_GROUP_ID).append(")) ");
            } else {
                builder.append(" ) ");
            }
        } else {
            if (hasUserId && !hasGroupIds) {
                builder.append(" AND s.userid in (:").append(PARAM_USER_ID).append(") ");
            }
            if (hasGroupIds) {
                builder.append(" AND s.usergroupid in (:").append(PARAM_GROUP_ID).append(") ");
            }
        }
        if (hasSubscriptionIds) {
            builder.append(" AND s.pkid in (:").append(PARAM_SUB_ID).append(") ");
        }
        if (hasSeriesIds) {
            builder.append(" AND n.series_pkid in (:").append(PARAM_SERIES_ID).append(") ");
        }
        builder.append(" order by pkid");
        String hql = builder.toString();

        LOG.trace("Creating HQL query: {}", hql);
        Query<Integer> query = getSession().createNativeQuery(hql);
        if (userAndGroup) {
            LOG.trace("Setting {} query parameter: {}", PARAM_USER_ID, userId);
            query.setParameter(PARAM_USER_ID, userId);
            if (hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_GROUP_ID, groupIds);
                query.setParameterList(PARAM_GROUP_ID, groupIds);
            }
        } else {
            if (hasUserId && !hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_USER_ID, userId);
                query.setParameter(PARAM_USER_ID, userId);
            }
            if (hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_GROUP_ID, groupIds);
                query.setParameterList(PARAM_GROUP_ID, groupIds);
            }
        }
        if (hasSubscriptionIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SUB_ID, subscriptionIds);
            query.setParameterList(PARAM_SUB_ID, subscriptionIds);
        }
        if (hasSeriesIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SERIES_ID, seriesIds);
            query.setParameterList(PARAM_SERIES_ID, seriesIds);
        }
        List<Integer> data = query.list();
        LOG.trace("Querying for notifications takes {} ms!", (System.currentTimeMillis() - startTime));
        return data;
    }

    private List<Integer> querySubscriptionIds(Collection<Integer> subscriptionIds, Collection<Integer> seriesIds, Long userId,
            Collection<Integer> groupIds, boolean userAndGroup) {
        LOG.trace("running querySubscriptionIds()");
        long startTime = System.currentTimeMillis();
        boolean hasSubscriptionIds = subscriptionIds != null && !subscriptionIds.isEmpty();
        boolean hasSeriesIds = seriesIds != null && !seriesIds.isEmpty();
        boolean hasGroupIds = groupIds != null && !groupIds.isEmpty();
        boolean hasUserId = userId != null;
        StringBuilder builder = new StringBuilder();
        builder.append(
                "SELECT DISTINCT s.pkid FROM ")
                .append(" {h-schema}notification as n, ")
                .append(" {h-schema}subscription as s, ")
                .append(" {h-schema}series as ser ")
                .append(" WHERE s.notification_pkid = n.pkid ")
                .append(" AND n.series_pkid = ser.pkid ");
        if (userAndGroup) {
            builder.append(" AND (s.userid in (:").append(PARAM_USER_ID).append(") ");
            if (hasGroupIds) {
                builder.append(" OR s.usergroupid in (:").append(PARAM_GROUP_ID).append(")) ");
            } else {
                builder.append(" ) ");
            }
        } else {
            if (hasUserId && !hasGroupIds) {
                builder.append(" AND s.userid in (:").append(PARAM_USER_ID).append(") ");
            }
            if (hasGroupIds) {
                builder.append(" AND s.usergroupid in (:").append(PARAM_GROUP_ID).append(") ");
            }
        }
        if (hasSubscriptionIds) {
            builder.append(" AND s.pkid in (:").append(PARAM_SUB_ID).append(") ");
        }
        if (hasSeriesIds) {
            builder.append(" AND n.series_pkid in (:").append(PARAM_SERIES_ID).append(") ");
        }
        builder.append(" order by pkid");
        String hql = builder.toString();

        LOG.trace("Creating HQL query: {}", hql);
        Query<Integer> query = getSession().createNativeQuery(hql);
        if (userAndGroup) {
            LOG.trace("Setting {} query parameter: {}", PARAM_USER_ID, userId);
            query.setParameter(PARAM_USER_ID, userId);
            if (hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_GROUP_ID, groupIds);
                query.setParameterList(PARAM_GROUP_ID, groupIds);
            }
        } else {
            if (hasUserId && !hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_USER_ID, userId);
                query.setParameter(PARAM_USER_ID, userId);
            }
            if (hasGroupIds) {
                LOG.trace("Setting {} query parameter: {}", PARAM_GROUP_ID, groupIds);
                query.setParameterList(PARAM_GROUP_ID, groupIds);
            }
        }
        if (hasSubscriptionIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SUB_ID, subscriptionIds);
            query.setParameterList(PARAM_SUB_ID, subscriptionIds);
        }
        if (hasSeriesIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SERIES_ID, seriesIds);
            query.setParameterList(PARAM_SERIES_ID, seriesIds);
        }
        List<Integer> data = query.list();
        LOG.trace("Querying for subscriptions takes {} ms!", (System.currentTimeMillis() - startTime));
        return data;
    }

    private QueryResult<SubscriptionMaxTime> querySubscriptionMaxTimes(
            Collection<Integer> subscriptionIds, Collection<Integer> seriesIds, Long userId,
            Collection<Integer> groupIds, DateTime start, DateTime end) {
        LOG.trace("running querySubscriptionMaxTimes()");
        long startTime = System.currentTimeMillis();
        boolean hasSubscriptionIds = subscriptionIds != null && !subscriptionIds.isEmpty();
        boolean hasSeriesIds = seriesIds != null && !seriesIds.isEmpty();
        boolean hasGroupIds = false; //groupIds != null && !groupIds.isEmpty();
        boolean hasUserId = userId != null;


        StringBuilder builder = new StringBuilder();
        builder.append(
                "SELECT r.sub_id, r.created , max(e.observation_time_stamp) as timestamp FROM ")
                .append(" (SELECT s.pkid as sub_id, max(e.time_stamp_created) as created FROM ")
                .append(" {h-schema}notification_events  as ne, ")
                .append(" {h-schema}subscription  as s, ")
                .append(" {h-schema}event_log as e, ")
                .append(" {h-schema}series as ser ")
                .append(" WHERE s.notification_pkid = ne.notification_pkid ")
                .append(" AND ne.event_log_pkid = e.pkid ")
                .append(" AND ne.series_pkid = ser.pkid ");
        if (hasUserId && !hasGroupIds) {
            builder.append(" AND s.userid in (:").append(PARAM_USER_ID).append(") ");
        }
        if (hasGroupIds) {
            builder.append(" AND s.usergroupid in (:").append(PARAM_GROUP_ID).append(") ");
        }
        if (hasSubscriptionIds) {
            builder.append(" AND s.pkid in (:").append(PARAM_SUB_ID).append(") ");
        }
        if (hasSeriesIds) {
            builder.append(" AND ne.series_pkid in (:").append(PARAM_SERIES_ID).append(") ");
        }
        builder.append(" GROUP BY sub_id ")
                .append(" ORDER BY sub_id) r, ")
                .append(" {h-schema}notification_events  as ne, ")
                .append(" {h-schema}subscription  as s, ")
                .append(" {h-schema}event_log as e ")
                .append(" WHERE s.notification_pkid = ne.notification_pkid ")
                .append(" AND s.pkid = r.sub_id ")
                .append(" AND ne.event_log_pkid=e.pkid ")
                .append(" AND e.time_stamp_created = r.created ")
                .append(" GROUP BY sub_id, created")
                .append(" ORDER BY sub_id");

        String hql = builder.toString();

        LOG.trace("Creating HQL query: {}", hql);
        Query<?> query = getSession().createNativeQuery(hql);
        query.setResultTransformer(subscriptionMaxTimeTransformer);
//        query.setParameter(PARAM_EVENT_FLAG, (short) 1);
        if (hasUserId && !hasGroupIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_USER_ID, userId);
            query.setParameter(PARAM_USER_ID, userId);
        }
        if (hasGroupIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_GROUP_ID, groupIds);
            query.setParameterList(PARAM_GROUP_ID, groupIds);
        }
        if (hasSubscriptionIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SUB_ID, subscriptionIds);
            query.setParameterList(PARAM_SUB_ID, subscriptionIds);
        }
        if (hasSeriesIds) {
            LOG.trace("Setting {} query parameter: {}", PARAM_SERIES_ID, seriesIds);
            query.setParameterList(PARAM_SERIES_ID, seriesIds);
        }

        int totalHits = 0;

        LOG.trace("Querying max times for subscriptions");

        List<SubscriptionMaxTime> data = (List<SubscriptionMaxTime>) query.list();
        if (totalHits == 0) {
            totalHits = data.size();
        }
        LOG.trace("Querying max times for subscriptions takes {} ms!", (System.currentTimeMillis() - startTime));
        return new QueryResult(data, totalHits);
    }

    @Override
    public void store(WvEvent o, boolean transactionInProgress)
            throws DatabaseException {
        if (o.getEventMessage() != null) {
            if (!transactionInProgress) {
                Transaction trans = getSession().beginTransaction();
                getSession().save(o.getEventMessage());
                trans.commit();
            } else {
                getSession().save(o.getEventMessage());
            }
        }

        super.store(o, transactionInProgress);
    }

    @Override
    public void remove(WvEvent o, boolean transactionInProgress)
            throws DatabaseException {
        if (o.getEventMessage() != null) {
            if (!transactionInProgress) {
                Transaction trans = getSession().beginTransaction();
                getSession().delete(o.getEventMessage());
                trans.commit();
            } else {
                getSession().delete(o.getEventMessage());
            }
        }

        super.remove(o, transactionInProgress);
    }

    @Override
    public QueryResult<WvEvent> retrieveWithFilter(Map<String, String[]> filter, Pagination pagination) {
        List<Integer> subscriptionIds = null;

        String[] subscriptionIdentifier = filter.get("subscriptions");
        if (subscriptionIdentifier != null && subscriptionIdentifier.length > 0) {
            subscriptionIds = Stream.of(subscriptionIdentifier).map(s -> Integer.parseInt(s)).distinct()
                    .collect(Collectors.toList());
        }

        String[] seriesIdentifier = filter.get("publications");
        List<Integer> seriesIds = null;
        if (seriesIdentifier != null && seriesIdentifier.length > 0) {
            seriesIds =
                    Stream.of(seriesIdentifier).distinct().map(s -> Integer.parseInt(s)).collect(Collectors.toList());
        }

        String[] groupIdentifier = filter.get("groups");
        List<Integer> groupIds = null;
        if (groupIdentifier != null && groupIdentifier.length > 0) {
            groupIds =
                    Stream.of(groupIdentifier).distinct().map(s -> Integer.parseInt(s)).collect(Collectors.toList());
        }

        String[] timespans = filter.get("timespan");
        DateTime start = null;
        DateTime end = null;
        if (timespans != null && timespans.length > 0) {
            String[] startEnd = timespans[0].split("/");
            start = new DateTime(startEnd[0]);
            end = new DateTime(startEnd[1]);
        }
        Long userId = (filter.containsKey("user") && filter.get("user")[0] != null && !filter.get("user")[0].isEmpty())
                ? Long.parseLong(filter.get("user")[0])
                : null;
        Integer eventId = (filter.containsKey("event") && filter.get("event")[0] != null && !filter.get("event")[0].isEmpty())
                ? Integer.parseInt(filter.get("event")[0])
                : null;

        if (filter.containsKey("latest") && Boolean.parseBoolean(filter.get("latest")[0])) {
            return internalRetrieveLatest(pagination, subscriptionIds, seriesIds, userId, groupIds, start, end);
        } else if (filter.containsKey("latestView") && Boolean.parseBoolean(filter.get("latestView")[0])) {
            return internalRetrieve(pagination, subscriptionIds, seriesIds, userId, groupIds, eventId, start, end);
        }
        return internalRetrieve(pagination, subscriptionIds, seriesIds, userId, groupIds, eventId, start, end);
    }

    private StringBuilder getBaseSelect() {
        return new StringBuilder().append("SELECT DISTINCT ev")
                .append(" FROM ")
                .append(WvEvent.class.getSimpleName()).append(" as ev, ")
                .append(WvNotificationEvent.class.getSimpleName()).append(" as noev ")
//                .append(" LEFT JOIN noev.event as ev ")
//                .append(" LEFT JOIN ev.series as ser ")
//                .append(" LEFT JOIN ser.notificationRules as rule ")
//                .append(" LEFT JOIN noti.subscriptions as sub ")
//                .append(" LEFT JOIN noti.notificationRules as rule ")
//                .append(" LEFT JOIN rule.notification as rulenoti ")
//                .append(" LEFT JOIN rulenoti.subscriptions as subs ")
                .append(" WHERE ")
                .append(" ev.id = noev.event AND ");
    }

    private StringBuilder getBaseCountSelect() {
        return new StringBuilder().append("SELECT count(DISTINCT ev.id) ")
                .append(" FROM ")
                .append(WvEvent.class.getSimpleName()).append(" as ev, ")
                .append(WvNotificationEvent.class.getSimpleName()).append(" as noev ")
//                .append(" FROM ")
//                .append(WvEvent.class.getSimpleName()).append(" as ev ")
//                .append(" LEFT JOIN noev.event as ev ")
//                .append(" LEFT JOIN noev.notification as noti ")
//                .append(" LEFT JOIN noti.subscriptions as sub ")
//                .append(" LEFT JOIN noti.notificationRules as rule ")
//                .append(" LEFT JOIN rule.notification as rulenoti ")
//                .append(" LEFT JOIN rulenoti.subscriptions as subs ")
                .append(" WHERE ")
                .append(" ev.id = noev.event AND ");
    }

    private void setQueryParameter(Query<?> query, Integer eventId, Collection<Integer> notificationIds,
            DateTime start, DateTime end) {
        if (eventId != null) {
            LOG.trace("Setting {} query parameter: {}", PARAM_EVENT_ID, eventId);
            query.setParameter(PARAM_EVENT_ID, eventId);
        } else {
            if (notificationIds != null && !notificationIds.isEmpty()) {
                LOG.trace("Setting {} query parameter: {}", PARAM_NOTI_IDS, notificationIds);
                query.setParameterList(PARAM_NOTI_IDS, notificationIds);
            }
            if (start != null) {
                LOG.trace("Setting {} query parameter: {}", "start", start);
                query.setParameter("start", start.toDate());
            }
            if (end != null) {
                LOG.trace("Setting {} query parameter: {}", "end", end);
                query.setParameter("end", end.toDate());
            }
        }
    }


    private void setLatestQueryParameter(Query<?> query, Set<Integer> subIds, List<SubscriptionMaxTime> subscriptionMaxTimes) {
        LOG.trace("Setting {} query parameter", PARAM_SUB_IDS);
        query.setParameterList(PARAM_SUB_IDS, subIds);
        for (int i = 0; i < subscriptionMaxTimes.size(); i++) {
            SubscriptionMaxTime smt = subscriptionMaxTimes.get(i);

            LOG.trace("Setting {} query parameter: {}", PARAM_SUB_ID + i, smt.getSubscription());
            query.setParameter(PARAM_SUB_ID + i, smt.getSubscription());

            LOG.trace("Setting {} query parameter: {}", PARAM_CREATED + i, smt.getCreated());
            query.setParameter(PARAM_CREATED + i, smt.getCreated());

            LOG.trace("Setting {} query parameter: {}", PARAM_TIMESTAMP + i, smt.getObservation());
            query.setParameter(PARAM_TIMESTAMP + i, smt.getObservation());
        }
    }


    private class SubscriptionMaxTimeTransformer
            implements
            ResultTransformer {
        private static final long serialVersionUID = -5900788146609835321L;

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            if (tuple != null && tuple[0] instanceof Integer) {
                SubscriptionMaxTime subscriptionMaxTime = new SubscriptionMaxTime((Integer) tuple[0]);
                if (tuple.length >= 2 && tuple[1] instanceof Date) {
                    subscriptionMaxTime.setCreated((Date) tuple[1]);
                }
                if (tuple.length == 3 && tuple[2] instanceof Date) {
                    subscriptionMaxTime.setObservation((Date) tuple[2]);
                }
                return subscriptionMaxTime;
            }
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes" })
        public List transformList(List collection) {
            return collection;
        }
    }

    private class SubscriptionMaxTime {
        private Integer subscription;
        private Date created;
        private Date observation;

        public SubscriptionMaxTime(Integer subscription) {
            setSubscription(subscription);
        }

        public Integer getSubscription() {
            return subscription;
        }

        public void setSubscription(Integer subscription) {
            this.subscription = subscription;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public Date getObservation() {
            return observation;
        }

        public void setObservation(Date observation) {
            this.observation = observation;
        }
    }

    private class LatestEventTransformer implements
    ResultTransformer {

        private static final long serialVersionUID = -4408731837345005394L;

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            if (tuple != null && tuple.length == 2 && tuple[1] != null) {
                return tuple[1];
            }
            return null;
        }

        @Override
        public List transformList(List collection) {
            return collection;
        }

    }

    @Deprecated
    private class EventSubscriptionsTransformer
            implements
            ResultTransformer {

        private static final long serialVersionUID = 1116726789610829703L;
        private Collection<Integer> subscriptions;

        public EventSubscriptionsTransformer(Collection<Integer> subIds) {
            this.subscriptions = subIds;
        }

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
//            if (tuple != null && tuple[0] instanceof WvEvent) {
//                WvEventSubscription eventSubscription = new WvEventSubscription((WvEvent) tuple[0]);
//                if (tuple.length == 2 && tuple[1] instanceof Integer) {
//                    eventSubscription.addSubscription((Integer) tuple[1]);
//                }
//                return eventSubscription;
//            }
            // return null;
            if (tuple != null && tuple[0] instanceof WvNotificationEvent) {
                WvNotificationEvent noev = (WvNotificationEvent) tuple[0];
                WvEventSubscription eventSubscription =
                        new WvEventSubscription(noev.getEvent(), getSubscriptions(noev));
                return eventSubscription;
            }
            return null;
        }

        private Set<WvSubscription> getSubscriptions(WvNotificationEvent noev) {
            if (noev.getNotification() != null) {
                Notification notification = noev.getNotification();
                if (notification.hasSubscriptions()) {
                    return checkWithRequested(notification.getSubscriptions());
                } else if (notification.hasNotificationRules()) {
                    Set<WvSubscription> subs = new LinkedHashSet<>();
                    for (NotificationRule nr : notification.getNotificationRules()) {
                       subs.addAll(nr.getNotification().getSubscriptions());
                    }
                    return checkWithRequested(subs);
                }
             }
            return Collections.emptySet();
        }

        private Set<WvSubscription> checkWithRequested(Set<WvSubscription> subs) {
            if (this.subscriptions != null) {
                return subs.stream().filter(s -> this.subscriptions.contains(s.getId())).collect(Collectors.toSet());
            }
            return subs;
        }

        @Override
        @SuppressWarnings({ "rawtypes" })
        public List transformList(List collection) {
            Map<Integer, WvEventSubscription> map = new LinkedHashMap<Integer, WvEventSubscription>();
            for (WvEventSubscription object : (List<WvEventSubscription>) collection) {
                if (map.containsKey(object.getId())) {
                    map.get(object.getId()).addSubscriptions(object.getSubscriptions());
                } else {
                   map.put(object.getId(), object);
                }
            }
            return Lists.newLinkedList(map.values());
//            return collection;
        }
    }

}
