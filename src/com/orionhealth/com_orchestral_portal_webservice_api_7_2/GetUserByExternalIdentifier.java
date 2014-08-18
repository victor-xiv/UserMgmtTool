
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getUserByExternalIdentifier complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getUserByExternalIdentifier">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="externalIdentifier" type="{http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/}externalIdentifierDto" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getUserByExternalIdentifier", propOrder = {
    "externalIdentifier"
})
public class GetUserByExternalIdentifier {

    protected ExternalIdentifierDto externalIdentifier;

    /**
     * Gets the value of the externalIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link ExternalIdentifierDto }
     *     
     */
    public ExternalIdentifierDto getExternalIdentifier() {
        return externalIdentifier;
    }

    /**
     * Sets the value of the externalIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExternalIdentifierDto }
     *     
     */
    public void setExternalIdentifier(ExternalIdentifierDto value) {
        this.externalIdentifier = value;
    }

}
