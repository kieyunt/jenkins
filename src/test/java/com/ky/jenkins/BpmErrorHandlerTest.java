package com.ky.jenkins;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BpmErrorHandlerTest {

	private BpmErrorHandler errorHandler;
	
	@Before
	public void setup() {
		errorHandler = new BpmErrorHandler();
	}
	
	@After
	public void destroy() {
		errorHandler = null;
	}
	
	@Test 
	public void testRefireFLErrorHandlerTask() {
		errorHandler.refireFLErrorHandlerTask();
		assertTrue(true);
	}
	
	@Test 
	public void testRefireCTErrorHandlerTask() {
		errorHandler.refireCTErrorHandlerTask();
		assertTrue(true);
	}
	
}
