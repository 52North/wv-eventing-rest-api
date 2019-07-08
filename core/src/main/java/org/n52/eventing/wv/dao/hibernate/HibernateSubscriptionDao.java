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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;

import com.google.common.collect.Sets;

import org.n52.eventing.wv.dao.SubscriptionDao;
import org.n52.eventing.wv.model.Notification;


/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateSubscriptionDao extends BaseHibernateDao<WvSubscription> implements SubscriptionDao {

    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_EVENT_FLAG = "flag";

    public HibernateSubscriptionDao(Session session) {
        super(session);
    }

    public QueryResult<WvSubscription> retrieveByUserNoPaging(WvUser user) throws DatabaseException {
        return new QueryResult<>(createUserQuery(user).list());
    }

    @Override
    public QueryResult<WvSubscription> retrieveByUser(WvUser user, Pagination pagination) throws DatabaseException {
       Query q = createUserQuery(user);

        int total = 0;
        if (pagination != null) {
            total = q.list().size();
            q.setFirstResult(pagination.getOffset());
            q.setMaxResults(pagination.getLimit());
        }

        List list = q.list();

        if (total == 0) {
            total =  list.size();
        }

        return new QueryResult<>(list, total);
    }

    @Override
    public QueryResult<WvSubscription> retrieveByUser(WvUser user) throws DatabaseException {
        return retrieveByUser(user, null);
    }

    @Override
    public QueryResult<WvSubscription> retrieveByGroups(Collection<Group> groups, Pagination pagination) throws DatabaseException {
        Query q = createGroupsQuery(groups);

        int total = 0;
        if (pagination != null) {
            total = q.list().size();
            q.setFirstResult(pagination.getOffset());
            q.setMaxResults(pagination.getLimit());
        }

        List list = q.list();

        if (total == 0) {
            total =  list.size();
        }

        return new QueryResult<>(list, total);
    }

    @Override
    public QueryResult<WvSubscription> retrieveByGroup(Group group) throws DatabaseException {
        return retrieveByGroup(group, null);
    }

    @Override
    public QueryResult<WvSubscription> retrieveByGroup(Group group, Pagination pagination)
            throws DatabaseException {
        return retrieveByGroups(Sets.newHashSet(group), null);
    }

    public QueryResult<WvSubscription> retrieveByGroupNoPagination(Group group) throws DatabaseException {
        return new QueryResult<>(createGroupsQuery(group).list());
    }

    public QueryResult<WvSubscription> retrieveByGroupsNoPagination(Collection<Group> groups) throws DatabaseException {
        return new QueryResult<>(createGroupsQuery(groups).list());
    }

    @Override
    public boolean hasEntity(WvSubscription subscription) {
        String paramGroup = "groupId";
        String paramNotification = "notificationId";
        StringBuilder builder = getBaseQuery();
        String hql = String.format(" join sub.notification noti WHERE noti.id=:%s ", paramNotification);
        if (subscription.getGroup() != null) {
            hql = String.format("%s AND sub.group.usergroupid=:%s", hql, paramGroup);
        }
        if (subscription.getUser()!= null) {
            hql = String.format("%s AND sub.user.userid=:%s", hql, PARAM_USER_ID);
        }
        builder.append(" AND noti.series.eventingFlag = :").append(PARAM_EVENT_FLAG);
        builder.append(hql);
        Query q = getSession().createQuery(builder.toString());
        q.setParameter(paramNotification, subscription.getNotification().getId());
        q.setParameter(PARAM_EVENT_FLAG, (short) 1);
        if (subscription.getGroup() != null) {
            q.setParameter(paramGroup, subscription.getGroup().getUsergroupid());
        }
        if (subscription.getUser()!= null) {
            q.setParameter(PARAM_USER_ID, subscription.getUser().getUserid());
        }

        return q.list().size() > 0;
    }

    @Override
    public QueryResult<WvSubscription> retrieveWithFilter(Map<String, String[]> filter, WvUser user, Pagination pagination) throws DatabaseException {
        List<Long> groupIds = null;
        String[] groupIdentifiers = filter.get("groups");
        if (groupIdentifiers != null && groupIdentifiers.length > 0) {
            groupIds = Stream.of(groupIdentifiers)
                    .distinct()
                    .map(s -> Long.parseLong(s))
                    .collect(Collectors.toList());
        }

        String[] seriesIdentifier = filter.get("publications");
        List<Integer> seriesIds = null;
        if (seriesIdentifier != null && seriesIdentifier.length > 0) {
            seriesIds = Stream.of(seriesIdentifier)
                    .distinct()
                    .map(s -> Integer.parseInt(s))
                    .collect(Collectors.toList());
        }

        String[] templateIdentifiers = filter.get("notifications");
        List<Integer> templateIds = null;
        if (templateIdentifiers != null && templateIdentifiers.length > 0) {
            templateIds = Stream.of(templateIdentifiers)
                    .distinct()
                    .map(s -> Integer.parseInt(s))
                    .collect(Collectors.toList());
        }

        // no filter defined --> return the subs of the user
        if (groupIds == null && seriesIdentifier == null && templateIds == null) {
            return retrieveByUser(user, pagination);
        }

        return internalRetrieve(pagination, groupIds, seriesIds, templateIds, user);
    }

    private QueryResult<WvSubscription> internalRetrieve(Pagination pagination, List<Long> groupIds, List<Integer> seriesIds, List<Integer> templateIds, WvUser user) {
        String paramGroupIds = "groupId";
        String paramSeriesIds = "seriesId";
        String paramNotificationIds = "notificationId";

        String subEntity = WvSubscription.class.getSimpleName();
        String groupEntity = Group.class.getSimpleName();
        String notiEntity = Notification.class.getSimpleName();

        boolean hasGroupIds = groupIds != null && !groupIds.isEmpty();
        boolean hasSeriesIds = seriesIds != null && !seriesIds.isEmpty();
        boolean hasNotificationIds = templateIds != null && !templateIds.isEmpty();
        boolean isNotAdmin = !user.isAdmin();

        String notificationClause = null;
        if (hasNotificationIds) {
            notificationClause = String.format(" sub.notification.id in (:%s) ", paramNotificationIds);
        }
        StringBuilder builder = getBaseQuery();
        if (isNotAdmin) {
            builder.append(" LEFT JOIN sub.group.users users");
        }

        if (hasGroupIds && hasSeriesIds) {
            builder.append(" JOIN sub.group gr ");
            if (isNotAdmin) {
                builder.append(" LEFT JOIN sub.group.users users");
            }
            builder.append(" WHERE ").append(notificationClause != null ? notificationClause + " AND " : "")
                    .append(" noti.series.id in (:").append(paramSeriesIds).append(")")
                    .append("AND sub.group.id in (:").append(paramGroupIds).append(")");
        }
        else if (hasGroupIds) {
            if (isNotAdmin) {
                builder.append(" LEFT JOIN sub.group.users users");
            }
            builder.append(" WHERE ").append(notificationClause != null ? notificationClause + " AND " : "")
                    .append(" sub.group.id in (:").append(paramGroupIds).append(")");
        }
        else if (hasSeriesIds) {
            builder.append(" WHERE ")
                    .append(notificationClause != null ? notificationClause + " AND " : "")
                    .append("noti.series.id in (:").append(paramSeriesIds).append(")");
        }
        else if (hasNotificationIds) {
            builder.append(" WHERE ").append(notificationClause);
        }

        else {
            throw new IllegalStateException("One filter (groups, notifications or series) has to provided");
        }

        if (isNotAdmin) {
            if (hasGroupIds) {
                builder.append(" AND (users.id = :").append(PARAM_USER_ID).append(" OR sub.user.id = :")
                        .append(PARAM_USER_ID).append(") ");
            } else {
                builder.append(" AND sub.user.id = :").append(PARAM_USER_ID);
            }
        }
        builder.append(" AND noti.series.eventingFlag = :").append(PARAM_EVENT_FLAG);
        builder.append(" order by sub.id asc");

        Query query = getSession().createQuery(builder.toString());
        query.setParameter(PARAM_EVENT_FLAG, (short) 1);
        if (isNotAdmin) {
            query.setParameter(PARAM_USER_ID, ((Integer)user.getId()).longValue());
        }
        if (hasGroupIds) {
            query.setParameterList(paramGroupIds, groupIds);
        }
        if (hasSeriesIds) {
            query.setParameterList(paramSeriesIds, seriesIds);
        }
        if (hasNotificationIds) {
            query.setParameter(paramNotificationIds, templateIds);
        }

        int total = 0;
        if (pagination != null) {
            total = query.list().size();
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }

        List list = query.list();

        if (total == 0) {
            total =  list.size();
        }

        return new QueryResult<>(list, total);
    }

    private Query createUserQuery(WvUser user) {
        StringBuilder builder = getBaseQuery();
        builder.append(" join sub.user u WHERE u.userid=:").append(PARAM_USER_ID)
                .append(" AND noti.series.eventingFlag = :").append(PARAM_EVENT_FLAG)
                .append(" order by sub.id asc");
        Query q = getSession().createQuery(builder.toString());
        q.setParameter(PARAM_USER_ID, user.getUserid());
        q.setParameter(PARAM_EVENT_FLAG, (short) 1);
        return q;
    }

    private Query createGroupsQuery(Group group) {
        return createGroupsQuery(Sets.newHashSet(group));
    }

    private Query createGroupsQuery(Collection<Group> groups) {
        String param = "groupIds";
        StringBuilder builder = getBaseQuery();
        builder.append(String.format(" join sub.group g WHERE g.usergroupids in (:%s)",
                param))
                .append(" AND noti.series.eventingFlag = :").append(PARAM_EVENT_FLAG)
                .append(" order by sub.id asc");
        Query q = getSession().createQuery(builder.toString());
        q.setParameter(param, groups.stream().map(group -> group.getUsergroupid()).collect(Collectors.toSet()));
        q.setParameter(PARAM_EVENT_FLAG, (short) 1);
        return q;
    }

    private StringBuilder getBaseQuery() {
        return new StringBuilder("SELECT distinct sub FROM ").append(WvSubscription.class.getSimpleName())
                .append(" sub ").append(" JOIN sub.notification noti ");
    }


}
