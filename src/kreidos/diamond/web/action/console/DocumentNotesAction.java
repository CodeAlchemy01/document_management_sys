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
import javax.servlet.http.HttpSession;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.model.AuditLogManager;
import kreidos.diamond.model.dao.DocumentDAO;
import kreidos.diamond.model.dao.DocumentNoteDAO;
import kreidos.diamond.model.vo.AuditLogRecord;
import kreidos.diamond.model.vo.Document;
import kreidos.diamond.model.vo.DocumentNote;
import kreidos.diamond.model.vo.Hit;
import kreidos.diamond.model.vo.User;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.console.AJAXResponseView;
import kreidos.diamond.web.view.console.DocumentNotesView;


/**
 * Author Rahul Kubadia
 */

public class DocumentNotesAction implements Action {
	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		User loggedInUser = (User)session.getAttribute(HTTPConstants.SESSION_KRYSTAL);
		String documentid = request.getParameter("documentid")!=null?request.getParameter("documentid"):"0";
		int documentId = 0;
		try{
			documentId = Integer.parseInt(documentid.trim());
		}catch(Exception ex){
			request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
			return new AJAXResponseView(request,response);
		}
		Document document = DocumentDAO.getInstance().readDocumentById(documentId);
		if(document == null){
			request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid document");
			return new AJAXResponseView(request,response);
		}
		String txtNote = request.getParameter("txtNote")!=null?request.getParameter("txtNote"):"";
		String noteType = request.getParameter("radNoteType")!=null?request.getParameter("radNoteType"):"";
		if(txtNote.trim().length()>0){
			DocumentNote documentNote = new DocumentNote();
			documentNote.setDocumentId(documentId);
			documentNote.setNoteData(txtNote);
			documentNote.setUserName(loggedInUser.getUserName());
			documentNote.setNoteType(noteType);

			DocumentNoteDAO.getInstance().addJournalNote(documentNote);
			document.setHasNote((byte)(document.getHasNote() | Hit.FLAG_HASNOTE));

			DocumentDAO.getInstance().updateDocument(document);
			AuditLogManager.log(new AuditLogRecord(
					documentId,
					AuditLogRecord.OBJECT_JOURNAL_NOTE,
					AuditLogRecord.ACTION_CREATED,
					loggedInUser.getUserName(),
					request.getRemoteAddr(),
					AuditLogRecord.LEVEL_FINE,
					"Document ID : " +documentNote.getDocumentId() ,
					"Journal Note Added"));
			request.setAttribute(HTTPConstants.REQUEST_MESSAGE, "Note added successfully");
		}
		String noteid = request.getParameter("noteid")!=null?request.getParameter("noteid"):"";
		if(noteid.trim().length() > 0 ){
			int noteId = 0;
			try{
				noteId = Integer.parseInt(noteid.trim());
			}catch(Exception ex){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
				return new AJAXResponseView(request,response);
			}
			DocumentNote note = DocumentNoteDAO.getInstance().readByNoteId(noteId);
			if(note == null){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid note");
				return new AJAXResponseView(request,response);
			}

			if(note.getUserName().equalsIgnoreCase(loggedInUser.getUserName()) || loggedInUser.isAdmin()){
				DocumentNoteDAO.getInstance().deleteJournalNote(noteId);

				if(DocumentNoteDAO.getInstance().readJournalNotes("DOCUMENTID=" + note.getDocumentId() +" AND ACTIVE='Y' ORDER BY 1 DESC").size() == 0){
					document = DocumentDAO.getInstance().readDocumentById(note.getDocumentId());
					document.setHasNote((byte)0);
					DocumentDAO.getInstance().updateDocument(document);
				}

				AuditLogManager.log(new AuditLogRecord(note.getDocumentId(),
						AuditLogRecord.OBJECT_JOURNAL_NOTE,
						AuditLogRecord.ACTION_DELETED,
						loggedInUser.getUserName(),
						request.getRemoteAddr(),
						AuditLogRecord.LEVEL_FINE,
						"Document ID : " +note.getDocumentId() ,
						"Journal Note Deleted")
						);
				request.setAttribute(HTTPConstants.REQUEST_MESSAGE,"Note deleted successfully");
			}else{
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Access Denied");
			}
		}
		ArrayList<DocumentNote> documentNotes = DocumentNoteDAO.getInstance().readJournalNotes("DOCUMENTID=" + documentId +" AND ACTIVE='Y' ORDER BY 1 DESC");
		ArrayList<DocumentNote> availableDocumentNotes  = new ArrayList<DocumentNote>();
		for(DocumentNote note : documentNotes){
			if(note.getNoteType().equalsIgnoreCase("P") || note.getUserName().equalsIgnoreCase(loggedInUser.getUserName())){
				availableDocumentNotes.add(note);
			}
		}
		request.setAttribute("DOCUMENT", document );
		request.setAttribute("NOTES",availableDocumentNotes );
		return (new DocumentNotesView(request, response));
	}
}

