package com.okta.scim.orangehrm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.okta.scim.server.capabilities.UserManagementCapabilities;
import com.okta.scim.server.example.SCIMServiceImpl;
import com.okta.scim.server.exception.DuplicateGroupException;
import com.okta.scim.server.exception.EntityNotFoundException;
import com.okta.scim.server.exception.OnPremUserManagementException;
import com.okta.scim.server.service.SCIMService;
import com.okta.scim.util.model.Email;
import com.okta.scim.util.model.Name;
import com.okta.scim.util.model.PaginationProperties;
import com.okta.scim.util.model.SCIMFilter;
import com.okta.scim.util.model.SCIMFilterType;
import com.okta.scim.util.model.SCIMGroup;
import com.okta.scim.util.model.SCIMGroupQueryResponse;
import com.okta.scim.util.model.SCIMUser;
import com.okta.scim.util.model.SCIMUserQueryResponse;


public class ConnectorImpl implements SCIMService {
	    private Map<String, SCIMUser> userMap = new HashMap<String, SCIMUser>();
	    private Map<String, SCIMGroup> groupMap = new HashMap<String, SCIMGroup>();
	    //private String userCustomUrn;


	    private static final Logger LOGGER = LoggerFactory.getLogger(SCIMServiceImpl.class);

	    @PostConstruct
	    public void afterCreation() throws Exception {
	    	System.out.println("One time run");
	        //userCustomUrn = SCIMOktaConstants.CUSTOM_URN_PREFIX + APP_NAME + SCIMOktaConstants.CUSTOM_URN_SUFFIX + UD_SCHEMA_NAME;
	        //updateCache();
	        return;
	    }

	    
	    @Override
	    public SCIMUser createUser(SCIMUser user) throws OnPremUserManagementException {
	        return null;
	    }

	    @Override
	    public SCIMUser updateUser(String id, SCIMUser user) throws OnPremUserManagementException, EntityNotFoundException {
	       return null;
	    }

	    /**
	     * Get all the users.
	     * <p>
	     * This method is invoked when a GET is made to /Users
	     * In order to support pagination (So that the client and the server are not overwhelmed), this method supports querying based on a start index and the
	     * maximum number of results expected by the client. The implementation is responsible for maintaining indices for the SCIM Users.
	     *
	     * @param pageProperties denotes the pagination properties
	     * @param filter         denotes the filter
	     * @return the response from the server, which contains a list of  users along with the total number of results, start index and the items per page
	     * @throws com.okta.scim.server.exception.OnPremUserManagementException
	     *
	     */
	    @Override
	    public SCIMUserQueryResponse getUsers(PaginationProperties pageProperties, SCIMFilter filter) throws OnPremUserManagementException {
	        List<SCIMUser> users = null;
	        if (filter != null) {
	            //Get users based on a filter
	            users = getUserByFilter(filter);
	            //Example to show how to construct a SCIMUserQueryResponse and how to set stuff.
	            SCIMUserQueryResponse response = new SCIMUserQueryResponse();
	            //The total results in this case is set to the number of users. But it may be possible that
	            //there are more results than what is being returned => totalResults > users.size();
	            response.setTotalResults(users.size());
	            //Actual results which need to be returned
	            response.setScimUsers(users);
	            //The input has some page properties => Set the start index.
	            if (pageProperties != null) {
	                response.setStartIndex(pageProperties.getStartIndex());
	            }
	            return response;
	        } else {
	            return getUsers(pageProperties);
	        }
	    }

	    private SCIMUserQueryResponse getUsers(PaginationProperties pageProperties) {
	    	updateCache();
	        SCIMUserQueryResponse response = new SCIMUserQueryResponse();
	        /**
	         * Below is an example to show how to deal with exceptional conditions while writing the connector.
	         * If you cannot complete the UserManagement operation on the on premises
	         * application because of any error/exception, you should throw the OnPremUserManagementException as shown below.
	         * <b>Note:</b> You can throw this exception from all the CRUD (Create/Retrieve/Update/Delete) operations defined on
	         * Users/Groups in the SCIM interface.
	         */
	        if (userMap == null) {
	            //Note that the Error Code "o34567" is arbitrary - You can use any code that you want to.
	            throw new OnPremUserManagementException("o34567", "Cannot get the users. The userMap is null");
	        }

	        int totalResults = userMap.size();
	        if (pageProperties != null) {
	            //Set the start index to the response.
	            response.setStartIndex(pageProperties.getStartIndex());
	        }
	        //In this example we are setting the total results to the number of results in this page. If there are more
	        //results than the number the client asked for (pageProperties.getCount()), then you need to set the total results correctly
	        response.setTotalResults(totalResults);
	        List<SCIMUser> users = new ArrayList<SCIMUser>();
	        SortedSet<String> keys = new TreeSet(userMap.keySet());
	        for (String key : keys) {
	            users.add(userMap.get(key));
	        }
	        //Set the actual results
	        response.setScimUsers(users);
	        return response;
	    }

