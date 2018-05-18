package se.independent.dbclassloader.jetty;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppProvider;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

@ManagedObject("Provider for start-up deployement of webapps based on presence in dataabse")
public class DbWebAppProvider extends AbstractLifeCycle implements AppProvider {

	private static final Logger LOG = Log.getLogger(DbWebAppProvider.class);

    private Map<String, App> _appMap = new HashMap<String, App>();

    private DeploymentManager _deploymentManager;
    private boolean _extractWars = false;
    private boolean _parentLoaderPriority = false;
    private String _defaultsDescriptor;
    private File _tempDirectory;
    private String[] _configurationClasses;
    
    private String _jdbcURL;	// = "jdbc:postgresql://192.168.1.2:5432/postgres";
    private String _user; 		// = "DBCLASSLOAD";
    private String _password; 	// = "Tr1ss";

    
    @ManagedAttribute("JDBC url")
    public String getJdbcURL() {
    	return _jdbcURL;
    }

    public void setJdbcURL(final String url) {
    	LOG.info(this.getClass().getSimpleName() + ".setJdbcURL(" + url + ")");
    	_jdbcURL = url;
    }
    
    
    @ManagedAttribute("JDBC user")
    public String getUser() {
    	return _user;
    }

    public void setUser(final String url) {
    	LOG.info(this.getClass().getSimpleName() + ".setUser(" + url + ")");
    	_user = url;
    }

    
    @ManagedAttribute("JDBC password")
    public String getPassword() {
    	return _password;
    }

    public void setPassword(final String url) {
    	LOG.info(this.getClass().getSimpleName() + ".setPassword(" + ")");
    	_password = url;
    }

    
	@Override
	public ContextHandler createContextHandler(final App app) throws Exception {
		LOG.info(this.getClass().getSimpleName() + ".createContextHandler(" + app + ")");
        String context = app.getOriginId();
        
        // Ensure "/" is Not Trailing in context paths.
        if (context.endsWith("/") && context.length() > 0) 
        {
            context = context.substring(0,context.length() - 1);
        }
        
        // Start building the webapplication
        DbWebAppContext wah = new DbWebAppContext(app.getOriginId());
        wah.setDisplayName(context);
        wah.setConn(_conn);
        //wah.setClassLoader(classLoader);
        
 
        // Ensure "/" is Prepended to all context paths.
        if (context.charAt(0) != '/') 
        {
            context = "/" + context;
        }


        wah.setContextPath(context.substring(0, context.lastIndexOf(".war")));
        
        wah.setWar(app.getOriginId());
        if (_defaultsDescriptor != null) 
        {
            wah.setDefaultsDescriptor(_defaultsDescriptor);
        }
        
        wah.setExtractWAR(_extractWars);
        wah.setParentLoaderPriority(_parentLoaderPriority);
        
        if (_configurationClasses != null) {
            wah.setConfigurationClasses(_configurationClasses);
        }

        if (_tempDirectory != null)
        {
            /* Since the Temp Dir is really a context base temp directory,
             * Lets set the Temp Directory in a way similar to how WebInfConfiguration does it,
             * instead of setting the
             * WebAppContext.setTempDirectory(File).  
             * If we used .setTempDirectory(File) all webapps will wind up in the
             * same temp / work directory, overwriting each others work.
             */
            wah.setAttribute(WebAppContext.BASETEMPDIR,_tempDirectory);
        }
        return wah; 
	}

	
	@Override
	public void setDeploymentManager(DeploymentManager arg0) {
		LOG.info(this.getClass().getSimpleName() + ".setDeploymentManager(" + arg0 + ")");
		_deploymentManager = arg0;
	}

	
	 /* ------------------------------------------------------------ */
    @Override
    protected void doStart() throws Exception {
        LOG.info(this.getClass().getSimpleName() + ".doStart()");
        
        System.setProperty("java.naming.factory.initial","org.eclipse.jetty.jndi.InitialContextFactory");
        //_deploymentManager.setContexts(contexts);
        
        Enumeration<String> en =_deploymentManager.getServer().getAttributeNames();
        while (en.hasMoreElements()) {
        	final String name = en.nextElement();
        	LOG.info(this.getClass().getSimpleName() + ".doStart() [server] name="+name);
        }
        
       for (Connector c : _deploymentManager.getServer().getConnectors()) {
    	  // c.get
       }
 
       
        Properties p = new Properties();
        p.setProperty("user", _user);
        p.setProperty("password", _password);

        connect(_jdbcURL, p);
        
        Statement stmnt = _conn.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from DBCLASSLOAD.WEBAPPS");
        
        while (rs.next()) {
	    	String filename = rs.getString("WAR_NAME");
	    	LOG.info(this.getClass().getSimpleName() + ".doStart() added ",filename);
	        App app =  new App(_deploymentManager,this,filename);
	        if (app != null)
	        {
	            _appMap.put(filename,app);
	            _deploymentManager.addApp(app);
	        }
        }
        
        rs.close();
        stmnt.close();
        
    }
    
