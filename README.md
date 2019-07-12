# Mybatis-pe
Mybatis proxy expand

	<dependency>
		<groupId>com.github.lftao</groupId>
		<artifactId>mybatis-pe</artifactId>
		<version>0.0.1</version>
	</dependency>
     
	<bean id="sqlSessionFactory" class="com.lftao.mybatis.support.SqlSessionFactoryBean">
		<property name="dataSource" ref="dataSource" />
		<property name="mapperLocations" value="classpath*:com/javatao/*/dao/*.xml" />
		<property name="tableMapping" value="classpath*:com/javatao/*/entity/*.xml" />
	</bean>
	
	<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
		<property name="basePackage" value="com.javatao.*.dao" />
		<property name="mapperFactoryBeanClass" value="com.lftao.mybatis.support.MapperBeanPrpxy" />
	</bean>
  
# Mapping.xml
<?xml version="1.0" encoding="UTF-8"?>
<mapping>
	<default>
		<id property="id" column="id" />
		<property name="name" column="name" ></property>
	</default>
	
	<class name="com.javatao.mybatis.entity.Demo" table="tb_demo">
		<id property="id" column="id" />
		<property name="namex" column="name" />
	</class>
</mapping>

# interface
public interface DemoDao extends DaoInterface<Demo>{

}
