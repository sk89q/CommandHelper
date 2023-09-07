package com.laytonsmith.database;

import com.laytonsmith.core.Profiles;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 */
public abstract class SQLProfile extends Profiles.Profile {

	public SQLProfile(String id, Map<String, String> elements) {
		super(id);
	}

	/**
	 * Given the connection details, this should return the proper connection string that the actual database connector
	 * will use to create a connection with this profile. Additionally, during this step, it should be verified that the
	 * SQL driver is present.
	 *
	 * @return
	 * @throws SQLException If the database driver doesn't exist.
	 */
	public abstract String getConnectionString() throws SQLException;

	/**
	 * Returns true if the query calls for autogenerated keys to be returned. By default, we return true, because most
	 * database systems can handle having it always be true, but some database systems will fail on things like SELECT
	 * statements if autogenerated keys are requested on non-INSERT statements.
	 *
	 * @param query The query to
	 * @return
	 */
	public boolean getAutogeneratedKeys(String query) {
		return true;
	}

	/**
	 * Returns true if parameter types are provided by this driver implementation before being set.
	 *
	 * @return
	 */
	public boolean providesParameterTypes() {
		return true;
	}
}
