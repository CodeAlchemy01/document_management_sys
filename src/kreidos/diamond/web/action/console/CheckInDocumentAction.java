/**
 * Created On 05-Jan-2014
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

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.model.AccessControlManager;
import kreidos.diamond.model.AuditLogManager;
import kreidos.diamond.model.IndexRecordManager;
import kreidos.diamond.model.RevisionManager;
import kreidos.diamond.model.dao.DocumentClassDAO;
import kreidos.diamond.model.dao.DocumentDAO;
import kreidos.diamond.model.vo.AuditLogRecord;
import kreidos.diamond.model.vo.CheckedOutDocument;
import kreidos.diamond.model.vo.Document;
import kreidos.diamond.model.vo.DocumentClass;
import kreidos.diamond.model.vo.Hit;
import kreidos.diamond.model.vo.IndexDefinition;
import kreidos.diamond.model.vo.User;
import kreidos.diamond.security.ACL;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.console.CheckInDocumentView;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.validator.GenericValidator;


/**
 * Author Rahul Kubadia
 */

public class CheckInDocumentAction implements Action {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		User loggedInUser = (User)session.getAttribute(HTTPConstants.SESSION_KRYSTAL);
		try{
			if("POST".equalsIgnoreCase(request.getMethod())){
				String errorMessage;
				String tempFilePath = System.getProperty("java.io.tmpdir");

				if ( !(tempFilePath.endsWith("/") || tempFilePath.endsWith("\\")) ){
					tempFilePath += System.getProperty("file.separator");
				}
				tempFilePath+=  loggedInUser.getUserName() +"_"+ session.getId();

				String revisionId = "", comments = "",  fileName = "", ext = "",  version = "";
				int documentId = 0;
				// Create a factory for disk-based file items
				FileItemFactory factory = new DiskFileItemFactory();
				// Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(factory);
				List items = upload.parseRequest((HttpServletRequest) request);
				upload.setHeaderEncoding(HTTPConstants.CHARACTER_ENCODING);
				//Create a file upload progress listener

				Iterator iter = items.iterator();
				FileItem item = null;
				File file = null;
				while (iter.hasNext()) {
					item = (FileItem) iter.next();
					if (item.isFormField()) {
						String name = item.getFieldName();
						String value = item.getString(HTTPConstants.CHARACTER_ENCODING);
						if (name.equals("documentid")) {
							try{
								documentId=Integer.parseInt(value);
							}catch(Exception ex){
								request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid input");
								return (new CheckInDocumentView(request,response));
							}
						} else if (name.equals("revisionid")) {
							revisionId = value;
						} else if (name.equals("txtNote")) {
							comments = value;
						}else if ("version".equalsIgnoreCase(name)){
							version = value;
						}
					} else {
						fileName = item.getName();
						ext = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
						file = new File(tempFilePath+"."+ext);
						item.write(file);
					}
				}
				iter = null;

				Document document =  DocumentDAO.getInstance().readDocumentById(documentId);
				if(document == null){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid document");
					return (new CheckInDocumentView(request,response));
				}
				if( document.getStatus().equalsIgnoreCase(Hit.STATUS_AVAILABLE)){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid check-in");
					return (new CheckInDocumentView(request,response));
				}
				revisionId = document.getRevisionId();
				DocumentClass documentClass = DocumentClassDAO.getInstance().readDocumentClassById(document.getClassId());
				AccessControlManager aclManager = new AccessControlManager();
				ACL acl = aclManager.getACL(documentClass, loggedInUser);
				if(! acl.canCheckin()){
					request.setAttribute(HTTPConstants.REQUEST_ERROR,"Access Denied");
					return (new CheckInDocumentView(request,response));
				}

				if (file.length() <= 0) {
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Zero length document");
					return (new CheckInDocumentView(request,response));
				}
				if (file.length() > documentClass.getMaximumFileSize() ) { //code for checking maximum size of document in a class
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Document size exceeded");
					return (new CheckInDocumentView(request,response));
				}

				String indexValue = "";
				String indexName = "";

				Hashtable indexRecord = new Hashtable();
				for (IndexDefinition indexDefinition : documentClass.getIndexDefinitions()){
					indexName = indexDefinition.getIndexColumnName();
					Iterator itemsIterator = items.iterator();
					while (itemsIterator.hasNext()) {
						FileItem fileItem = (FileItem) itemsIterator.next();
						if (fileItem.isFormField()) {
							String name = fileItem.getFieldName();
							String value = fileItem.getString(HTTPConstants.CHARACTER_ENCODING);
							if (name.equals(indexName)) {
								indexValue = value;
								if(indexValue != null){
									if(indexDefinition.isMandatory()){
										if(indexValue.trim().length() <=0){
											errorMessage =  "Invalid input for "  + indexDefinition.getIndexDisplayName();
											request.setAttribute(HTTPConstants.REQUEST_ERROR,errorMessage);
											return (new CheckInDocumentView(request,response));
										}
									}
									if(IndexDefinition.INDEXTYPE_NUMBER.equalsIgnoreCase(indexDefinition.getIndexType())){
										if(indexValue.trim().length() > 0){
											if(!GenericValidator.matchRegexp(indexValue, HTTPConstants.NUMERIC_REGEXP)){
												errorMessage = "Invalid input for "  + indexDefinition.getIndexDisplayName();
												request.setAttribute(HTTPConstants.REQUEST_ERROR,errorMessage);
												return (new CheckInDocumentView(request,response));
											}
										}
									}else if(IndexDefinition.INDEXTYPE_DATE.equalsIgnoreCase(indexDefinition.getIndexType())){
										if(indexValue.trim().length() > 0){
											if(!GenericValidator.isDate(indexValue, "yyyy-MM-dd",true)){
												errorMessage = "Invalid input for "  + indexDefinition.getIndexDisplayName();
												request.setAttribute(HTTPConstants.REQUEST_ERROR, errorMessage);
												return (new CheckInDocumentView(request,response));
											}
										}
									}

									if (indexValue.trim().length() > indexDefinition.getIndexMaxLength()){ //code for checking maximum length of index field
										errorMessage = 	"Document index length exceeded.  Index Name :" +

												indexDefinition.getIndexDisplayName() + " [ " +
												"Index Length : " + indexDefinition.getIndexMaxLength() + " , " +
												"Actual Length  : " + indexValue.length() + " ]" ;
										request.setAttribute(HTTPConstants.REQUEST_ERROR, errorMessage );
										return (new CheckInDocumentView(request,response));
									}
								}
								indexRecord.put(indexName,indexValue);
							}
						}
						fileItem = null;
					}// while iter
					itemsIterator = null;
				}// while indexDefinitionItr

				CheckedOutDocument checkedOutDocument = new CheckedOutDocument();
				checkedOutDocument.setDocumentId(documentId);
				// Added by Viral Visaria. For the Version Control minor and major.
				// In minor revision increment by 0.1. (No Changes required for the minor revision its handled in the core logic) 
				// In major revision increment by 1.0  (Below chages are incremented by 0.9 and rest 0.1 will be added in the core logic. (0.9 + 0.1 = 1.0)
				double rev = Double.parseDouble(revisionId);
				if("major".equals(version)){
					rev = Math.floor(rev);
					rev = rev + 0.9;
					revisionId = String.valueOf(rev);
				}
				checkedOutDocument.setRevisionId(revisionId);
				checkedOutDocument.setUserName(loggedInUser.getUserName());
				RevisionManager revisionManager = new RevisionManager();
				revisionManager.checkIn(checkedOutDocument,documentClass,indexRecord,file,comments,ext,loggedInUser.getUserName());

				//revision id incremented by 0.1 for making entry in audit log 
				rev += 0.1;
				revisionId = String.valueOf(rev);
				//add to audit log 
				AuditLogManager.log(new AuditLogRecord(
						documentId,
						AuditLogRecord.OBJECT_DOCUMENT,
						AuditLogRecord.ACTION_CHECKIN,
						loggedInUser.getUserName(),
						request.getRemoteAddr(),
						AuditLogRecord.LEVEL_INFO,
						"Document ID :  " + documentId + " Revision ID :" + revisionId,
						"Checked In" 
						));
				request.setAttribute(HTTPConstants.REQUEST_MESSAGE,"Document checked in successfully");
				return (new CheckInDocumentView(request,response));
			}
			int documentId = 0;
			try{
				documentId=Integer.parseInt(request.getParameter("documentid")!=null?request.getParameter("documentid"):"0");
			}catch(Exception e){
				request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid input");
				return (new CheckInDocumentView(request,response));
			}
			Document document =  DocumentDAO.getInstance().readDocumentById(documentId);
			if(document == null){
				request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid document");
				return (new CheckInDocumentView(request,response));
			}
			if(! Hit.STATUS_LOCKED.equalsIgnoreCase(document.getStatus())){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,  "Invalid checkin");
				return (new CheckInDocumentView(request,response));
			}
			DocumentClass documentClass = DocumentClassDAO.getInstance().readDocumentClassById(document.getClassId());
			LinkedHashMap<String,String> documentIndexes = IndexRecordManager.getInstance().readIndexRecord(documentClass, documentId, document.getRevisionId());

			request.setAttribute("DOCUMENTCLASS", documentClass);
			request.setAttribute("DOCUMENT", document);
			request.setAttribute("DOCUMENTINDEXES", documentIndexes);

		}catch(Exception e){
			e.printStackTrace();
		}
		return (new CheckInDocumentView(request,response));
	}
}