    protected Connection _conn;
    

	public void connect(String url, final Properties p) {
		LOG.info(this.getClass().getSimpleName() + ".connect(" + url +")");

		try {
			
			DriverManager.getDriver(url);
			_conn = DriverManager.getConnection(url, p);
			
			LOG.info(this.getClass().getSimpleName() + ".connect() [database] " + _conn.getMetaData().getDatabaseProductName() + " " + _conn.getMetaData().getDatabaseProductVersion());
		} catch (Exception ex) {
			LOG.info(this.getClass().getSimpleName() + ".connect(" + url + ")", ex);
		} finally {
		}
	}

 
	/* ------------------------------------------------------------ */
    @Override
    protected void doStop() throws Exception
    {
    	if (_conn != null) {
	        _conn.close();
	        _conn = null;
    	}
    }
    
    /* ------------------------------------------------------------ */
    /** Get the extractWars.
     * @return the extractWars
     */
    public boolean isExtractWars()
    {
        return _extractWars;
    }

    /* ------------------------------------------------------------ */
    /** Set the extractWars.
     * @param extractWars the extractWars to set
     */
    public void setExtractWars(boolean extractWars)
    {
        _extractWars = extractWars;
    }

    /* ------------------------------------------------------------ */
    /** Get the parentLoaderPriority.
     * @return the parentLoaderPriority
     */
    public boolean isParentLoaderPriority()
    {
        return _parentLoaderPriority;
    }

    /* ------------------------------------------------------------ */
    /** Set the parentLoaderPriority.
     * @param parentLoaderPriority the parentLoaderPriority to set
     */
    public void setParentLoaderPriority(boolean parentLoaderPriority)
    {
        _parentLoaderPriority = parentLoaderPriority;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the defaultsDescriptor.
     * @return the defaultsDescriptor
     */
    public String getDefaultsDescriptor()
    {
    	LOG.info(this.getClass().getSimpleName() + ".getDefaultDescriptor()");
        return _defaultsDescriptor;
    }

    /* ------------------------------------------------------------ */
    /** Set the defaultsDescriptor.
     * @param defaultsDescriptor the defaultsDescriptor to set
     */
    public void setDefaultsDescriptor(String defaultsDescriptor)
    {
    	LOG.info(this.getClass().getSimpleName() + ".setDefaultDescriptor(" + defaultsDescriptor + ")");
        _defaultsDescriptor = defaultsDescriptor;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param configurations The configuration class names.
     */
    public void setConfigurationClasses(String[] configurations)
    {
    	LOG.info(this.getClass().getSimpleName() + ".setConfigurationClasses(" + configurations + ")");
        _configurationClasses = configurations==null?null:(String[])configurations.clone();
    }  
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public String[] getConfigurationClasses()
    {
        return _configurationClasses;
    }
    
    /**
     * Set the Work directory where unpacked WAR files are managed from.
     * <p>
     * Default is the same as the <code>java.io.tmpdir</code> System Property.
     * 
     * @param directory the new work directory
     */
    public void setTempDir(File directory)
    {
    	LOG.info(this.getClass().getSimpleName() + ".setTempDir(" + directory + ")");
        _tempDirectory = directory;
    }
    
    /**
     * Get the user supplied Work Directory.
     * 
     * @return the user supplied work directory (null if user has not set Temp Directory yet)
     */
    public File getTempDir()
    {
        return _tempDirectory;
    }
}
