/**
 * Created On 09-Jan-2014
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

package kreidos.diamond.web.view.cpanel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kreidos.diamond.constants.HTTPConstants;
import kreidos.diamond.web.view.WebPageTemplate;
import kreidos.diamond.web.view.WebView;


/**
 * @author Rahul Kubadia
 *
 */
public class ReportsView extends WebView {

	public ReportsView (HttpServletRequest request, HttpServletResponse response) throws Exception{
		init(request, response);
	}

	public void render() throws Exception{
		WebPageTemplate template = new WebPageTemplate(request,response);
		template.generateHeader();
		printReports();
		template.generateFooter();
	}
	private void printBreadCrumbs() throws Exception{
		out.println("<ol class=\"breadcrumb\">");
		out.println("<li><a href=\"/cpanel\">Control Panel</a></li>");
		out.println("<li class=\"active\">System Reports</li>");
		out.println("</ol>");
	}
	private void printReports() throws Exception{
		printBreadCrumbs();
		out.println("<div class=\"panel panel-default\">");
		out.println("<div class=\"panel-heading\">");
		out.println("<h4><i class=\"fa fa-lg fa-bar-chart-o\"></i> System Reports</h4>");
		out.println("</div>");
		out.println("<div class=\"panel-body\">");
		
		if(request.getAttribute(HTTPConstants.REQUEST_ERROR) != null){
			printErrorDismissable((String)request.getAttribute(HTTPConstants.REQUEST_ERROR));
		}
		if(request.getAttribute(HTTPConstants.REQUEST_MESSAGE) != null){
			printSuccessDismissable((String)request.getAttribute(HTTPConstants.REQUEST_MESSAGE));
		}

		try{
			out.println("<div class=\"row\">");
			
			out.println("<div class=\"col-sm-4\">");
			out.println("<div class=\"panel panel-default\">");
			out.println("<div class=\"panel-body\">");
			out.println("<h4>Repository Content Summary</h4>");
			out.println("<p>Summary Report of Contents available in Repository</p>");
			out.println("</div>"); //panel-body
			out.println("<div class=\"panel-footer\">");
			out.println("<a href=\"/cpanel/summary\">View Report</a>");
			out.println("</div>");
			out.println("</div>"); //panel
			out.println("</div>"); //col-sm-4

			out.println("<div class=\"col-sm-4\">");
			out.println("<div class=\"panel panel-default\">");
			out.println("<div class=\"panel-body\">");
			out.println("<h4>Access History</h4>");
			out.println("<p>Access History Report for Users of the system</p>");
			out.println("</div>"); //panel-body
			out.println("<div class=\"panel-footer\">");
			out.println("<a href=\"/cpanel/useraccesshistory\">View Report</a>");
			out.println("</div>");
			out.println("</div>"); //panel
			out.println("</div>"); //col-sm-4

			out.println("<div class=\"col-sm-4\">");
			out.println("<div class=\"panel panel-default\">");
			out.println("<div class=\"panel-body\">");
			out.println("<h4>Document Class Access History</h4>");
			out.println("<p>Access History Report for Document Classes</p>");
			out.println("</div>"); //panel-body
			out.println("<div class=\"panel-footer\">");
			out.println("<a href=\"/cpanel/documentclassaccesshistory\">View Report</a>");
			out.println("</div>");
			out.println("</div>"); //panel
			out.println("</div>"); //col-sm-4
			
			out.println("</div>"); //row
			
			out.println("</div>");//panel-body
			out.println("</div>");//panel
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

}

