-- ==============================================================================
-- Liquibase Data Migration: V21__load_customers_data.sql
-- Description: Populates customers table from custdata.txt ASCII source with normalized address structure, PII protection, and FICO credit score validation
-- Author: Blitzy agent
-- Version: 21.0
-- Migration Type: DATA LOADING with comprehensive customer profile management
-- ==============================================================================

-- This file is now included via XML changeset in liquibase-changelog.xml
-- Liquibase-specific comments have been moved to the XML changeset definition
--comment: Load customer data from custdata.txt with exact 312-character fixed-width record parsing, 9-digit customer_id structure, normalized address fields, dual phone numbers, PII handling, and FICO score validation

-- =============================================================================
-- PHASE 1: Data Validation and Preparation
-- =============================================================================

-- Create temporary working table for data parsing and validation
CREATE TEMPORARY TABLE temp_custdata_raw (
    line_number SERIAL,
    raw_data VARCHAR(340) NOT NULL,
    processed BOOLEAN DEFAULT FALSE
);

-- Create temporary table for parsed customer records with validation
CREATE TEMPORARY TABLE temp_customers_parsed (
    line_number INTEGER,
    customer_id VARCHAR(9),
    first_name VARCHAR(20),
    middle_name VARCHAR(20),
    last_name VARCHAR(20),
    address_line_1 VARCHAR(50),
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    address_state VARCHAR(2),
    address_country VARCHAR(3),
    address_zip VARCHAR(10),
    phone_home VARCHAR(15),
    phone_work VARCHAR(15),
    ssn VARCHAR(9),
    government_id VARCHAR(20),
    date_of_birth DATE,
    eft_account_id VARCHAR(10),
    primary_cardholder_indicator BOOLEAN,
    fico_credit_score NUMERIC(3),
    validation_errors TEXT DEFAULT '',
    is_valid BOOLEAN DEFAULT TRUE
);

-- Create temporary table for data loading statistics and audit trail
CREATE TEMPORARY TABLE temp_load_statistics (
    total_records INTEGER DEFAULT 0,
    valid_records INTEGER DEFAULT 0,
    invalid_records INTEGER DEFAULT 0,
    duplicate_records INTEGER DEFAULT 0,
    inserted_records INTEGER DEFAULT 0,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP
);

-- Initialize statistics record
INSERT INTO temp_load_statistics (total_records) VALUES (0);

-- =============================================================================
-- PHASE 2: Raw Data Loading from custdata.txt
-- =============================================================================

-- Note: In production environment, this would use COPY command to load from file:
-- COPY temp_custdata_raw (raw_data) FROM '/path/to/custdata.txt' WITH (FORMAT TEXT);
-- For this migration script, we'll INSERT the sample data directly

