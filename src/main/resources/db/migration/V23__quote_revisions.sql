ALTER TABLE tblquotes
    ADD COLUMN pcba_revision VARCHAR(40) NULL AFTER assy_number,
    ADD COLUMN pcb_revision VARCHAR(40) NULL AFTER pcb_number;
