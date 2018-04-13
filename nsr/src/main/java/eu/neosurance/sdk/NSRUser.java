package eu.neosurance.sdk;


import org.json.JSONObject;

import java.util.Date;

public class NSRUser {
    private String  code;
    private String  email;
    private String  firstname;
    private String  lastname;
    private String  mobile;
    private String  fiscalCode;
    private String  gender;
    private Date birthday;
    private String  address;
    private String  zipCode;
    private String  city;
    private String  stateProvince;
    private String  country;
    private JSONObject extra;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public void setFiscalCode(String fiscalCode) {
        this.fiscalCode = fiscalCode;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public JSONObject getExtra() {
        return extra;
    }

    public void setExtra(JSONObject extra) {
        this.extra = extra;
    }

    public JSONObject toJsonObject() throws Exception {
        JSONObject jsonObject = new JSONObject();
        if (code != null) {
            jsonObject.put("code", code);
        }
        if (email != null) {
            jsonObject.put("email", email);
        }
        if (firstname != null) {
            jsonObject.put("firstname", firstname);
        }
        if (lastname != null) {
            jsonObject.put("lastname", lastname);
        }
        if (mobile != null) {
            jsonObject.put("mobile", mobile);
        }
        if (fiscalCode != null) {
            jsonObject.put("fiscalCode", fiscalCode);
        }
        if (gender != null) {
            jsonObject.put("gender", gender);
        }
        if (birthday != null) {
            jsonObject.put("birthday", birthday);
        }
        if (address != null) {
            jsonObject.put("address", address);
        }
        if (zipCode != null) {
            jsonObject.put("zipCode", zipCode);
        }
        if (city != null) {
            jsonObject.put("city", city);
        }
        if (stateProvince != null) {
            jsonObject.put("stateProvince", stateProvince);
        }
        if (country != null) {
            jsonObject.put("country", country);
        }
        if (extra != null) {
            jsonObject.put("extra", extra);
        }
        return jsonObject;
    }
}
