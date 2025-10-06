package org.example.domain;

public enum CompanyType {

    PrivateCompany,
    StateOwnedCompany,
    PublicInstitution;

    private final String value;

    CompanyType() {
        this.value = this.name();
    }

    @Override
    public String toString() {
        return "CompanyType{" +
                "value='" + value + '\'' +
                '}';
    }
}