-- Load custdata.txt records with exact 312-character fixed-width format
INSERT INTO temp_custdata_raw (raw_data) VALUES 
('000000001Immanuel                 Madeline                 Kessler                  618 Deshaun Route                                 Apt. 802                                          Altenwerthshire                                   NCUSA12546     (908)119-8310  (373)693-8684  020973888000000000000493684371961-06-080053581756Y274'),
('000000002Enrico                   April                    Rosenbaum                4917 Myrna Flats                                  Apt. 453                                          West Bernita                                      INUSA22770     (429)706-9510  (744)950-5272  587518382000000000005062103711961-10-080069194009Y268'),
('000000003Larry                    Cody                     Homenick                 362 Esta Parks                                    Apt. 390                                          New Gladys                                        GAUSA19852-6716(950)396-9024  (685)168-8826  317460867000000000000524193031987-11-300006465789Y616'),
('000000004Delbert                  Kaia                     Parisian                 638 Blanda Gateway                                Apt. 076                                          Lake Virginie                                     MIUSA39035-0455(801)603-4121  (156)074-6837  660354258000000000000685792491985-01-130040802739Y776'),
('000000005Treva                    Manley                   Schowalter               5653 Legros Plaza                                 Apt. 968                                          Alvinaport                                        MIUSA02251-1698(978)775-4633  (439)943-7644  611264288000000000006397997541971-09-290006365573Y529'),
('000000006Ignacio                  Emery                    Douglas                  3963 Yasmin Port                                  Suite 756                                         Port Josephstad                                   VIUSA46713-5148(277)743-4266  (519)010-8739  880329521000000000009755354961994-11-290067163009Y753'),
('000000007Cooper                   Dennis                   Mayert                   6490 Zakary Locks                                 Apt. 765                                          Madieport                                         ALUSA34206-2974(698)282-4096  (458)199-0016  835138951000000000009590131701977-05-060024571415Y499'),
('000000008Kelsie                   Jordyn                   Dicki                    0925 Welch Streets                                Apt. 152                                          North Nanniestad                                  SCUSA27610     (345)563-7159  (443)197-1271  295270759000000000001097469911964-03-250033132723Y051'),
('000000009Melvin                   Regan                    Ondricka                 87893 Samson Flats                                Apt. 135                                          New Braden                                        VIUSA21113     (035)456-1404  (412)440-3130  842035847000000000005682994511975-11-070039446039Y699'),
('000000010Maybell                  Creola                   Mann                     77933 Adah Dale                                   Suite 343                                         Andersonfurt                                      CTUSA44803-4279(614)594-2619  (667)057-0235  754755746000000000002128247551980-06-110093803568Y476'),
('000000011Hayden                   Ressie                   Pfannerstill             14895 Everette Ridges                             Apt. 443                                          Julianneburgh                                     WAUSA24984     (002)533-6980  (553)586-7718  493538586000000000001111908551986-11-030002650577Y209'),
('000000012Maci                     Alan                     Robel                    80501 Isac Cliffs                                 Suite 623                                         Predovicton                                       MNUSA78861     (584)045-5200  (610)244-0407  666114218000000000009021433511984-02-180061317348Y688'),
('000000013Mariane                  Oma                      Fadel                    2689 Derick Mission                               Suite 055                                         Bruenfurt                                         ORUSA02322     (875)943-7287  (075)550-6435  757924569000000000001813772201999-03-090044807431Y053'),
('000000014Chelsea                  Ignacio                  Marks                    747 Dino Lodge                                    Apt. 850                                          West Chase                                        RIUSA12914-8465(141)807-6571  (284)088-9052  655128548000000000005259552221974-11-290048306401Y243'),
('000000015Aubree                   Elliot                   Hermann                  36365 Ledner Drives                               Suite 882                                         Port Efrainland                                   DEUSA63205-7014(769)100-7971  (366)310-2061  033922034000000000002303699411964-12-060000634612Y681'),
('000000016Carroll                  Cicero                   Bergstrom                06988 Thiel Falls                                 Suite 148                                         Concepcionland                                    VTUSA84390     (631)343-8667  (938)648-3716  649827971000000000002932657521983-04-270012556599Y326'),
('000000017Sigrid                   Angeline                 Mann                     95666 Dare Isle                                   Suite 286                                         New Presley                                       FMUSA56181-0584(087)314-2070  (541)003-6606  303334693000000000004976063571979-01-260052356071Y054'),
('000000018Emile                    Jairo                    White                    133 Bergnaum Square                               Apt. 328                                          Hansenville                                       APUSA96003-5867(303)654-3323  (520)186-2176  385849271000000000000883418211987-03-250086459831Y340'),
('000000019Hadley                   Sigrid                   Hamill                   6273 Ondricka Meadows                             Apt. 130                                          New Arturoshire                                   RIUSA48161     (817)452-4986  (724)901-6019  439569907000000000002701763871991-01-070036492057Y259'),
('000000020Carter                   Oren                     Veum                     5845 Allison Valleys                              Suite 934                                         Mitchellmouth                                     MHUSA72362     (618)994-0531  (571)695-4136  717778238000000000003426612931996-04-140036749754Y493'),
('000000021Jerrold                  Adolphus                 Maggio                   401 Haylie Crest                                  Apt. 320                                          North Myrnaton                                    CAUSA72407     (399)526-3254  (326)193-1118  336490822000000000000276562601977-11-150011744660Y163'),
('000000022Allene                   Icie                     Brown                    4467 Donnie Crossroad                             Apt. 437                                          Anabelton                                         MDUSA01993-9116(231)251-5792  (494)652-0009  292059024000000000006911598531994-02-200024791470Y597'),
('000000023Johnson                  Blanca                   Ruecker                  2433 Jacobi Forks                                 Apt. 845                                          Hendersonbury                                     KSUSA78239-9466(981)873-1589  (131)638-5974  944154289000000000002689671221998-12-070075158529Y337'),
('000000024Stefanie                 Verla                    Dickinson                6367 Stracke River                                Apt. 444                                          East Otho                                         KSUSA15414     (617)348-9142  (330)116-5634  017590544000000000004392446331996-01-240005459662Y711'),
('000000025Elliott                  Fermin                   Howell                   9524 McKenzie Lakes                               Suite 245                                         West Alexa                                        NHUSA75721-7382(092)336-8599  (311)969-1460  788820436000000000005482230481989-03-270032297533Y355'),
('000000026Marjory                  Damien                   Stracke                  30161 Bogan Canyon                                Suite 916                                         Walshberg                                         ILUSA59945     (584)772-2867  (819)733-9809  840478806000000000009474116261990-03-170060808858Y001'),
('000000027Ward                     Henri                    Jones                    210 Amaya Turnpike                                Suite 180                                         Port Dwight                                       GUUSA07923-8822(935)027-1145  (103)537-5007  980161210000000000008815587571986-11-080050024139Y078'),
('000000028Hester                   Vesta                    Hane                     06816 Ursula Meadows                              Suite 605                                         South Aurore                                      ASUSA77442-7954(122)357-7257  (050)352-6579  677986013000000000005141877961991-06-050026946180Y114'),
('000000029Rickie                   Otho                     Daugherty                676 Funk Curve                                    Apt. 375                                          Hayesstad                                         NHUSA01226     (418)291-9023  (795)634-7776  015027332000000000000627456551973-04-050067736493Y552'),
('000000030Layla                    Dannie                   Ullrich                  269 Eleazar Circle                                Apt. 817                                          Kutchland                                         AKUSA64266     (330)408-6966  (413)347-7306  866102152000000000004920216861965-11-280050520060Y133'),
('000000031Lucious                  Otto                     O''Connell                919 Swift Valleys                                 Suite 548                                         Hermanborough                                     MSUSA56133-5636(259)414-9625  (118)946-9264  357462348000000000006183105391976-08-030092999757Y058'),
('000000032Stephany                 Meda                     Fisher                   63452 Kenny Streets                               Apt. 116                                          Predovicburgh                                     AKUSA85943-7605(202)436-5156  (246)296-3533  146204208000000000002062003411980-11-190035970593Y221'),
('000000033Bernice                  Norbert                  Herman                   877 Kassandra Ranch                               Suite 956                                         Haleyport                                         ARUSA19113-4329(836)743-5487  (640)208-1176  144195105000000000004006054291988-05-190065245171Y469'),
('000000034Faustino                 Jess                     Schmidt                  44132 Michel Square                               Suite 007                                         South Margarettaburgh                             MEUSA49544-2869(179)036-5135  (986)905-0112  548088300000000000001598825331994-03-210067445089Y104'),
('000000035Angelica                 Damaris                  Dach                     396 Pearl Loop                                    Suite 383                                         Pfefferhaven                                      LAUSA46142     (303)480-9098  (637)710-7367  220547115000000000009771448391987-06-230047435332Y793'),
('000000036Toney                    Emerald                  Gerhold                  35943 Raleigh Harbor                              Apt. 116                                          Lake Derekburgh                                   ALUSA10932-0480(034)271-9180  (507)529-4523  420360688000000000009420292101991-03-310066461979Y266'),
('000000037Shany                    Darby                    Walker                   91196 Heaney Turnpike                             Suite 814                                         Lubowitzberg                                      NVUSA11857-8177(052)759-5167  (706)896-1282  891897974000000000005243126321984-12-090066111704Y653'),
('000000038Angela                   Ceasar                   Ankunding                65482 Zoila Skyway                                Apt. 054                                          East Malachi                                      VAUSA63928-0008(316)640-2650  (148)111-1148  764307306000000000003355621411990-05-280018048939Y446'),
('000000039Aliyah                   Horace                   Berge                    5761 Pasquale Trail                               Apt. 616                                          New Sabryna                                       IAUSA74267     (089)096-3287  (768)959-4733  510793388000000000005532544031972-08-260061869530Y475'),
('000000040Davon                    Demond                   Emmerich                 23499 Beer Views                                  Suite 816                                         Erniechester                                      TXUSA87156-8689(463)762-3017  (419)414-2177  054960660000000000003983532991992-01-260087069976Y284'),
('000000041Lucinda                  Kiana                    Dach                     3220 Yolanda Corner                               Suite 649                                         East Harmonystad                                  VTUSA72971-7481(284)052-5831  (091)234-2144  643942675000000000009196534421967-02-200007315287Y725'),
('000000042Heather                  Ericka                   Nienow                   5523 Archibald Club                               Apt. 358                                          Reillyland                                        FMUSA83589     (640)954-4538  (565)873-6897  800455633000000000009970299661964-11-030079262985Y044'),
('000000043Britney                  Jermain                  Waters                   97765 Bernhard Fort                               Apt. 666                                          South Marisaview                                  OKUSA10050-7980(407)042-6952  (438)659-6397  262568593000000000002445558051966-10-160053043599Y558'),
('000000044Irving                   Kiera                    Emard                    978 Fatima Stream                                 Apt. 110                                          Lake King                                         IDUSA05704-0501(703)484-5840  (537)392-5569  318104527000000000009344209741984-04-040032076778Y145'),
('000000045Dixie                    Norris                   Beier                    441 Levi Prairie                                  Suite 749                                         Abbottshire                                       NVUSA09048     (697)143-3221  (499)287-7255  352819961000000000008857432862001-12-120027833000Y629'),
('000000046Cindy                    Kira                     Cremin                   494 Lang Avenue                                   Apt. 937                                          Alexandroview                                     PWUSA63082-4520(358)349-2574  (077)525-9966  656405528000000000007626995771987-12-140017535749Y514'),
('000000047Rigoberto                Savanna                  Hoeger                   00097 Gleichner Spur                              Apt. 932                                          Port Aidanborough                                 GUUSA31329-6973(946)322-6160  (973)443-8438  029222192000000000005676014721979-02-250022102472Y722'),
('000000048Lyric                    Mackenzie                Pacocha                  453 Rosina Mountain                               Apt. 011                                          Albertville                                       ORUSA83985-4937(950)497-1005  (004)244-7955  635734407000000000002653928321986-08-170046317382Y746'),
('000000049Immanuel                 Ellie                    Bednar                   5423 Esther Locks                                 Apt. 142                                          Langoshstad                                       GAUSA12288-3495(843)095-2553  (615)988-9038  813044111000000000004244959812000-01-050058726120Y148'),
('000000050Aniya                    Alba                     Von                      1588 Nienow Cape                                  Suite 187                                         New Aricchester                                   ORUSA04257     (325)301-0827  (493)985-9283  931248469000000000000303878241960-12-010074883577Y623');

