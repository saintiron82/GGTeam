package com.ggteam.cs.common.enums;

/** 데모 더미 테이블 상태 enum 모음. 운영 시스템 연동 시 대체된다. */
public final class DemoEnums {

    private DemoEnums() {}

    public enum PaymentStatus { SUCCESS, FAILED, PENDING, REFUNDED }

    public enum DeliveryStatus { DELIVERED, NOT_DELIVERED, PARTIAL, PENDING }

    public enum AccountStatus { ACTIVE, SUSPENDED, DORMANT, BANNED }
}