	    /**
	     * A simple example of how to use <code>SCIMFilter</code> to return a list of users which match the filter criteria.
	     * <p/>
	     * An Admin who configures the UM would specify a SCIM field name as the UniqueId field name. This field and its value would be sent by Okta in the filter.
	     * While implementing the connector, the below points should be noted about the filters.
	     * <p/>
	     * If you choose a single valued attribute as the UserId field name while configuring the App Instance on Okta,
	     * you would get an equality filter here.
	     * For example, if you choose userName, the Filter object below may represent an equality filter like "userName eq "someUserName""
	     * If you choose the name.familyName as the UserId field name, the filter object may represent an equality filter like
	     * "name.familyName eq "someLastName""
	     * If you choose a multivalued attribute (email, for example), the <code>SCIMFilter</code> object below may represent an OR filter consisting of two sub-filters like
	     * "email eq "abc@def.com" OR email eq "def@abc.com""
	     * Of the few multi valued attributes part of the SCIM Core Schema (Like email, address, phone number), only email would be supported as a UserIdField name on Okta.
	     * So, you would have to deal with OR filters only if you choose email.
	     * <p/>
	     * When you get a <code>SCIMFilter</code>, you should check the filter field name (And make sure it is the same field which was configured with Okta), value, condition, etc. as shown in the examples below.
	     *
	     * @param filter the SCIM filter
	     * @return list of users that match the filter
	     */
	    private List<SCIMUser> getUserByFilter(SCIMFilter filter) {
	        List<SCIMUser> users = new ArrayList<SCIMUser>();

	        SCIMFilterType filterType = filter.getFilterType();

	        if (filterType.equals(SCIMFilterType.EQUALS)) {
	            //Example to show how to deal with an Equality filter
	            users = getUsersByEqualityFilter(filter);
	        } else if (filterType.equals(SCIMFilterType.OR)) {
	            //Example to show how to deal with an OR filter containing multiple sub-filters.
	            users = getUsersByOrFilter(filter);
	        } else if (filterType.equals(SCIMFilterType.GREATER_THAN)) {
	            //Example to show how to deal with a GT filter as in incremental imports
	            users = getUsersByGreaterThanFilter(filter);
	        } else {
	            LOGGER.error("The Filter " + filter + " contains a condition that is not supported");
	        }
	        return users;
	    }

	    /**
	     * A simple (and nonperformant) example of how to handle the GT filter utilized in the incremental imports flow
	     * <p/>
	     * Since that is the only Okta use-case which leverages this filter, the example will make the assumption that
	     * the filter attribute is meta.lastModified and that the attribute value represents the date of the most recent
	     * Okta import done from this app instance.
	     *
	     * @param filter
	     * @return
	     */
	    private List<SCIMUser> getUsersByGreaterThanFilter(SCIMFilter filter) {
	    	updateCache();
	        String fieldName = filter.getFilterAttribute().toString();
	        String value = filter.getFilterValue();

	        List<SCIMUser> users = new ArrayList<SCIMUser>();

	        // Iterate over users, collect those that meet filter condition
	        for (Map.Entry<String, SCIMUser> entry : userMap.entrySet()) {
	            SCIMUser user = entry.getValue();
	            // Since this is incremental import, we are only concerned with this fieldName in this example
	            if (fieldName.equals("meta.lastModified")) {
	                // Dates will be sent from Okta in ISO-8601 format
	                SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	                Date filterDate;
	                try {
	                    filterDate = isoFormatter.parse(value);
	                } catch (ParseException pe) {
	                    //Note that the Error Code "o5327899" is arbitrary - You can use any code that you want to.
	                    throw new OnPremUserManagementException("o5327899", "Cannot parse the filter string to ISO date");
	                }
	                // Note that in this example, users without a lastModified date are excluded from the download.
	                // This means that they will not be picked up by an incremental import, only by a full import.
	                // This behavior may be changed in your implementation.
	                if (user.getLastModified() != null) {
	                    if (user.getLastModified().compareTo(filterDate) > 0) {
	                        users.add(user);
	                    }
	                }
	            }
	        }
	        return users;
	    }

