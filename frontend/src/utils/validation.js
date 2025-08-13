/**
 * Validation utility module that translates COBOL validation routines from mainframe copybooks
 * Provides date validation, phone area code validation, US state code validation,
 * and state-ZIP code combination validation for use in React form components
 *
 * Translated from COBOL copybooks:
 * - CSUTLDPY.cpy: Date validation procedures
 * - CSUTLDWY.cpy: Validation data structures
 * - CSLKPCDY.cpy: Lookup code validation
 */

import { number } from 'yup';

// North American Phone Area Codes from NANPA (North American Numbering Plan Administrator)
// Translated from CSLKPCDY.cpy VALID-PHONE-AREA-CODE values
export const VALID_PHONE_AREA_CODES = [
  '201', '202', '203', '204', '205', '206', '207', '208', '209', '210',
  '212', '213', '214', '215', '216', '217', '218', '219', '220', '223',
  '224', '225', '226', '228', '229', '231', '234', '236', '239', '240',
  '242', '246', '248', '249', '250', '251', '252', '253', '254', '256',
  '260', '262', '264', '267', '268', '269', '270', '272', '276', '279',
  '281', '284', '289', '301', '302', '303', '304', '305', '306', '307',
  '308', '309', '310', '312', '313', '314', '315', '316', '317', '318',
  '319', '320', '321', '323', '325', '326', '330', '331', '332', '334',
  '336', '337', '339', '340', '341', '343', '345', '346', '347', '351',
  '352', '360', '361', '364', '365', '367', '368', '380', '385', '386',
  '401', '402', '403', '404', '405', '406', '407', '408', '409', '410',
  '412', '413', '414', '415', '416', '417', '418', '419', '423', '424',
  '425', '430', '431', '432', '434', '435', '437', '438', '440', '441',
  '442', '443', '445', '447', '448', '450', '458', '463', '464', '469',
  '470', '473', '474', '475', '478', '479', '480', '484', '501', '502',
  '503', '504', '505', '506', '507', '508', '509', '510', '512', '513',
  '514', '515', '516', '517', '518', '519', '520', '530', '531', '534',
  '539', '540', '541', '548', '551', '559', '561', '562', '563', '564',
  '567', '570', '571', '572', '573', '574', '575', '579', '580', '581',
  '582', '585', '586', '587', '601', '602', '603', '604', '605', '606',
  '607', '608', '609', '610', '612', '613', '614', '615', '616', '617',
  '618', '619', '620', '623', '626', '628', '629', '630', '631', '636',
  '639', '640', '641', '646', '647', '649', '650', '651', '656', '657',
  '658', '659', '660', '661', '662', '664', '667', '669', '670', '671',
  '672', '678', '680', '681', '682', '683', '684', '689', '701', '702',
  '703', '704', '705', '706', '707', '708', '709', '712', '713', '714',
  '715', '716', '717', '718', '719', '720', '721', '724', '725', '726',
  '727', '731', '732', '734', '737', '740', '742', '743', '747', '753',
  '754', '757', '758', '760', '762', '763', '765', '767', '769', '770',
  '771', '772', '773', '774', '775', '778', '779', '780', '781', '782',
  '784', '785', '786', '787', '800', '801', '802', '803', '804', '805', '806',
  '807', '808', '809', '810', '812', '813', '814', '815', '816', '817',
  '818', '819', '820', '825', '826', '828', '829', '830', '831', '832', '833',
  '838', '839', '840', '843', '844', '845', '847', '848', '849', '850', '854',
  '855', '856', '857', '858', '859', '860', '862', '863', '864', '865', '866', '867',
  '868', '869', '870', '872', '873', '876', '877', '878', '888', '901', '902', '903',
  '904', '905', '906', '907', '908', '909', '910', '912', '913', '914',
  '915', '916', '917', '918', '919', '920', '925', '928', '929', '930',
  '931', '934', '936', '937', '938', '939', '940', '941', '943', '945',
  '947', '948', '949', '951', '952', '954', '956', '959', '970', '971',
  '972', '973', '978', '979', '980', '983', '984', '985', '986', '989',
];

// US State Codes including territories
// Translated from CSLKPCDY.cpy VALID-US-STATE-CODE values
export const VALID_US_STATE_CODES = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY',
  'DC', 'AS', 'GU', 'MP', 'PR', 'VI',
];

