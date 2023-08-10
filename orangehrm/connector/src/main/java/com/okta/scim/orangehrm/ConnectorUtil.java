package com.okta.scim.orangehrm;

import com.okta.scim.util.model.Email;
import com.okta.scim.util.model.Name;
import com.okta.scim.util.model.SCIMGroup;
import com.okta.scim.util.model.SCIMUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Indranil
 */
public class ConnectorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorUtil.class);
    
    private static String myDriver = "com.mysql.cj.jdbc.Driver";
    private static String myUrl = "jdbc:mysql://<host>/<db>";
    private static Connection dbconn = null;

   

    /**
     * Read the users from DB into a users map
     *
     * @param userMap
     * @throws java.io.IOException
     */
    public static void readUsersFromSource(Map<String, SCIMUser> userMap) throws IOException, Exception {
    	
    	if ((dbconn == null)||(dbconn.isClosed()))
    		bootStrapConnection();
    	
    	Map<String, SCIMUser> dataMap = queryUsersFromDB();
        if (dataMap.size() == 0) {
            return;
        }
        
        Iterator<Entry<String, SCIMUser>> it = dataMap.entrySet().iterator();

        while (it.hasNext()) {
        	SCIMUser user = (SCIMUser)it.next().getValue();
            userMap.put(user.getId(), user);
        }
        
        System.out.println(userMap.toString());
    }

    /**
     * Read the groups from DB into a groups map
     *
     * @param groupMap
     * @throws Exception
     */
    public static void readGroupsFromSource(Map<String, SCIMGroup> groupMap) throws IOException, Exception {
    	
    	if ((dbconn == null)||(dbconn.isClosed()))
    		bootStrapConnection();
    	
    	Map<String, SCIMGroup> dataMap = queryGroupsFromDB();
        if (dataMap.size() == 0) {
            return;
        }
        
        Iterator<Entry<String, SCIMGroup>> it = dataMap.entrySet().iterator();

        while (it.hasNext()) {
        	SCIMGroup group = (SCIMGroup)it.next().getValue();
        	groupMap.put(group.getId(), group);
        }
        
        System.out.println(groupMap.toString());
    }
    
    private static Map<String, SCIMUser> queryUsersFromDB() throws SQLException {
        try {
        
            String query = "SELECT * FROM hs_hr_employee";
            
            
            SCIMUser user = null;
            Map<String, SCIMUser> dataMap = new HashMap<String, SCIMUser>();

            Statement st = dbconn.createStatement();
            ResultSet rs = st.executeQuery(query);
            
            while (rs.next())
            {
              int empId = rs.getInt("employee_id");
              String firstName = rs.getString("emp_firstname");
              String lastName = rs.getString("emp_lastname");
              String userName = firstName.toLowerCase().substring(0, 1) + lastName.toLowerCase();
              
              user = new SCIMUser();
              
              user.setId(String.valueOf(empId));
              user.setUserName(userName);
              user.setName(new Name(firstName + " " + lastName, lastName, firstName));
              
              user.setEmails(Arrays.asList(new Email(userName + "@atko.email", "work", true)));
              user.setActive(true);
              user.setPassword("wicdemo");
              user.setLastModified(Date.from(LocalDate.of(2000, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
              
              
              
              dataMap.put(user.getId(), user);
            }
            st.close();
            
            return dataMap;
        } catch (SQLException e) {
            LOGGER.error("Error in SQL Execution:", e);
            throw e;
        }
        
    }
    
    private static Map<String, SCIMGroup> queryGroupsFromDB() throws SQLException {
        try {
            System.out.println("Querying from DB");
            throw new SQLException();
        } catch (SQLException e) {
        	LOGGER.error("Error in SQL Execution:", e);
        }

        return null;
    }
    
    private static void bootStrapConnection() throws Exception {
    	try {
    		Class.forName(myDriver);
    		dbconn = DriverManager.getConnection(myUrl, "root", "orangehrm");
    	} catch (ClassNotFoundException e) {
        	LOGGER.error("Cannot find class:", e);
        	throw new Exception(e);
        }catch (SQLException e) {
        	LOGGER.error("DB initialization error:", e);
        	throw new Exception(e);
        }
    }
    
    public static void main(String[] args) throws Exception{
    	readUsersFromSource(new HashMap<String, SCIMUser>());
    	readUsersFromSource(new HashMap<String, SCIMUser>());
    }


}
