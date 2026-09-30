package com.lmt.fyp.flowerplus.module.product.entity;

import com.lmt.fyp.flowerplus.common.entity.AuditableEntity;
import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Version
    @Column(nullable = false)
    private Long version;

    public Category(String name) {
        this.name = StringNormalizer.strip(name);
    }

    public void rename(String name) {
        this.name = StringNormalizer.strip(name);
    }
}
