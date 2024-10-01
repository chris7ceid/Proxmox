package org.ceid_uni.config;
import org.ceid_uni.models.ERole;
import org.ceid_uni.models.Role;
import org.ceid_uni.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class Setup implements ApplicationListener<ContextRefreshedEvent>{

    private static final Logger logger = LoggerFactory.getLogger(Setup.class);

    private RoleRepository roleRepository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        List<Role> roles = roleRepository.findAll();
        if (roles.isEmpty()){
            List<Role> iRoles = Arrays.asList(
                    new Role(ERole.ROLE_USER),
                    new Role(ERole.ROLE_ADMIN),
                    new Role(ERole.ROLE_MODERATOR)
            );
            roleRepository.saveAll(iRoles);
        }
        logger.info("Init tasks completed successfully.");
    }
    @Autowired
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
}