	    /**
	     * This is an example for how to deal with an OR filter. An OR filter consists of multiple sub equality filters.
	     *
	     * @param filter the OR filter with a set of sub filters expressions
	     * @return list of users that match any of the filters
	     */
	    private List<SCIMUser> getUsersByOrFilter(SCIMFilter filter) {
	    	updateCache();
	        //An OR filter would contain a list of filter expression. Each expression is a SCIMFilter by itself.
	        //Ex : "email eq "abc@def.com" OR email eq "def@abc.com""
	        List<SCIMFilter> subFilters = filter.getFilterExpressions();
	        LOGGER.info("OR Filter : " + subFilters);
	        List<SCIMUser> users = new ArrayList<SCIMUser>();
	        //Loop through the sub filters to evaluate each of them.
	        //Ex : "email eq "abc@def.com""
	        for (SCIMFilter subFilter : subFilters) {
	            //Name of the sub filter (email)
	            String fieldName = subFilter.getFilterAttribute().getAttributeName();
	            //Value (abc@def.com)
	            String value = subFilter.getFilterValue();
	            //For all the users, check if any of them have this email
	            for (Map.Entry<String, SCIMUser> entry : userMap.entrySet()) {
	                boolean userFound = false;
	                SCIMUser user = entry.getValue();
	                //In this example, since we assume that the field name configured with Okta is "email", checking if we got the field name as "email" here
	                if (fieldName.equalsIgnoreCase("email")) {
	                    //Get the user's emails and check if the value is the same as in the filter
	                    Collection<Email> emails = user.getEmails();
	                    if (emails != null) {
	                        for (Email email : emails) {
	                            if (email.getValue().equalsIgnoreCase(value)) {
	                                userFound = true;
	                                break;
	                            }
	                        }
	                    }
	                }
	                if (userFound) {
	                    users.add(user);
	                }
	            }
	        }
	        return users;
	    }

	    /**
	     * This is an example of how to deal with an equality filter.<p>
	     * If you choose a custom field/complex field (name.familyName) or any other singular field (userName/externalId), you should get an equality filter here.
	     *
	     * @param filter the EQUALS filter
	     * @return list of users that match the filter
	     */
	    private List<SCIMUser> getUsersByEqualityFilter(SCIMFilter filter) {
	    	updateCache();
	        String fieldName = filter.getFilterAttribute().getAttributeName();
	        String value = filter.getFilterValue();
	        LOGGER.info("Equality Filter : Field Name [ " + fieldName + " ]. Value [ " + value + " ]");
	        List<SCIMUser> users = new ArrayList<SCIMUser>();

	        //A basic example of how to return users that match the criteria
	        for (Map.Entry<String, SCIMUser> entry : userMap.entrySet()) {
	            SCIMUser user = entry.getValue();
	            boolean userFound = false;
	            //Ex : "userName eq "someUserName""
	            if (fieldName.equalsIgnoreCase("userName")) {
	                String userName = user.getUserName();
	                if (userName != null && userName.equals(value)) {
	                    userFound = true;
	                }
	            } else if (fieldName.equalsIgnoreCase("id")) {
	                //"id eq "someId""
	                String id = user.getId();
	                if (id != null && id.equals(value)) {
	                    userFound = true;
	                }
	            } else if (fieldName.equalsIgnoreCase("name")) {
	                String subFieldName = filter.getFilterAttribute().getSubAttributeName();
	                Name name = user.getName();
	                if (name == null || subFieldName == null) {
	                    continue;
	                }
	                if (subFieldName.equalsIgnoreCase("familyName")) {
	                    //"name.familyName eq "someFamilyName""
	                    String familyName = name.getLastName();
	                    if (familyName != null && familyName.equals(value)) {
	                        userFound = true;
	                    }
	                } else if (subFieldName.equalsIgnoreCase("givenName")) {
	                    //"name.givenName eq "someGivenName""
	                    String givenName = name.getFirstName();
	                    if (givenName != null && givenName.equals(value)) {
	                        userFound = true;
	                    }
	                }
	            } 

	            if (userFound) {
	                users.add(user);
	            }
	        }
	        return users;
	    }

