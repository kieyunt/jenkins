package com.ky.jenkins;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.transaction.HeuristicMixedException;

import org.apache.log4j.Logger;

import com.sun.mail.util.BASE64DecoderStream;

import my.ep.bpm.dto.BpmRequestDTO;
import my.ep.bpm.dto.BpmResponseDTO;
import my.ep.bpm.service.IBpmSO;
import my.ep.bpm.service.IGenericSO;
import my.ep.bpm.vo.WorklistItemVO;
import my.ep.bpm.vo.WorklistVO;
import my.ep.exception.EpBpmException;
import my.ep.exception.ExceptionCode;
import weblogic.jndi.Environment;
import weblogic.security.auth.Authenticate;

public class BpmErrorHandler {

	private static final String STATE_ASSIGNED = "ASSIGNED";
	private static final String ASSIGNMENT_ALL = "ALL";

	private Context initContext = null;
	private static Subject subject = null;

	private IBpmSO bpmSO;

	final static Logger log = Logger.getLogger(BpmErrorHandler.class);

	public void refireFLErrorHandlerTask() {
		log.info("Start refireFLErrorHandlerTask()"); 
		refireErrorHandlerTask("FL");
	}

	public void refireCTErrorHandlerTask() {
		log.info("Start refireCTErrorHandlerTask()");
		refireErrorHandlerTask("CT");
	}

	private void refireErrorHandlerTask(String moduleCode) {
		bpmSO = (IBpmSO) getServiceObject(IBpmSO.class);
		String loginId = "cdcadmin";
		BpmRequestDTO reqDTO = new BpmRequestDTO();

		int lastPage = 4;
		int limit = 50;
		for (int currentPage = 1; currentPage <= lastPage; currentPage++) {
			reqDTO.setLimit(limit);
			reqDTO.setOffset(((currentPage - 1) * 50) + 1);
			reqDTO.setState(STATE_ASSIGNED);
			reqDTO.setAssignment(ASSIGNMENT_ALL);
			Object token = bpmSO.authenticate(loginId);
			reqDTO.setCtxToken(token);
			BpmResponseDTO responseDTO = bpmSO.getWorklistItems(reqDTO);
			WorklistVO workListVO = responseDTO.getWorklistVO();
			List<WorklistItemVO> listItems = workListVO.getWorklistItem();
			if(listItems != null){
				for(WorklistItemVO worklistItemVO : listItems){
					String title = worklistItemVO.getTitle();
					if(title.startsWith(moduleCode)) {
						log.info(worklistItemVO.getInstanceId()+">>"+title);
						String taskId = worklistItemVO.getTaskId();
						submitError(taskId, token, title);
					}
				}
			}
			if (workListVO != null) {
				int rowSize = listItems.size();
				int totalItem = workListVO.getTotal();
				lastPage = (int) Math.ceil((double)totalItem /(double) limit);
				log.info("Total Page : " + lastPage);
				log.info("Current Page : " + currentPage);
				log.info("testing BPM******size="	+ rowSize);
				log.info("testing BPM******total=" + totalItem);
			} 			
		}
	}
	
	private void submitError(String taskId, Object token, String title){
		BpmRequestDTO reqDTO = new BpmRequestDTO();
		BpmResponseDTO resDTO = new BpmResponseDTO();
		
		reqDTO.setCtxToken(token);
		reqDTO.setTaskId(taskId);
		reqDTO.setOutcome("SUBMIT");
		resDTO = bpmSO.updateTask(reqDTO);
		if(resDTO.isSuccessful()){
			log.info("Successfully refired : " + title);
		} else {
			log.info("Fail refired : " + title);
		}
	}

