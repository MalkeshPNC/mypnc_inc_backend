package com.pnc.masters.ncmaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblpcba_notes")
public class PcbaNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pcba_note_id")
    private Long pcbaNoteId;

    @Column(name = "pcba_part_number", nullable = false, unique = true, length = 120)
    private String pcbaPartNumber;

    @Column(name = "pcba_notes", columnDefinition = "TEXT")
    private String pcbaNotes;

    public Long getPcbaNoteId() { return pcbaNoteId; }
    public void setPcbaNoteId(Long pcbaNoteId) { this.pcbaNoteId = pcbaNoteId; }
    public String getPcbaPartNumber() { return pcbaPartNumber; }
    public void setPcbaPartNumber(String pcbaPartNumber) { this.pcbaPartNumber = pcbaPartNumber; }
    public String getPcbaNotes() { return pcbaNotes; }
    public void setPcbaNotes(String pcbaNotes) { this.pcbaNotes = pcbaNotes; }
}
