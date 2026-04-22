package com.ssafya408.cutlineparsing.dao;

import com.ssafya408.cutlineparsing.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보 조회를 위한 Repository
 * 
 * @author AI Assistant
 * @version 1.1
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 조회
     * 
     * @param email 사용자 이메일
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByEmail(String email);
}
