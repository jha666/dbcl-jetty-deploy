package se.independent.dbclassloader.jetty;


import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import se.independent.dbclassloader.DbClassLoader;

@ManagedObject("Db Web Application ContextHandler")
public class DbWebAppContext extends WebAppContext {

	static final Logger LOG = Log.getLogger(DbWebAppContext.class);

	public DbWebAppContext(final String appOriginId) {
		super();
		DbClassLoader dbcl = new JettyDbClassLoader(DbWebAppContext.class.getClassLoader()!=null?DbWebAppContext.class.getClassLoader()
                :ClassLoader.getSystemClassLoader());
		setClassLoader(dbcl);
		setConfigurationClasses(WebAppContext.DEFAULT_CONFIGURATION_CLASSES);
		dbcl.setClasspathName(appOriginId);
		Thread.currentThread().setContextClassLoader(dbcl);
	}

	public Resource newResource(final String name) {
		ClassLoader cl = getClassLoader();
		URL url = cl.getResource(name);
		LOG.info(this.getClass().getSimpleName() + ".newResource(" + name + ") url=" + url);
		return Resource.newResource(url);
	}

	
    private Throwable _unavailableException;

	@Override
	protected void doStart() throws Exception {
		LOG.info(this.getClass().getSimpleName() + ".doStart()");
		try {
			connect();
			// _metadata.setAllowDuplicateFragmentNames(isAllowDuplicateFragmentNames());
			//Boolean validate = (Boolean)getAttribute(MetaData.VALIDATE_XML);
			//_metadata.setValidateXml((validate!=null && validate.booleanValue()));
			
			preConfigure();
			super.doStart();
			postConfigure();
			//
			if (isLogUrlOnStart()) {
				dumpUrl();
			}
		} catch (Throwable t) {
			// start up of the webapp context failed, make sure it is not started
			LOG.warn("Failed startup of context " + this, t);
			 _unavailableException=t;
			setAvailable(false); // webapp cannot be accessed (results in status code 503)
			if (isThrowUnavailableOnStartupException())
				throw t;
		}
	}

	/* ------------------------------------------------------------ */
	/*
	 * @see org.eclipse.thread.AbstractLifeCycle#doStop()
	 */
	@Override
	protected void doStop() throws Exception {
		LOG.info(this.getClass().getSimpleName() + ".doStop()");

		ClassLoader x = getClassLoader();
		if (x != null && x instanceof JettyDbClassLoader) {
			((JettyDbClassLoader)x).close();
		}
		
		super.doStop();
	}

	@Override
	public String getDescriptor() {
		LOG.info(this.getClass().getSimpleName() + ".getDescriptor()");
		return "WEB-INF/web.xml";
	}

	public Resource getWebInfClassesDir() {
		LOG.info(this.getClass().getSimpleName() + ".getWebInfClassesDir()");
		ClassLoader cl = getClassLoader();
		URL url = cl.getResource("WEB-INF/classes");
		return Resource.newResource(url);
	}

	
	public Collection<? extends Resource> listWebInfLibJars() {
		LOG.info(this.getClass().getSimpleName() + ".listWebInfLibJars()");
		ClassLoader cl = getClassLoader();
		List<Resource> rv = new ArrayList<Resource>();
		if (cl instanceof JettyDbClassLoader) {
			List<String> pes = ((JettyDbClassLoader)cl).listWebInfLibJars();
			for (String pe : pes) {
				URL url = cl.getResource("WEB-INF/lib/"+pe.trim());
				Resource r = Resource.newResource(url);
				rv.add(r);
				LOG.info(this.getClass().getSimpleName() + ".listWebInfLibJars() resource=" + r);
			}
		}
		
//		Statement stmnt = null;
//		ResultSet rs = null;
//		
//		this.getInitParameter("foo");
//		this.getAttribute("foo");
//		
//		
//		try {
//			stmnt = _conn.createStatement();				
//			rs = stmnt.executeQuery("select path_element from dbclassload.dbcl_classpath where name ='"+ getWar() +"' and path_element like '%.jar'");
//			
//			while (rs.next() ) {
//				final String pe = rs.getString("path_element");
//				if (pe != null) {
//					URL url = cl.getResource("WEB-INF/lib/"+pe.trim());
//					Resource r = Resource.newResource(url);
//					rv.add(r);
//					LOG.info(this.getClass().getSimpleName() + ".listWebInfLibJars() resource=" + r);
//				}
//			}
//				
//		} catch (SQLException sqx) {
//			
//		} finally {
//			try { rs.close(); } catch ( Exception ign) {}
//			try { stmnt.close(); } catch ( Exception ign) {}
//		}
		
		return rv;
	}

	@Override
	public Resource getBaseResource() {
		Resource rv = null;
		ClassLoader cl = getClassLoader();
		if (cl != null) {
			final String war = getWar();
			final URL url = cl.getResource(war);
			rv = Resource.newResource(url);	
		} 
//		else
//			rv = super.getBaseResource();
//		LOG.info(this.getClass().getSimpleName() + ".getBaseResource() = " + rv);
		return rv;
	}
	
	
	// ========================================================================
//	private String _jdbcURL = "jdbc:postgresql://192.168.1.12:5432/postgres";
//	private String _user = "DBCLASSLOAD";
//	private String _password = "Tr1ss";
	protected Connection _conn;
//	protected String _driver = "org.postgresql.Driver";

	private void connect() {
		LOG.info(this.getClass().getSimpleName() + ".connect()");
		try {
//			Class.forName(_driver);
//
//			Properties p = new Properties();
//			p.setProperty("user", _user);
//			p.setProperty("password", _password);
//
//			_conn = DriverManager.getConnection(_jdbcURL, p);
//
//			LOG.info("- connect() [database] " + _conn.getMetaData().getDatabaseProductName() + " "
//					+ _conn.getMetaData().getDatabaseProductVersion());

			ClassLoader x = getClassLoader();
			if (x instanceof JettyDbClassLoader) {
				((JettyDbClassLoader)x).inject(_conn);
			}

		} catch ( Exception e) {
			LOG.warn(this.getClass().getSimpleName() + ".connect()", e);
		}
	}

	private void dumpUrl() {
		Connector[] connectors = getServer().getConnectors();
		for (int i = 0; i < connectors.length; i++) {
			String displayName = getDisplayName();
			if (displayName == null)
				displayName = "DbWebApp@" + Arrays.hashCode(connectors);

			LOG.info(displayName + " at http://" + connectors[i].toString() + getContextPath());
		}
	}

	public void setConn(final Connection c) {
		_conn = c;
	}

}
