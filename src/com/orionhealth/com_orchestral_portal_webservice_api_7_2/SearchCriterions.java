
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for searchCriterions complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="searchCriterions">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}searchCriterion">
 *       &lt;sequence>
 *         &lt;choice maxOccurs="unbounded" minOccurs="0">
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}and"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}or"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}memberOfGroup"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userAttributeMatches"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userIdMatches"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}userIdFilter"/>
 *           &lt;element ref="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}not"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "searchCriterions", propOrder = {
    "andOrOrOrMemberOfGroup"
})
@XmlSeeAlso({
    OrSearchCriterions.class,
    AndSearchCriterions.class
})
public abstract class SearchCriterions
    extends SearchCriterion
{

    @XmlElements({
        @XmlElement(name = "and", type = AndSearchCriterions.class),
        @XmlElement(name = "or", type = OrSearchCriterions.class),
        @XmlElement(name = "memberOfGroup", type = MemberOfGroupCriterion.class),
        @XmlElement(name = "userAttributeMatches", type = UserAttributeCriterion.class),
        @XmlElement(name = "userIdMatches", type = UserIdCriterion.class),
        @XmlElement(name = "userIdFilter", type = UserIdFilterCriterion.class),
        @XmlElement(name = "not", type = NotSearchCriterion.class)
    })
    protected List<SearchCriterion> andOrOrOrMemberOfGroup;

    /**
     * Gets the value of the andOrOrOrMemberOfGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the andOrOrOrMemberOfGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAndOrOrOrMemberOfGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AndSearchCriterions }
     * {@link OrSearchCriterions }
     * {@link MemberOfGroupCriterion }
     * {@link UserAttributeCriterion }
     * {@link UserIdCriterion }
     * {@link UserIdFilterCriterion }
     * {@link NotSearchCriterion }
     * 
     * 
     */
    public List<SearchCriterion> getAndOrOrOrMemberOfGroup() {
        if (andOrOrOrMemberOfGroup == null) {
            andOrOrOrMemberOfGroup = new ArrayList<SearchCriterion>();
        }
        return this.andOrOrOrMemberOfGroup;
    }

}
