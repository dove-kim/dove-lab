package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 모듈 표시 설정 저장소.
 */
public interface MemberModuleDisplayRepository extends JpaRepository<MemberModuleDisplay, Long> {

    Optional<MemberModuleDisplay> findByMemberIdAndModuleCode(Long memberId, ModuleCode moduleCode);

    List<MemberModuleDisplay> findAllByMemberId(Long memberId);
}