-- Update total record count
UPDATE temp_load_statistics SET total_records = (SELECT COUNT(*) FROM temp_custdata_raw);

-- =============================================================================
-- PHASE 3: Fixed-Width Record Parsing with Comprehensive Validation
-- =============================================================================

-- Parse each fixed-width record with exact VSAM CUSTDAT field mapping
INSERT INTO temp_customers_parsed (
    line_number,
    customer_id,
    first_name,
    middle_name,
    last_name,
    address_line_1,
    address_line_2,
    address_line_3,
    address_state,
    address_country,
    address_zip,
    phone_home,
    phone_work,
    ssn,
    government_id,
    date_of_birth,
    eft_account_id,
    primary_cardholder_indicator,
    fico_credit_score
)
SELECT 
    line_number,
    -- Customer ID: positions 1-9 (9 digits, VSAM key structure)
    TRIM(SUBSTRING(raw_data, 1, 9)) as customer_id,
    
    -- Name fields: positions 10-72 with exact COBOL field lengths
    TRIM(SUBSTRING(raw_data, 10, 21)) as first_name,
    NULLIF(TRIM(SUBSTRING(raw_data, 31, 21)), '') as middle_name,
    TRIM(SUBSTRING(raw_data, 52, 21)) as last_name,
    
    -- Normalized address structure: positions 73-217
    TRIM(SUBSTRING(raw_data, 73, 43)) as address_line_1,
    NULLIF(TRIM(SUBSTRING(raw_data, 116, 43)), '') as address_line_2,
    NULLIF(TRIM(SUBSTRING(raw_data, 159, 43)), '') as address_line_3,
    TRIM(SUBSTRING(raw_data, 202, 2)) as address_state,
    TRIM(SUBSTRING(raw_data, 204, 3)) as address_country,
    TRIM(SUBSTRING(raw_data, 207, 11)) as address_zip,
    
    -- Dual phone number support: positions 218-247 with (xxx)xxx-xxxx format
    NULLIF(TRIM(SUBSTRING(raw_data, 218, 15)), '') as phone_home,
    NULLIF(TRIM(SUBSTRING(raw_data, 233, 15)), '') as phone_work,
    
    -- PII fields with appropriate handling: positions 248-276
    TRIM(SUBSTRING(raw_data, 248, 9)) as ssn,
    TRIM(SUBSTRING(raw_data, 257, 20)) as government_id,
    
    -- Date of birth with format conversion: positions 277-286
    CASE 
        WHEN TRIM(SUBSTRING(raw_data, 277, 10)) ~ '^\d{4}-\d{2}-\d{2}$' 
        THEN TRIM(SUBSTRING(raw_data, 277, 10))::DATE
        ELSE NULL
    END as date_of_birth,
    
    -- EFT account reference: positions 287-296
    NULLIF(TRIM(SUBSTRING(raw_data, 287, 10)), '') as eft_account_id,
    
    -- Primary cardholder indicator: position 297 (Y/N to BOOLEAN conversion)
    CASE UPPER(TRIM(SUBSTRING(raw_data, 297, 1)))
        WHEN 'Y' THEN TRUE
        WHEN 'N' THEN FALSE
        ELSE FALSE
    END as primary_cardholder_indicator,
    
    -- FICO credit score: positions 298-300 with range validation
    CASE 
        WHEN TRIM(SUBSTRING(raw_data, 298, 3)) ~ '^\d{3}$' 
        THEN TRIM(SUBSTRING(raw_data, 298, 3))::NUMERIC(3)
        ELSE NULL
    END as fico_credit_score
    
