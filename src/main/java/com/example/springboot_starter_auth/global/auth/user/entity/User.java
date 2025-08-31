package com.example.springboot_starter_auth.global.auth.user.entity;

import com.example.springboot_starter_auth.global.config.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
//* 1. JPA를 위해 기본 생성자는 필요하지만, 외부에서 함부로 쓰지 못하게 protected로 제한합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    @Column(nullable = false)
    private String nickname; // 카카오 실명

    @Column(length = 500)
    private String profileImageUrl;
    private String email;

}