// Valid State + ZIP code first 2 digits combinations
// Translated from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO values
export const VALID_STATE_ZIP_COMBINATIONS = [
  'AA34', 'AE90', 'AE91', 'AE92', 'AE93', 'AE94', 'AE95', 'AE96', 'AE97', 'AE98',
  'AK99', 'AL35', 'AL36', 'AP96', 'AR71', 'AR72', 'AS96', 'AZ85', 'AZ86',
  'CA90', 'CA91', 'CA92', 'CA93', 'CA94', 'CA95', 'CA96', 'CO80', 'CO81',
  'CT60', 'CT61', 'CT62', 'CT63', 'CT64', 'CT65', 'CT66', 'CT67', 'CT68', 'CT69',
  'DC20', 'DC56', 'DC88', 'DE19', 'FL32', 'FL33', 'FL34', 'FM96', 'GA30', 'GA31', 'GA39',
  'GU96', 'HI96', 'IA50', 'IA51', 'IA52', 'ID83', 'IL60', 'IL61', 'IL62',
  'IN46', 'IN47', 'KS66', 'KS67', 'KY40', 'KY41', 'KY42', 'LA70', 'LA71',
  'MA10', 'MA11', 'MA12', 'MA13', 'MA14', 'MA15', 'MA16', 'MA17', 'MA18', 'MA19',
  'MA20', 'MA21', 'MA22', 'MA23', 'MA24', 'MA25', 'MA26', 'MA27', 'MA55',
  'MD20', 'MD21', 'ME39', 'ME40', 'ME41', 'ME42', 'ME43', 'ME44', 'ME45', 'ME46', 'ME47', 'ME48', 'ME49',
  'MH96', 'MI48', 'MI49', 'MN55', 'MN56', 'MO63', 'MO64', 'MO65', 'MO72',
  'MP96', 'MS38', 'MS39', 'MT59', 'NC27', 'NC28', 'ND58', 'NE68', 'NE69',
  'NH30', 'NH31', 'NH32', 'NH33', 'NH34', 'NH35', 'NH36', 'NH37', 'NH38',
  'NJ70', 'NJ71', 'NJ72', 'NJ73', 'NJ74', 'NJ75', 'NJ76', 'NJ77', 'NJ78', 'NJ79',
  'NJ80', 'NJ81', 'NJ82', 'NJ83', 'NJ84', 'NJ85', 'NJ86', 'NJ87', 'NJ88', 'NJ89',
  'NM87', 'NM88', 'NV88', 'NV89', 'NY50', 'NY54', 'NY63', 'NY10', 'NY11', 'NY12', 'NY13', 'NY14',
  'OH43', 'OH44', 'OH45', 'OK73', 'OK74', 'OR97', 'PA15', 'PA16', 'PA17', 'PA18', 'PA19',
  'PR60', 'PR61', 'PR62', 'PR63', 'PR64', 'PR65', 'PR66', 'PR67', 'PR68', 'PR69',
  'PR70', 'PR71', 'PR72', 'PR73', 'PR74', 'PR75', 'PR76', 'PR77', 'PR78', 'PR79',
  'PR90', 'PR91', 'PR92', 'PR93', 'PR94', 'PR95', 'PR96', 'PR97', 'PR98',
  'PW96', 'RI28', 'RI29', 'SC29', 'SD57', 'TN37', 'TN38',
  'TX73', 'TX75', 'TX76', 'TX77', 'TX78', 'TX79', 'TX88', 'UT84',
  'VA20', 'VA22', 'VA23', 'VA24', 'VI80', 'VI82', 'VI83', 'VI84', 'VI85',
  'VT50', 'VT51', 'VT52', 'VT53', 'VT54', 'VT56', 'VT57', 'VT58', 'VT59',
  'WA98', 'WA99', 'WI53', 'WI54', 'WV24', 'WV25', 'WV26', 'WY82', 'WY83',
];

/**
 * Helper function to check if a year is a leap year
 * @param {number} fullYear - Full year (e.g., 2024)
 * @param {number} year - Last two digits of year (e.g., 24)
 * @returns {boolean} True if leap year
 */
const isLeapYear = (fullYear, year) => {
  // Leap year calculation from CSUTLDPY.cpy
  if (year === 0) {
    // Century year - divisible by 400
    return (fullYear % 400 === 0);
  } else {
    // Non-century year - divisible by 4
    return (fullYear % 4 === 0);
  }
};

