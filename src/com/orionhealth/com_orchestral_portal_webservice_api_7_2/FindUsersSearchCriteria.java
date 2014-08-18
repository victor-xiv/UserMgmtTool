
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for findUsersSearchCriteria complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="findUsersSearchCriteria">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}searchCriteria">
 *       &lt;choice>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}and"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}or"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}memberOfGroup"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userAttributeMatches"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userIdMatches"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userIdFilter"/>
 *         &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}not"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "findUsersSearchCriteria", propOrder = {
    "and",
    "or",
    "memberOfGroup",
    "userAttributeMatches",
    "userIdMatches",
    "userIdFilter",
    "not"
})
public class FindUsersSearchCriteria
    extends SearchCriteria
{

    protected AndSearchCriterions and;
    protected OrSearchCriterions or;
    protected MemberOfGroupCriterion memberOfGroup;
    protected UserAttributeCriterion userAttributeMatches;
    protected UserIdCriterion userIdMatches;
    protected UserIdFilterCriterion userIdFilter;
    protected NotSearchCriterion not;

    /**
     * Gets the value of the and property.
     * 
     * @return
     *     possible object is
     *     {@link AndSearchCriterions }
     *     
     */
    public AndSearchCriterions getAnd() {
        return and;
    }

    /**
     * Sets the value of the and property.
     * 
     * @param value
     *     allowed object is
     *     {@link AndSearchCriterions }
     *     
     */
    public void setAnd(AndSearchCriterions value) {
        this.and = value;
    }

    /**
     * Gets the value of the or property.
     * 
     * @return
     *     possible object is
     *     {@link OrSearchCriterions }
     *     
     */
    public OrSearchCriterions getOr() {
        return or;
    }

    /**
     * Sets the value of the or property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrSearchCriterions }
     *     
     */
    public void setOr(OrSearchCriterions value) {
        this.or = value;
    }

    /**
     * Gets the value of the memberOfGroup property.
     * 
     * @return
     *     possible object is
     *     {@link MemberOfGroupCriterion }
     *     
     */
    public MemberOfGroupCriterion getMemberOfGroup() {
        return memberOfGroup;
    }

    /**
     * Sets the value of the memberOfGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemberOfGroupCriterion }
     *     
     */
    public void setMemberOfGroup(MemberOfGroupCriterion value) {
        this.memberOfGroup = value;
    }

    /**
     * Gets the value of the userAttributeMatches property.
     * 
     * @return
     *     possible object is
     *     {@link UserAttributeCriterion }
     *     
     */
    public UserAttributeCriterion getUserAttributeMatches() {
        return userAttributeMatches;
    }

    /**
     * Sets the value of the userAttributeMatches property.
     * 
     * @param value
     *     allowed object is
     *     {@link UserAttributeCriterion }
     *     
     */
    public void setUserAttributeMatches(UserAttributeCriterion value) {
        this.userAttributeMatches = value;
    }

    /**
     * Gets the value of the userIdMatches property.
     * 
     * @return
     *     possible object is
     *     {@link UserIdCriterion }
     *     
     */
    public UserIdCriterion getUserIdMatches() {
        return userIdMatches;
    }

    /**
     * Sets the value of the userIdMatches property.
     * 
     * @param value
     *     allowed object is
     *     {@link UserIdCriterion }
     *     
     */
    public void setUserIdMatches(UserIdCriterion value) {
        this.userIdMatches = value;
    }

    /**
     * Gets the value of the userIdFilter property.
     * 
     * @return
     *     possible object is
     *     {@link UserIdFilterCriterion }
     *     
     */
    public UserIdFilterCriterion getUserIdFilter() {
        return userIdFilter;
    }

    /**
     * Sets the value of the userIdFilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link UserIdFilterCriterion }
     *     
     */
    public void setUserIdFilter(UserIdFilterCriterion value) {
        this.userIdFilter = value;
    }

    /**
     * Gets the value of the not property.
     * 
     * @return
     *     possible object is
     *     {@link NotSearchCriterion }
     *     
     */
    public NotSearchCriterion getNot() {
        return not;
    }

    /**
     * Sets the value of the not property.
     * 
     * @param value
     *     allowed object is
     *     {@link NotSearchCriterion }
     *     
     */
    public void setNot(NotSearchCriterion value) {
        this.not = value;
    }

}
