package com.ggteam.cs.sim;

/** 시뮬레이션 시나리오. weight 합 = 100. */
public enum SimScenario {
    PAID_NOT_DELIVERED(25),
    DUPLICATE_CHARGE(15),
    PAYMENT_FAILED(12),
    PARTIAL_DELIVERY(10),
    REFUND_PENDING(10),
    POINT_NOT_CHARGED(8),
    WRONG_ITEM(5),
    ACCOUNT_ISSUE(10),
    ETC(5);

    private final int weight;

    SimScenario(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