/**
 * Helper function to validate month-day combination
 * @param {number} month - Month (1-12)
 * @param {number} day - Day (1-31)
 * @param {number} fullYear - Full year for leap year calculation
 * @param {number} year - Last two digits of year
 * @returns {object} Validation result
 */
const validateMonthDay = (month, day, fullYear, year) => {
  // Check month-specific day limits
  // 31-day months: 1, 3, 5, 7, 8, 10, 12 (from CSUTLDWY.cpy)
  const thirtyOneDayMonths = [1, 3, 5, 7, 8, 10, 12];

  if (!thirtyOneDayMonths.includes(month) && day === 31) {
    return { isValid: false, errorMessage: 'Cannot have 31 days in this month.' };
  }

  // February specific validation
  if (month === 2) {
    if (day === 30) {
      return { isValid: false, errorMessage: 'Cannot have 30 days in this month.' };
    }

    // Leap year logic for February 29th
    if (day === 29 && !isLeapYear(fullYear, year)) {
      return { isValid: false, errorMessage: 'Not a leap year. Cannot have 29 days in this month.' };
    }
  }

  return { isValid: true, errorMessage: '' };
};

/**
 * Validates date in CCYYMMDD format matching COBOL EDIT-DATE-CCYYMMDD logic
 * Translated from CSUTLDPY.cpy and CSUTLDWY.cpy
 *
 * @param {string} dateString - Date string in CCYYMMDD format
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateDate = (dateString) => {
  const result = { isValid: true, errorMessage: '' };

  // Check if date is provided and not empty
  if (!dateString || dateString.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'Date must be supplied.';
    return result;
  }

  // Check length - must be exactly 8 characters for CCYYMMDD
  if (dateString.length !== 8) {
    result.isValid = false;
    result.errorMessage = 'Date must be 8 digits in CCYYMMDD format.';
    return result;
  }

  // Check if all characters are numeric
  if (!/^\d{8}$/.test(dateString)) {
    result.isValid = false;
    result.errorMessage = 'Date must be 8 digit number.';
    return result;
  }

  // Extract date components
  const century = parseInt(dateString.substring(0, 2), 10);
  const year = parseInt(dateString.substring(2, 4), 10);
  const month = parseInt(dateString.substring(4, 6), 10);
  const day = parseInt(dateString.substring(6, 8), 10);
  const fullYear = century * 100 + year;

  // Validate century (19 or 20 only, as per COBOL logic)
  if (century !== 19 && century !== 20) {
    result.isValid = false;
    result.errorMessage = 'Century is not valid.';
    return result;
  }

  // Validate month (1-12)
  if (month < 1 || month > 12) {
    result.isValid = false;
    result.errorMessage = 'Month must be a number between 1 and 12.';
    return result;
  }

  // Validate day (1-31)
  if (day < 1 || day > 31) {
    result.isValid = false;
    result.errorMessage = 'Day must be a number between 1 and 31.';
    return result;
  }

  // Validate month-day combination
  const monthDayValidation = validateMonthDay(month, day, fullYear, year);
  if (!monthDayValidation.isValid) {
    result.isValid = false;
    result.errorMessage = monthDayValidation.errorMessage;
    return result;
  }

  // Date of birth reasonableness check - cannot be in the future
  const inputDate = new Date(fullYear, month - 1, day);
  const currentDate = new Date();
  currentDate.setHours(0, 0, 0, 0); // Reset time for date-only comparison

  if (inputDate > currentDate) {
    result.isValid = false;
    result.errorMessage = 'Date cannot be in the future.';
    return result;
  }

  return result;
};

/**
 * Validates Social Security Number format
 * Standard SSN format: XXX-XX-XXXX or XXXXXXXXX
 *
 * @param {string} ssn - Social Security Number
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateSSN = (ssn) => {
  const result = { isValid: true, errorMessage: '' };

  if (!ssn || ssn.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'SSN must be supplied.';
    return result;
  }

  // Remove dashes for validation
  const cleanSSN = ssn.replace(/-/g, '');

  // Check if it's exactly 9 digits
  if (!/^\d{9}$/.test(cleanSSN)) {
    result.isValid = false;
    result.errorMessage = 'SSN must be 9 digits.';
    return result;
  }

  // Check for invalid SSN patterns
  const area = cleanSSN.substring(0, 3);
  const group = cleanSSN.substring(3, 5);
  const serial = cleanSSN.substring(5, 9);

  // Area number cannot be 000, 666, or 900-999
  if (area === '000' || area === '666' || (parseInt(area, 10) >= 900 && parseInt(area, 10) <= 999)) {
    result.isValid = false;
    result.errorMessage = 'Invalid SSN area number.';
    return result;
  }

  // Group number cannot be 00
  if (group === '00') {
    result.isValid = false;
    result.errorMessage = 'Invalid SSN group number.';
    return result;
  }

  // Serial number cannot be 0000
  if (serial === '0000') {
    result.isValid = false;
    result.errorMessage = 'Invalid SSN serial number.';
    return result;
  }

  return result;
};

/**
 * Validates phone area code against North American Numbering Plan
 * Translated from CSLKPCDY.cpy VALID-PHONE-AREA-CODE logic
 *
 * @param {string} areaCode - 3-digit area code
 * @returns {object} Validation result with isValid flag and error message
 */
