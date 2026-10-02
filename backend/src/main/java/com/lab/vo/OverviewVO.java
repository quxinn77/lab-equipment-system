package com.lab.vo;

import lombok.Data;

@Data
public class OverviewVO {

    private long deviceTotal;
    private long idleCount;
    private long borrowedCount;
    private long repairingCount;
    private long scrappedCount;
    private long reservedCount;
    private long monthBorrowCount;
    private long pendingBorrowCount;
    private long overdueCount;
    private long pendingRepairCount;
}
