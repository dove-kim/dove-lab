package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberSubMenuGrant;
import com.dove.userfeature.domain.enums.SubMenuCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 하위 메뉴 부여 기록 저장소.
 */
public interface MemberSubMenuGrantRepository extends JpaRepository<MemberSubMenuGrant, Long> {

    Optional<MemberSubMenuGrant> findByMemberIdAndSubMenuCode(Long memberId, SubMenuCode subMenuCode);

    List<MemberSubMenuGrant> findAllByMemberIdAndActiveTrue(Long memberId);
}
