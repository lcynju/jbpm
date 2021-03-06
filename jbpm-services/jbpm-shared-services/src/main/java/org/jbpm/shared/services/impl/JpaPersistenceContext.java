package org.jbpm.shared.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.kie.internal.command.Context;
import org.kie.internal.command.World;

public class JpaPersistenceContext implements Context {

	public final static String FIRST_RESULT = "firstResult";
    public final static String MAX_RESULTS = "maxResults";
	
	private EntityManager em;
	
	public JpaPersistenceContext(EntityManager em) {
		this.em = em;
	}
	
	protected Query getQueryByName(String queryName, Map<String, Object> params) {
		String queryStr = QueryManager.get().getQuery(queryName, params);
		Query query = null;
		if (queryStr != null) {
			query = this.em.createQuery(queryStr);
		} else {
			query = this.em.createNamedQuery(queryName);
		}
		
		return query;
	}
	
	
	public <T> T queryWithParametersInTransaction(String queryName,
			Map<String, Object> params, Class<T> clazz) {
		check();
		
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, false, LockModeType.NONE, clazz, query);
	}

	
	public <T> T queryAndLockWithParametersInTransaction(String queryName,
			Map<String, Object> params, boolean singleResult, Class<T> clazz) {
		check();
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, singleResult, LockModeType.PESSIMISTIC_WRITE, clazz, query);
	}

	
	@SuppressWarnings("unchecked")
	public <T> T queryInTransaction(String queryName, Class<T> clazz) {
		check();
		Query query = this.em.createNamedQuery(queryName);
		return (T) query.getResultList();
	}

	
	@SuppressWarnings("unchecked")
	public <T> T queryStringInTransaction(String queryString, Class<T> clazz) {
		check();
		Query query = this.em.createQuery(queryString);
		return (T) query.getResultList();
	}

	
	public <T> T queryStringWithParametersInTransaction(String queryString,
			Map<String, Object> params, Class<T> clazz) {
		check();
		Query query = this.em.createQuery(queryString);
				
		return queryStringWithParameters(params, false, LockModeType.NONE, clazz, query);
	}

	
		
	public <T> T queryAndLockStringWithParametersInTransaction(
			String queryName, Map<String, Object> params, boolean singleResult,
			Class<T> clazz) {
		check();
		Query query = this.em.createNamedQuery(queryName);
		return queryStringWithParameters(params, singleResult, LockModeType.PESSIMISTIC_WRITE, clazz, query);	
	}

	
	public int executeUpdateString(String updateString) {
		check();
		Query query = this.em.createQuery(updateString);
		return query.executeUpdate();
	}

	
	public HashMap<String, Object> addParametersToMap(Object... parameterValues) {
		HashMap<String, Object> parameters = new HashMap<String, Object>();
        
        if( parameterValues.length % 2 != 0 ) { 
            throw new RuntimeException("Expected an even number of parameters, not " + parameterValues.length);
        }
        
        for( int i = 0; i < parameterValues.length; ++i ) {
            String parameterName = null;
            if( parameterValues[i] instanceof String ) { 
                parameterName = (String) parameterValues[i];
            } else { 
                throw new RuntimeException("Expected a String as the parameter name, not a " + parameterValues[i].getClass().getSimpleName());
            }
            ++i;
            parameters.put(parameterName, parameterValues[i]);
        }
        
        return parameters;
	}

	
	public <T> T persist(T object) {
		check();
		this.em.persist( object );        
        return object;
	}

	
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		check();
        return this.em.find( entityClass, primaryKey );
	}

	
	public <T> T remove(T entity) {
		check();
		em.remove( entity );
		return entity;
	}

	
	public <T> T merge(T entity) {
		check();
		return this.em.merge(entity);
	}

	@SuppressWarnings("unchecked")
	private <T> T queryStringWithParameters(Map<String, Object> params, boolean singleResult, LockModeType lockMode,
			Class<T> clazz, Query query) {
		;
		if (lockMode != null) {
			query.setLockMode(lockMode);
		}
		if (params != null && !params.isEmpty()) {
			for (String name : params.keySet()) {
				if (FIRST_RESULT.equals(name)) {
					query.setFirstResult((Integer) params.get(name));
					continue;
				}
				else if (MAX_RESULTS.equals(name)) {
					query.setMaxResults((Integer) params.get(name));
					continue;
				} 
				// skip control parameters
				else if (QueryManager.ASCENDING_KEY.equals(name) 
						|| QueryManager.DESCENDING_KEY.equals(name)
						|| QueryManager.ORDER_BY_KEY.equals(name)) {
					continue;
				}
				query.setParameter(name, params.get(name));
			}
		}
		if (singleResult) {
			return (T) query.getSingleResult();
		}
		return (T) query.getResultList();
	}

	
	public boolean isOpen() {
		if (this.em == null) {
			return false;
		}
		return this.em.isOpen();
	}

	
	public void joinTransaction() {
		if (this.em == null) {
			return;
		}
		this.em.joinTransaction();
	}

	
	public void close(boolean txOwner) {
		check();
		if (txOwner) {
			this.em.clear();
		}
		this.em.close();
	}
	
	protected void check() {
		if (em == null || !em.isOpen()) {
			throw new IllegalStateException("Entity manager is null or is closed, exiting...");
		}
	}


	@Override
	public World getContextManager() {
		return null;
	}


	@Override
	public String getName() {
		return this.getClass().getName();
	}


	@Override
	public Object get(String identifier) {
		return null;
	}


	@Override
	public void set(String identifier, Object value) {

	}


	@Override
	public void remove(String identifier) {

	}
}