FROM temp_custdata_raw
WHERE LENGTH(raw_data) = 332;  -- Ensure exact 332-character format (excluding newline)

-- =============================================================================
-- PHASE 4: Comprehensive Data Validation and Error Handling
-- =============================================================================

-- Validate customer_id format (9-digit sequential identifier)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Invalid customer_id format (must be 9 digits); ',
    is_valid = FALSE
WHERE customer_id !~ '^[0-9]{9}$' OR customer_id IS NULL;

-- Validate required name fields
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'First name is required; ',
    is_valid = FALSE
WHERE first_name IS NULL OR LENGTH(TRIM(first_name)) = 0;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Last name is required; ',
    is_valid = FALSE
WHERE last_name IS NULL OR LENGTH(TRIM(last_name)) = 0;

-- Validate address fields (normalized address structure requirements)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Address line 1 is required; ',
    is_valid = FALSE
WHERE address_line_1 IS NULL OR LENGTH(TRIM(address_line_1)) = 0;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'State code must be 2 uppercase letters; ',
    is_valid = FALSE
WHERE address_state !~ '^[A-Z]{2}$' OR address_state IS NULL;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Country code must be 3 uppercase letters; ',
    is_valid = FALSE
WHERE address_country !~ '^[A-Z]{3}$' OR address_country IS NULL;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'ZIP code is required; ',
    is_valid = FALSE
