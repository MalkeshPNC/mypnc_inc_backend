-- Commission % replaces the unused Sales % on the quote header: it is the rate
-- applied to every quantity line's sub total to reach Total (S%).
ALTER TABLE tblquotes
    ADD COLUMN commission_percentage DECIMAL(6, 2) NULL AFTER quote_array,
    DROP COLUMN sales_percentage;

-- service_charge feeds the sub total, so it has to be money rather than text.
ALTER TABLE tblquote_quantities
    MODIFY COLUMN service_charge DECIMAL(12, 2) NULL;
