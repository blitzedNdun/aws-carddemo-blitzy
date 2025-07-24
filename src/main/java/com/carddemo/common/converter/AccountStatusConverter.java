package com.carddemo.common.converter;

import com.carddemo.common.enums.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for AccountStatus enum to single character database storage.
 * 
 * This converter ensures AccountStatus enum values are stored as single character
 * codes in the database ('Y' for ACTIVE, 'N' for INACTIVE) matching the original
 * COBOL ACCT-ACTIVE-STATUS PIC X(01) field definition.
 * 
 * Conversion Logic:
 * - ACTIVE enum -> 'Y' database value
 * - INACTIVE enum -> 'N' database value
 * - 'Y' database value -> ACTIVE enum
 * - 'N' database value -> INACTIVE enum
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    /**
     * Converts AccountStatus enum to database column value.
     * 
     * @param attribute AccountStatus enum value
     * @return Single character code for database storage
     */
    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    /**
     * Converts database column value to AccountStatus enum.
     * 
     * @param dbData Single character code from database
     * @return AccountStatus enum value
     */
    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return AccountStatus.fromCode(dbData);
    }
}