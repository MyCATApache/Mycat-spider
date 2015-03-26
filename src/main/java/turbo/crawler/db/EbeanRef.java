/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package turbo.crawler.db;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;

/**
 * EbeanRef 由于Ebean对scala的支持在如下代码中存在兼容性问题，故而此处采用JAVA替代
 * 
 * @author mclaren
 *
 */
public class EbeanRef {
	public static EbeanServer getEbeanServer(DBOfEbean dbRef) {
		DataSourceConfig ds = new DataSourceConfig();
		ds.setDriver(dbRef.jdbcDriver());
		ds.setUrl(dbRef.jdbcUrl());
		ds.setUsername(dbRef.jdbcUsername());
		ds.setMaxConnections(dbRef.dbMaxThreads());
		ds.setMinConnections(dbRef.dbMaxThreads());
		ds.setPassword(dbRef.jdbcPassword());

		ServerConfig cs = new ServerConfig();
		cs.setDataSourceConfig(ds);
		for (Class<?> entity : dbRef.entityTypes()) {
			cs.addClass(entity);
		}
		cs.setName(dbRef.name());

		cs.setDefaultServer(true);
		// 生产过程需关闭：否则每次表结构有变化都会重新建表导致原数据丢失
		cs.setDdlGenerate(dbRef.jdbcDdlAutoRun());
		cs.setDdlRun(dbRef.jdbcDdlAutoRun());

		System.out.println("++++++++++++++");
		return EbeanServerFactory.create(cs);
	}
}
