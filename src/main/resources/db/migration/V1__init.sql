CREATE TABLE customers (
    id UUID PRIMARY KEY,
    card_number VARCHAR(32) NOT NULL UNIQUE,
    pin_hash VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,
    balance NUMERIC(19,2) NOT NULL,
    daily_limit NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_customers_card_number ON customers(card_number);
CREATE INDEX idx_transactions_account_date ON transactions(account_id, occurred_at);
