package filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

public class ResponseExceptionFilter implements Filter{
	Logger logger = Logger.getRootLogger();

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		
		try{
			filterChain.doFilter(request, response);
		} catch (Exception e){
			Logger.getRootLogger().error("Unknown exception.", e);
			HttpSession session = ((HttpServletRequest)request).getSession(true);
			Enumeration<String> sessionAttrs = session.getAttributeNames();
			while(sessionAttrs.hasMoreElements()){
				String attr = sessionAttrs.nextElement();
				session.removeAttribute(attr);
			}
			
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			PrintWriter output = response.getWriter();
			output.write("There's an error occured while processing your request. Please contact Orion Health Support Team.");
		}
	}
	
	
	

	

}
