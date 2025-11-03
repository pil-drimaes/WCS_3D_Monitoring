package com.example.WCS_DataStream.etl.scheduler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
public class SpringContext implements ApplicationContextAware {

	private static ApplicationContext context;

	@Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		SpringContext.context = applicationContext;
	}

	public static <T> T getBean(Class<T> type) {
		if (context == null) return null;
		try {
			return context.getBean(type);
		} catch (Exception e) {
			return null;
		}
	}
}


