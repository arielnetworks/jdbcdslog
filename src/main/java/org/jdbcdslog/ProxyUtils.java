/*
 *  ProxyUtils.java
 *
 *  $id$
 *
 * Copyright (C) FIL Limited. All rights reserved
 *
 * This software is the confidential and proprietary information of
 * FIL Limited You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license
 * agreement you entered into with FIL Limited.
 */

package org.jdbcdslog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import javax.sql.PooledConnection;
import javax.sql.XAConnection;

/**
 * @author a511990
 */
public class ProxyUtils {
    private static Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    /**
     * Find out all interfaces from clazz that is compatible with requiredInterface,
     * and generate proxy base on that.
     *
     * @param clazz
     * @param requiredInterface
     * @param invocationHandler
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T proxyForCompatibleInterfaces(Class<?> clazz, Class<T> requiredInterface, InvocationHandler invocationHandler) {
        // TODO: can cache clazz+iface vs compatibleInterfaces to avoid repetitive lookup
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(),
                                        findCompatibleInterfaces(clazz, requiredInterface),
                                        invocationHandler);
    }

    /**
     * Find all interfaces of clazz that is-a requiredInterface.
     *
     * @param clazz
     * @param requiredInterface
     * @return
     */
    public static Class<?>[] findCompatibleInterfaces(Class<?> clazz, Class<?> requiredInterface) {
        ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();
        for ( ; ! clazz.equals(Object.class) ; clazz = clazz.getSuperclass()) {
            for (Class<?> iface : clazz.getInterfaces()) {
                if (requiredInterface.isAssignableFrom(iface)) {
                    interfaces.add(iface);
                }
            }
        }
        return interfaces.toArray(EMPTY_CLASS_ARRAY);
    }

    private static HandlerCreator2<LogMetaData, Statement, InvocationHandler> statementHandlerCreator
            = new HandlerCreator2<LogMetaData, Statement, InvocationHandler>() {
        public InvocationHandler apply(LogMetaData logMetaData, Statement statement) {
            return new StatementLoggingHandler(logMetaData, statement);
        }
    };
    private static HandlerCreator3<LogMetaData, PreparedStatement, String, InvocationHandler> preparedStatementHandlerCreator
            = new HandlerCreator3<LogMetaData, PreparedStatement, String, InvocationHandler>() {
        public InvocationHandler apply(LogMetaData logMetaData, PreparedStatement ps, String sql) {
            return new PreparedStatementLoggingHandler(logMetaData, ps, sql);
        }
    };
    private static HandlerCreator3<LogMetaData, CallableStatement, String, InvocationHandler> callableStatementHandlerCreator
            = new HandlerCreator3<LogMetaData, CallableStatement, String, InvocationHandler>() {
        public InvocationHandler apply(LogMetaData logMetaData, CallableStatement statement, String sql) {
            return new CallableStatementLoggingHandler(logMetaData, statement, sql);
        }
    };
    private static HandlerCreator2<LogMetaData, Connection, InvocationHandler> connectionHandlerCreator
            = new HandlerCreator2<LogMetaData, Connection, InvocationHandler>() {
        public InvocationHandler apply(LogMetaData logMetaData, Connection connection) {
            return new ConnectionLoggingHandler(logMetaData, connection);
        }
    };
    private static HandlerCreator2<LogMetaData, ResultSet, InvocationHandler> resultSetHandlerCreator
            = new HandlerCreator2<LogMetaData, ResultSet, InvocationHandler>() {
        public InvocationHandler apply(LogMetaData logMetaData, ResultSet resultSet) {
            return new ResultSetLoggingHandler(logMetaData, resultSet);
        }
    };
    private static HandlerCreator<Object, InvocationHandler> connectionSourceHandlerCreator
            = new HandlerCreator<Object, InvocationHandler>() {
        public InvocationHandler apply(Object target) {
            return new ConnectionSourceLoggingHandler(target);
        }
    };

    public static void setStatementHandlerCreator(HandlerCreator2<LogMetaData, Statement, InvocationHandler> creator) {
        statementHandlerCreator = creator;
    }

