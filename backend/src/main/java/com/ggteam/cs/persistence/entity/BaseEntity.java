package com.ggteam.cs.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * 전 엔티티 공통 식별자 기반 클래스.
 *
 * <p>모든 엔티티가 공유하는 유일한 속성은 UUID 기본키이다. {@code createdAt}/{@code timestamp}/
 * {@code lastLogin} 등 시각 컬럼은 엔티티마다 의미·컬럼명이 달라 각 엔티티에서 개별 정의한다
 * (domain-entities.md §2~3). 모든 시각은 KST로 저장한다 (BR-41).
 *
 * <p>담당: 백엔드 A (persistence 단독 소유).
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
