package se.independent.dbclassloader.jetty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import se.independent.dbclassloader.JDBCDbClassLoader;

public class JettyDbClassLoader extends JDBCDbClassLoader {
	
	static final Logger LOG = Log.getLogger(JettyDbClassLoader.class);
	
	public JettyDbClassLoader(ClassLoader cl) {
		super(cl);
	}

	public void inject(Connection c) {
		LOG.info(this.getClass().getSimpleName() + ".inject(" + c + ")");
		conn = c;
		prepare();
	}
	
	@Override
	public void close() {
		LOG.info(this.getClass().getSimpleName() + ".close()");
//		conn = null;
//		super.close();
	}
	
	
	public List<String> listWebInfLibJars() {
		LOG.info("> " + this.getClass().getSimpleName() + ".listWebInfLibJars()");
		List<String> rv = new ArrayList<String>();
		Statement stmnt = null;
		ResultSet rs = null;

		try {
			stmnt = conn.createStatement();				
			rs = stmnt.executeQuery("select path_element from dbclassload.dbcl_classpath where name ='"+ getClasspathName() +"' and path_element like '%.jar'");
		
			while (rs.next() ) {
				final String pe = rs.getString("path_element");
				if (pe != null) {
					rv.add(pe);
				}
			}	
		} catch (SQLException sqx) {
			LOG.warn("# " + this.getClass().getSimpleName() + ".listWebInfLibJars()", sqx);
		} finally {
			try { rs.close(); } catch ( Exception ign) {}
			try { stmnt.close(); } catch ( Exception ign) {}
		}
		
		LOG.info("< " + this.getClass().getSimpleName() + ".listWebInfLibJars()");
		return rv;
	}

//	 @Override
//	 public Enumeration<URL> getResources(String name) throws IOException {
//		 Enumeration<URL> rv = super.getResources(name);
//		 if (rv == null || rv.hasMoreElements()) {
//			 List<URL> resources = new ArrayList<URL>();
//			 URL url = this.findResource(name);
//			 if (url != null) {
//				 resources.add(url);
//				 rv = Collections.enumeration(resources);
//			 }
//		 }
//		 LOG.info("- getResources(" + name + ") = " + rv);
//		 return rv;
//	 }
	
	@Override
	protected Package getPackage(String name) {
		Package rv = super.getPackage(name);
		LOG.info(this.getClass().getSimpleName() + ".getPackage(" + name + ")");
		return rv;
	}
	
	@Override
	protected Package[] getPackages() {
		LOG.info(this.getClass().getSimpleName() + ".getPackages()");
		return super.getPackages();
	}
}
