/**
 * Created On 28-Nov-2014
 * Copyright 2010 by Primeleaf Consulting (P) Ltd.,
 * #29,784/785 Hendre Castle,
 * D.S.Babrekar Marg,
 * Gokhale Road(North),
 * Dadar,Mumbai 400 028
 * India
 * 
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Primeleaf Consulting (P) Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Primeleaf Consulting (P) Ltd.
 */

package kreidos.diamond.web.action.console;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.model.AccessControlManager;
import kreidos.diamond.model.DocumentManager;
import kreidos.diamond.model.dao.DocumentClassDAO;
import kreidos.diamond.model.dao.DocumentDAO;
import kreidos.diamond.model.dao.DocumentRevisionDAO;
import kreidos.diamond.model.vo.Document;
import kreidos.diamond.model.vo.DocumentClass;
import kreidos.diamond.model.vo.DocumentRevision;
import kreidos.diamond.model.vo.Hit;
import kreidos.diamond.model.vo.User;
import kreidos.diamond.security.ACL;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.console.DownloadDocumentView;


/**
 * Author Rahul Kubadia
 */

public class BulkDownloadAction implements Action {
	static final int BUFFER = 2048;

	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		User loggedInUser = (User)session.getAttribute(HTTPConstants.SESSION_KRYSTAL);
		try{
			DocumentClass documentClass  = null;
			AccessControlManager aclManager = new AccessControlManager();
			DocumentManager documentManager = new DocumentManager();
			Document document = null;
			DocumentRevision documentRevision;
			ACL acl = null;
			String[] documentids = request.getParameterValues("chkDocumentId");
			for(String documentID  : documentids){
				int documentId = 0;
				try{
					documentId=Integer.parseInt(documentID);
				}catch(Exception e){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid input");
					return (new DownloadDocumentView(request,response));
				}
				
				document =  DocumentDAO.getInstance().readDocumentById(documentId);
				if(document == null){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid document");
					return (new DownloadDocumentView(request,response));
				}
				
				if(Hit.STATUS_DELETED.equalsIgnoreCase(document.getStatus())){
					request.setAttribute(HTTPConstants.REQUEST_ERROR,  "Invalid document");
					return (new DownloadDocumentView(request,response));
				}
				
				if(documentClass == null){
					documentClass = DocumentClassDAO.getInstance().readDocumentClassById(document.getClassId());
				}
				
				acl = aclManager.getACL(documentClass, loggedInUser);
				
				if(! acl.canDownload()){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Access Denied");
					return (new DownloadDocumentView(request,response));
				}

			}

			byte data[] = new byte[BUFFER];
			BufferedInputStream origin = null;
			String fileName = documentClass.getClassName() + ".zip";		
			File zipFile = new File( System.getProperty("java.io.tmpdir") + File.separator + session.getLastAccessedTime() + "_" +  fileName );
			FileOutputStream dest = new FileOutputStream(zipFile);
			@SuppressWarnings("resource")
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			ArrayList<String> fileList = new ArrayList<>();
			
			for(String documentID  : documentids){
				int documentId = 0;
				try{
					documentId=Integer.parseInt(documentID);
				}catch(Exception e){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid input");
					return (new DownloadDocumentView(request,response));
				}
				document =  DocumentDAO.getInstance().readDocumentById(documentId);
				documentRevision = DocumentRevisionDAO.getInstance().readDocumentRevisionById(documentId, document.getRevisionId());
				document.setRevisionId(documentRevision.getRevisionId());
				documentRevision = documentManager.retreiveDocument(document);
				File downloadFile = new File(documentRevision.getDocumentFile().getAbsolutePath());
				FileInputStream fis = new FileInputStream(downloadFile);
				origin = new BufferedInputStream(fis, BUFFER);
				String entryName = document.getFullFilename();
				if(fileList.contains(entryName))
					entryName = new StringBuilder(entryName).insert(entryName.lastIndexOf("."), "_" + document.getDocumentId()).toString();
				fileList.add(entryName);
				ZipEntry entry = new ZipEntry(entryName);
				out.putNextEntry(entry);
				int count;
				while((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
				fis.close();
			}
			out.close();
			
			ServletContext servletContext = request.getServletContext();
			String mimeType = servletContext.getMimeType(fileName);
			FileInputStream fis = new FileInputStream(zipFile);
			response.setHeader("Content-Disposition", "attachment; filename=\""+fileName+"\"");
			response.setContentType(mimeType);
			int fileSize = (int) zipFile.length();
			response.setContentLength(fileSize);
			OutputStream os = response.getOutputStream();
			byte buf[] = new byte[fileSize];
			fis.read(buf);
			os.write(buf, 0, fileSize);
			os.flush();
			os.close();
			fis.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}

