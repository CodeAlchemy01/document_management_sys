/**
 * Created On 09-Jan-2014
 * Copyright 2014 by Primeleaf Consulting (P) Ltd.,
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
import kreidos.diamond.constants.ServerConstants;
import kreidos.diamond.model.AuditLogManager;
import kreidos.diamond.model.dao.UserDAO;
import kreidos.diamond.model.vo.AuditLogRecord;
import kreidos.diamond.model.vo.User;
import kreidos.diamond.web.action.Action;
import kreidos.diamond.web.view.WebView;
import kreidos.diamond.web.view.cpanel.EditUserView;

import org.apache.commons.validator.GenericValidator;


/**
 * Author Rahul Kubadia
 */

public class EditUserAction implements Action {
	public WebView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		User loggedInUser = (User)session.getAttribute(HTTPConstants.SESSION_KRYSTAL);

		if(request.getMethod().equalsIgnoreCase("POST")){
			short userId = 0;
			try{
				userId=Short.parseShort(request.getParameter("userid")!=null?request.getParameter("userid"):"0");
			}catch(Exception e){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
				return (new UsersAction().execute(request,response));
			}
			UserDAO.getInstance().setReadCompleteObject(true);
			User user = UserDAO.getInstance().readUserById(userId);

			if(user == null){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid User");
				return (new UsersAction().execute(request,response));
			}

			String userName = request.getParameter("txtUserName")!=null?request.getParameter("txtUserName"):"";
			String realName = request.getParameter("txtRealName")!=null?request.getParameter("txtRealName"):"";
			String userEmail = request.getParameter("txtUserEmail")!=null?request.getParameter("txtUserEmail"):"";
			String userDescription = request.getParameter("txtDescription")!=null?request.getParameter("txtDescription"):"";
			String active = request.getParameter("radActive")!=null?request.getParameter("radActive"):"Y";
			String userType = request.getParameter("radUserType")!=null?request.getParameter("radUserType"):"A";

			if(!GenericValidator.maxLength(userName, 15)){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Value too large for User Name");
				return (new UsersAction().execute(request,response));
			}
			if(!GenericValidator.maxLength(realName, 50)){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Value too large for Real Name");
				return (new UsersAction().execute(request,response));
			}
			if(! GenericValidator.isEmail(userEmail)){
				request.setAttribute(HTTPConstants.REQUEST_ERROR, "Invalid input");
				return (new UsersAction().execute(request,response));
			}
			if(! GenericValidator.maxLength(userEmail, 50)){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,  "Value too large for User Email");
				return (new UsersAction().execute(request,response));
			}
			if(!GenericValidator.maxLength(userDescription, 50)){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Value too large for User Description");
				return (new UsersAction().execute(request,response));
			}
			if(! "Y".equalsIgnoreCase(active)){
				active="N";
			}
			if(! User.USER_TYPE_ADMIN.equalsIgnoreCase(userType)){
				userType=User.USER_TYPE_USER;
			}

			//check if new email address is different then existing one
			if(!user.getUserEmail().equalsIgnoreCase(userEmail)){
				//it is different so validate
				if(UserDAO.getInstance().validateUserEmail(userEmail)){
					request.setAttribute(HTTPConstants.REQUEST_ERROR,"User with " + userEmail + " already exist");
					return (new UsersAction().execute(request,response));
				}
			}

			if(user.getUserName().equalsIgnoreCase(ServerConstants.SYSTEM_ADMIN_USER)){
				active="Y"; //Administrator user can never be in activated.
				userType = User.USER_TYPE_ADMIN;
			}
			try{
				user.setUserId(userId);
				user.setRealName(realName);
				user.setUserName(userName);
				user.setUserDescription(userDescription);
				user.setUserEmail(userEmail);

				if("Y".equalsIgnoreCase(active)){
					user.setActive(true);
				}else{
					user.setActive(false);
				}

				user.setUserType(userType);

				UserDAO.getInstance().updateUser(user,false);

				AuditLogManager.log(new AuditLogRecord(
						user.getUserId(),
						AuditLogRecord.OBJECT_USER,
						AuditLogRecord.ACTION_EDITED,
						loggedInUser.getUserName(),
						request.getRemoteAddr(),
						AuditLogRecord.LEVEL_INFO,
						"ID :" + user.getUserId(),
						"Name : " + user.getUserName()));
				request.setAttribute(HTTPConstants.REQUEST_MESSAGE,"User " + userName.toUpperCase() + " updated successfully");
				return (new UsersAction().execute(request,response));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			int userId = 0;
			try{
				userId=Integer.parseInt(request.getParameter("userid")!=null?request.getParameter("userid"):"0");
			}catch(Exception e){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid input");
				return (new UsersAction().execute(request,response));
			}
			UserDAO.getInstance().setReadCompleteObject(true);
			User user = UserDAO.getInstance().readUserById(userId);
			UserDAO.getInstance().setReadCompleteObject(false);

			if(user == null){
				request.setAttribute(HTTPConstants.REQUEST_ERROR,"Invalid User");
				return (new UsersAction().execute(request,response));
			}

			request.setAttribute("USER",user);

		}
		return (new EditUserView(request, response));
	}
}

