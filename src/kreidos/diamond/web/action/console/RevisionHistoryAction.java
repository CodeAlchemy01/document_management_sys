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

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.model.RevisionManager;
import kreidos.diamond.model.dao.DocumentClassDAO;
import kreidos.diamond.model.dao.DocumentDAO;
import kreidos.diamond.model.vo.Document;
import kreidos.diamond.model.vo.DocumentClass;
import kreidos.diamond.model.vo.RevisionRecord;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.console.AJAXResponseView;
import kreidos.diamond.web.view.console.RevisionHistoryView;


/**
 * Author Rahul Kubadia
 */

public class RevisionHistoryAction implements Action {
	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String documentid = request.getParameter("documentid").trim();// get object id
		
		int documentId = 0;
		try{
			documentId = Integer.parseInt(documentid);
		}catch(Exception ex){
			request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
			return new AJAXResponseView(request,response);
		}
		Document document = DocumentDAO.getInstance().readDocumentById(documentId);
		if(document == null){
			request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid document");
			return new AJAXResponseView(request,response);
		}
		RevisionManager revisionManager = new RevisionManager();
		ArrayList<RevisionRecord> revisionHistory = revisionManager.getRevisionHistory(documentId);
		DocumentClass documentClass = DocumentClassDAO.getInstance().readDocumentClassById(document.getClassId());
		request.setAttribute("DOCUMENT", document);
		request.setAttribute("DOCUMENTCLASS", documentClass);
		request.setAttribute("REVISIONHISTORY", revisionHistory);
		return (new RevisionHistoryView(request, response));
	}
}