	    /**
	     * Get a particular user.
	     * <p>
	     * This method is invoked when a GET is made to /Users/{id}
	     *
	     * @param id the Id of the SCIM User
	     * @return the user corresponding to the id
	     * @throws com.okta.scim.server.exception.OnPremUserManagementException
	     *
	     */
	    @Override
	    public SCIMUser getUser(String id) throws OnPremUserManagementException, EntityNotFoundException {
	    	updateCache();
	        SCIMUser user = userMap.get(id);
	        if (user != null) {
	            return user;
	        } else {
	            //If you do not find a user/group by the ID, you can throw this exception.
	            throw new EntityNotFoundException();
	        }
	    }

	    @Override
	    public SCIMGroup createGroup(SCIMGroup group) throws OnPremUserManagementException, DuplicateGroupException {
	        return null;
	    }

	    
	    @Override
	    public SCIMGroup updateGroup(String id, SCIMGroup group) throws OnPremUserManagementException {
	    	return null;
	    }

	  
	    @Override
	    public SCIMGroupQueryResponse getGroups(PaginationProperties pageProperties) throws OnPremUserManagementException {
	    	SCIMGroupQueryResponse response = new SCIMGroupQueryResponse();
	       
	        if (groupMap == null) {
	            //Note that the Error Code "o34567" is arbitrary - You can use any code that you want to.
	            throw new OnPremUserManagementException("o34567", "Cannot get the groups. The groupMap is null");
	        }

	        int totalResults = groupMap.size();
	        if (pageProperties != null) {
	            //Set the start index to the response.
	            response.setStartIndex(pageProperties.getStartIndex());
	        }
	        //In this example we are setting the total results to the number of results in this page. If there are more
	        //results than the number the client asked for (pageProperties.getCount()), then you need to set the total results correctly
	        response.setTotalResults(totalResults);
	        List<SCIMGroup> groups = new ArrayList<SCIMGroup>();
	        //Set the actual results
	        response.setScimGroups(groups);
	        return response;
	    }

	    @Override
	    public SCIMGroup getGroup(String id) throws OnPremUserManagementException {
	    	return null;
	    }

	    @Override
	    public void deleteGroup(String id) throws OnPremUserManagementException, EntityNotFoundException {
	    }

	    /**
	     * Get all the Okta User Management capabilities that this SCIM Service has implemented.
	     * <p>
	     * This method is invoked when a GET is made to /ServiceProviderConfigs. It is called only when you are testing
	     * or modifying your connector configuration from the Okta Application instance UM UI. If you change the return values
	     * at a later time please re-test and re-save your connector settings to have your new return values respected.
	     * <p>
	     * These User Management capabilities help customize the UI features available to your app instance and tells Okta
	     * all the possible commands that can be sent to your connector.
	     *
	     * @return all the implemented User Management capabilities.
	     */
	    @Override
	    public UserManagementCapabilities[] getImplementedUserManagementCapabilities() {
	    	return new UserManagementCapabilities[] {UserManagementCapabilities.IMPORT_NEW_USERS, UserManagementCapabilities.IMPORT_PROFILE_UPDATES,
	    			UserManagementCapabilities.OPP_SCIM_INCREMENTAL_IMPORTS};
	        //return UserManagementCapabilities.values();
	    }

	    /**
	     * Update the cache based on the data stored in the files
	     */
	    private synchronized void updateCache() {

	        userMap.clear();
	        groupMap.clear();

	        try {
	            ConnectorUtil.readUsersFromSource(userMap);
	        } catch (Exception e) {
	            throw new OnPremUserManagementException("Exception in building the user cache from DB", e);
	        }

	        /*try {
	           ConnectorUtil.readGroupsFromSource(groupMap);
	        } catch (Exception e) {
	            throw new OnPremUserManagementException("Exception in building the group cache from DB", e);
	        }*/
	    }
	}

