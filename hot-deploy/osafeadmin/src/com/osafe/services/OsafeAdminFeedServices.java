package com.osafe.services;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import javolution.util.FastList;
import javolution.util.FastMap;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import com.osafe.feeds.FeedsUtil;
import com.osafe.feeds.osafefeeds.*;
import com.osafe.util.OsafeAdminUtil;
import org.ofbiz.base.util.MessageString;

public class OsafeAdminFeedServices 
{
    public static final String module = OsafeAdminFeedServices.class.getName();
    private static final String resource = "OSafeAdminUiLabels";
    
    public static Map<String, Object> clientProductRatingUpdate(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsInRatingDir = (String)context.get("feedsInRatingDir");
        String feedsInSuccessSubDir = (String)context.get("feedsInSuccessSubDir");
        String feedsInErrorSubDir = (String)context.get("feedsInErrorSubDir");

        // Check passed params
        if (UtilValidate.isEmpty(feedsInRatingDir)) 
        {
        	feedsInRatingDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_RATING_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsInSuccessSubDir)) 
        {
        	feedsInSuccessSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_SUCCESS_SUB_DIR");
        }
        if (UtilValidate.isEmpty(feedsInErrorSubDir)) 
        {
        	feedsInErrorSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_ERROR_SUB_DIR");
        }
        String processedDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInSuccessSubDir)) 
        {
        	processedDir = feedsInSuccessSubDir + processedDir; 
        }
        String errorDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInErrorSubDir)) 
        {
        	errorDir = feedsInErrorSubDir + errorDir; 
        }
        
        if (UtilValidate.isNotEmpty(feedsInRatingDir)) 
        {
            long pauseLong = 0;
            File baseDir = new File(feedsInRatingDir);

            if (baseDir.isDirectory() && baseDir.canRead()) 
            {
                File[] fileArray = baseDir.listFiles();
                FastList<File> files = FastList.newInstance();
                
                for (File file: fileArray) 
                {
                    if (file.getName().toUpperCase().endsWith("XML")) 
                    {
                        files.add(file);
                    }
                }
                int passes=0;
                int lastUnprocessedFilesCount = 0;
                FastList<File> unprocessedFiles = FastList.newInstance();
                while (files.size()>0 && files.size() != lastUnprocessedFilesCount) 
                {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = FastList.newInstance();
                    for (File f: files) 
                    {
                    	String uploadTempDir = System.getProperty("ofbiz.home") + "/runtime/tmp/upload/";
                    	try 
                    	{
                    	    FileUtils.copyFileToDirectory(f, new File(uploadTempDir));
                    	} 
                    	catch (IOException e) 
                    	{
                    		Debug.log("Can not copy file " + f.getName() + " to Directory " +uploadTempDir);
						}
                    	
                        Map<String, Object> importClientProductRatingXMLTemplateCtx = UtilMisc.toMap("xmlDataDir", uploadTempDir,
                                "autoLoad", Boolean.TRUE,
                                "userLogin", userLogin);
                        try 
                        {
	                        String xmlDataFile = uploadTempDir + f.getName();
	                            importClientProductRatingXMLTemplateCtx.put("xmlDataFile", xmlDataFile);
	                            Map result  = dispatcher.runSync("importClientProductRatingXMLTemplate", importClientProductRatingXMLTemplateCtx);
	                            List<String> serviceMsg = (List)result.get("messages");
	                            if(serviceMsg.size() > 0 && serviceMsg.contains("SUCCESS")) 
	                            {
	                                try 
	                                {
	                        	        FileUtils.copyFileToDirectory(f, new File(feedsInRatingDir , processedDir));
	                        	        f.delete();
	                        	    } 
	                                catch (IOException e) 
	                                {
	                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +processedDir);
	    						    }
	                            } 
	                            else 
	                            {
	                            	try 
	                            	{
	                        	        FileUtils.copyFileToDirectory(f, new File(feedsInRatingDir , errorDir));
	                        	        f.delete();
	                        	    } 
	                            	catch (IOException e) 
	                            	{
	                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +errorDir);
	    						    }
	                            }
                        } 
                        catch (Exception e) 
                        {
                            unprocessedFiles.add(f);
                            Debug.log("Failed " + f + " adding to retry list for next pass");
                        }
                        // pause in between files
                        if (pauseLong > 0) 
                        {
                            Debug.log("Pausing for [" + pauseLong + "] seconds - " + UtilDateTime.nowTimestamp());
                            try 
                            {
                                Thread.sleep((pauseLong * 1000));
                            } 
                            catch (InterruptedException ie) 
                            {
                                Debug.log("Pause finished - " + UtilDateTime.nowTimestamp());
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                }
                lastUnprocessedFilesCount=unprocessedFiles.size();
                
            } 
            else 
            {
            	Debug.log("path not found or can't be read");
            }
        } 
        else 
        {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    
    public static Map<String, Object> clientProductUpdate(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsInProductDir = (String)context.get("feedsInProductDir");
        String feedsInSuccessSubDir = (String)context.get("feedsInSuccessSubDir");
        String feedsInErrorSubDir = (String)context.get("feedsInErrorSubDir");

        // Check passed params
        if (UtilValidate.isEmpty(feedsInProductDir)) 
        {
        	feedsInProductDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_PRODUCT_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsInSuccessSubDir)) 
        {
        	feedsInSuccessSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_SUCCESS_SUB_DIR");
        }
        if (UtilValidate.isEmpty(feedsInErrorSubDir)) 
        {
        	feedsInErrorSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_ERROR_SUB_DIR");
        }

        String processedDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInSuccessSubDir)) 
        {
        	processedDir = feedsInSuccessSubDir + processedDir; 
        }
        String errorDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInErrorSubDir)) 
        {
        	errorDir = feedsInErrorSubDir + errorDir; 
        }
        
        if (UtilValidate.isNotEmpty(feedsInProductDir)) 
        {
            long pauseLong = 0;
            File baseDir = new File(feedsInProductDir);

            if (baseDir.isDirectory() && baseDir.canRead()) 
            {
                File[] fileArray = baseDir.listFiles();
                FastList<File> files = FastList.newInstance();
                
                for (File file: fileArray) 
                {
                    if (file.getName().toUpperCase().endsWith("XML")) 
                    {
                        files.add(file);
                    }
                }
                int passes=0;
                int lastUnprocessedFilesCount = 0;
                FastList<File> unprocessedFiles = FastList.newInstance();
                while (files.size()>0 && files.size() != lastUnprocessedFilesCount) 
                {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = FastList.newInstance();
                    for (File f: files) 
                    {
                    	String uploadTempDir = System.getProperty("ofbiz.home") + "/runtime/tmp/upload/";
                    	try 
                    	{
                    	    FileUtils.copyFileToDirectory(f, new File(uploadTempDir));
                    	} 
                    	catch (IOException e) 
                    	{
                    		Debug.log("Can not copy file " + f.getName() + " to Directory " +uploadTempDir);
						}
                    	
                        Map<String, Object> importClientProductTemplateCtx = UtilMisc.toMap("xmlDataDir", uploadTempDir,"removeAll",Boolean.FALSE,"autoLoad",Boolean.TRUE,"userLogin",userLogin);
                        try 
                        {
                        	String xmlDataFile = uploadTempDir + f.getName();
                        	importClientProductTemplateCtx.put("xmlDataFile", xmlDataFile);
                        	
                        	  List prodCatErrorList = FastList.newInstance();
	                          List prodCatWarningList = FastList.newInstance();
	                          List productErrorList = FastList.newInstance();
	                          List productWarningList = FastList.newInstance();
	                          List productAssocErrorList = FastList.newInstance();
	                          List productAssocWarningList = FastList.newInstance();
	                          List productFeatureSwatchErrorList = FastList.newInstance();
	                          List productFeatureSwatchWarningList = FastList.newInstance();
	                          List productManufacturerErrorList = FastList.newInstance();
	                          List productManufacturerWarningList = FastList.newInstance();
                        	
                        	try 
                            {
                        		List productCatDataList = FastList.newInstance();
                        		List productDataList = FastList.newInstance();
                        		List productAssocDataList = FastList.newInstance();
                        		List productFeatureSwatchDataList = FastList.newInstance();
                        		List manufacturerDataList = FastList.newInstance();
                        		List errorMessageList = FastList.newInstance();
                        		
                        		if (UtilValidate.isNotEmpty(uploadTempDir) && UtilValidate.isNotEmpty(f.getName())) 
                        		{
                        			Map<String, Object> productDataListSvcCtx = FastMap.newInstance();
                        			productDataListSvcCtx.put("productFilePath", uploadTempDir);
                        			productDataListSvcCtx.put("productFileName", f.getName());
                        			
                        			Map productDataListSvcRes = dispatcher.runSync("getProductDataListFromFile", productDataListSvcCtx);
                        			
                        			productCatDataList = UtilGenerics.checkList(productDataListSvcRes.get("productCatDataList"), Map.class);
                        			productDataList = UtilGenerics.checkList(productDataListSvcRes.get("productDataList"), Map.class);
                        			productAssocDataList = UtilGenerics.checkList(productDataListSvcRes.get("productAssocDataList"), Map.class);
                        			productFeatureSwatchDataList = UtilGenerics.checkList(productDataListSvcRes.get("productFeatureSwatchDataList"), Map.class);
                        			manufacturerDataList = UtilGenerics.checkList(productDataListSvcRes.get("manufacturerDataList"), Map.class);
                        			
                        			errorMessageList = UtilGenerics.checkList(productDataListSvcRes.get("errorMessageList"), String.class);
                        		}
                        		
	                              Map<String, Object> svcCtx = FastMap.newInstance();
		                          svcCtx.put("productCatDataList", productCatDataList);
		                          svcCtx.put("productDataList", productDataList);
		                          svcCtx.put("productAssocDataList", productAssocDataList);
		                          svcCtx.put("productFeatureSwatchDataList", productFeatureSwatchDataList);
		                          svcCtx.put("manufacturerDataList", manufacturerDataList);

		                          Map svcRes = dispatcher.runSync("validateProductData", svcCtx);

		                          prodCatErrorList = UtilGenerics.checkList(svcRes.get("prodCatErrorList"), String.class);
		                          prodCatWarningList = UtilGenerics.checkList(svcRes.get("prodCatWarningList"), String.class);
		                          productErrorList = UtilGenerics.checkList(svcRes.get("productErrorList"), String.class);
		                          productWarningList = UtilGenerics.checkList(svcRes.get("productWarningList"), String.class);
		                          productAssocErrorList = UtilGenerics.checkList(svcRes.get("productAssocErrorList"), String.class);
		                          productAssocWarningList = UtilGenerics.checkList(svcRes.get("productAssocWarningList"), String.class);
		                          productFeatureSwatchErrorList = UtilGenerics.checkList(svcRes.get("productFeatureSwatchErrorList"), String.class);
		                          productFeatureSwatchWarningList = UtilGenerics.checkList(svcRes.get("productFeatureSwatchWarningList"), String.class);
		                          productManufacturerErrorList = UtilGenerics.checkList(svcRes.get("productManufacturerErrorList"), String.class);
		                          productManufacturerWarningList = UtilGenerics.checkList(svcRes.get("productManufacturerWarningList"), String.class);
	                          } 
	                          catch (Exception e)
	                          {
	                        	  e.printStackTrace();
	                          }
                        	
                        	if(prodCatErrorList.size() > 0 || productErrorList.size()>0 || productAssocErrorList.size()>0 || productFeatureSwatchErrorList.size()>0 || productManufacturerErrorList.size()>0)
                        	{
                        		// Log the Errors and Warnings into Log File
                        	}
                        	else
                        	{
                        		Map result  = dispatcher.runSync("importClientProductXMLTemplate", importClientProductTemplateCtx);
                                List<String> serviceMsg = (List)result.get("messages");
                                if(serviceMsg.size() > 0 && serviceMsg.contains("SUCCESS")) 
                                {
                                    try 
                                    {
                            	        FileUtils.copyFileToDirectory(f, new File(feedsInProductDir , processedDir));
                            	        f.delete();
                            	    } 
                                    catch (IOException e) 
                                    {
                            		    Debug.log("Can not copy file " + f.getName() + " to Directory " +processedDir);
        						    }
                                } 
                                else 
                                {
                                	try 
                                	{
                            	        FileUtils.copyFileToDirectory(f, new File(feedsInProductDir , errorDir));
                            	        f.delete();
                            	    }
                                	catch (IOException e) 
                                	{
                            		    Debug.log("Can not copy file " + f.getName() + " to Directory " +errorDir);
        						    }
                                }
                        	}
                        } catch (Exception e) 
                        {
                            unprocessedFiles.add(f);
                            Debug.log("Failed " + f + " adding to retry list for next pass");
                        }
                        // pause in between files
                        if (pauseLong > 0) 
                        {
                            Debug.log("Pausing for [" + pauseLong + "] seconds - " + UtilDateTime.nowTimestamp());
                            try 
                            {
                                Thread.sleep((pauseLong * 1000));
                            } 
                            catch (InterruptedException ie) 
                            {
                                Debug.log("Pause finished - " + UtilDateTime.nowTimestamp());
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                }
                lastUnprocessedFilesCount=unprocessedFiles.size();
                
            } else 
            {
            	Debug.log("path not found or can't be read");
            }
        } 
        else 
        {
        	Debug.log("No path specified, doing nothing.");
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> clientStoreUpdate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsInStoreDir = (String)context.get("feedsInStoreDir");
        String feedsInSuccessSubDir = (String)context.get("feedsInSuccessSubDir");
        String feedsInErrorSubDir = (String)context.get("feedsInErrorSubDir");

        // Check passed params
        if (UtilValidate.isEmpty(feedsInStoreDir)) 
        {
        	feedsInStoreDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_STORE_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsInSuccessSubDir)) 
        {
        	feedsInSuccessSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_SUCCESS_SUB_DIR");
        }
        if (UtilValidate.isEmpty(feedsInErrorSubDir)) 
        {
        	feedsInErrorSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_ERROR_SUB_DIR");
        }

        String processedDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInSuccessSubDir)) {
        	processedDir = feedsInSuccessSubDir + processedDir; 
        }
        String errorDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInErrorSubDir)) {
        	errorDir = feedsInErrorSubDir + errorDir; 
        }
        
        if (UtilValidate.isNotEmpty(feedsInStoreDir)) {
            long pauseLong = 0;
            File baseDir = new File(feedsInStoreDir);

            if (baseDir.isDirectory() && baseDir.canRead()) {
                File[] fileArray = baseDir.listFiles();
                FastList<File> files = FastList.newInstance();
                
                for (File file: fileArray) {
                    if (file.getName().toUpperCase().endsWith("XML")) {
                        files.add(file);
                    }
                }
                int passes=0;
                int lastUnprocessedFilesCount = 0;
                FastList<File> unprocessedFiles = FastList.newInstance();
                while (files.size()>0 &&
                        files.size() != lastUnprocessedFilesCount) {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = FastList.newInstance();
                    for (File f: files) {
                    	String uploadTempDir = System.getProperty("ofbiz.home") + "/runtime/tmp/upload/";
                    	try {
                    	    FileUtils.copyFileToDirectory(f, new File(uploadTempDir));
                    	} catch (IOException e) {
                    		Debug.log("Can not copy file " + f.getName() + " to Directory " +uploadTempDir);
						}
                    	
                        Map<String, Object> importClientStoreXMLTemplateCtx = UtilMisc.toMap("xmlDataDir", uploadTempDir,"autoLoad",Boolean.TRUE,"userLogin",userLogin);
                        try {
                        	String xmlDataFile = uploadTempDir + f.getName();
                        	importClientStoreXMLTemplateCtx.put("xmlDataFile", xmlDataFile);
                            Map result  = dispatcher.runSync("importClientStoreXMLTemplate", importClientStoreXMLTemplateCtx);
                            List<String> serviceMsg = (List)result.get("messages");
                            if(serviceMsg.size() > 0 && serviceMsg.contains("SUCCESS")) {
                                try {
                        	        FileUtils.copyFileToDirectory(f, new File(feedsInStoreDir , processedDir));
                        	        f.delete();
                        	    } catch (IOException e) {
                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +processedDir);
    						    }
                            } else {
                            	try {
                        	        FileUtils.copyFileToDirectory(f, new File(feedsInStoreDir , errorDir));
                        	        f.delete();
                        	    } catch (IOException e) {
                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +errorDir);
    						    }
                            }
                            
                        } catch (Exception e) {
                            unprocessedFiles.add(f);
                            Debug.log("Failed " + f + " adding to retry list for next pass");
                        }
                        // pause in between files
                        if (pauseLong > 0) {
                            Debug.log("Pausing for [" + pauseLong + "] seconds - " + UtilDateTime.nowTimestamp());
                            try {
                                Thread.sleep((pauseLong * 1000));
                            } catch (InterruptedException ie) {
                                Debug.log("Pause finished - " + UtilDateTime.nowTimestamp());
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                }
                lastUnprocessedFilesCount=unprocessedFiles.size();
                
            } else {
            	Debug.log("path not found or can't be read");
            }
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> clientOrderStatusUpdate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsInOrderStatusDir = (String)context.get("feedsInOrderStatusDir");
        String feedsInSuccessSubDir = (String)context.get("feedsInSuccessSubDir");
        String feedsInErrorSubDir = (String)context.get("feedsInErrorSubDir");

        // Check passed params
        if (UtilValidate.isEmpty(feedsInOrderStatusDir)) 
        {
        	feedsInOrderStatusDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_ORDER_STATUS_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsInSuccessSubDir)) 
        {
        	feedsInSuccessSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_SUCCESS_SUB_DIR");
        }
        if (UtilValidate.isEmpty(feedsInErrorSubDir)) 
        {
        	feedsInErrorSubDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_IN_ERROR_SUB_DIR");
        }

        String processedDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInSuccessSubDir)) {
        	processedDir = feedsInSuccessSubDir + processedDir; 
        }
        String errorDir = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss");
        if(UtilValidate.isNotEmpty(feedsInErrorSubDir)) {
        	errorDir = feedsInErrorSubDir + errorDir; 
        }
        
        if (UtilValidate.isNotEmpty(feedsInOrderStatusDir)) {
            long pauseLong = 0;
            File baseDir = new File(feedsInOrderStatusDir);

            if (baseDir.isDirectory() && baseDir.canRead()) {
                File[] fileArray = baseDir.listFiles();
                FastList<File> files = FastList.newInstance();
                
                for (File file: fileArray) {
                    if (file.getName().toUpperCase().endsWith("XML")) {
                        files.add(file);
                    }
                }
                int passes=0;
                int lastUnprocessedFilesCount = 0;
                FastList<File> unprocessedFiles = FastList.newInstance();
                while (files.size()>0 &&
                        files.size() != lastUnprocessedFilesCount) {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = FastList.newInstance();
                    for (File f: files) {
                    	String uploadTempDir = System.getProperty("ofbiz.home") + "/runtime/tmp/upload/";
                    	try {
                    	    FileUtils.copyFileToDirectory(f, new File(uploadTempDir));
                    	} catch (IOException e) {
                    		Debug.log("Can not copy file " + f.getName() + " to Directory " +uploadTempDir);
						}
                    	
                        Map<String, Object> importClientOrderStatusXMLTemplateCtx = UtilMisc.toMap("xmlDataDir", uploadTempDir,"autoLoad",Boolean.TRUE,"userLogin",userLogin);
                        try {
                        	String xmlDataFile = uploadTempDir + f.getName();
                        	importClientOrderStatusXMLTemplateCtx.put("xmlDataFile", xmlDataFile);
                            Map result  = dispatcher.runSync("importClientOrderStatusXMLTemplate", importClientOrderStatusXMLTemplateCtx);
                            List<String> serviceMsg = (List)result.get("messages");
                            if(serviceMsg.size() > 0 && serviceMsg.contains("SUCCESS")) {
                                try {
                        	        FileUtils.copyFileToDirectory(f, new File(feedsInOrderStatusDir , processedDir));
                        	        f.delete();
                        	    } catch (IOException e) {
                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +processedDir);
    						    }
                            } else {
                            	try {
                        	        FileUtils.copyFileToDirectory(f, new File(feedsInOrderStatusDir , errorDir));
                        	        f.delete();
                        	    } catch (IOException e) {
                        		    Debug.log("Can not copy file " + f.getName() + " to Directory " +errorDir);
    						    }
                            }
                            
                        } catch (Exception e) {
                            unprocessedFiles.add(f);
                            Debug.log("Failed " + f + " adding to retry list for next pass");
                        }
                        // pause in between files
                        if (pauseLong > 0) {
                            Debug.log("Pausing for [" + pauseLong + "] seconds - " + UtilDateTime.nowTimestamp());
                            try {
                                Thread.sleep((pauseLong * 1000));
                            } catch (InterruptedException ie) {
                                Debug.log("Pause finished - " + UtilDateTime.nowTimestamp());
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                }
                lastUnprocessedFilesCount=unprocessedFiles.size();
                
            } else {
            	Debug.log("path not found or can't be read");
            }
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> bigFishCustomerFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsOutCustomerDir = (String)context.get("feedsOutCustomerDir");
        String feedsOutCustomerPrefix = (String)context.get("feedsOutCustomerPrefix");

        // Check passed params
        if (UtilValidate.isEmpty(feedsOutCustomerDir)) 
        {
        	feedsOutCustomerDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_CUSTOMER_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsOutCustomerPrefix)) 
        {
        	feedsOutCustomerPrefix = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_CUSTOMER_PREFIX");
        }
        if (UtilValidate.isNotEmpty(feedsOutCustomerDir)) {
        	Map<String, Object> findPartyCtx = UtilMisc.toMap("lookupFlag", "Y",
                    "showAll", "N","extInfo", "N", "statusId", "ANY",
                    "userLogin", userLogin);
        	findPartyCtx.put("roleTypeId", "CUSTOMER");
        	findPartyCtx.put("partyTypeId", "PERSON");
        	findPartyCtx.put("isDownloaded", "N");
        	
        	Map results;
        	List<GenericValue> completePartyList = FastList.newInstance();
			try {
				results = dispatcher.runSync("findParty", findPartyCtx);
				completePartyList = (List<GenericValue>) results.get("completePartyList");
			} catch (GenericServiceException e1) {
				e1.printStackTrace();
			}
        	
        	List<String> partyList = FastList.newInstance();
        	if(UtilValidate.isNotEmpty(completePartyList)) {
        		for(GenericValue party : completePartyList) {
        			partyList.add(party.getString("partyId"));
        		}
        	}
        	if(UtilValidate.isNotEmpty(partyList)) {
        		Map<String, Object> exportCustomerXMLCtx = UtilMisc.toMap("customerList", partyList,
                        "productStoreId", productStoreId,
                        "userLogin", userLogin);
        		Map exportResults;
				try {
					exportResults = dispatcher.runSync("exportCustomerXML", exportCustomerXMLCtx);
					String feedsDirectoryPath = (String)exportResults.get("feedsDirectoryPath");
	        		String feedsFileName = (String)exportResults.get("feedsFileName");
	        		List<String> feedsExportedIdList = (List)exportResults.get("feedsExportedIdList");
	        		File exportedFileSrc = new File(feedsDirectoryPath, feedsFileName);
	        		String exportedFileName = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss")+".xml";
	                if(UtilValidate.isNotEmpty(feedsOutCustomerPrefix)) {
	                	exportedFileName = feedsOutCustomerPrefix + exportedFileName; 
	                }
	        		try {
	        	        FileUtils.copyFile(exportedFileSrc, new File(feedsOutCustomerDir, exportedFileName));
	        	        exportedFileSrc.delete();
	        	        //Set the IS_DOWNLOADED Attribute to 'Y'
	        	        if(UtilValidate.isNotEmpty(feedsExportedIdList)) {
	        	        	Map<String, Object> createUpdateDownloadedArrtibuteCtx = UtilMisc.toMap("feedsExportedIdList", feedsExportedIdList,
		                            "entityName", "PartyAttribute", "entityPrimaryColumnName", "partyId",
		                            "userLogin", userLogin);
	        	        	dispatcher.runSync("createUpdateDownloadedArrtibute", createUpdateDownloadedArrtibuteCtx);
	        	        }
	        	        
	        	    } catch (IOException e) {
	        		    Debug.log("Can not copy file " + exportedFileSrc.getName() + " to Directory " +feedsOutCustomerDir);
				    }
				} catch (Exception e1) {
					e1.printStackTrace();
				}
        		
        	}
            
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> bigFishOrderFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        List lProductStoreId = FastList.newInstance();
        if(UtilValidate.isNotEmpty(productStoreId)) {
        	lProductStoreId.add(productStoreId);
        }
        String feedsOutOrderDir = (String)context.get("feedsOutOrderDir");
        String feedsOutOrderPrefix = (String)context.get("feedsOutOrderPrefix");
        // Check passed params
        if (UtilValidate.isEmpty(feedsOutOrderDir)) 
        {
        	feedsOutOrderDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_ORDER_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsOutOrderPrefix)) 
        {
        	feedsOutOrderPrefix = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_ORDER_PREFIX");
        }

        Integer viewIndex = 1;
        Integer viewSize = 10000;
        if (UtilValidate.isNotEmpty(feedsOutOrderDir)) {
        	Map<String, Object> searchOrdersCtx = UtilMisc.toMap("showAll", "N", "userLogin", userLogin);
        	searchOrdersCtx.put("viewIndex", viewIndex);
        	searchOrdersCtx.put("viewSize", viewSize);
        	searchOrdersCtx.put("productStoreId", lProductStoreId);
        	searchOrdersCtx.put("isDownloaded", "N");
        	Map results;
        	List<GenericValue> completeOrderList = FastList.newInstance();
			try {
				results = dispatcher.runSync("searchOrders", searchOrdersCtx);
				completeOrderList = (List<GenericValue>) results.get("completeOrderList");
			} catch (GenericServiceException e1) {
				e1.printStackTrace();
			}
        	
        	List<String> orderList = FastList.newInstance();
        	if(UtilValidate.isNotEmpty(completeOrderList)) {
        		for(GenericValue order : completeOrderList) {
        			orderList.add(order.getString("orderId"));
        		}
        	}
        	if(UtilValidate.isNotEmpty(orderList)) {
        		Map<String, Object> exportOrderXMLCtx = UtilMisc.toMap("orderList", orderList,
                        "productStoreId", productStoreId,
                        "userLogin", userLogin);
        		Map exportResults;
				try {
					exportResults = dispatcher.runSync("exportOrderXML", exportOrderXMLCtx);
					String feedsDirectoryPath = (String)exportResults.get("feedsDirectoryPath");
	        		String feedsFileName = (String)exportResults.get("feedsFileName");
	        		List<String> feedsExportedIdList =  (List) exportResults.get("feedsExportedIdList");
	        		
	        		File exportedFileSrc = new File(feedsDirectoryPath, feedsFileName);
	        		String exportedFileName = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss")+".xml";
	                if(UtilValidate.isNotEmpty(feedsOutOrderPrefix)) {
	                	exportedFileName = feedsOutOrderPrefix + exportedFileName; 
	                }
	        		try {
	        	        FileUtils.copyFile(exportedFileSrc, new File(feedsOutOrderDir, exportedFileName));
	        	        exportedFileSrc.delete();
	        	        
	        	        //Set the IS_DOWNLOADED Attribute to 'Y'
	        	        if(UtilValidate.isNotEmpty(feedsExportedIdList)) {
	        	        	Map<String, Object> createUpdateDownloadedArrtibuteCtx = UtilMisc.toMap("feedsExportedIdList", feedsExportedIdList,
		                            "entityName", "OrderAttribute", "entityPrimaryColumnName", "orderId",
		                            "userLogin", userLogin);
	        	        	dispatcher.runSync("createUpdateDownloadedArrtibute", createUpdateDownloadedArrtibuteCtx);
	        	        }
	        	    } catch (IOException e) {
	        		    Debug.log("Can not copy file " + exportedFileSrc.getName() + " to Directory " +feedsOutOrderDir);
				    }
				} catch (Exception e1) {
					e1.printStackTrace();
				}
        		
        	}
            
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
	public static Map<String, Object> bigFishContactUsFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsOutContactUsDir = (String)context.get("feedsOutContactUsDir");
        String feedsOutContactUsPrefix = (String)context.get("feedsOutContactUsPrefix");

        // Check passed params
        if (UtilValidate.isEmpty(feedsOutContactUsDir)) 
        {
        	feedsOutContactUsDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_CONTACT_US_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsOutContactUsPrefix)) 
        {
        	feedsOutContactUsPrefix = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_CONTACT_US_PREFIX");
        }

        if (UtilValidate.isNotEmpty(feedsOutContactUsDir)) {
        	List<String> custRequestIdList = FastList.newInstance();
			try {
				List custRequestAttrIdList = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("CustRequestAttribute", UtilMisc.toMap("attrName","IS_DOWNLOADED", "attrValue","N")), "custRequestId", true);
				if(UtilValidate.isNotEmpty(custRequestAttrIdList)){
					List<EntityExpr> custRequestExpr = FastList.newInstance();
					custRequestExpr.add(EntityCondition.makeCondition("custRequestId", EntityOperator.IN, custRequestAttrIdList));
					custRequestExpr.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_CONTACT_US"));
					custRequestExpr.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
					custRequestIdList = EntityUtil.getFieldListFromEntityList(delegator.findList("CustRequest", EntityCondition.makeCondition(custRequestExpr, EntityOperator.AND), null, null, null, false),"custRequestId",true);
				}
			} catch (GenericEntityException e2) {
				e2.printStackTrace();
			}
        	
        	if(UtilValidate.isNotEmpty(custRequestIdList)) {
        		Map<String, Object> exportCustRequestContactUsXMLCtx = UtilMisc.toMap("custRequestIdList", custRequestIdList,
                        "productStoreId", productStoreId,
                        "userLogin", userLogin);
        		Map exportResults;
				try {
					exportResults = dispatcher.runSync("exportCustRequestContactUsXML", exportCustRequestContactUsXMLCtx);
					String feedsDirectoryPath = (String)exportResults.get("feedsDirectoryPath");
	        		String feedsFileName = (String)exportResults.get("feedsFileName");
	        		List<String> feedsExportedIdList =  (List) exportResults.get("feedsExportedIdList");
	        		File exportedFileSrc = new File(feedsDirectoryPath, feedsFileName);
	        		String exportedFileName = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss")+".xml";
	                if(UtilValidate.isNotEmpty(feedsOutContactUsPrefix)) {
	                	exportedFileName = feedsOutContactUsPrefix + exportedFileName; 
	                }
	        		try {
	        	        FileUtils.copyFile(exportedFileSrc, new File(feedsOutContactUsDir, exportedFileName));
	        	        exportedFileSrc.delete();
	        	        
	        	        //Set the IS_DOWNLOADED Attribute to 'Y'
	        	        if(UtilValidate.isNotEmpty(feedsExportedIdList)) {
	        	        	Map<String, Object> createUpdateDownloadedArrtibuteCtx = UtilMisc.toMap("feedsExportedIdList", feedsExportedIdList,
		                            "entityName", "CustRequestAttribute", "entityPrimaryColumnName", "custRequestId",
		                            "userLogin", userLogin);
	        	        	dispatcher.runSync("createUpdateDownloadedArrtibute", createUpdateDownloadedArrtibuteCtx);
	        	        }
	        	    } catch (IOException e) {
	        		    Debug.log("Can not copy file " + exportedFileSrc.getName() + " to Directory " +feedsOutContactUsDir);
				    }
				} catch (Exception e1) {
					e1.printStackTrace();
				}
        		
        	}
            
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> bigFishRequestCatalogFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        
        String feedsOutRequestCatalogDir = (String)context.get("feedsOutRequestCatalogDir");
        String feedsOutRequestCatalogPrefix = (String)context.get("feedsOutRequestCatalogPrefix");

        // Check passed params
        if (UtilValidate.isEmpty(feedsOutRequestCatalogDir)) 
        {
        	feedsOutRequestCatalogDir = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_REQUEST_CATALOG_URL_DIR");
        }
        if (UtilValidate.isEmpty(feedsOutRequestCatalogPrefix)) 
        {
        	feedsOutRequestCatalogPrefix = OsafeAdminUtil.getProductStoreParm(delegator, productStoreId, "FEEDS_OUT_REQUEST_CATALOG_PREFIX");
        }

        if (UtilValidate.isNotEmpty(feedsOutRequestCatalogDir)) {
        	List<String> custRequestIdList = FastList.newInstance();
			try {
				List custRequestAttrIdList = EntityUtil.getFieldListFromEntityList(delegator.findByAnd("CustRequestAttribute", UtilMisc.toMap("attrName","IS_DOWNLOADED", "attrValue","N")), "custRequestId", true);
				if(UtilValidate.isNotEmpty(custRequestAttrIdList)){
					List<EntityExpr> custRequestExpr = FastList.newInstance();
					custRequestExpr.add(EntityCondition.makeCondition("custRequestId", EntityOperator.IN, custRequestAttrIdList));
					custRequestExpr.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_CATALOG"));
					custRequestExpr.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
					custRequestIdList = EntityUtil.getFieldListFromEntityList(delegator.findList("CustRequest", EntityCondition.makeCondition(custRequestExpr, EntityOperator.AND), null, null, null, false),"custRequestId",true);
				}
			} catch (GenericEntityException e2) {
				e2.printStackTrace();
			}
        	
        	if(UtilValidate.isNotEmpty(custRequestIdList)) {
        		Map<String, Object> exportCustRequestCatalogXMLCtx = UtilMisc.toMap("custRequestIdList", custRequestIdList,
                        "productStoreId", productStoreId,
                        "userLogin", userLogin);
        		Map exportResults;
				try {
					exportResults = dispatcher.runSync("exportCustRequestCatalogXML", exportCustRequestCatalogXMLCtx);
					String feedsDirectoryPath = (String)exportResults.get("feedsDirectoryPath");
	        		String feedsFileName = (String)exportResults.get("feedsFileName");
	        		List<String> feedsExportedIdList =  (List) exportResults.get("feedsExportedIdList");
	        		File exportedFileSrc = new File(feedsDirectoryPath, feedsFileName);
	        		String exportedFileName = "_"+UtilDateTime.nowDateString("yyyyMMdd")+"_"+UtilDateTime.nowDateString("HHmmss")+".xml";
	                if(UtilValidate.isNotEmpty(feedsOutRequestCatalogPrefix)) {
	                	exportedFileName = feedsOutRequestCatalogPrefix + exportedFileName; 
	                }
	        		try {
	        	        FileUtils.copyFile(exportedFileSrc, new File(feedsOutRequestCatalogDir, exportedFileName));
	        	        exportedFileSrc.delete();
	        	      //Set the IS_DOWNLOADED Attribute to 'Y'
	        	        if(UtilValidate.isNotEmpty(feedsExportedIdList)) {
	        	        	Map<String, Object> createUpdateDownloadedArrtibuteCtx = UtilMisc.toMap("feedsExportedIdList", feedsExportedIdList,
		                            "entityName", "CustRequestAttribute", "entityPrimaryColumnName", "custRequestId",
		                            "userLogin", userLogin);
	        	        	dispatcher.runSync("createUpdateDownloadedArrtibute", createUpdateDownloadedArrtibuteCtx);
	        	        }
	        	    } catch (IOException e) {
	        		    Debug.log("Can not copy file " + exportedFileSrc.getName() + " to Directory " +feedsOutRequestCatalogDir);
				    }
				} catch (Exception e1) {
					e1.printStackTrace();
				}
        		
        	}
            
        } else {
        	Debug.log("No path specified, doing nothing.");
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    
    public static Map<String, Object> importOrderStatusChangeXml(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        List<MessageString> error_list = new ArrayList<MessageString>();
        MessageString tmp = null;
        if(UtilValidate.isEmpty(uploadedFileName)) {
        	//error_list.add(UtilProperties.getMessage(resource, "BlankFeedFileError", locale));
        	tmp = new MessageString(UtilProperties.getMessage(resource, "BlankFeedFileError", locale),"uploadedFile",true);
        	error_list.add(tmp);
        }
        Map result = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) {
        	if(!(uploadedFileName.toUpperCase()).endsWith("XML")) {
        		//error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXmlError", locale));
            	tmp = new MessageString(UtilProperties.getMessage(resource, "FeedFileNotXmlError", locale),"uploadedFile",true);
            	error_list.add(tmp);
        	} else {
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try {
        			result = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} catch (GenericServiceException e) {
        			e.printStackTrace();
        		}
        	}
        }
        if(UtilValidate.isNotEmpty(result.get("uploadFilePath")) && UtilValidate.isNotEmpty(result.get("uploadFileName"))) {
        	try {
        	    JAXBContext jaxbContext = JAXBContext.newInstance("com.osafe.feeds.osafefeeds");
        	    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        	    JAXBElement<BigFishOrderStatusUpdateFeedType> bfOrderStatusUpdateFeedType;
				bfOrderStatusUpdateFeedType = (JAXBElement<BigFishOrderStatusUpdateFeedType>)unmarshaller.unmarshal(new File((String)result.get("uploadFilePath") + (String)result.get("uploadFileName")));
				List<OrderStatusType> orderList = bfOrderStatusUpdateFeedType.getValue().getOrder();
				List<String> carrierIdList = FastList.newInstance();
				List<String> shippingMethodIdList = FastList.newInstance();
				try {
					
					List<GenericValue> carrierShipmentMethodList = delegator.findByAnd("ProductStoreShipmentMethView", UtilMisc.toMap("productStoreId", productStoreId));
					if(UtilValidate.isNotEmpty(carrierShipmentMethodList)) {
						for(GenericValue carrierMethod : carrierShipmentMethodList) {
							carrierIdList.add(carrierMethod.getString("partyId"));
							shippingMethodIdList.add(carrierMethod.getString("shipmentMethodTypeId"));
						}
					}
					
				} catch (GenericEntityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if(orderList.size() > 0) {
					List dataRows = ImportServices.buildOrderStatusXMLDataRows(orderList);
					for (int i=0 ; i < dataRows.size() ; i++) 
	                {
		            	 Map mRow = (Map)dataRows.get(i);
		            	 
			             if(Integer.parseInt((String)mRow.get("totalOrderItems")) > 0) {
			            	 for(int orderItemNo = 0; orderItemNo < Integer.parseInt((String)mRow.get("totalOrderItems")); orderItemNo++) {
			            		boolean carrierIdMatch = true;
			            		boolean shippingMethodIdMatch = true;
			            		List<GenericValue> orderItems = FastList.newInstance();
		             		    try {
		             		    	if(UtilValidate.isNotEmpty(mRow.get("productId_" + (orderItemNo + 1))) && UtilValidate.isEmpty(mRow.get("orderItemSequenceId_" + (orderItemNo + 1)))) {
		         					    orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", mRow.get("orderId"), "productId", mRow.get("productId_" + (orderItemNo + 1))));
		         					    if(UtilValidate.isEmpty(orderItems)) {
		         					    	//error_list.add(UtilProperties.getMessage(resource, "OrderItemProductIdMatchingError", UtilMisc.toMap("productId", mRow.get("productId_" + (orderItemNo + 1))), locale));
		         					    	tmp = new MessageString(UtilProperties.getMessage(resource, "OrderItemProductIdMatchingError", UtilMisc.toMap("productId", mRow.get("productId_" + (orderItemNo + 1))) ,locale),"productId_" + (orderItemNo + 1),true);
		         			            	error_list.add(tmp);
		         					    }
		             		    	}
		             		    	if(UtilValidate.isNotEmpty(mRow.get("productId_" + (orderItemNo + 1))) && UtilValidate.isNotEmpty(mRow.get("orderItemSequenceId_" + (orderItemNo + 1)))) {
		         					    orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", mRow.get("orderId"), "productId", mRow.get("productId_" + (orderItemNo + 1)), "orderItemSeqId", mRow.get("orderItemSequenceId_" + (orderItemNo + 1))));
		         					    if(UtilValidate.isEmpty(orderItems)) {
		         						    //error_list.add(UtilProperties.getMessage(resource, "OrderItemProductIdSeqIdMatchingError", UtilMisc.toMap("productId", mRow.get("productId_" + (orderItemNo + 1)), "sequenceId", mRow.get("orderItemSequenceId_" + (orderItemNo + 1))), locale));
		         					    	tmp = new MessageString(UtilProperties.getMessage(resource, "OrderItemProductIdSeqIdMatchingError", UtilMisc.toMap("productId", mRow.get("productId_" + (orderItemNo + 1)), "sequenceId", mRow.get("orderItemSequenceId_" + (orderItemNo + 1))),locale),"productId_" + (orderItemNo + 1),true);
		         			            	error_list.add(tmp);
		         					    }
		             		    	}
		             		    	if(UtilValidate.isNotEmpty(mRow.get("orderItemCarrier_" + (orderItemNo + 1)))) {
		             		    		if(!carrierIdList.contains(mRow.get("orderItemCarrier_" + (orderItemNo + 1)))) {
		             		    			carrierIdMatch = false;
		             		    		}
		             		    	}
		             		    	if(UtilValidate.isNotEmpty(mRow.get("orderItemShipMethod_" + (orderItemNo + 1)))) {
                                        if(!shippingMethodIdList.contains(mRow.get("orderItemShipMethod_" + (orderItemNo + 1)))) {
                                        	shippingMethodIdMatch = false;
		             		    		}
		             		    	}
		             		    	if(!carrierIdMatch || !shippingMethodIdMatch) {
		             		    		//error_list.add(UtilProperties.getMessage(resource, "OrderItemShippingMethodMatchingError", UtilMisc.toMap("carrierId", mRow.get("orderItemCarrier_" + (orderItemNo + 1)), "shippingMethodId", mRow.get("orderItemShipMethod_" + (orderItemNo + 1))), locale));
		             		    		tmp = new MessageString(UtilProperties.getMessage(resource, "OrderItemShippingMethodMatchingError", UtilMisc.toMap("carrierId", mRow.get("orderItemCarrier_" + (orderItemNo + 1)), "shippingMethodId", mRow.get("orderItemShipMethod_" + (orderItemNo + 1))),locale),"orderItemCarrier_" + (orderItemNo + 1),true);
	         			            	error_list.add(tmp);
		             		    	}
		             		    } catch (GenericEntityException e) {
		         					e.printStackTrace();
		         				}
			            	}
			             } else {
			            	boolean carrierIdMatch = true;
			            	boolean shippingMethodIdMatch = true;
			            	if(UtilValidate.isNotEmpty(mRow.get("orderShipCarrier"))) {
                                if(!carrierIdList.contains(mRow.get("orderShipCarrier"))) {
                                	 carrierIdMatch = false;
	             		     }
	             		    }
			            	if(UtilValidate.isNotEmpty(mRow.get("orderShipMethod"))) {
                                if(!shippingMethodIdList.contains(mRow.get("orderShipMethod"))) {
                                 	shippingMethodIdMatch = false;
	             		    	}
	             		    }
	             		    if(!carrierIdMatch || !shippingMethodIdMatch) {
	             		    	tmp = new MessageString(UtilProperties.getMessage(resource, "OrderItemShippingMethodMatchingError", UtilMisc.toMap("carrierId", mRow.get("orderShipCarrier"), "shippingMethodId", mRow.get("orderShipMethod")),locale),"orderItemCarrier",true);
     			            	error_list.add(tmp);
	             		    }
			             }
		                 
		             }
				}
			} catch (JAXBException e) {
				e.printStackTrace();
			}
        	
        	
        }
        
        if(error_list.size() > 0) {
            return ServiceUtil.returnError(error_list);
        }
        
		
		Map<String, Object> importOrderStatusChangeXMLTemplateCtx = FastMap.newInstance();
		importOrderStatusChangeXMLTemplateCtx.put("userLogin",userLogin);
		importOrderStatusChangeXMLTemplateCtx.put("xmlDataFile",(String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
		importOrderStatusChangeXMLTemplateCtx.put("xmlDataDir",(String)result.get("uploadFilePath"));
		importOrderStatusChangeXMLTemplateCtx.put("autoLoad",Boolean.TRUE);
		try {
			dispatcher.runSync("importClientOrderStatusXMLTemplate", importOrderStatusChangeXMLTemplateCtx);
		} catch (GenericServiceException e) {
			e.printStackTrace();
		}
        return ServiceUtil.returnSuccess();
    }
    
    
    public static Map<String, Object> importCustomerXml(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String)context.get("productStoreId");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        List<String> error_list = new ArrayList<String>();
        if(UtilValidate.isEmpty(uploadedFileName)) {
        	error_list.add(UtilProperties.getMessage(resource, "BlankFeedFileError", locale));
        }
        Map result = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) {
        	if(!(uploadedFileName.toUpperCase()).endsWith("XML")) {
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXmlError", locale));	
        	} else {
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try {
        			result = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} catch (GenericServiceException e) {
        			e.printStackTrace();
        		}
        	}
        }
        if(UtilValidate.isNotEmpty(result.get("uploadFilePath")) && UtilValidate.isNotEmpty(result.get("uploadFileName"))) {
        	try {
        	    JAXBContext jaxbContext = JAXBContext.newInstance("com.osafe.feeds.osafefeeds");
        	    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        	    JAXBElement<BigFishCustomerFeedType> bfCustomerFeedType;
        	    bfCustomerFeedType = (JAXBElement<BigFishCustomerFeedType>)unmarshaller.unmarshal(new File((String)result.get("uploadFilePath") + (String)result.get("uploadFileName")));
				List<CustomerType> customerList = bfCustomerFeedType.getValue().getCustomer();
				
				if(customerList.size() > 0) {
					List dataRows = ImportServices.buildCustomerXMLDataRows(customerList);
					for (int i=0 ; i < dataRows.size() ; i++) 
	                {
		            	 Map mRow = (Map)dataRows.get(i);
		            	 if(UtilValidate.isNotEmpty(mRow.get("userName"))) {
		            		 if(UtilValidate.isNotEmpty(mRow.get("customerId")))
			            	 {
			            		 try {
			            		     GenericValue userLoginGv = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", mRow.get("userName")));
			            		     if(UtilValidate.isNotEmpty(userLoginGv) && !userLoginGv.getString("partyId").equals(mRow.get("customerId")))
			            		     {
			            			     error_list.add(UtilProperties.getMessage(resource, "UserNameUniqueError", UtilMisc.toMap(), locale));
			            		     }
			            		 }
			            		 catch (GenericEntityException gee) {
			            			 gee.printStackTrace();
			            		 }
			            	 } else {
			            		 try {
			            			 GenericValue userLoginGv = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", mRow.get("userName")));
				            		 if(UtilValidate.isNotEmpty(userLoginGv)) {
				            			 error_list.add(UtilProperties.getMessage(resource, "UserNameAssociateAnotherPartyIdError", UtilMisc.toMap(), locale));
				            		 }	 
			            		 } catch (GenericEntityException gee) {
			            			 gee.printStackTrace();
								}
			            	 }
		            	 }
		            	 
		             }
				}
			} catch (JAXBException e) {
				e.printStackTrace();
			}
        	
        	
        }
        
        if(error_list.size() > 0) {
            return ServiceUtil.returnError(error_list);
        }
        
		
		Map<String, Object> importCustomerXMLTemplateCtx = FastMap.newInstance();
		importCustomerXMLTemplateCtx.put("userLogin",userLogin);
		importCustomerXMLTemplateCtx.put("xmlDataFile",(String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
		importCustomerXMLTemplateCtx.put("xmlDataDir",(String)result.get("uploadFilePath"));
		importCustomerXMLTemplateCtx.put("autoLoad",Boolean.TRUE);
		try {
			dispatcher.runSync("importCustomerXMLTemplate", importCustomerXMLTemplateCtx);
		} catch (GenericServiceException e) {
			e.printStackTrace();
		}
        return ServiceUtil.returnSuccess();
    }
    
    
    
    public static Map<String, Object> importBluedartPrepaid(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        List<String> error_list = new ArrayList<String>();
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlankFeedFileError", locale));
        }
        Map result = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "BlueDartFeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
        			result = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(result.get("uploadFilePath")) && UtilValidate.isNotEmpty(result.get("uploadFileName"))) 
        {
        	try 
        	{
        		File inputWorkbook = new File((String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 0)
                            {
                            	List dataRows = BlueDartImportServices.buildDataRows(BlueDartImportServices.buildBlueDartPrepaidHeader(),s);
                            	List<String> duplicates = new ArrayList();
                                HashSet uniques = new HashSet();
                            	for (int i=0 ; i < dataRows.size() ; i++) 
                                {
                            		Map<String, String> mRow = (Map<String, String>)dataRows.get(i);
                            		String pinCode = (String)mRow.get("pincode");
                                     if (uniques.contains(pinCode))
                                     {
                                    	 duplicates.add(pinCode);
                                     }
                                     else
                                     {
                                         uniques.add(pinCode);
                                     }
                                }
                            	for(String pincode : duplicates)
                            	{
                            		error_list.add(UtilProperties.getMessage(resource, "PinCodeUniqueError",  UtilMisc.toMap("pincode", pincode), locale));
                            	}
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        
		
		Map<String, Object> importBlueDartPrepaidTemplateCtx = FastMap.newInstance();
		importBlueDartPrepaidTemplateCtx.put("userLogin",userLogin);
		importBlueDartPrepaidTemplateCtx.put("xlsDataFile",(String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
		importBlueDartPrepaidTemplateCtx.put("xmlDataDir",(String)result.get("uploadFilePath"));
		importBlueDartPrepaidTemplateCtx.put("autoLoad",Boolean.TRUE);
		try 
		{
			dispatcher.runSync("importBlueDartPrepaidTemplate", importBlueDartPrepaidTemplateCtx);
		} 
		catch (GenericServiceException e) 
		{
			e.printStackTrace();
		}
        return ServiceUtil.returnSuccess();
    }
    
    
    public static Map<String, Object> importBluedartCoD(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        List<String> error_list = new ArrayList<String>();
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlueDartFeedFileNotXlsError", locale));
        }
        Map result = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
        			result = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(result.get("uploadFilePath")) && UtilValidate.isNotEmpty(result.get("uploadFileName"))) 
        {
        	try 
        	{
        		File inputWorkbook = new File((String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 0)
                            {
                            	List dataRows = BlueDartImportServices.buildDataRows(BlueDartImportServices.buildBlueDartCoDHeader(),s);
                            	List<String> duplicates = new ArrayList();
                                HashSet uniques = new HashSet();
                            	for (int i=0 ; i < dataRows.size() ; i++) 
                                {
                            		Map<String, String> mRow = (Map<String, String>)dataRows.get(i);
                            		String pinCode = (String)mRow.get("pincode");
                                     if (uniques.contains(pinCode))
                                     {
                                    	 duplicates.add(pinCode);
                                     }
                                     else
                                     {
                                         uniques.add(pinCode);
                                     }
                                }
                            	for(String pincode : duplicates)
                            	{
                            		error_list.add(UtilProperties.getMessage(resource, "PinCodeUniqueError",  UtilMisc.toMap("pincode", pincode), locale));
                            	}
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        
		
		Map<String, Object> importBlueDartCoDTemplateCtx = FastMap.newInstance();
		importBlueDartCoDTemplateCtx.put("userLogin",userLogin);
		importBlueDartCoDTemplateCtx.put("xlsDataFile",(String)result.get("uploadFilePath") + (String)result.get("uploadFileName"));
		importBlueDartCoDTemplateCtx.put("xmlDataDir",(String)result.get("uploadFilePath"));
		importBlueDartCoDTemplateCtx.put("autoLoad",Boolean.TRUE);
		try 
		{
			dispatcher.runSync("importBlueDartCoDTemplate", importBlueDartCoDTemplateCtx);
		} 
		catch (GenericServiceException e) 
		{
			e.printStackTrace();
		}
        return ServiceUtil.returnSuccess();
    }
    
    
    public static Map<String, Object> convertProductRatings(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        
        String productStoreId = (String)context.get("productStoreId");
        
        List<String> error_list = new ArrayList<String>();
        
        String bigfishXmlFile = "";
        
        String importDataPath = getConvertedBigfishXMLFilePath();
        
        if (!new File(importDataPath).exists()) 
        {
            new File(importDataPath).mkdirs();
        }
        
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlankXLSFileError", locale));
        }
        Map result = FastMap.newInstance();
        Map resultUploadSvc = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
                	resultUploadSvc = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFilePath")) && UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFileName"))) 
        {
        	try 
        	{
        		bigfishXmlFile = UtilDateTime.nowDateString("yyyyMMddHHmmss")+".xml";
        		File inputWorkbook = new File((String)resultUploadSvc.get("uploadFilePath") + (String)resultUploadSvc.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    
                    ObjectFactory factory = new ObjectFactory();
                    
                    BigFishProductRatingFeedType bfProductRatingFeedType = factory.createBigFishProductRatingFeedType();
                    
                    File tempFile = new File(importDataPath, "temp" + bigfishXmlFile);
                    
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 0)
                            {
                            	List dataRows = ImportServices.buildDataRows(ImportServices.buildProductRatingHeader(),s);
                            	List productRatingList =  bfProductRatingFeedType.getProductRating();
                            	ImportServices.createProductRatingXmlFromXls(factory, productRatingList, dataRows, productStoreId);
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                    
                    FeedsUtil.marshalObject(new JAXBElement<BigFishProductRatingFeedType>(new QName("", "BigFishProductRatingFeed"), BigFishProductRatingFeedType.class, null, bfProductRatingFeedType), tempFile);
              	    
              	    new File(importDataPath, bigfishXmlFile).delete();
                    File renameFile =new File(importDataPath, bigfishXmlFile);
                    RandomAccessFile out = new RandomAccessFile(renameFile, "rw");
                    InputStream inputStr = new FileInputStream(tempFile);
                    byte[] bytes = new byte[102400];
                    int bytesRead;
                    while ((bytesRead = inputStr.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, bytesRead);
                    }
                    out.close();
                    inputStr.close();
                    tempFile.delete();
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        result.put("convertedFileName", bigfishXmlFile);
        return result;
    }
    
    public static Map<String, Object> convertStores(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        
        String productStoreId = (String)context.get("productStoreId");
        
        List<String> error_list = new ArrayList<String>();
        
        String bigfishXmlFile = "";
        
        String importDataPath = getConvertedBigfishXMLFilePath();
        
        if (!new File(importDataPath).exists()) 
        {
            new File(importDataPath).mkdirs();
        }
        
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlankXLSFileError", locale));
        }
        Map result = FastMap.newInstance();
        Map resultUploadSvc = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
                	resultUploadSvc = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFilePath")) && UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFileName"))) 
        {
        	try 
        	{
        		bigfishXmlFile = UtilDateTime.nowDateString("yyyyMMddHHmmss")+".xml";
        		File inputWorkbook = new File((String)resultUploadSvc.get("uploadFilePath") + (String)resultUploadSvc.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    
                    ObjectFactory factory = new ObjectFactory();
                    
                    BigFishStoreFeedType bfStoreFeedType = factory.createBigFishStoreFeedType();
                    
                    File tempFile = new File(importDataPath, "temp" + bigfishXmlFile);
                    
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 0)
                            {
                            	List dataRows = ImportServices.buildDataRows(ImportServices.buildStoreHeader(),s);
                            	List storeList =  bfStoreFeedType.getStore();
                            	ImportServices.createStoreXmlFromXls(factory, storeList, dataRows, productStoreId);
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                    
                    FeedsUtil.marshalObject(new JAXBElement<BigFishStoreFeedType>(new QName("", "BigFishStoreFeed"), BigFishStoreFeedType.class, null, bfStoreFeedType), tempFile);
              	    
              	    new File(importDataPath, bigfishXmlFile).delete();
                    File renameFile =new File(importDataPath, bigfishXmlFile);
                    RandomAccessFile out = new RandomAccessFile(renameFile, "rw");
                    InputStream inputStr = new FileInputStream(tempFile);
                    byte[] bytes = new byte[102400];
                    int bytesRead;
                    while ((bytesRead = inputStr.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, bytesRead);
                    }
                    out.close();
                    inputStr.close();
                    tempFile.delete();
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        result.put("convertedFileName", bigfishXmlFile);
        return result;
    }
    
    public static Map<String, Object> convertOrderStatus(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        
        String productStoreId = (String)context.get("productStoreId");
        
        List<String> error_list = new ArrayList<String>();
        
        String bigfishXmlFile = "";
        
        String importDataPath = getConvertedBigfishXMLFilePath();
        
        if (!new File(importDataPath).exists()) 
        {
            new File(importDataPath).mkdirs();
        }
        
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlankXLSFileError", locale));
        }
        Map result = FastMap.newInstance();
        Map resultUploadSvc = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
                	resultUploadSvc = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFilePath")) && UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFileName"))) 
        {
        	try 
        	{
        		bigfishXmlFile = UtilDateTime.nowDateString("yyyyMMddHHmmss")+".xml";
        		File inputWorkbook = new File((String)resultUploadSvc.get("uploadFilePath") + (String)resultUploadSvc.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    
                    ObjectFactory factory = new ObjectFactory();
                    
                    BigFishOrderStatusUpdateFeedType bfOrderStatusUpdateFeedType = factory.createBigFishOrderStatusUpdateFeedType();
                    
                    File tempFile = new File(importDataPath, "temp" + bigfishXmlFile);
                    
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 0)
                            {
                            	List dataRows = ImportServices.buildDataRows(ImportServices.buildOrderStatusUpdateHeader(),s);
                            	List orderStatusUpdateList =  bfOrderStatusUpdateFeedType.getOrder();
                            	ImportServices.createOrderStatusUpdateXmlFromXls(factory, orderStatusUpdateList, dataRows, productStoreId);
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                    
                    FeedsUtil.marshalObject(new JAXBElement<BigFishOrderStatusUpdateFeedType>(new QName("", "BigFishOrderStatusUpdateFeed"), BigFishOrderStatusUpdateFeedType.class, null, bfOrderStatusUpdateFeedType), tempFile);
              	    
              	    new File(importDataPath, bigfishXmlFile).delete();
                    File renameFile =new File(importDataPath, bigfishXmlFile);
                    RandomAccessFile out = new RandomAccessFile(renameFile, "rw");
                    InputStream inputStr = new FileInputStream(tempFile);
                    byte[] bytes = new byte[102400];
                    int bytesRead;
                    while ((bytesRead = inputStr.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, bytesRead);
                    }
                    out.close();
                    inputStr.close();
                    tempFile.delete();
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        result.put("convertedFileName", bigfishXmlFile);
        return result;
    }
    
    public static Map<String, Object> convertProductLoadFile(DispatchContext dctx, Map<String, ? extends Object> context) 
    {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String uploadedFileName = (String)context.get("_uploadedFile_fileName");
        ByteBuffer uploadBytes = (ByteBuffer) context.get("uploadedFile");
        
        String productStoreId = (String)context.get("productStoreId");
        
        List<String> error_list = new ArrayList<String>();
        
        String bigfishXmlFile = "";
        
        String importDataPath = getConvertedBigfishXMLFilePath();
        
        if (!new File(importDataPath).exists()) 
        {
            new File(importDataPath).mkdirs();
        }
        
        if(UtilValidate.isEmpty(uploadedFileName)) 
        {
        	error_list.add(UtilProperties.getMessage(resource, "BlankXLSFileError", locale));
        }
        Map result = FastMap.newInstance();
        Map resultUploadSvc = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(uploadedFileName)) 
        {
        	if(!((uploadedFileName.toUpperCase()).endsWith("XLS") || (uploadedFileName.toUpperCase()).endsWith("XLSX"))) 
        	{
        		error_list.add(UtilProperties.getMessage(resource, "FeedFileNotXlsError", locale));	
        	} 
        	else 
        	{
        		Map<String, Object> uploadFileCtx = FastMap.newInstance();
                uploadFileCtx.put("userLogin",userLogin);
                uploadFileCtx.put("uploadedFile",uploadBytes);
                uploadFileCtx.put("_uploadedFile_fileName",uploadedFileName);
                
                try 
                {
                	resultUploadSvc = dispatcher.runSync("uploadFile", uploadFileCtx);
        		} 
                catch (GenericServiceException e) 
                {
        			e.printStackTrace();
        		}
        	}
        }
        
        if(UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFilePath")) && UtilValidate.isNotEmpty(resultUploadSvc.get("uploadFileName"))) 
        {
        	try 
        	{
        		bigfishXmlFile = UtilDateTime.nowDateString("yyyyMMddHHmmss")+".xml";
        		File inputWorkbook = new File((String)resultUploadSvc.get("uploadFilePath") + (String)resultUploadSvc.get("uploadFileName"));
        		if (inputWorkbook != null) 
                {
        			WorkbookSettings ws = new WorkbookSettings();
                    ws.setLocale(new Locale("en", "EN"));
                    Workbook wb = Workbook.getWorkbook(inputWorkbook,ws);
                    
                    ObjectFactory factory = new ObjectFactory();
                    
                    BigFishProductFeedType bfProductFeedType = factory.createBigFishProductFeedType();
                    
                    File tempFile = new File(importDataPath, "temp" + bigfishXmlFile);
                    
                    for (int sheet = 0; sheet < wb.getNumberOfSheets(); sheet++) 
                    {
                        BufferedWriter bw = null; 
                        try 
                        {
                            Sheet s = wb.getSheet(sheet);
                            String sTabName=s.getName();
                            
                            if (sheet == 1)
                            {
                                List dataRows = ImportServices.buildDataRows(ImportServices.buildCategoryHeader(),s);
                                ProductCategoryType productCategoryType = factory.createProductCategoryType();
                    	        List productCategoryList =  productCategoryType.getCategory();
                    	        ImportServices.createProductCategoryXmlFromXls(factory, productCategoryList, dataRows);
                    	  	    bfProductFeedType.setProductCategory(productCategoryType);
                            }
                            if (sheet == 2)
                            {
                                List dataRows = ImportServices.buildDataRows(ImportServices.buildProductHeader(),s);
                                ProductsType productsType = factory.createProductsType();
                    	  	    List productList = productsType.getProduct();
                    	  	    ImportServices.createProductXmlFromXls(factory, productList, dataRows);
                    	  	    bfProductFeedType.setProducts(productsType);
                            }
                            if (sheet == 3)
                            {
                                List dataRows = ImportServices.buildDataRows(ImportServices.buildProductAssocHeader(),s);
                                ProductAssociationType productAssociationType = factory.createProductAssociationType();
                    	  	    List productAssocList = productAssociationType.getAssociation();
                    	  	    ImportServices.createProductAssocXmlFromXls(factory, productAssocList, dataRows);
                    	  	    bfProductFeedType.setProductAssociation(productAssociationType);
                            }
                            if (sheet == 4)
                            {
                                List dataRows = ImportServices.buildDataRows(ImportServices.buildProductFeatureSwatchHeader(),s);
                                ProductFeatureSwatchType productFeatureSwatchType = factory.createProductFeatureSwatchType();
                    	  	    List featureList = productFeatureSwatchType.getFeature();
                    	  	    ImportServices.createProductFeatureSwatchXmlFromXls(factory, featureList, dataRows);
                    	  	    bfProductFeedType.setProductFeatureSwatch(productFeatureSwatchType);
                            }
                            if (sheet == 5)
                            {
                                List dataRows = ImportServices.buildDataRows(ImportServices.buildManufacturerHeader(),s);
                                ProductManufacturerType productManufacturerType = factory.createProductManufacturerType();
                    	  	    List manufacturerList = productManufacturerType.getManufacturer();
                    	  	    ImportServices.createProductManufacturerXmlFromXls(factory, manufacturerList, dataRows);
                    	  	    bfProductFeedType.setProductManufacturer(productManufacturerType);
                            }
                        } 
                        catch (Exception exc) 
                        {
                            Debug.logError(exc, module);
                        } 
                    }
                    
                    FeedsUtil.marshalObject(new JAXBElement<BigFishProductFeedType>(new QName("", "BigFishProductFeed"), BigFishProductFeedType.class, null, bfProductFeedType), tempFile);
              	    
              	    new File(importDataPath, bigfishXmlFile).delete();
                    File renameFile =new File(importDataPath, bigfishXmlFile);
                    RandomAccessFile out = new RandomAccessFile(renameFile, "rw");
                    InputStream inputStr = new FileInputStream(tempFile);
                    byte[] bytes = new byte[102400];
                    int bytesRead;
                    while ((bytesRead = inputStr.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, bytesRead);
                    }
                    out.close();
                    inputStr.close();
                    tempFile.delete();
                }
        	}
        	catch(Exception e)
        	{
        		
        	}
        }
        
        if(error_list.size() > 0) 
        {
            return ServiceUtil.returnError(error_list);
        }
        result.put("convertedFileName", bigfishXmlFile);
        return result;
    }
    
    public static String getConvertedBigfishXMLFilePath()
    {
    	return System.getProperty("ofbiz.home") + "/runtime/tmp/upload/bigfishXmlFile/";
    }
}