export const validatePhoneAreaCode = (areaCode) => {
  const result = { isValid: true, errorMessage: '' };

  if (!areaCode || areaCode.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'Area code must be supplied.';
    return result;
  }

  // Check if it's exactly 3 digits
  if (!/^\d{3}$/.test(areaCode)) {
    result.isValid = false;
    result.errorMessage = 'Area code must be 3 digits.';
    return result;
  }

  // Check against valid area codes from NANPA
  if (!VALID_PHONE_AREA_CODES.includes(areaCode)) {
    result.isValid = false;
    result.errorMessage = 'Invalid area code.';
    return result;
  }

  return result;
};

/**
 * Validates US state code
 * Translated from CSLKPCDY.cpy VALID-US-STATE-CODE logic
 *
 * @param {string} stateCode - 2-character state code
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateStateCode = (stateCode) => {
  const result = { isValid: true, errorMessage: '' };

  if (!stateCode || stateCode.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'State code must be supplied.';
    return result;
  }

  // Convert to uppercase for validation
  const upperStateCode = stateCode.toUpperCase();

  // Check if it's exactly 2 characters
  if (upperStateCode.length !== 2) {
    result.isValid = false;
    result.errorMessage = 'State code must be 2 characters.';
    return result;
  }

  // Check against valid state codes
  if (!VALID_US_STATE_CODES.includes(upperStateCode)) {
    result.isValid = false;
    result.errorMessage = 'Invalid state code.';
    return result;
  }

  return result;
};

/**
 * Validates state and ZIP code combination
 * Translated from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO logic
 *
 * @param {string} stateCode - 2-character state code
 * @param {string} zipCode - ZIP code (uses first 2 digits)
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateStateZipCode = (stateCode, zipCode) => {
  const result = { isValid: true, errorMessage: '' };

  if (!stateCode || stateCode.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'State code must be supplied.';
    return result;
  }

  if (!zipCode || zipCode.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'ZIP code must be supplied.';
    return result;
  }

  // Convert state to uppercase
  const upperStateCode = stateCode.toUpperCase();

  // Extract first 2 digits of ZIP code
  const zipFirst2 = zipCode.substring(0, 2);

  // Validate ZIP code format
  if (!/^\d{5}/.test(zipCode)) {
    result.isValid = false;
    result.errorMessage = 'ZIP code must be at least 5 digits.';
    return result;
  }

  // Create state-ZIP combination
  const stateZipCombo = upperStateCode + zipFirst2;

  // Check against valid combinations
  if (!VALID_STATE_ZIP_COMBINATIONS.includes(stateZipCombo)) {
    result.isValid = false;
    result.errorMessage = 'Invalid state and ZIP code combination.';
    return result;
  }

  return result;
};

/**
 * Validates FICO credit score
 * FICO scores range from 300 to 850
 *
 * @param {number|string} score - FICO score
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateFICO = (score) => {
  const result = { isValid: true, errorMessage: '' };

  if (score === null || score === undefined || score === '') {
    result.isValid = false;
    result.errorMessage = 'FICO score must be supplied.';
    return result;
  }

  // Check if the input is a valid number (handles both string and number inputs)
  const scoreStr = score.toString();
  if (!/^\d+$/.test(scoreStr)) {
    result.isValid = false;
    result.errorMessage = 'FICO score must be a number.';
    return result;
  }

  const numericScore = parseInt(score, 10);

  // Check range (300-850)
  if (numericScore < 300 || numericScore > 850) {
    result.isValid = false;
    result.errorMessage = 'FICO score must be between 300 and 850.';
    return result;
  }

  return result;
};

/**
 * Validates user ID based on typical mainframe user ID rules
 * User ID should be 1-8 characters, alphanumeric, starting with a letter
 *
 * @param {string} userID - User ID
 * @returns {object} Validation result with isValid flag and error message
 */
