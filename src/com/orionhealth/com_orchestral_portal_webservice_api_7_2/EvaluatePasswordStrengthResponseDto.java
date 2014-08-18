
package com.orionhealth.com_orchestral_portal_webservice_api_7_2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for evaluatePasswordStrengthResponseDto complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="evaluatePasswordStrengthResponseDto">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="passwordStrong" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="weaknesses" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="weakness" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "evaluatePasswordStrengthResponseDto", propOrder = {
    "passwordStrong",
    "weaknesses"
})
public class EvaluatePasswordStrengthResponseDto {

    protected boolean passwordStrong;
    protected EvaluatePasswordStrengthResponseDto.Weaknesses weaknesses;

    /**
     * Gets the value of the passwordStrong property.
     * 
     */
    public boolean isPasswordStrong() {
        return passwordStrong;
    }

    /**
     * Sets the value of the passwordStrong property.
     * 
     */
    public void setPasswordStrong(boolean value) {
        this.passwordStrong = value;
    }

    /**
     * Gets the value of the weaknesses property.
     * 
     * @return
     *     possible object is
     *     {@link EvaluatePasswordStrengthResponseDto.Weaknesses }
     *     
     */
    public EvaluatePasswordStrengthResponseDto.Weaknesses getWeaknesses() {
        return weaknesses;
    }

    /**
     * Sets the value of the weaknesses property.
     * 
     * @param value
     *     allowed object is
     *     {@link EvaluatePasswordStrengthResponseDto.Weaknesses }
     *     
     */
    public void setWeaknesses(EvaluatePasswordStrengthResponseDto.Weaknesses value) {
        this.weaknesses = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="weakness" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "weakness"
    })
    public static class Weaknesses {

        protected List<String> weakness;

        /**
         * Gets the value of the weakness property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the weakness property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getWeakness().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getWeakness() {
            if (weakness == null) {
                weakness = new ArrayList<String>();
            }
            return this.weakness;
        }

    }

}
