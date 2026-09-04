-- tblcustomer_salespersons predates Flyway, so V2's foreign keys were never applied and
-- the customer side kept the default NO ACTION rule. Assignment rows carry no meaning
-- without their customer, so restore the ON DELETE CASCADE V2 asked for.
ALTER TABLE tblcustomer_salespersons
    DROP FOREIGN KEY csp_cust_id;

ALTER TABLE tblcustomer_salespersons
    ADD CONSTRAINT fk_csp_customer FOREIGN KEY (cust_id)
        REFERENCES tblcustomers (cust_id) ON DELETE CASCADE;