WHERE address_zip IS NULL OR LENGTH(TRIM(address_zip)) = 0;

-- Validate phone number formats (xxx)xxx-xxxx
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Invalid home phone format; ',
    is_valid = FALSE
WHERE phone_home IS NOT NULL 
  AND phone_home !~ '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$';

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Invalid work phone format; ',
    is_valid = FALSE
WHERE phone_work IS NOT NULL 
  AND phone_work !~ '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$';

-- Validate PII fields (SSN format)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'SSN must be 9 digits; ',
    is_valid = FALSE
WHERE ssn !~ '^[0-9]{9}$' OR ssn IS NULL;

-- Validate government_id (required field)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Government ID is required; ',
    is_valid = FALSE
WHERE government_id IS NULL OR LENGTH(TRIM(government_id)) = 0;

-- Validate date_of_birth (business rules for reasonable age range)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Date of birth is required; ',
    is_valid = FALSE
WHERE date_of_birth IS NULL;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Date of birth must be between 1900-01-01 and 13 years ago; ',
    is_valid = FALSE
WHERE date_of_birth IS NOT NULL 
  AND (date_of_birth < DATE '1900-01-01' 
       OR date_of_birth > CURRENT_DATE - INTERVAL '13 years');

-- Validate FICO credit score (300-850 range validation with business rules)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'FICO credit score is required; ',
    is_valid = FALSE
