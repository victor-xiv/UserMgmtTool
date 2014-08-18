
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for userIdFilterCriterion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="userIdFilterCriterion">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}searchCriterion">
 *       &lt;sequence>
 *         &lt;element name="userId" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="caseInsensitive" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "userIdFilterCriterion", propOrder = {
    "userId",
    "caseInsensitive"
})
public class UserIdFilterCriterion
    extends SearchCriterion
{

    protected List<String> userId;
    protected Boolean caseInsensitive;

    /**
     * Gets the value of the userId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the userId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUserId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getUserId() {
        if (userId == null) {
            userId = new ArrayList<String>();
        }
        return this.userId;
    }

    /**
     * Gets the value of the caseInsensitive property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Sets the value of the caseInsensitive property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCaseInsensitive(Boolean value) {
        this.caseInsensitive = value;
    }

}
