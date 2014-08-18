
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for renameUser complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="renameUser">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="oldUserId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="newUserId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "renameUser", propOrder = {
    "oldUserId",
    "newUserId"
})
public class RenameUser {

    protected String oldUserId;
    protected String newUserId;

    /**
     * Gets the value of the oldUserId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOldUserId() {
        return oldUserId;
    }

    /**
     * Sets the value of the oldUserId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOldUserId(String value) {
        this.oldUserId = value;
    }

    /**
     * Gets the value of the newUserId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewUserId() {
        return newUserId;
    }

    /**
     * Sets the value of the newUserId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewUserId(String value) {
        this.newUserId = value;
    }

}
