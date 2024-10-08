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

package kreidos.diamond.web.action.cpanel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.model.AuditLogManager;
import kreidos.diamond.model.dao.PasswordHistoryDAO;
import kreidos.diamond.model.dao.UserDAO;
import kreidos.diamond.model.vo.AuditLogRecord;
import kreidos.diamond.model.vo.PasswordHistory;
import kreidos.diamond.model.vo.User;
import kreidos.diamond.security.PasswordService;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.console.AJAXResponseView;
import kreidos.diamond.web.view.cpanel.ChangeUserPasswordView;


/**
 * @author Rahul Kubadia
 */

public class ChangeUserPasswordAction implements Action {
	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		User loggedInUser = (User)session.getAttribute(HTTPConstants.SESSION_KRYSTAL);

		if("POST".equalsIgnoreCase(request.getMethod())) {
			try{
				String newPassword = request.getParameter("txtNewPassword")!=null?request.getParameter("txtNewPassword"):"";
				String confirmPassword = request.getParameter("txtConfirmPassword")!=null?request.getParameter("txtConfirmPassword"):"";
				String userid = request.getParameter("userid")!=null? request.getParameter("userid"):"";
				int userId = 0;
				try{
					userId=Short.parseShort(userid);
				}catch(Exception e){
					request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
					return (new UsersAction().execute(request,response));
				}
				UserDAO.getInstance().setReadCompleteObject(true);
				User user = UserDAO.getInstance().readUserById(userId);
				UserDAO.getInstance().setReadCompleteObject(false);
				if(user == null){
					request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid user");
					return (new UsersAction().execute(request,response));
				}
				
				if(! newPassword.equals(confirmPassword)){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Password do not match");
					return (new AJAXResponseView(request, response));
				}

				if(PasswordHistoryDAO.getInstance().isPasswordExistInHistory(user.getUserId(), newPassword)){
					request.setAttribute(HTTPConstants.REQUEST_ERROR, "Password already used");
					return (new AJAXResponseView(request, response));
				}
				user.setPassword(newPassword);
				UserDAO.getInstance().updateUser(user,true);

				PasswordHistory passwordHistory = new PasswordHistory();
				passwordHistory.setUserId(user.getUserId());
				passwordHistory.setPassword(newPassword);
				if(PasswordHistoryDAO.getInstance().readByUserId(user.getUserId()).size()>=5){
					PasswordHistoryDAO.getInstance().deleteLastHistory(user.getUserId());
				}
				//Password is again set in loggedInUser in encrypted form 
				user.setPassword(PasswordService.getInstance().encrypt(newPassword));
				PasswordHistoryDAO.getInstance().create(passwordHistory);
				AuditLogManager.log(new AuditLogRecord(
						user.getUserId(),
						AuditLogRecord.OBJECT_USER,
						AuditLogRecord.ACTION_PASSWORDCHANGED,
						loggedInUser.getUserName(),
						request.getRemoteAddr(),
						AuditLogRecord.LEVEL_INFO,
						"",
						"Password Changed"));
				request.setAttribute(HTTPConstants.REQUEST_MESSAGE, "Password changed successfully");
				return (new AJAXResponseView(request, response));
			}catch(Exception ex){
				ex.printStackTrace();
			}
			return (new AJAXResponseView(request, response));
		}else{
			String userid = request.getParameter("userid")!=null? request.getParameter("userid"):"";
			int userId = 0;
			try{
				userId=Short.parseShort(userid);
			}catch(Exception e){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
				return (new UsersAction().execute(request,response));
			}
			UserDAO.getInstance().setReadCompleteObject(true);
			User user = UserDAO.getInstance().readUserById(userId);
			UserDAO.getInstance().setReadCompleteObject(false);
			if(user == null){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid user");
				return (new UsersAction().execute(request,response));
			}
			request.setAttribute("USER", user);
			return (new ChangeUserPasswordView(request, response));
		}
	}
}

