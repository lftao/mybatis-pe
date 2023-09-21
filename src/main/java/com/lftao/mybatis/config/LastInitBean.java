package com.lftao.mybatis.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;

import com.lftao.mybatis.support.DaoInterface;

@Order(Integer.MAX_VALUE)
public class LastInitBean implements ApplicationListener<ContextRefreshedEvent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoMapping.class);
	private Set<Class<?>> loadingClass = new HashSet<>();

	private AutoMapping autoMapping;
	private boolean isload = false;

	public void setAutoMapping(AutoMapping autoMapping) {
		this.autoMapping = autoMapping;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(isload) {
			LOGGER.info("auto mapping is loaded");
			return;
		}
		ApplicationContext context = event.getApplicationContext();
		// 设置- Configuration
		SqlSessionFactory sessionFactory = context.getBean(SqlSessionFactory.class);
		autoMapping.setConfiguration(sessionFactory.getConfiguration());

		// 自动配置范型实体
		Map<String, DaoInterface> mapDao = context.getBeansOfType(DaoInterface.class);
		Collection<DaoInterface> daos = mapDao.values();
		LOGGER.info("init auto mapping ....");
		for (DaoInterface dao : daos) {
			Class entity = dao.getClassType();
			if (!loadingClass.contains(entity)) {
				autoMapping.doMappingTable(entity);
				loadingClass.add(entity);
			}
		}
		LOGGER.info("init auto mapping  end.");
		isload = true;
	}

}