export const validateUserID = (userID) => {
  const result = { isValid: true, errorMessage: '' };

  if (!userID || userID.trim() === '') {
    result.isValid = false;
    result.errorMessage = 'User ID must be supplied.';
    return result;
  }

  const trimmedUserID = userID.trim();

  // Check length (1-8 characters)
  if (trimmedUserID.length < 1 || trimmedUserID.length > 8) {
    result.isValid = false;
    result.errorMessage = 'User ID must be 1-8 characters.';
    return result;
  }

  // Check if it starts with a letter
  if (!/^[A-Za-z]/.test(trimmedUserID)) {
    result.isValid = false;
    result.errorMessage = 'User ID must start with a letter.';
    return result;
  }

  // Check if it contains only alphanumeric characters
  if (!/^[A-Za-z0-9]+$/.test(trimmedUserID)) {
    result.isValid = false;
    result.errorMessage = 'User ID must contain only letters and numbers.';
    return result;
  }

  return result;
};

/**
 * Creates Yup validation schema for date fields
 * Uses custom validation function that matches COBOL logic
 *
 * @param {string} fieldName - Name of the field for error messages
 * @returns {object} Yup schema for date validation
 */
export const createDateValidationSchema = (fieldName = 'Date') => {
  return {
    test: {
      name: 'date-format',
      message: `${fieldName} format is invalid`,
      test(value) {
        if (!value) {
          return false;
        }
        const validation = validateDate(value);
        if (!validation.isValid) {
          return this.createError({ message: validation.errorMessage });
        }
        return true;
      },
    },
  };
};

/**
 * Creates Yup validation schema for phone number fields
 * Validates area code using North American Numbering Plan
 *
 * @param {string} fieldName - Name of the field for error messages
 * @returns {object} Yup schema for phone validation
 */
export const createPhoneValidationSchema = (fieldName = 'Phone') => {
  return {
    test: {
      name: 'phone-area-code',
      message: `${fieldName} area code is invalid`,
      test(value) {
        if (!value) {
          return false;
        }

        // Extract area code from phone number (first 3 digits)
        const phoneDigits = value.replace(/\D/g, '');
        if (phoneDigits.length < 3) {
          return this.createError({ message: `${fieldName} must include area code` });
        }

        const areaCode = phoneDigits.substring(0, 3);
        const validation = validatePhoneAreaCode(areaCode);
        if (!validation.isValid) {
          return this.createError({ message: validation.errorMessage });
        }
        return true;
      },
    },
  };
};

/**
 * Creates Yup validation schema for state and ZIP code combination
 * Validates that state and ZIP code combination is valid per USPS standards
 *
 * @param {string} stateFieldName - Name of the state field
 * @param {string} zipFieldName - Name of the ZIP field
 * @returns {object} Yup schema for state-ZIP validation
 */
export const createStateZipValidationSchema = (stateFieldName = 'State', zipFieldName = 'ZIP Code') => {
  return {
    test: {
      name: 'state-zip-combo',
      message: `Invalid ${stateFieldName} and ${zipFieldName} combination`,
      test(value, context) {
        // Get the state value from the form context
        const stateValue = context.parent.state || context.parent.stateCode;
        const zipValue = value;

        if (!stateValue || !zipValue) {
          return false;
        }

        const validation = validateStateZipCode(stateValue, zipValue);
        if (!validation.isValid) {
          return this.createError({ message: validation.errorMessage });
        }
        return true;
      },
    },
  };
};

/**
 * Creates Yup validation schema for FICO score fields
 * Uses yup number validation with proper range constraints
 * Utilizing imported yup number function and its methods
 *
 * @returns {object} Yup number schema for FICO score validation
 */
export const createFICOValidationSchema = () => {
  return number()
    .required('FICO score is required')
    .min(300, 'FICO score must be at least 300')
    .max(850, 'FICO score must be no more than 850')
    .integer('FICO score must be a whole number')
    .positive('FICO score must be positive');
};