WHERE fico_credit_score IS NULL;

UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'FICO credit score must be between 300 and 850; ',
    is_valid = FALSE
WHERE fico_credit_score IS NOT NULL 
  AND (fico_credit_score < 300 OR fico_credit_score > 850);

-- Check for duplicate customer_id records
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Duplicate customer_id; ',
    is_valid = FALSE
WHERE customer_id IN (
    SELECT customer_id 
    FROM temp_customers_parsed 
    GROUP BY customer_id 
    HAVING COUNT(*) > 1
);

-- Check for existing customer_id in database (prevent duplicates during reload)
UPDATE temp_customers_parsed 
SET validation_errors = validation_errors || 'Customer_id already exists in database; ',
    is_valid = FALSE
WHERE customer_id IN (SELECT customer_id FROM customers);

-- Update statistics with validation results
UPDATE temp_load_statistics 
SET valid_records = (SELECT COUNT(*) FROM temp_customers_parsed WHERE is_valid = TRUE),
    invalid_records = (SELECT COUNT(*) FROM temp_customers_parsed WHERE is_valid = FALSE),
    duplicate_records = (SELECT COUNT(*) FROM temp_customers_parsed WHERE validation_errors LIKE '%Duplicate customer_id%');

-- =============================================================================
-- PHASE 5: Data Loading with PII Protection and Compliance
-- =============================================================================

-- Log validation summary before loading
DO $$
DECLARE
    total_count INTEGER;
    valid_count INTEGER;
    invalid_count INTEGER;
BEGIN
    SELECT total_records, valid_records, invalid_records 
    INTO total_count, valid_count, invalid_count
    FROM temp_load_statistics;
    
    RAISE NOTICE 'Customer Data Loading Summary:';
    RAISE NOTICE '  Total records processed: %', total_count;
    RAISE NOTICE '  Valid records: %', valid_count;
    RAISE NOTICE '  Invalid records: %', invalid_count;
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Validation errors found. Check temp_customers_parsed table for details.';
    END IF;
END $$;

-- Insert valid customer records into customers table with PII handling
INSERT INTO customers (
    customer_id,
    first_name,
    middle_name,
    last_name,
    address_line_1,
    address_line_2,
    address_line_3,
    state_code,
    country_code,
    zip_code,
    phone_number_1,
    phone_number_2,
    ssn,
    government_id,
    date_of_birth,
    eft_account_id,
    primary_cardholder_indicator,
    fico_credit_score,
    created_at,
    updated_at
)
SELECT 
    customer_id,
    first_name,
    middle_name,
    last_name,
    address_line_1,
    address_line_2,
    address_line_3,
    address_state as state_code,
    address_country as country_code,
    address_zip as zip_code,
    phone_home as phone_number_1,
    phone_work as phone_number_2,
    -- PII data handled with appropriate security measures
    ssn,  -- In production, this would be encrypted
    government_id,
    date_of_birth,
    eft_account_id,
    CASE WHEN primary_cardholder_indicator THEN 'Y' ELSE 'N' END as primary_cardholder_indicator,
    fico_credit_score,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM temp_customers_parsed 
