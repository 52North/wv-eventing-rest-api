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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.NotificationDao;
import org.n52.eventing.wv.model.Notification;


/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateNotificationDao extends BaseHibernateDao<Notification> implements NotificationDao {

    public HibernateNotificationDao(Session session) {
        super(session);
    }


    @Override
    public QueryResult<Notification> retrieveBySeries(Pagination pagination, String... seriesIdentifier) throws DatabaseException {
        Set<Integer> idStream = Stream.of(seriesIdentifier).distinct().map(id -> Integer.parseInt(id)).collect(Collectors.toSet());
//        Map<String, Integer> idMap = idStream.collect(Collectors.toMap(id -> "param"+id, id -> Integer.parseInt(id)));

        String entity = Notification.class.getSimpleName();
        String paramEventingFlag = "paramEventingFlag";
        String paramSeriesIds = "paramSeriesId";
//        String whereClause = idMap.keySet().stream().map(id -> String.format("s.id=:%s", id)).collect(Collectors.joining(" OR "));
        String whereClause = String.format("s.id in (:%s)", paramSeriesIds);
        String hql = String.format("SELECT r FROM %s r join r.series s WHERE s.eventingFlag = :%s AND %s order by r.id asc", entity, paramEventingFlag, whereClause);
        Query q = getSession().createQuery(hql);

//        idMap.keySet().stream().forEach(param -> q.setParameter(param, idMap.get(param)));
        q.setParameter(paramSeriesIds, idStream);
        q.setParameter(paramEventingFlag, (short) 1);

        int total = 0;
        if (pagination != null) {
            total = q.list().size();
            q.setFirstResult(pagination.getOffset());
            q.setMaxResults(pagination.getLimit());
        }

        List list = q.list();
        if (total == 0) {
            total = list.size();
        }

        return new QueryResult<>(list, total);
    }

    @Override
    protected List<Predicate> customCriteria(CriteriaBuilder criteriaBuilder, Root<Notification> from) {
        List<Predicate> predicates = new LinkedList<>();
        Join<Object, Object> seriesJoin = from.join("series");
        predicates.add(criteriaBuilder.equal(seriesJoin.get("eventingFlag"), (short) 1));
        return predicates;
    }

    @Override
    public QueryResult<Notification> retrieveBySeries(String... seriesIdentifier) throws DatabaseException {
        return retrieveBySeries(null, seriesIdentifier);
    }

    @Override
    public boolean hasEntity(Notification rule) {
        return false;
    }

}
