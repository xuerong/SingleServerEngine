package com.mm.engine.framework.data.persistence.ds.impl;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * 默认数据源工厂
 * <br/>
 * 基于 Apache Commons DBCP 实现
 *
 * @author huangyong
 * @since 2.3
 */
public class DefaultDataSourceFactory extends AbstractDataSourceFactory<BasicDataSource> {

    @Override
    public BasicDataSource createDataSource() {
        return new BasicDataSource();
    }

    @Override
    public void setDriver(BasicDataSource ds, String driver) {
        ds.setDriverClassName(driver);
    }

    @Override
    public void setUrl(BasicDataSource ds, String url) {
        ds.setUrl(url);
    }

    @Override
    public void setUsername(BasicDataSource ds, String username) {
        ds.setUsername(username);
    }

    @Override
    public void setPassword(BasicDataSource ds, String password) {
        ds.setPassword(password);
    }

    @Override
    public void setAdvancedConfig(BasicDataSource ds) {
        /**
         * <!-- 配置初始化大小、最小、最大 -->
         <property name="initialSize" value="1" />
         <property name="minIdle" value="1" />
         <property name="maxActive" value="100" />

         <!-- 配置获取连接等待超时的时间 -->
         <property name="maxWait" value="10000" />

         <!-- 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 -->
         <property name="timeBetweenEvictionRunsMillis" value="600000" />

         <!-- 配置一个连接在池中最小生存的时间，单位是毫秒 -->
         <property name="minEvictableIdleTimeMillis" value="600000" />

         <property name="testWhileIdle" value="true" />

         <!-- 这里建议配置为TRUE，防止取到的连接不可用 -->
         <property name="testOnBorrow" value="false" />
         <property name="testOnReturn" value="false" />

         <!-- 验证连接有效与否的SQL，不同的数据配置不同 -->
         <property name="validationQuery" value="select 1 " />
         */
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(100);
        ds.setMaxWait(10000);
        ds.setTimeBetweenEvictionRunsMillis(600000);
        ds.setMinEvictableIdleTimeMillis(600000);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setValidationQuery("select 1");
        // 解决 java.sql.SQLException: Already closed. 的问题（连接池会自动关闭长时间没有使用的连接）
//        ds.setValidationQuery("select 1 from dual");
    }
}
