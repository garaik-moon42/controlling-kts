CREATE TABLE BANK_TRANSACTIONS
(
    ID                     INT PRIMARY KEY,
    ACCOUNT_NUMBER         VARCHAR(26)    NOT NULL,
    ACCOUNT_NAME           VARCHAR(64)    NOT NULL,
    ENTRY_DATE             DATETIME       NOT NULL, -- Note: ZonedDateTime's zone details will be lost.
    VALUE_DATE             DATE           NOT NULL,
    MONTH                  DATE           NOT NULL,
    COUNTER_ACCOUNT_NUMBER VARCHAR(34),             -- 34 characters are the upper limit of IBAN bank account numbers.
    PARTNER                VARCHAR(100),
    AMOUNT                 DECIMAL(20, 2) NOT NULL, -- You can adjust precision (20) and scale (2) as needed.
    CURRENCY               VARCHAR(3)     NOT NULL,
    CTRL_CATEGORY          VARCHAR(100),
    CTRL_MONTH             DATE,
    CTRL_INCLUDE           TINYINT(1),
    CTRL_VAT               TINYINT(1),
    CTRL_AMOUNT            DECIMAL(20, 2),
    NOTICE                 VARCHAR(255),
    TRANSACTION_BANK_ID    VARCHAR(48),
    TRANSACTION_TYPE_CODE  VARCHAR(5),
    TRANSACTION_TYPE_NAME  VARCHAR(50),
    CTRL_INVOICE_URL       VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

