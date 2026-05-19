package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.UserSubMenuGrant;
import com.dove.userfeature.domain.enums.SubMenuCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSubMenuGrantRepository extends JpaRepository<UserSubMenuGrant, Long> {

    Optional<UserSubMenuGrant> findByUserIdAndSubMenuCode(Long userId, SubMenuCode subMenuCode);

    List<UserSubMenuGrant> findAllByUserIdAndActiveTrue(Long userId);
}
