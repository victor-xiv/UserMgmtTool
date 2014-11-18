package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

import beans.AccountRequestsBean;
import tools.AccountHelper;
import tools.ConcertoAPI;
import tools.EmailClient;
import tools.PasswordGenerator;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class AddUserServlet extends HttpServlet {
	Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
		response.sendRedirect(redirectURL);
    }
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger.debug("AddUserServlet about to process Post request: " + request.getQueryString());
		
		HttpSession session = request.getSession(true);
		Map<String,String[]> paramMaps = (Map<String,String[]>)request.getParameterMap();
		Map<String,String[]> maps = new HashMap<String, String[]>();
		maps.putAll(paramMaps);
		maps.put("isLdapClient", new String[] { "true" });
		
		boolean isPswGenerated = false;
		if (!maps.containsKey("password01")
				|| maps.get("password01") == null
				|| ((String[]) maps.get("password01"))[0].trim().equals("GenPsw")
				|| ((String[]) maps.get("password01"))[0].trim().isEmpty()) {
			
			maps.put("password01", new String[] { PasswordGenerator.generatePswForLength(8) });
			isPswGenerated = true;
		}
		
		
		
		String result = AccountHelper.createAccount(maps, isPswGenerated);
		
		response.getWriter().write(result);
	}
}