    public static void setPreparedStatementHandlerCreator(HandlerCreator3<LogMetaData, PreparedStatement, String, InvocationHandler> creator) {
        preparedStatementHandlerCreator = creator;
    }

    public static void setCallableStatementHandlerCreator(HandlerCreator3<LogMetaData, CallableStatement, String, InvocationHandler> creator) {
        callableStatementHandlerCreator = creator;
    }

    public static void setConnectionHandlerCreator(HandlerCreator2<LogMetaData, Connection, InvocationHandler> creator) {
        connectionHandlerCreator = creator;
    }

    public static void setResultSetHandlerCreator(HandlerCreator2<LogMetaData, ResultSet, InvocationHandler> creator) {
        resultSetHandlerCreator = creator;
    }

    public static void setconnectionSourceHandlerCreator(HandlerCreator<Object, InvocationHandler> creator) {
        connectionSourceHandlerCreator = creator;
    }

    public static Statement wrapByStatementProxy(LogMetaData logMetaData, Statement s) {
        return ProxyUtils.proxyForCompatibleInterfaces(s.getClass(), Statement.class, statementHandlerCreator.apply(logMetaData, s));
    }

    public static PreparedStatement wrapByPreparedStatementProxy(LogMetaData logMetaData, PreparedStatement ps, String sql) {
        return ProxyUtils.proxyForCompatibleInterfaces(ps.getClass(), PreparedStatement.class, preparedStatementHandlerCreator.apply(logMetaData, ps, sql));
    }

    public static CallableStatement wrapByCallableStatementProxy(LogMetaData logMetaData, CallableStatement cs, String sql) {
        return ProxyUtils.proxyForCompatibleInterfaces(cs.getClass(), CallableStatement.class, callableStatementHandlerCreator.apply(logMetaData, cs, sql));
    }

    public static Connection wrapByConnectionProxy(Connection c) {
        return ProxyUtils.proxyForCompatibleInterfaces(c.getClass(), Connection.class, connectionHandlerCreator.apply(null, c));
    }

    public static Connection wrapByConnectionProxy(LogMetaData logMetaData, Connection c) {
        return ProxyUtils.proxyForCompatibleInterfaces(c.getClass(), Connection.class, connectionHandlerCreator.apply(logMetaData, c));
    }

    public static ResultSet wrapByResultSetProxy(LogMetaData logMetaData, ResultSet r) {
        return ProxyUtils.proxyForCompatibleInterfaces(r.getClass(), ResultSet.class, resultSetHandlerCreator.apply(logMetaData, r));
    }

    public static XAConnection wrapByXaConnection(XAConnection con) {
        return ProxyUtils.proxyForCompatibleInterfaces(con.getClass(), XAConnection.class, connectionSourceHandlerCreator.apply(con));
    }

    public static PooledConnection wrapByPooledConnection(PooledConnection con) {
        return ProxyUtils.proxyForCompatibleInterfaces(con.getClass(), PooledConnection.class, connectionSourceHandlerCreator.apply(con));
    }

    public static Object wrapByConnectionSourceProxy(Object r, Class<?> interf) {
        return ProxyUtils.proxyForCompatibleInterfaces(r.getClass(), interf, connectionSourceHandlerCreator.apply(r));
    }


    /**
     * Convenient helper to wrap object base on its type.
     *
     * @param r
     * @param args
     * @return
     * @throws Exception
     */
    public static Object wrap(LogMetaData logMetaData, Object r, Object...args) {
        if (r instanceof Connection) {
            return wrapByConnectionProxy(logMetaData, (Connection)r);
        } else if (r instanceof CallableStatement) {
            return wrapByCallableStatementProxy(logMetaData, (CallableStatement) r, (String) args[0]);
        } else if (r instanceof PreparedStatement) {
            return wrapByPreparedStatementProxy(logMetaData, (PreparedStatement) r, (String) args[0]);
        } else if (r instanceof Statement) {
            return wrapByStatementProxy(logMetaData, (Statement)r);
        } else if (r instanceof ResultSet) {
            return wrapByResultSetProxy(logMetaData, (ResultSet) r);
        } else {
            return r;
        }
    }


}