	@SuppressWarnings("rawtypes")
	private IGenericSO getServiceObject(Class argClazz) {
		Environment environment = new Environment();
		environment.setSecurityPrincipal("app_system");
		environment.setSecurityCredentials(decrypt("qo5Zdiwn3UdAgkvmvTfPvA=="));
		environment.setProviderUrl(
				"t3://prdbpmsoa01.eperolehan.com.my:9001,prdbpmsoa01.eperolehan.com.my:9002,prdbpmsoa02.eperolehan.com.my:9003,prdbpmsoa02.eperolehan.com.my:9004");

		Subject clientSubject = new Subject();
		try {
			Authenticate.authenticate(environment, clientSubject);
			subject = clientSubject;
			initContext = environment.getInitialContext();
		} catch (LoginException e) {
			String errorMsg = "Error authenticating remote server with credentials UserName :"
					+ environment.getSecurityPrincipal() + " Password : " + environment.getSecurityCredentials();
			log.error(errorMsg);
			throw new EpBpmException(e.getMessage(), ExceptionCode.AUTHENTICATION_EXCEPTION);

		} catch (RemoteException e) {
			log.error("Error while while accessing remote server " + environment.getProviderUrl());
			throw new EpBpmException(e.getMessage(), ExceptionCode.COMMUNICATION_EXCEPTION);

		} catch (IOException e) {
			log.error("Error while authenticating remote server " + e);
			throw new EpBpmException(e.getMessage(), ExceptionCode.COMMUNICATION_EXCEPTION);

		} catch (NamingException e) {
			log.error("Error while creating the Initial Context" + e);
			throw new EpBpmException(e.getMessage(), ExceptionCode.GENERAL_EXCEPTION);
		}
		log.info("Remote Context created successfully");
		log.info("subject[" + subject + "]");
		if (log.isDebugEnabled()) {
			log.debug("Remote Context created successfully");
		}

		String argName = argClazz.getSimpleName();
		IGenericSO iGenericSO = null;
		try {
			String interfaceName = "IR" + argName.substring(1, argName.length());
			String str = argName + "#" + argClazz.getPackage().getName() + "." + interfaceName;
			log.info("interfaceName =" + interfaceName);
			log.info("str =" + str);
			EpBpmAction sampleAction = new EpBpmAction(str, initContext);
			iGenericSO = (IGenericSO) weblogic.security.Security.runAs(subject, sampleAction);
		} catch (Exception ex) {
			iGenericSO = null;
			log.error("Not able to create SO for " + argName, ex);
		}
		iGenericSO = (IGenericSO) Proxy.newProxyInstance(iGenericSO.getClass().getClassLoader(),
				new Class<?>[] { argClazz }, new EPWeblogicProxyHandler(iGenericSO));
		return iGenericSO;
	}

	private String decrypt(String str) {
		try {
			byte[] keyValue = "commercedc".getBytes();
			DESKeySpec desKeySpec = new DESKeySpec(keyValue);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(desKeySpec);
			Cipher dcipher = Cipher.getInstance("DES");

			dcipher.init(Cipher.DECRYPT_MODE, key);

			// decode with base64 to get bytes
			byte[] dec = BASE64DecoderStream.decode(str.getBytes());
			byte[] utf8 = dcipher.doFinal(dec);

			// create new string based on the specified charset
			return new String(utf8, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private class EpBpmAction implements PrivilegedAction {
		private String serviceName;
		private Context context;

		public EpBpmAction(String serviceName, Context context) {
			this.serviceName = serviceName;
			this.context = context;
		}

		public IGenericSO run() {
			IGenericSO iGenericSO;
			try {
				iGenericSO = (IGenericSO) context.lookup(this.serviceName);
				System.out.println("Success...");
			} catch (NamingException ex) {
				System.out.println("Exception START");
				ex.printStackTrace();
				System.out.println("Exception END");
				iGenericSO = null;
			}
			return iGenericSO;
		}
	}

	private static class EPWeblogicProxyHandler implements InvocationHandler {
		private Object so;
		public EPWeblogicProxyHandler(Object so) {
			this.so = so;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getDeclaringClass().equals(Object.class)) {
				return method.invoke(so, args);
			}
			return weblogic.security.Security.runAs(subject, new EPActionExecute(so, method, args));
		}
	}

	private static class EPActionExecute implements PrivilegedAction<Object> {
		private Object so;
		private Method method;
		private Object[] args;

		public EPActionExecute(Object so, Method method, Object[] args) {
			this.so = so;
			this.method = method;
			this.args = args;
		}

		@Override
		public Object run() {
			Object result = null;
			// TODO Auto-generated method stub
			try {
				result = method.invoke(so, args);

			} catch (IllegalArgumentException e) {
				log.error("IllegalArgumentException Error while Invocation of Method : " + method.getName()
						+ " on Target Class :" + method.getDeclaringClass().getSimpleName());
				log.error(e.getCause());
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				log.error("IllegalAccessException Error while Invocation of Method : " + method.getName()
						+ " on Target Class :" + method.getDeclaringClass().getSimpleName());
				log.error(e.getCause());
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				// Workaround for heuristic, return success
				if (e.getTargetException().getCause() instanceof HeuristicMixedException) {
					log.warn(e.getTargetException().getCause().getMessage(), e.getTargetException().getCause());
					return null;
				} else {
					log.error("InvocationTargetException Error while Invocation of Method : " + method.getName()
							+ " on Target Class :" + method.getDeclaringClass().getSimpleName());
					log.error(e.getCause());
					log.error(e.getTargetException().getCause());
					throw new RuntimeException(e);
				}
			}
			return result;
		}

	}
}