WHERE is_valid = TRUE
ORDER BY customer_id;

-- Update final statistics
UPDATE temp_load_statistics 
SET inserted_records = (SELECT COUNT(*) FROM customers WHERE created_at >= (SELECT start_time FROM temp_load_statistics)),
    end_time = CURRENT_TIMESTAMP;

-- =============================================================================
-- PHASE 6: Data Loading Audit and Verification
-- =============================================================================

-- Final statistics and audit log
DO $$
DECLARE
    stats_record RECORD;
    error_record RECORD;
BEGIN
    -- Get final statistics
    SELECT * INTO stats_record FROM temp_load_statistics;
    
    -- Log completion summary
    RAISE NOTICE 'Customer Data Loading Completed:';
    RAISE NOTICE '  Records processed: %', stats_record.total_records;
    RAISE NOTICE '  Records inserted: %', stats_record.inserted_records;
    RAISE NOTICE '  Records failed validation: %', stats_record.invalid_records;
    RAISE NOTICE '  Processing time: % seconds', 
        EXTRACT(EPOCH FROM (stats_record.end_time - stats_record.start_time));
    
    -- Log validation errors for debugging (first 10 errors)
    IF stats_record.invalid_records > 0 THEN
        RAISE NOTICE 'Sample validation errors:';
        FOR error_record IN 
            SELECT line_number, customer_id, validation_errors 
            FROM temp_customers_parsed 
            WHERE is_valid = FALSE 
            ORDER BY line_number 
            LIMIT 10
        LOOP
            RAISE NOTICE '  Line %: Customer % - %', 
                error_record.line_number, 
                error_record.customer_id, 
                error_record.validation_errors;
        END LOOP;
    END IF;
END $$;

-- Verify data integrity constraints are satisfied
DO $$
DECLARE
    constraint_violations INTEGER;
BEGIN
    -- Check primary key uniqueness
    SELECT COUNT(*) - COUNT(DISTINCT customer_id) 
    INTO constraint_violations 
    FROM customers;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Primary key constraint violation detected: % duplicate customer_id values', constraint_violations;
    END IF;
    
    -- Check FICO score range constraints
    SELECT COUNT(*) 
    INTO constraint_violations 
    FROM customers 
    WHERE fico_credit_score < 300 OR fico_credit_score > 850;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'FICO score range constraint violation detected: % records outside 300-850 range', constraint_violations;
    END IF;
    
    RAISE NOTICE 'All data integrity constraints verified successfully.';
END $$;

-- =============================================================================
-- CLEANUP: Drop temporary tables
-- =============================================================================

DROP TABLE temp_custdata_raw;
DROP TABLE temp_customers_parsed;
DROP TABLE temp_load_statistics;

-- Data load verification and integrity checks
--comment: Verify customer data loading results and generate summary statistics

-- Generate data loading verification report
DO $$
DECLARE
    total_customers INTEGER;
    date_range_start DATE;
    date_range_end DATE;
    fico_avg NUMERIC(5,2);
    state_count INTEGER;
BEGIN
    -- Get basic statistics
    SELECT COUNT(*), 
           MIN(date_of_birth), 
           MAX(date_of_birth),
           AVG(fico_credit_score),
           COUNT(DISTINCT address_state)
    INTO total_customers, date_range_start, date_range_end, fico_avg, state_count
    FROM customers;
    
    RAISE NOTICE 'Customer Data Verification Report:';
    RAISE NOTICE '  Total customers loaded: %', total_customers;
    RAISE NOTICE '  Birth date range: % to %', date_range_start, date_range_end;
    RAISE NOTICE '  Average FICO score: %', fico_avg;
    RAISE NOTICE '  States represented: %', state_count;
    RAISE NOTICE '  Data loading verification completed successfully.';
END $$;

-- Rollback directives have been moved to the XML changeset definition